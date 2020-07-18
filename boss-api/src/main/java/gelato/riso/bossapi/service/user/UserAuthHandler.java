package gelato.riso.bossapi.service.user;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import gelato.riso.bossapi.support.utils.JwtUtils;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserAuthHandler {

    private final UserService userService;

    public Mono<ServerResponse> signUp(ServerRequest request) {
        return request.bodyToMono(SignUp.Request.class)
                      .flatMap(param -> userService.signUp(param.getUsername(), param.getPassword()))
                      .flatMap(user -> ServerResponse
                              .ok().bodyValue(SignUp.Response.builder()
                                                             .token(JwtUtils.generateToken(user))
                                                             .build()));
    }

    public Mono<ServerResponse> signIn(ServerRequest request) {
        return request.bodyToMono(SignIn.Request.class)
                      .flatMap(param -> userService.signIn(param.getUsername(), param.getPassword()))
                      .flatMap(user -> ServerResponse
                              .ok().bodyValue(SignIn.Response.builder()
                                                             .token(JwtUtils.generateToken(user))
                                                             .build()));
    }

    public static class SignIn {
        @Value
        @Builder
        private static class Request {
            String username;
            String password;
        }

        @Value
        @Builder
        private static class Response {
            String token;
        }
    }

    public static class SignUp {
        @Value
        @Builder
        private static class Request {
            String username;
            String password;
        }

        @Value
        @Builder
        private static class Response {
            String token;
        }
    }

}
