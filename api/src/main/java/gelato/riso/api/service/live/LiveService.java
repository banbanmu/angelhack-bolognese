package gelato.riso.api.service.live;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;

import gelato.riso.api.service.live.LiveHandler.LiveStop.CookClipInfo;
import gelato.riso.recorder.RecordingSampleM;
import reactor.core.publisher.Mono;

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
        LiveInfo liveInfo = LiveInfo.of(userId, channelName);

        return valueOps.set(Integer.toString(liveInfo.getUserId()), liveInfo.getChannelName())
                       .then(Mono.just(liveInfo));
    }

    public Mono<Boolean> stop(SecurityContext context, List<CookClipInfo> clipInfos) {
        Integer userId = (Integer) context.getAuthentication().getCredentials();
        return valueOps.delete(Integer.toString(userId));
    }

    private static String generateChannelName(Integer uid) {
        return "channel_" + uid;
    }
}
