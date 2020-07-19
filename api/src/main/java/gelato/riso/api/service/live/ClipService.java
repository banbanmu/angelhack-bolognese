package gelato.riso.api.service.live;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;

import gelato.riso.api.service.live.LiveHandler.LiveStop.CookClipInfo;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ClipService {

    private final String bucketName;

    private static final ExecutorService EXECUTOR = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public ClipService(@Value("${amazon.s3.bucket.name}") String bucketName) {
        this.bucketName = bucketName;
    }

    public Mono<Void> clippingVideo(SecurityContext context, List<CookClipInfo> clipInfos) {
        Integer userId = (Integer) context.getAuthentication().getCredentials();
        log.info("Clipping start. infos: {}", clipInfos);

        return Mono.fromFuture(CompletableFuture.runAsync(() -> Clipper.start(userId, clipInfos, bucketName), EXECUTOR));
    }
}
