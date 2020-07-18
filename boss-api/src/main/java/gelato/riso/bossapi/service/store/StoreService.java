package gelato.riso.bossapi.service.store;

import java.util.List;
import java.util.function.Predicate;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;

import gelato.riso.bossapi.service.store.Store.Food;
import gelato.riso.bossapi.support.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    public Mono<Store> getMyHome(SecurityContext context) {
        String id = context.getAuthentication().getCredentials().toString();
        return storeRepository.findById(id)
                              .switchIfEmpty(Mono.error(new StoreNotFoundException()));
    }

    public Mono<Store> registerStore(SecurityContext context, String name, String address,
                                     String phoneNumber, String category, List<Food> menu) {
        return Mono.just(context.getAuthentication())
                   .map(authentication -> Store.builder()
                                               .id(new ObjectId(authentication.getCredentials().toString()))
                                               .name(name)
                                               .address(address)
                                               .phoneNumber(phoneNumber)
                                               .category(category)
                                               .menu(menu)
                                               .build())
                   .flatMap(storeRepository::insert);
    }

    public Mono<Store> editStore(SecurityContext context, String id, String name, String address,
                                 String phoneNumber, String category, List<Food> menu) {
        return Mono.just(context.getAuthentication())
                   .map(Authentication::getCredentials)
                   .filter(Predicate.isEqual(id))
                   .switchIfEmpty(Mono.error(new NotAllowedEditException()))
                   .then(Mono.just(Store.builder()
                                        .id(new ObjectId(id))
                                        .name(name)
                                        .address(address)
                                        .phoneNumber(phoneNumber)
                                        .category(category)
                                        .menu(menu)
                                        .build()))
                   .flatMap(storeRepository::save);
    }

    private static class NotAllowedEditException extends BaseException {
        private static final long serialVersionUID = 1143674303077608995L;

        @Getter
        private final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    }

    private static class StoreNotFoundException extends BaseException {
        private static final long serialVersionUID = 1143674303077608995L;

        @Getter
        private final HttpStatus httpStatus = HttpStatus.NOT_FOUND;
    }
}
