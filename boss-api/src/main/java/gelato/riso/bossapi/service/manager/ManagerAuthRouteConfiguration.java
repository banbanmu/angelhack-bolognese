package gelato.riso.bossapi.service.manager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ManagerAuthRouteConfiguration {

    private final ManagerAuthHandler managerAuthHandler;

    @Bean
    public RouterFunction<?> userAuthRouterFunction() {
        return RouterFunctions.route()
                              .POST("/auth/signUp", managerAuthHandler::signUp)
                              .POST("/auth/signIn", managerAuthHandler::signIn)
                              .build();
    }
}
