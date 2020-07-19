package gelato.riso.api.service.live;

import lombok.Value;

@Value
public class LiveInfo {
    Long startTimeStamp;
    Integer storeId;
    String channelName;

    static LiveInfo of(Long startTimeStamp, Integer userId, String channelName) {
        return new LiveInfo(startTimeStamp, userId, channelName);
    }
}
