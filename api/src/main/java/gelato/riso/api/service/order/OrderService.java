package gelato.riso.api.service.order;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;

import gelato.riso.api.service.order.Order.State;
import gelato.riso.api.support.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public Mono<List<Order>> getOrders(SecurityContext context) {
        Integer storeId = (Integer) context.getAuthentication().getCredentials();
        return orderRepository.findAllByStoreIdAndStateIsNot(storeId, State.FINISHED)
                              .collectList();
    }

    public Mono<Order> start(SecurityContext context, String orderId, Integer storeId) {
        Integer userId = (Integer) context.getAuthentication().getCredentials();
        return editOrderState(userId, orderId, storeId, State.NOT_STARTED, State.STARTED);
    }

    public Mono<Order> finish(SecurityContext context, String orderId, Integer storeId) {
        Integer userId = (Integer) context.getAuthentication().getCredentials();
        return editOrderState(userId, orderId, storeId, State.STARTED, State.FINISHED);
    }

    private Mono<Order> editOrderState(
            Integer userId, String orderId, Integer storeId, State currentState, State newState) {
        if (false == storeId.equals(userId)) {
            return Mono.error(new NotAllowedOrderEditException());
        }

        return orderRepository.findByIdAndState(new ObjectId(orderId), currentState)
                              .filter(order -> order.getStoreId().equals(storeId))
                              .switchIfEmpty(Mono.error(new Exception()))
                              .map(order -> order.withState(newState))
                              .flatMap(orderRepository::save);

    }
    private static class NotAllowedOrderEditException extends BaseException {
        private static final long serialVersionUID = 1143674303077608995L;

        @Getter
        private final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    }

}
