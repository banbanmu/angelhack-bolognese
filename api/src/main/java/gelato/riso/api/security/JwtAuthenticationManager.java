package gelato.riso.api.security;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import gelato.riso.api.support.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();

        if (JwtUtils.isTokenExpired(authToken)) {
            return Mono.empty();
        }

        Claims allClaimsFromToken = JwtUtils.getAllClaimsFromToken(authToken);

        return Mono.just(allClaimsFromToken)
                   .map(claims -> claims.entrySet().stream().filter(e -> "role".equals(e.getKey()))
                                        .map(Entry::getValue)
                                        .flatMap(roles -> ((Collection<String>) roles).stream())
                                        .map(SimpleGrantedAuthority::new)
                                        .collect(Collectors.toList()))
                   .map(authorities -> new UsernamePasswordAuthenticationToken(
                           allClaimsFromToken.getSubject(), allClaimsFromToken.get("id"), authorities))
                   .onErrorResume(t -> Mono.empty())
                   .map(Function.identity());
    }
}
