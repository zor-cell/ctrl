package at.ac.tuwien.inso.ctrl.config.security;

import at.ac.tuwien.inso.ctrl.user.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final CustomUserDetailsService userDetailsService;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path != null && path.startsWith("/ws");
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.debug(
          "JWT auth skipped for {} {}: missing bearer token",
          request.getMethod(),
          request.getRequestURI());
      filterChain.doFilter(request, response);
      return;
    }

    String jwt = authHeader.substring("Bearer ".length());
    String username;
    try {
      username = jwtService.extractUsername(jwt);
    } catch (Exception e) {
      log.debug(
          "JWT auth failed for {} {}: token could not be parsed ({})",
          request.getMethod(),
          request.getRequestURI(),
          e.getClass().getSimpleName());
      filterChain.doFilter(request, response);
      return;
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails;
      try {
        userDetails = this.userDetailsService.loadUserByUsername(username);
      } catch (UsernameNotFoundException e) {
        log.debug(
            "JWT auth failed for {} {}: user '{}' from token was not found",
            request.getMethod(),
            request.getRequestURI(),
            username);
        filterChain.doFilter(request, response);
        return;
      }

      if (jwtService.isTokenValid(jwt, userDetails)) {
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.debug(
            "JWT auth accepted for {} {} as '{}'",
            request.getMethod(),
            request.getRequestURI(),
            username);
      } else {
        log.debug(
            "JWT auth failed for {} {}: token for '{}' is invalid or expired",
            request.getMethod(),
            request.getRequestURI(),
            username);
      }
    }
    filterChain.doFilter(request, response);
  }
}
