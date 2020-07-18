package gelato.riso.bossapi.support.exception;

import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@Component
public class ApiErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public ApiErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                       ResourceProperties resourceProperties,
                                       ServerCodecConfigurer serverCodecConfigurer,
                                       ApplicationContext applicationContext) {
        super(errorAttributes, resourceProperties, applicationContext);
        setMessageWriters(serverCodecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable throwable = getError(request);

        return ServerResponse.status(getHttpStatueFromThrowable(throwable))
                             .build();
    }

    private static HttpStatus getHttpStatueFromThrowable(Throwable throwable) {
        if (throwable instanceof BaseException) {
            return ((BaseException) throwable).getHttpStatus();
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
