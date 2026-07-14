package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.security.config.SpindleSecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LoginAttemptService — brute-force / credential-stuffing throttle.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * THE GAP THIS CLOSES
 * ══════════════════════════════════════════════════════════════════════════
 * Before this class, NEITHER login surface had any rate limit whatsoever:
 *
 *   POST /login          → DaoAuthenticationProvider → BCrypt(12) compare.
 *                          Wrong password? /login?error. Try again. Forever.
 *   POST /account/login  → StorefrontAuthService.login() → BCrypt(12) compare.
 *                          Wrong password? "Invalid phone/email or password."
 *                          Try again. Forever.
 *
 * sec_users.account_non_locked existed as a column and was checked at login —
 * but NOTHING IN THE ENTIRE CODEBASE EVER SET IT TO FALSE. It could only ever
 * be flipped by an admin editing a user by hand. So the lock existed on paper
 * and never once fired in practice.
 *
 * BCrypt(12) is ~250ms per attempt, which people often mistake for "that IS
 * the rate limit". It is not. It is a rate limit on the ATTACKER's CPU, not on
 * yours, and it is trivially parallelised: 200 concurrent connections × 4
 * guesses/sec/connection = 800 guesses/sec against a 4-digit PIN-style
 * password. Meanwhile each of those 800 requests is also burning one of your
 * 20 Hikari connections and 250ms of YOUR CPU — so an unthrottled BCrypt login
 * endpoint is simultaneously a password oracle AND a self-inflicted DoS
 * amplifier. Rate limiting has to happen BEFORE the hash comparison, which is
 * exactly where this service sits.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * DESIGN
 * ══════════════════════════════════════════════════════════════════════════
 * Two independent counters per login surface:
 *
 *   • per-IDENTIFIER  — stops a targeted attack on one account.
 *   • per-IP          — stops password SPRAYING (one common password tried
 *                       against hundreds of different accounts), which the
 *                       per-identifier counter alone cannot see, because each
 *                       individual account only ever gets 1–2 failures.
 *
 * Rolling window (default 15 min) → N failures → lock for M minutes.
 * A success clears the identifier counter immediately (but NOT the IP counter —
 * an attacker who lands one valid credential among a spray must not thereby
 * reset the spray detector).
 *
 * ── Storage: in-memory, ConcurrentHashMap ────────────────────────────────
 * Deliberate. The alternatives were:
 *
 *   (a) DB columns on sec_users (failed_attempts, locked_until)
 *       → requires a schema change to User.java. This project has NO Flyway
 *         dependency in pom.xml — schema is managed by
 *         spring.jpa.hibernate.ddl-auto=update. Adding columns to the User
 *         entity is exactly the kind of change that has previously broken
 *         Spring Session serialisation in this codebase. Not worth it for a
 *         counter that is meant to be ephemeral anyway.
 *   (b) Redis / Bucket4j / Resilience4j
 *       → new infrastructure + new dependencies for a single-node app.
 *
 * ★ KNOWN LIMITATION — STATED, NOT HIDDEN:
 *   Counters are per-JVM. If Spindle is ever scaled to more than one app
 *   instance behind a load balancer, an attacker gets N × maxAttempts before
 *   being locked (N = instance count), and the lock does not follow them
 *   across instances. It also resets on restart. For the current single-node
 *   deployment this is correct and sufficient. When you scale out, swap the
 *   ConcurrentHashMap for Redis — the public API of this class does not change,
 *   only the four private map operations do.
 *
 * ── Memory safety ────────────────────────────────────────────────────────
 * The key space is attacker-controlled (any string can be submitted as a
 * username), so an unbounded map here would itself be a memory-exhaustion DoS.
 * Guarded by MAX_KEYS + opportunistic eviction of expired entries on write.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    /** Hard ceiling on tracked keys — the key space is attacker-controlled. */
    private static final int MAX_KEYS = 50_000;

    private final SpindleSecurityProperties props;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    /** Which login surface a call refers to — selects the right thresholds. */
    public enum Surface { ERP, STOREFRONT }

    // ══════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ══════════════════════════════════════════════════════════════════════

    /**
     * @return seconds remaining on the lock, or 0 if this identifier+IP pair is
     *         free to attempt a login right now.
     */
    public long blockedSeconds(Surface surface, String identifier, String ip) {
        long now = System.currentTimeMillis();
        long byId = remaining(key(surface, "id", identifier), now);
        long byIp = remaining(key(surface, "ip", ip), now);
        return Math.max(byId, byIp);
    }

    public boolean isBlocked(Surface surface, String identifier, String ip) {
        return blockedSeconds(surface, identifier, ip) > 0;
    }

    /** Record a failed authentication. Locks the identifier and/or IP if over threshold. */
    public void loginFailed(Surface surface, String identifier, String ip) {
        int maxId  = (surface == Surface.ERP) ? props.getLogin().getMaxAttempts()
                                              : props.getStorefront().getMaxLoginAttempts();
        int maxIp  = (surface == Surface.ERP) ? props.getLogin().getMaxAttemptsPerIp()
                                              : props.getStorefront().getMaxLoginAttemptsPerIp();
        int window = (surface == Surface.ERP) ? props.getLogin().getWindowMinutes()
                                              : props.getStorefront().getWindowMinutes();
        int lock   = (surface == Surface.ERP) ? props.getLogin().getLockMinutes()
                                              : props.getStorefront().getLockMinutes();

        boolean idLocked = bump(key(surface, "id", identifier), maxId, window, lock);
        boolean ipLocked = bump(key(surface, "ip", ip),         maxIp, window, lock);

        if (idLocked) {
            log.warn("THROTTLE  surface={} identifier='{}' LOCKED for {} min after {} failures",
                    surface, WebSecurityUtils.sanitizeForLog(identifier), lock, maxId);
        }
        if (ipLocked) {
            log.warn("THROTTLE  surface={} ip='{}' LOCKED for {} min after {} failures " +
                     "(password-spraying pattern)",
                    surface, WebSecurityUtils.sanitizeForLog(ip), lock, maxIp);
        }
    }

    /**
     * Record a successful authentication — clears the IDENTIFIER counter only.
     *
     * The IP counter is deliberately NOT cleared: in a password-spraying attack
     * the attacker WILL eventually guess one account correctly, and clearing the
     * IP counter on that success would hand them a free reset of the spray
     * detector and let them carry straight on.
     */
    public void loginSucceeded(Surface surface, String identifier) {
        counters.remove(key(surface, "id", identifier));
    }

    /**
     * Registration flood control: is this IP over its hourly signup quota?
     * Separate counter namespace, separate (much longer) window.
     */
    public boolean registrationBlocked(String ip) {
        String k = key(Surface.STOREFRONT, "reg", ip);
        return remaining(k, System.currentTimeMillis()) > 0;
    }

    /** Count one registration from this IP; locks the IP for an hour once over quota. */
    public void registrationAttempted(String ip) {
        boolean locked = bump(key(Surface.STOREFRONT, "reg", ip),
                props.getStorefront().getMaxRegistrationsPerIpPerHour(), 60, 60);
        if (locked) {
            log.warn("THROTTLE  registration flood from ip='{}' — blocked for 60 min",
                    WebSecurityUtils.sanitizeForLog(ip));
        }
    }

    /** Admin/operational escape hatch — clear a lock immediately. */
    public void clear(Surface surface, String identifier, String ip) {
        counters.remove(key(surface, "id", identifier));
        counters.remove(key(surface, "ip", ip));
    }

    /** Diagnostics — exposed for the security dashboard if it ever wants it. */
    public int trackedKeys() {
        return counters.size();
    }

    // ══════════════════════════════════════════════════════════════════════
    // INTERNALS
    // ══════════════════════════════════════════════════════════════════════

    private static String key(Surface surface, String kind, String value) {
        String v = (value == null || value.isBlank()) ? "unknown" : value.trim().toLowerCase();
        if (v.length() > 160) v = v.substring(0, 160);   // bound the key length too
        return surface.name() + ':' + kind + ':' + v;
    }

    /** @return true if THIS failure is the one that tipped the key into a lock. */
    private boolean bump(String key, int maxAttempts, int windowMinutes, int lockMinutes) {
        long now = System.currentTimeMillis();
        evictIfNeeded(now);

        Counter c = counters.compute(key, (k, existing) -> {
            if (existing == null || existing.isStale(now, windowMinutes)) {
                return new Counter(now);
            }
            return existing;
        });

        int failures = c.failures.incrementAndGet();

        if (failures >= maxAttempts && c.blockedUntil < now) {
            c.blockedUntil = now + Duration.ofMinutes(lockMinutes).toMillis();
            return true;
        }
        return false;
    }

    private long remaining(String key, long now) {
        Counter c = counters.get(key);
        if (c == null) return 0L;
        long ms = c.blockedUntil - now;
        if (ms <= 0) return 0L;
        return (ms / 1000) + 1;
    }

    /**
     * Opportunistic eviction. Runs only when the map grows past the ceiling, so
     * the common path stays a single hash lookup. No @Scheduled — this project
     * does not enable scheduling, and adding @EnableScheduling to fix a cache
     * would be a heavier change than the problem warrants.
     */
    private void evictIfNeeded(long now) {
        if (counters.size() < MAX_KEYS) return;

        int removed = 0;
        Iterator<Map.Entry<String, Counter>> it = counters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Counter> e = it.next();
            Counter c = e.getValue();
            // Expired lock AND no recent activity → safe to drop.
            if (c.blockedUntil < now && c.isStale(now, 60)) {
                it.remove();
                removed++;
            }
        }

        // Still over the ceiling → the map is being deliberately flooded with
        // fresh keys. Drop everything rather than run out of heap; an attacker
        // gains a counter reset, but the process stays up. Log loudly.
        if (counters.size() >= MAX_KEYS) {
            log.error("THROTTLE  key table flooded ({} keys) — clearing. This is almost " +
                      "certainly a deliberate memory-exhaustion attempt against the login " +
                      "endpoint. Put a WAF / edge rate limit in front of the app.",
                      counters.size());
            counters.clear();
        } else if (removed > 0) {
            log.info("THROTTLE  evicted {} expired attempt counters ({} remain)", removed, counters.size());
        }
    }

    /** Mutable counter with a rolling window. */
    private static final class Counter {
        final AtomicInteger failures = new AtomicInteger(0);
        final long firstFailureAt;
        volatile long blockedUntil = 0L;

        Counter(long now) { this.firstFailureAt = now; }

        boolean isStale(long now, int windowMinutes) {
            return now - firstFailureAt > Duration.ofMinutes(windowMinutes).toMillis();
        }
    }
}
