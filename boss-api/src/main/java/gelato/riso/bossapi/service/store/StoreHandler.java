package gelato.riso.bossapi.service.store;


import java.util.List;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import gelato.riso.bossapi.service.store.Store.Food;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class StoreHandler {

    private final StoreService storeService;

    public Mono<ServerResponse> getMyHome(ServerRequest request) {
        return ReactiveSecurityContextHolder
                .getContext()
                .flatMap(storeService::getMyHome)
                .flatMap(store -> ServerResponse.ok().bodyValue(store));

    }

    public Mono<ServerResponse> registerStore(ServerRequest request) {
        return Mono.zip(ReactiveSecurityContextHolder.getContext(),
                        request.bodyToMono(RegisterStore.Request.class))
                   .flatMap(tuple -> {
                       SecurityContext context = tuple.getT1();
                       RegisterStore.Request param = tuple.getT2();
                       return storeService.registerStore(
                               context, param.name, param.address, param.phoneNumber,
                               param.category, param.menu);
                   }).flatMap(store -> ServerResponse.ok().build());

    }

    public Mono<ServerResponse> editStore(ServerRequest request) {
        return Mono.zip(ReactiveSecurityContextHolder.getContext(),
                        request.bodyToMono(EditStore.Request.class))
                   .flatMap(tuple -> {
                       SecurityContext context = tuple.getT1();
                       EditStore.Request param = tuple.getT2();
                       return storeService.editStore(
                               context, param.id, param.name, param.address,
                               param.phoneNumber, param.category, param.menu);
                   }).flatMap(store -> ServerResponse.ok().build());
    }

    private static class RegisterStore {
        @Value
        @Builder
        private static class Request {
            String name;
            String address;
            String phoneNumber;
            String category;
            List<Food> menu;
        }
    }

    private static class EditStore {
        @Value
        @Builder
        private static class Request {
            String id;
            String name;
            String address;
            String phoneNumber;
            String category;
            List<Food> menu;
        }
    }
}
