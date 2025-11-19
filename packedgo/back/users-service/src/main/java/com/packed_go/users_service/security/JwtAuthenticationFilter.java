package com.packed_go.users_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtro JWT para users-service
 * Valida tokens generados por auth-service y establece la autenticación
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator tokenValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getTokenFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenValidator.validateToken(jwt)) {
                String username = tokenValidator.getUsernameFromToken(jwt);
                String role = tokenValidator.getRoleFromToken(jwt);
                List<String> authorities = tokenValidator.getAuthoritiesFromToken(jwt);

                // Create authorities list
                List<SimpleGrantedAuthority> grantedAuthorities;
                if (authorities != null && !authorities.isEmpty()) {
                    grantedAuthorities = authorities.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                } else {
                    // If no authorities, use role
                    grantedAuthorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
                }

                // Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, grantedAuthorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Set authentication for user: {} with role: {}", username, role);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
