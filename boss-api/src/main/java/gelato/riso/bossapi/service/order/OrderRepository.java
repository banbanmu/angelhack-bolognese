package gelato.riso.bossapi.service.order;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import gelato.riso.bossapi.service.order.Order.State;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends ReactiveMongoRepository<Order, String> {

    Flux<Order> findAllByStoreIdAndStateIsNot(String storeId, State state);
    Mono<Order> findByIdAndState(ObjectId id, State state);
}
