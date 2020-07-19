package gelato.riso.api.service.live;

import lombok.Value;

@Value
public class LiveInfo {
    Integer userId;
    String channelName;

    static LiveInfo of(Integer userId, String channelName) {
        return new LiveInfo(userId, channelName);
    }
}
