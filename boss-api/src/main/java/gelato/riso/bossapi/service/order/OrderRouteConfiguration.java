package gelato.riso.bossapi.service.order;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class OrderRouteConfiguration {

    private final OrderHandler orderHandler;

    @Bean
    public RouterFunction<?> orderRouterFunction() {
        return RouterFunctions.route()
                              .GET("/order", request -> orderHandler.getOrders())
                              .POST("/order/start", orderHandler::start)
                              .POST("/order/finish", orderHandler::finish)
                              .build();
    }
}
