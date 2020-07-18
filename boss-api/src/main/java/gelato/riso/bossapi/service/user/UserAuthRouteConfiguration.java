package gelato.riso.bossapi.service.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class UserAuthRouteConfiguration {

    private final UserAuthHandler userAuthHandler;

    @Bean
    public RouterFunction<?> userAuthRouterFunction() {
        return RouterFunctions.route()
                              .POST("/auth/signUp", userAuthHandler::signUp)
                              .POST("/auth/signIn", userAuthHandler::signIn)
                              .build();
    }
}
