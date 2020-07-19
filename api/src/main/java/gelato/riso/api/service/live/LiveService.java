package gelato.riso.api.service.live;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;

import gelato.riso.recorder.RecordingSampleM;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class LiveService {

    private static final ExecutorService EXECUTOR = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final String appId;
    private final ReactiveValueOperations<String, String> valueOps;

    public LiveService(@Value("${recording.app.id}") String appId, ReactiveStringRedisTemplate redisTemplate) {
        this.appId = appId;
        valueOps = redisTemplate.opsForValue();
    }

    public Mono<LiveInfo> start(SecurityContext context) {
        Integer userId = (Integer) context.getAuthentication().getCredentials();
        String channelName = generateChannelName(userId);
        EXECUTOR.submit(() -> RecordingSampleM.start(userId, appId, channelName));
        log.info("Recording started. userId: {}, channelName: {}", userId, channelName);
        LiveInfo liveInfo = LiveInfo.of(userId, channelName);

        return valueOps.set(Integer.toString(liveInfo.getUserId()), liveInfo.getChannelName())
                       .then(Mono.just(liveInfo));
    }

    public Mono<Boolean> stop(SecurityContext context) {
        Integer userId = (Integer) context.getAuthentication().getCredentials();
        log.info("Recording stopped. userId: {}", userId);
        return valueOps.delete(Integer.toString(userId));
    }

    private static String generateChannelName(Integer uid) {
        return "channel_" + uid;
    }
}
