package com.localconnect.backend.security;

import com.localconnect.backend.entity.User;
import com.localconnect.backend.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                String email = jwtService.extractEmail(token);

                if (email != null &&
                    jwtService.isTokenValid(token, email)) {

                    User user = userRepository.findByEmail(email)
                            .orElse(null);

                    if (user != null &&
                        Boolean.TRUE.equals(user.getEnabled())) {

                        var authority = new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        );

                        var authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user.getEmail(),
                                        null,
                                        List.of(authority)
                                );

                        authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                        );

                        SecurityContext context =
                                SecurityContextHolder.createEmptyContext();

                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);
                    }
                }

            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}