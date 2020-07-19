package gelato.riso.api.service.store;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import gelato.riso.api.service.store.Store.Category;
import gelato.riso.api.service.store.Store.Food;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class StoreHandler {

    private final StoreService storeService;

    public Mono<ServerResponse> getMyStore() {
        return ReactiveSecurityContextHolder
                .getContext()
                .flatMap(storeService::getMyStore)
                .flatMap(store -> ServerResponse.ok().bodyValue(MyStore.Response.from(store)));
    }

    public Mono<ServerResponse> getAllCategory() {
        return storeService.getAllCategory()
                           .flatMap(categories -> {
                               List<CategoryResponse> categoryResponses = categories.stream()
                                                                                    .map(CategoryResponse::of)
                                                                                    .collect(Collectors
                                                                                                     .toList());

                               return ServerResponse.ok().bodyValue(
                                       AllCategory.Response.builder()
                                                           .categories(categoryResponses)
                                                           .build());
                           });
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

    static class MyStore {
        @Value
        @Builder
        static class Response {
            Integer id;
            String name;
            String address;
            String phoneNumber;
            CategoryResponse category;
            List<Food> menu;

            static Response from(Store store) {
                return builder()
                        .id(store.getId())
                        .name(store.getName())
                        .address(store.getAddress())
                        .phoneNumber(store.getPhoneNumber())
                        .category(CategoryResponse.of(store.getCategory()))
                        .menu(store.getMenu())
                        .build();
            }
        }

    }

    static class RegisterStore {
        @Value
        @Builder
        static class Request {
            String name;
            String address;
            String phoneNumber;
            Category category;
            List<Food> menu;
        }
    }

    static class EditStore {
        @Value
        @Builder
        static class Request {
            Integer id;
            String name;
            String address;
            String phoneNumber;
            Category category;
            List<Food> menu;
        }
    }

    static class AllCategory {
        @Value
        @Builder
        static class Response {
            List<CategoryResponse> categories;
        }

    }

    @Value
    static class CategoryResponse {
        String key;
        String name;

        static CategoryResponse of(Category category) {
            return new CategoryResponse(category.name(), category.getKorean());
        }
    }
}
