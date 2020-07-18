package gelato.riso.api.service.store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class StoreRouteConfiguration {

    private final StoreHandler storeHandler;

    @Bean
    public RouterFunction<?> storeRouterFunction() {
        return RouterFunctions.route()
                              .GET("/store", request -> storeHandler.getMyStore())
                              .POST("/store", storeHandler::registerStore)
                              .PUT("/store", storeHandler::editStore)
                              .GET("/store/category", request -> storeHandler.getAllCategory())
                              .build();
    }

}
