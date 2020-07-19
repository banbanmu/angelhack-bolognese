package gelato.riso.api.service.live;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gelato.riso.api.service.live.LiveHandler.LiveStop.CookClipInfo;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ClipService {

    private static final ExecutorService EXECUTOR = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final String bucketName;

    public ClipService(@Value("${amazon.s3.bucket.name}") String bucketName) {
        this.bucketName = bucketName;
    }

    public Mono<Void> clippingVideo(Integer userId, List<CookClipInfo> clipInfos, String value) {
        log.info("Clipping start. infos: {}. value: {}", clipInfos, value);
        String startTimestamp = value.split("AAAA")[0];
        return Mono.fromFuture(CompletableFuture.runAsync(
                           () -> Clipper.start(Long.valueOf(startTimestamp), userId, clipInfos, bucketName), EXECUTOR));
    }
}
