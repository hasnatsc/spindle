package com.asg.spindleserp.security.auth;

import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles 403 Forbidden.
 * - AJAX / API calls (Accept: application/json) → returns JSON 403
 * - Browser requests → redirects to /access-denied page
 */
@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {

        String username = (request.getUserPrincipal() != null)
                ? request.getUserPrincipal().getName() : "anonymous";

        log.warn("ACCESS DENIED  user='{}' uri='{}' method='{}'",
                username, request.getRequestURI(), request.getMethod());

        if (isAjaxRequest(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    """
                    {"success":false,"message":"Access denied. You do not have permission for this action."}
                    """);
        } else {
            response.sendRedirect(request.getContextPath() + "/access-denied");
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String xhr    = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(xhr)
                || (accept != null && accept.contains("application/json"));
    }
}
