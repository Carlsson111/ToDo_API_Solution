package se.lexicon.todo_app.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklistStorage {
    private final ConcurrentHashMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Instant expiryDate) {
        blacklistedTokens.put(token, expiryDate);
        removeExpiredTokens();
    }

    public boolean isBlacklisted(String token) {
        removeExpiredTokens();
        return blacklistedTokens.containsKey(token);
    }

    private void removeExpiredTokens() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}