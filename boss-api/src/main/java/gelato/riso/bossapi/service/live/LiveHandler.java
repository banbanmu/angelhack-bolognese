package gelato.riso.bossapi.service.live;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@Component
public class LiveHandler {

    public Mono<ServerResponse> start(ServerRequest request) {
        return ServerResponse.ok().build();
    }

    public Mono<ServerResponse> stop(ServerRequest request) {
        return ServerResponse.ok().build();
    }
}
