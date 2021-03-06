package com.altimetrik.gateway.config.security.jwt;

import com.altimetrik.gateway.config.Constants;
import com.altimetrik.gateway.dto.AuthenticationResponse;
import com.altimetrik.gateway.repository.AuthenticationRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.invoke.MethodHandles;
import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtProvider implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("${altimetrik.jwt.secret}")
    private String secretKey;

    @Value("${altimetrik.jwt.validityInSeconds}")
    private Integer validityInSeconds;

    private AuthenticationRepository authenticationRepository;
    private Key key;

    public JwtProvider(AuthenticationRepository authenticationRepository) {
        this.authenticationRepository = authenticationRepository;
    }

    @Override
    public void afterPropertiesSet() {
        byte[] keyBytes;

        log.debug("Using a Base64-encoded JWT secret key");
        keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();

        Collection<? extends GrantedAuthority> authorities;
        if (!StringUtils.isEmpty(claims.get(Constants.AUTHORITIES_KEY))) {
            authorities =
                    Arrays.stream(claims.get(Constants.AUTHORITIES_KEY).toString().split(Constants.COMMA))
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

        } else {
            authorities = new ArrayList<>();
        }

        User principal = new User(claims.getSubject(), Constants.EMPTY, authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public AuthenticationResponse createToken(Authentication authentication, String userId) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(Constants.COMMA));

        Date validity = new Date(System.currentTimeMillis() + 1000 * validityInSeconds);

        Map<String, Object> claimMap = new HashMap<>();
        claimMap.put(Constants.USER_ID_KEY, userId);

        String jwt = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(Constants.AUTHORITIES_KEY, authorities)
                .addClaims(claimMap)
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity)
                .compact();

        AuthenticationResponse response = new AuthenticationResponse(jwt, validity);
        authenticationRepository.save(response);

        return response;
    }

    public AuthenticationResponse refreshToken(String token) {

        authenticationRepository.deleteById(token);
        Authentication authentication = this.getAuthentication(token);
        return this.createToken(authentication, this.extractUsername(token));
    }

    public boolean validateToken(String authToken, HttpServletRequest request) {
        try {
            Jwts.parser().setSigningKey(key).parseClaimsJws(authToken); // Validity is checked here.
            Optional<AuthenticationResponse> response = authenticationRepository.findById(authToken);
            if (response.isEmpty()) {
                request.setAttribute(Constants.JWT_EXCEPTION, Constants.EXPIRED_JWT_EXCEPTION);
                return false;
            }
            return true;
        } /*catch (JwtException | IllegalArgumentException e) {
            //log.info("Invalid JWT token.");
            //log.trace("Invalid JWT token trace.", e);
            //throw e;
        }*/ catch (SignatureException ex) {
            log.info("Invalid JWT Signature");
            request.setAttribute(Constants.JWT_EXCEPTION, Constants.SIGNATURE_EXCEPTION);
        } catch (MalformedJwtException ex) {
            log.info("Invalid JWT token");
            request.setAttribute(Constants.JWT_EXCEPTION, Constants.MALFORMED_JWT_EXCEPTION);
        } catch (ExpiredJwtException ex) {
            log.info("Expired JWT token");
            request.setAttribute(Constants.JWT_EXCEPTION, Constants.EXPIRED_JWT_EXCEPTION);
        } catch (UnsupportedJwtException ex) {
            log.info("Unsupported JWT exception");
            request.setAttribute(Constants.JWT_EXCEPTION, Constants.UNSUPPORTED_JWT_EXCEPTION);
        } catch (IllegalArgumentException ex) {
            log.info("Jwt claims string is empty");
            request.setAttribute(Constants.JWT_EXCEPTION, Constants.ILLEGAL_ARGUMENT_EXCEPTION);
        }
        return false;
    }
}