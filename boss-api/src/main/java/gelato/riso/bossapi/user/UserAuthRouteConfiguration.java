package gelato.riso.bossapi.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class UserAuthRouteConfiguration {

    private final UserAuthHandler userAuthHandler;

    @Bean
    public RouterFunction<?> userRouterFunction() {
        return RouterFunctions.route()
                              .POST("/auth/signUp", userAuthHandler::signUp)
                              .POST("/auth/signIn", userAuthHandler::signIn)
                              .GET("/test/get", request -> ServerResponse.ok().bodyValue("Hi"))
                              .build();
    }
}
