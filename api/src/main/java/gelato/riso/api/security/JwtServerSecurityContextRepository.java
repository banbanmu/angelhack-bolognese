package gelato.riso.api.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtServerSecurityContextRepository implements ServerSecurityContextRepository {

    private final ReactiveAuthenticationManager authenticationManager;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest()
                                        .getHeaders()
                                        .getFirst(HttpHeaders.AUTHORIZATION))
                   .filter(authHeader -> authHeader != null && authHeader.startsWith("Bearer "))
                   .map(authHeader -> authHeader.substring(7))
                   .map(authToken -> new UsernamePasswordAuthenticationToken(authToken, authToken))
                   .flatMap(authenticationManager::authenticate)
                   .map(SecurityContextImpl::new);
    }
}
