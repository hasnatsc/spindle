// Path: com/asg/spindleserp/ecommerce/storefront/security/StorefrontAuthException.java
package com.asg.spindleserp.ecommerce.storefront.security;

import lombok.Getter;

/**
 * StorefrontAuthException — every failure the customer auth surface is allowed
 * to describe to the customer.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * WHY A TYPED EXCEPTION INSTEAD OF RAW IllegalArgumentException
 * ══════════════════════════════════════════════════════════════════════════
 * The controllers all did this:
 *
 *     } catch (Exception e) {
 *         res.put("success", false);
 *         res.put("message", e.getMessage());     // ← straight to the browser
 *     }
 *
 * Catching Exception and echoing getMessage() to the client means ANY
 * unexpected failure inside the service — a Postgres constraint violation, a
 * LazyInitializationException, a NullPointerException, a JDBC connection
 * error — has its internal message rendered in the customer's browser.
 * Postgres constraint messages in particular happily name the table, the
 * column and the constraint. That is free reconnaissance for an attacker and
 * an unprofessional experience for a real shopper.
 *
 * Extending IllegalArgumentException keeps every existing `catch
 * (IllegalArgumentException)` and `catch (Exception)` in the codebase working
 * exactly as before, so this is a drop-in change — while the rewritten
 * controller now distinguishes "an exception I authored, whose message is safe
 * to show" from "something unexpected blew up, log it and show a generic
 * message".
 * ══════════════════════════════════════════════════════════════════════════
 */
public class StorefrontAuthException extends IllegalArgumentException {

    public StorefrontAuthException(String safeMessage) {
        super(safeMessage);
    }

    /**
     * Thrown when LoginAttemptService has locked this identifier or IP.
     * Carries the remaining lock time so the UI can show a countdown and so the
     * response can set a Retry-After header.
     */
    @Getter
    public static class TooManyAttempts extends StorefrontAuthException {

        private final long retryAfterSeconds;

        public TooManyAttempts(long retryAfterSeconds) {
            super(buildMessage(retryAfterSeconds));
            this.retryAfterSeconds = retryAfterSeconds;
        }

        private static String buildMessage(long seconds) {
            long minutes = Math.max(1, (seconds + 59) / 60);
            return "Too many failed attempts. Please try again in "
                    + minutes + (minutes == 1 ? " minute." : " minutes.");
        }
    }
}
