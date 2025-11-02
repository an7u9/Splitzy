package org.splitzy.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.splitzy.auth.entity.AuthUser;
import org.splitzy.common.exception.ValidationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "BLACKLIST:";
    private static final String USER_ID_CLAIM = "USER_ID";
    private static final String USERNAME_CLAIM = "USERNAME";
    private static final String EMAIL_CLAIM = "EMAIL";
    private static final String ROLE_CLAIM = "ROLE";
    private static final String TOKEN_TYPE_CLAIM = "token_type";

    public JwtTokenService(RedisTemplate<String, String> redisTemplate, @Value("${jwt.secret}") String secret, @Value("${jwt.access-token.validity-ms:3600000}") long accessTokenValiditySeconds, @Value("${jwt.refresh-token.validity-ms:604800000}")  long refreshTokenValiditySeconds) {
        this.redisTemplate = redisTemplate;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    public String generateAcessToken(AuthUser authUser) {
        return generateToken(authUser, accessTokenValiditySeconds, "ACCESS");
    }

    public String generateRefreshToken(AuthUser user) {
        return generateToken(user, refreshTokenValiditySeconds, "REFRESH");
    }

    //Generate JWT token with specified validity and type
    private String generateToken(AuthUser authUser, long validityMs, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validityMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_CLAIM, authUser.getId());
        claims.put(USERNAME_CLAIM, authUser.getUsername());
        claims.put(EMAIL_CLAIM, authUser.getEmail());
        claims.put(TOKEN_TYPE_CLAIM, tokenType);
        claims.put(ROLE_CLAIM, authUser.getRole());

        return Jwts.builder().setClaims(claims).setSubject(authUser.getEmail()).setIssuedAt(now).setExpiration(expiryDate).signWith(SignatureAlgorithm.HS512, secretKey).compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object userIdObj = claims.get(USER_ID_CLAIM);
        if(userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        }
        return (Long) userIdObj;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (String) claims.get(USERNAME_CLAIM);
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (String) claims.get(EMAIL_CLAIM);
    }

    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (String) claims.get(ROLE_CLAIM);
    }

    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (String) claims.get(TOKEN_TYPE_CLAIM);
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    public LocalDateTime getExpirationAsLocalDateTime(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public Boolean isTokenExpired(String token) {
        try{
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch(JwtException e){
            return true;
        }
    }

    public Boolean validateToken(String token) {
        try {
            if (isTokenBlacklisted(token)) {
                log.debug("Token is blacklisted");
                return false;
            }

            getClaimsFromToken(token); // This will throw exception if invalid
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateRefreshToken(String token) {
        try{
            if (!validateToken(token)) {
                return false;
            }
            String tokenType = getTokenTypeFromToken(token);
            return "REFRESH".equals(tokenType);
        } catch(Exception e){
            log.debug("Refresh Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public void blacklistToken(String token) {
        try{
            Date expiration = getExpirationDateFromToken(token);
            long tt1 = expiration.getTime() - System.currentTimeMillis();

            if(tt1 > 0) {
                String key = BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(key, "blacklisted", tt1, TimeUnit.MILLISECONDS);
                log.debug("Token is blacklisted successfully");
            }
        } catch(Exception e){
            log.error("Failed to blacklist token: {}", e.getMessage());
            throw new ValidationException("Failed to logout user");
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try{
            String key = BLACKLIST_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch(Exception e){
            log.error("Failed to check token status: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaimsFromToken(String token){
        try{
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e){
            log.debug("Token expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e){
            log.debug("Unsupported JWT token: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e){
            log.debug("Malformed JWT token: {}", e.getMessage());
            throw e;
        } catch (SecurityException e){
            log.debug("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e){
            log.debug("JWT token compact of handler are invalid: {}", e.getMessage());
            throw e;
        }
    }

    public long getRemainingValidityInSeconds(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return remaining > 0 ? remaining / 1000 : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}