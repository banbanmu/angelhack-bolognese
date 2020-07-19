package gelato.riso.api.service.live;

import java.util.List;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LiveHandler {

    private final LiveService liveService;

    public Mono<ServerResponse> start() {
        return ReactiveSecurityContextHolder
                .getContext()
                .flatMap(liveService::start)
                .flatMap(liveInfo -> ServerResponse.ok().bodyValue(
                        new LiveStart.Response(liveInfo.getUserId(), liveInfo.getChannelName())));
    }

    public Mono<ServerResponse> stop(ServerRequest request) {
        return Mono.zip(ReactiveSecurityContextHolder.getContext(),
                        request.bodyToMono(LiveStop.Request.class))
                   .map(tuple -> {
                       SecurityContext context = tuple.getT1();
                       LiveStop.Request param = tuple.getT2();
                       return liveService.stop(context, param.clipInfos);
                   }).flatMap(b -> ServerResponse.ok().build());
    }

    static class LiveStart {
        @Value
        static class Response {
            Integer uid;
            String channelName;
        }
    }

    static class LiveStop {
        @Value
        @Builder
        static class Request {
            List<CookClipInfo> clipInfos;
        }

        @Value
        @Builder
        static class CookClipInfo {
            String name;
            Integer startSecond;
            Integer duration;
        }
    }
}
