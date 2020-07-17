package gelato.riso.bossapi.utils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import gelato.riso.bossapi.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JwtUtils {

    private static final String secret = "AENHAPBARMKTLMKMLWAOKGWAIWFKNENKNKLGN1243nkAFNK";

    private static final long expirationTime = 28800;

    private static final Key key = Keys.hmacShaKeyFor(secret.getBytes());

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRoles());
        return doGenerateToken(claims, user.getUsername());
    }

    private String doGenerateToken(Map<String, Object> claims, String username) {

        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + expirationTime * 1000);

        return Jwts.builder()
                   .setClaims(claims)
                   .setSubject(username)
                   .setIssuedAt(createdDate)
                   .setExpiration(expirationDate)
                   .signWith(key)
                   .compact();
    }

//    public Claims getAllClaimsFromToken(String token) {
//        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
//    }
//
//    public String getUsernameFromToken(String token) {
//        return getAllClaimsFromToken(token).getSubject();
//    }
//
//    public Date getExpirationDateFromToken(String token) {
//        return getAllClaimsFromToken(token).getExpiration();
//    }
//
//    private Boolean isTokenExpired(String token) {
//        final Date expiration = getExpirationDateFromToken(token);
//        return expiration.before(new Date());
//    }
//
//    public Boolean validateToken(String token) {
//        return !isTokenExpired(token);
//    }

}
