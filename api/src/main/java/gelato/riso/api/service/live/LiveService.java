package gelato.riso.api.service.live;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class LiveService {

    public Mono<Void> start() {
        return Mono.empty();
    }
}
