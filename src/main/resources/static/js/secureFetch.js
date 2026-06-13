/**
 * secureFetch — CSRF-aware fetch wrapper for Spindle ERP
 *
 * Usage (same API as native fetch):
 *   const res = await secureFetch('/api/v1/items', { method: 'POST', body: JSON.stringify(data) });
 *   const json = await res.json();
 *
 * What it does:
 *   • Reads the XSRF-TOKEN cookie (set by Spring Security CookieCsrfTokenRepository)
 *   • Injects it as the X-XSRF-TOKEN header on every mutating request (POST/PUT/PATCH/DELETE)
 *   • Sets Content-Type: application/json if body is present and no Content-Type set
 *   • On 401 → shows session-expired toast and redirects to /login
 *   • On 403 → shows access-denied toast
 */

'use strict';

(function (global) {

    // Read a cookie value by name
    function getCookie(name) {
        const match = document.cookie.match(
            new RegExp('(?:^|;\\s*)' + name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '=([^;]*)')
        );
        return match ? decodeURIComponent(match[1]) : null;
    }

    // Methods that require CSRF token
    const CSRF_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

    async function secureFetch(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();

        // Build headers
        const headers = new Headers(options.headers || {});

        // Inject CSRF token for state-changing requests
        if (CSRF_METHODS.has(method)) {
            const csrfToken = getCookie('XSRF-TOKEN');
            if (csrfToken) {
                headers.set('X-XSRF-TOKEN', csrfToken);
            }
        }

        // Default Content-Type to JSON if body is present
        if (options.body && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json;charset=UTF-8');
        }

        // Always expect JSON back from our API
        if (!headers.has('Accept')) {
            headers.set('Accept', 'application/json');
        }

        // Mark as AJAX so CustomAccessDeniedHandler / AuthenticationEntryPoint
        // returns JSON instead of HTML redirects
        headers.set('X-Requested-With', 'XMLHttpRequest');

        let response;
        try {
            response = await fetch(url, { ...options, headers });
        } catch (networkError) {
            showToast('Network error. Please check your connection.', 'danger');
            throw networkError;
        }

        // Handle auth errors globally
        if (response.status === 401) {
            showToast('Session expired. Redirecting to login…', 'warning');
            setTimeout(() => { window.location.href = '/login?sessionExpired'; }, 1500);
            return response;
        }

        if (response.status === 403) {
            showToast('Access denied. You do not have permission for this action.', 'danger');
            return response;
        }

        return response;
    }

    // ── Toast helper (SweetAlert2 toast or simple fallback) ───────────────

    function showToast(message, type = 'info') {
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                toast: true,
                position: 'top-end',
                icon: type === 'danger' ? 'error' : (type === 'warning' ? 'warning' : 'info'),
                title: message,
                showConfirmButton: false,
                timer: 3000,
                timerProgressBar: true
            });
        } else {
            console.warn('[secureFetch]', type.toUpperCase(), message);
        }
    }

    // Expose globally
    global.secureFetch = secureFetch;

})(window);
