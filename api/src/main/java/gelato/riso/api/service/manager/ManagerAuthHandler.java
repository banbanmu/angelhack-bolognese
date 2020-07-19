package gelato.riso.api.service.manager;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import gelato.riso.api.service.manager.ManagerAuthHandler.SignUp.Response;
import gelato.riso.api.support.utils.JwtUtils;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ManagerAuthHandler {

    private final ManagerService managerService;

    public Mono<ServerResponse> signUp(ServerRequest request) {
        return request.bodyToMono(SignUp.Request.class)
                      .flatMap(param -> managerService.signUp(
                              param.getUsername(), param.getPassword(), param.getPhoneNumber()))
                      .flatMap(manager -> ServerResponse
                              .ok().bodyValue(Response.builder()
                                                      .id(manager.getId())
                                                      .username(manager.getUsername())
                                                      .token(JwtUtils.generateToken(manager))
                                                      .build()));
    }

    public Mono<ServerResponse> signIn(ServerRequest request) {
        return request.bodyToMono(SignIn.Request.class)
                      .flatMap(param -> managerService.signIn(param.getUsername(), param.getPassword()))
                      .flatMap(manager -> ServerResponse
                              .ok().bodyValue(SignIn.Response.builder()
                                                             .id(manager.getId())
                                                             .username(manager.getUsername())
                                                             .token(JwtUtils.generateToken(manager))
                                                             .build()));
    }

    static class SignIn {
        @Value
        @Builder
        static class Request {
            String username;
            String password;
        }

        @Value
        @Builder
        private static class Response {
            Integer id;
            String username;
            String token;
        }
    }

    static class SignUp {
        @Value
        @Builder
        static class Request {
            String username;
            String password;
            String phoneNumber;
        }

        @Value
        @Builder
        static class Response {
            Integer id;
            String username;
            String token;
        }
    }

}
