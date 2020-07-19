package gelato.riso.api.service.live;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
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
    private final ReactiveStringRedisTemplate redisTemplate;

    public LiveService(@Value("${recording.app.id}") String appId, ReactiveStringRedisTemplate redisTemplate) {
        this.appId = appId;
        this.redisTemplate = redisTemplate;
    }

    public Mono<LiveInfo> start(SecurityContext context) {
        Integer userId = (Integer) context.getAuthentication().getCredentials();
        String channelName = generateChannelName(userId);
        EXECUTOR.submit(() -> RecordingSampleM.start(userId, appId, channelName));
        log.info("Recording started. userId: {}, channelName: {}", userId, channelName);
        Date startDate = new Date();
        LiveInfo liveInfo = LiveInfo.of(startDate.getTime(), userId, channelName);

        return redisTemplate.opsForValue().set(Integer.toString(liveInfo.getUserId()),
                                               liveInfo.getStartTimeStamp() + ':' + liveInfo.getChannelName())
                            .then(Mono.just(liveInfo));
    }

    public Mono<Boolean> stop(SecurityContext context) {
        Integer userId = (Integer) context.getAuthentication().getCredentials();
        log.info("Recording stopped. userId: {}", userId);
        return redisTemplate.opsForValue().delete(Integer.toString(userId));
    }

    public Mono<List<LiveInfo>> list() {
        return redisTemplate.keys("*")
                            .flatMap(key -> redisTemplate.opsForValue().get(key)
                                                         .map(value -> {
                                                             String[] split = value.split(":");
                                                             return LiveInfo.of(Long.valueOf(split[0]),
                                                                                Integer.valueOf(key), split[1]);
                                                         }))
                            .collectList();
    }

    private static String generateChannelName(Integer uid) {
        return "channel_" + uid;
    }
}
