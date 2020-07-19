package gelato.riso.api.service.order;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import gelato.riso.api.service.order.Order.State;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends ReactiveMongoRepository<Order, String> {

    Flux<Order> findAllByStoreIdAndStateIsNot(Integer storeId, State state);
    Mono<Order> findByIdAndState(ObjectId id, State state);
}
