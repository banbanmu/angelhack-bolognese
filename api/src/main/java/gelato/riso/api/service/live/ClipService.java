package gelato.riso.api.service.live;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.core.context.SecurityContext;
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
    private final ReactiveValueOperations<String, String> valueOps;

    public ClipService(@Value("${amazon.s3.bucket.name}") String bucketName,
                       ReactiveStringRedisTemplate redisTemplate) {
        this.bucketName = bucketName;
        valueOps = redisTemplate.opsForValue();
    }

    public Mono<Void> clippingVideo(SecurityContext context, List<CookClipInfo> clipInfos) {
        Integer userId = (Integer) context.getAuthentication().getCredentials();
        log.info("Clipping start. infos: {}", clipInfos);

        return valueOps.get(Integer.toString(userId))
                       .map(value -> value.split(":")[0])
                       .flatMap(startTimestamp -> Mono.fromFuture(CompletableFuture.runAsync(
                               () -> Clipper.start(Long.valueOf(startTimestamp), userId, clipInfos, bucketName), EXECUTOR)));
    }
}
