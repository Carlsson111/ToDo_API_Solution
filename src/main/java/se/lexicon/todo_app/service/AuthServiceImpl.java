package se.lexicon.todo_app.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import se.lexicon.todo_app.dto.AuthRequestDto;
import se.lexicon.todo_app.dto.AuthResponseDto;
import se.lexicon.todo_app.security.JwtTokenUtil;
import se.lexicon.todo_app.security.TokenBlacklistStorage;
import se.lexicon.todo_app.service.AuthService;

import java.util.Date;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenBlacklistStorage tokenBlacklistStorage;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtTokenUtil jwtTokenUtil,
                           TokenBlacklistStorage tokenBlacklistStorage) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenBlacklistStorage = tokenBlacklistStorage;
    }

    @Override
    public AuthResponseDto login(AuthRequestDto request) {
        System.out.println("Login attempt for user: " + request.username());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String jwt = jwtTokenUtil.generateToken(userDetails);

        String[] roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toArray(String[]::new);

        AuthResponseDto response = AuthResponseDto.builder()
                .token(jwt)
                .type("Bearer")
                .username(userDetails.getUsername())
                .roles(roles)
                .build();

        System.out.println("Login successful for user: " + request.username());
        return response;
    }


    @Override
    public void logout(String authHeader) {
        System.out.println("authHeader = " + authHeader);
        if (authHeader == null) {
            throw new IllegalArgumentException("Authorization header is missing");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token must be a Bearer token");
        }

        String token = authHeader.substring(7);

        try {
            // First check if token is already blacklisted
            if (tokenBlacklistStorage.isBlacklisted(token)) {
                throw new IllegalArgumentException("Token has already been invalidated");
            }

            Date expiryDate;
            try {
                // Validate token format and signature
                expiryDate = jwtTokenUtil.getExpirationDateFromToken(token);
            } catch (ExpiredJwtException e) {
                // Even if token is expired, we should blacklist it
                expiryDate = e.getClaims().getExpiration();
                tokenBlacklistStorage.blacklistToken(token, expiryDate.toInstant());
                SecurityContextHolder.clearContext();
                throw new IllegalArgumentException("Token has expired but has been blacklisted");
            }

            // Blacklist the token regardless of expiration
            tokenBlacklistStorage.blacklistToken(token, expiryDate.toInstant());
            SecurityContextHolder.clearContext();
            System.out.println("Successfully logged out user");

        } catch (ExpiredJwtException e) {
            // This should not be reached due to inner try-catch
            System.out.println("Token has expired: " + e.getMessage());
            throw new IllegalArgumentException("Token has expired");
        } catch (Exception e) {
            System.out.println("Error during logout: " + e.getMessage());
            throw new IllegalArgumentException("Invalid token");
        }
    }


}