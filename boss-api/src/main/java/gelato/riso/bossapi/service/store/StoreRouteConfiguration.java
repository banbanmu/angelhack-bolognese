package gelato.riso.bossapi.service.store;

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
                              .GET("/store", storeHandler::getMyHome)
                              .POST("/store", storeHandler::registerStore)
                              .PUT("/store", storeHandler::editStore)
                              .build();
    }

}
