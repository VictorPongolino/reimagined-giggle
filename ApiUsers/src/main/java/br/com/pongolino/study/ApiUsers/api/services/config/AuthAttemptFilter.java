package br.com.pongolino.study.ApiUsers.api.services.config;

import br.com.pongolino.study.ApiUsers.api.data.UserAuthentication;
import br.com.pongolino.study.ApiUsers.api.services.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

public class AuthAttemptFilter extends UsernamePasswordAuthenticationFilter {

    private final UserService userService;
    private final Environment environment;
    private final Long JWT_EXPIRATION_TIME;
    private final SecretKey SECRET_KEY;

    public AuthAttemptFilter(AuthenticationManager authenticationManager, UserService userService, Environment environment) {
        super(authenticationManager);
        this.userService = userService;
        this.environment = environment;
        JWT_EXPIRATION_TIME = Long.parseLong(Objects.requireNonNull(environment.getProperty("jwt.expiration_ms")));
        final String SECRET_KEY = Objects.requireNonNull(environment.getProperty("jwt.token"));
        this.SECRET_KEY = new SecretKeySpec(Base64.getEncoder().encode(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256.getJcaName());
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;

        String username = obtainUsername(servletRequest);
        String password = obtainPassword(servletRequest);

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
        setDetails(request, token);
        return getAuthenticationManager().authenticate(token);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        UserAuthentication user = (UserAuthentication)authResult.getPrincipal();

        Instant now = Instant.now();
        String token = Jwts.builder()
                .setExpiration(Date.from(now.plusMillis(JWT_EXPIRATION_TIME)))
                .setIssuedAt(Date.from(now))
                .setSubject(user.getUser().getId().toString())
                .signWith(SECRET_KEY)
                .compact();

        response.addHeader("token", token);
    }
}
