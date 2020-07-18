package gelato.riso.api.support.utils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import gelato.riso.api.service.manager.Manager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JwtUtils {

    private static final String SECRET = "AENHAPBARMKTLMKMLWAOKGWAIWFKNENKNKLGN1243nkAFNK";
    private static final long EXPIRATION_TIME = 28800;
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    private static final JwtParser JWT_PARSER = Jwts.parserBuilder().setSigningKey(KEY).build();

    public String generateToken(Manager manager) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", manager.getIdToString());
        claims.put("role", manager.getRoles());
        return doGenerateToken(claims, manager.getUsername());
    }

    private String doGenerateToken(Map<String, Object> claims, String username) {

        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + EXPIRATION_TIME * 1000);

        return Jwts.builder()
                   .setClaims(claims)
                   .setSubject(username)
                   .setIssuedAt(createdDate)
                   .setExpiration(expirationDate)
                   .signWith(KEY)
                   .compact();
    }

    public Claims getAllClaimsFromToken(String token) {
        return JWT_PARSER.parseClaimsJws(token).getBody();
    }

    public Date getExpirationDateFromToken(String token) {
        return getAllClaimsFromToken(token).getExpiration();
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

}
