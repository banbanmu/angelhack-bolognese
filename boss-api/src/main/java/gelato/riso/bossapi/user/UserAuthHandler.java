package gelato.riso.bossapi.user;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import gelato.riso.bossapi.utils.JwtUtils;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserAuthHandler {

    private final UserAuthService userAuthService;

    public Mono<ServerResponse> signUp(ServerRequest request) {
        return request.bodyToMono(SignUp.Request.class)
                      .flatMap(param -> userAuthService.signUp(param.getUsername(), param.getPassword()))
                      .flatMap(user -> ServerResponse
                              .ok().bodyValue(SignUp.Response.builder()
                                                             .token(JwtUtils.generateToken(user))
                                                             .build()));
    }

    public Mono<ServerResponse> signIn(ServerRequest request) {
        return request.bodyToMono(SignIn.Request.class)
                      .flatMap(param -> userAuthService.signIn(param.getUsername(), param.getPassword()))
                      .flatMap(user -> ServerResponse
                              .ok().bodyValue(SignIn.Response.builder()
                                                             .token(JwtUtils.generateToken(user))
                                                             .build()));
    }

    public static class SignIn {
        @Value
        @Builder
        public static class Request {
            String username;
            String password;
        }

        @Value
        @Builder
        public static class Response {
            String token;
        }
    }

    public static class SignUp {
        @Value
        @Builder
        public static class Request {
            String username;
            String password;
        }

        @Value
        @Builder
        public static class Response {
            String token;
        }
    }

}
