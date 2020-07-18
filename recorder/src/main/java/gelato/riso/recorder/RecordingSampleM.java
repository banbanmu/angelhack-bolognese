package gelato.riso.recorder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import io.agora.recording.RecordingEventHandler;
import io.agora.recording.RecordingSDK;
import io.agora.recording.common.Common;
import io.agora.recording.common.Common.AUDIO_FORMAT_TYPE;
import io.agora.recording.common.Common.AUDIO_FRAME_TYPE;
import io.agora.recording.common.Common.AudioFrame;
import io.agora.recording.common.Common.AudioVolumeInfo;
import io.agora.recording.common.Common.CHANNEL_PROFILE_TYPE;
import io.agora.recording.common.Common.CONNECTION_CHANGED_REASON_TYPE;
import io.agora.recording.common.Common.CONNECTION_STATE_TYPE;
import io.agora.recording.common.Common.MIXED_AV_CODEC_TYPE;
import io.agora.recording.common.Common.REMOTE_STREAM_STATE;
import io.agora.recording.common.Common.REMOTE_STREAM_STATE_CHANGED_REASON;
import io.agora.recording.common.Common.REMOTE_VIDEO_STREAM_TYPE;
import io.agora.recording.common.Common.RecordingStats;
import io.agora.recording.common.Common.RemoteAudioStats;
import io.agora.recording.common.Common.RemoteVideoStats;
import io.agora.recording.common.Common.VIDEO_FORMAT_TYPE;
import io.agora.recording.common.Common.VideoFrame;
import io.agora.recording.common.RecordingConfig;
import io.agora.recording.common.RecordingEngineProperties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RecordingSampleM implements RecordingEventHandler {
    // java run status flag
    private boolean isMixMode;
    private int width;
    private int height;

    private String storageDir = "./";
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private boolean m_receivingAudio;
    private boolean m_receivingVideo;
    private int keepLastFrame;
    private CHANNEL_PROFILE_TYPE profile_type;
    Vector<Long> m_peers = new Vector<>();
    private RecordingConfig config;
    private final RecordingSDK RecordingSDKInstance;
    private final HashSet<Long> subscribedVideoUids = new HashSet<>();
    private final HashSet<String> subscribeVideoUserAccount = new HashSet<>();

    HashMap<String, UserInfo> audioChannels = new HashMap<>();
    HashMap<String, UserInfo> videoChannels = new HashMap<>();
    Timer cleanTimer = new Timer();
    private int layoutMode;
    private long maxResolutionUid = -1;
    private String maxResolutionUserAccount = "";
    public static final int BESTFIT_LAYOUT = 1;
    public static final int VERTICALPRESENTATION_LAYOUT = 2;
    private String userAccount = "";

    public RecordingSampleM(RecordingSDK recording) {
        RecordingSDKInstance = recording;
        RecordingSDKInstance.registerOberserver(this);
    }

    public static void start(String... args) {
        RecordingSDK RecordingSdk = new RecordingSDK();
        RecordingSampleM ars = new RecordingSampleM(RecordingSdk);
        Thread thread = new RecordingWorkerThread(ars, args);
        thread.start();
        while (true) {
            if (ars.stopped.get() || !thread.isAlive()) {
                log.info("Jni layer has been exited,now exiting Java...!");

                break;
            }
        }
        log.info("exit java process...");
        ars.unRegister();
    }

    public void unRegister() {
        RecordingSDKInstance.unRegisterOberserver(this);
    }

    private boolean IsMixMode() {
        return isMixMode;
    }

    @Override
    public void onLeaveChannel(int reason) {
        log.info("RecordingSDK onLeaveChannel,code:" + reason);
    }

    @Override
    public void onError(int error, int stat_code) {
        log.info("RecordingSDK onError,error:" + error + ",stat code:" + stat_code);
    }

    @Override
    public void onWarning(int warn) {
        log.warn("RecordingSDK onWarning,warn:" + warn);
    }

    @Override
    public void onJoinChannelSuccess(String channelId, long uid) {
        cleanTimer.schedule(new RecordingCleanTimerM(this), 10000);
        log.info("RecordingSDK joinChannel success, channelId:" + channelId + ", uid:" + uid);
    }

    @Override
    public void onRejoinChannelSuccess(String channelid, long uid) {}

    @Override
    public void onConnectionStateChanged(CONNECTION_STATE_TYPE state, CONNECTION_CHANGED_REASON_TYPE reason) {}

    @Override
    public void onRemoteVideoStats(long uid, RemoteVideoStats stats) {}

    @Override
    public void onRemoteAudioStats(long uid, RemoteAudioStats stats) {}

    @Override
    public void onRecordingStats(RecordingStats stats) {}

    @Override
    public void onRemoteVideoStreamStateChanged(long uid, REMOTE_STREAM_STATE state, REMOTE_STREAM_STATE_CHANGED_REASON reason) {
        log.info("OnRemoteVideoStreamState changed, state " + state + ", reason :" + reason);
    }

    @Override
    public void onRemoteAudioStreamStateChanged(long uid, REMOTE_STREAM_STATE state, REMOTE_STREAM_STATE_CHANGED_REASON reason) {
        log.info("OnRemoteAudioStreamState changed, state " + state + ", reason :" + reason);
    }

    @Override
    public void onUserOffline(long uid, int reason) {
        log.info("RecordingSDK onUserOffline uid:" + uid + ",offline reason:" + reason);
        m_peers.remove(uid);
        //PrintUsersInfo(m_peers);
        SetVideoMixingLayout();
    }

    protected void clean() {
        synchronized(this) {
            long now = System.currentTimeMillis();

            Iterator<Map.Entry<String, UserInfo>> audio_it = audioChannels.entrySet().iterator();
            cleanWithIterator(audio_it, now);

            Iterator<Map.Entry<String, UserInfo>> video_it = videoChannels.entrySet().iterator();
            cleanWithIterator(video_it, now);
        }
        cleanTimer.schedule(new RecordingCleanTimerM(this), 10000);
    }

    private static void cleanWithIterator(Iterator<Map.Entry<String, UserInfo>> it, long now) {
        while (it.hasNext()) {
            Map.Entry<String, UserInfo> entry = it.next();
            UserInfo info = entry.getValue();
            if (now - info.last_receive_time > 3000) {
                try {
                    info.channel.close();
                } catch (IOException ignored) {
                }
                it.remove();
            }
        }

    }

    @Override
    public void onUserJoined(long uid, String recordingDir) {
        log.info("onUserJoined uid:" + uid + ",recordingDir:" + recordingDir);
        storageDir = recordingDir;
        m_peers.add(uid);
        //PrintUsersInfo(m_peers);
        // When the user joined, we can re-layout the canvas

        if (userAccount.length() > 0) {
            if (layoutMode != VERTICALPRESENTATION_LAYOUT || RecordingSDKInstance.getUidByUserAccount(maxResolutionUserAccount) != 0) {
                SetVideoMixingLayout();
            }
        } else {
            SetVideoMixingLayout();
        }
    }

    @Override
    public void onLocalUserRegistered(long uid, String userAccount) {
        log.info("onLocalUserRegistered: " + uid + " => " + userAccount);
    }

    @Override
    public void onUserInfoUpdated(long uid, String userAccount) {
        log.info("onUserInfoUpdated: " + uid + " => " + userAccount);

        if (subscribeVideoUserAccount.contains(userAccount)) {
            subscribedVideoUids.add(uid);
        }
        SetVideoMixingLayout();
    }

    private void checkUser(long uid, boolean isAudio) {
        String path = storageDir + uid;
        String key = Long.toString(uid);
        synchronized(this) {
            if (isAudio && !audioChannels.containsKey(key)) {
                if (config.decodeAudio == AUDIO_FORMAT_TYPE.AUDIO_FORMAT_AAC_FRAME_TYPE ||
                    config.decodeAudio == AUDIO_FORMAT_TYPE.AUDIO_FORMAT_PCM_FRAME_TYPE ||
                    config.decodeAudio == AUDIO_FORMAT_TYPE.AUDIO_FORMAT_MIXED_PCM_FRAME_TYPE) {
                    String audioPath;
                    if (config.decodeAudio == AUDIO_FORMAT_TYPE.AUDIO_FORMAT_AAC_FRAME_TYPE) {
                        audioPath = path + ".aac";
                    } else {
                        audioPath = path + ".pcm";
                    }
                    createUserInfo(key, audioPath, audioChannels);
                }
            }

            if (!isAudio && !videoChannels.containsKey(key)) {
                if (config.decodeVideo == VIDEO_FORMAT_TYPE.VIDEO_FORMAT_YUV_FRAME_TYPE ||
                    config.decodeVideo == VIDEO_FORMAT_TYPE.VIDEO_FORMAT_ENCODED_FRAME_TYPE) {
                    String videoPath;
                    if (config.decodeVideo == VIDEO_FORMAT_TYPE.VIDEO_FORMAT_ENCODED_FRAME_TYPE) {
                        videoPath = path + ".h264";
                    } else {
                        videoPath = path + ".yuv";
                    }
                    createUserInfo(key, videoPath, videoChannels);
                }
            }
        }
    }

    private static void createUserInfo(String key, String path, HashMap<String, UserInfo> channels) {
        try {
            UserInfo info = new UserInfo();
            info.channel = new FileOutputStream(path, true);
            info.last_receive_time = System.currentTimeMillis();
            channels.put(key, info);
        } catch (FileNotFoundException e) {
            log.info("Can't find file : " + path);
        }
    }

    @Override
    public void onActiveSpeaker(long uid) {
        log.info("User:" + uid + "is speaking");
    }

    @Override
    public void onAudioVolumeIndication(AudioVolumeInfo[] infos) {
        if (infos.length == 0) {
            return;
        }

        for (AudioVolumeInfo info : infos) {
            log.info("User:" + info.uid + ", audio volume:" + info.volume);
        }
    }

    @Override
    public void audioFrameReceived(long uid, AudioFrame frame) {
        byte[] buf;
        long size;
        checkUser(uid, true);
        if (frame.type == AUDIO_FRAME_TYPE.AUDIO_FRAME_RAW_PCM) { // pcm
            buf = frame.pcm.pcmBuf;
            size = frame.pcm.pcmBufSize;
        } else { // aac
            buf = frame.aac.aacBuf;
            size = frame.aac.aacBufSize;
        }
        WriteBytesToFileClassic(uid, buf, size, true);
    }

    @Override
    public void videoFrameReceived(long uid, int type, VideoFrame frame, int rotation) { // rotation:0,90,180,270
        byte[] buf = null;
        long size = 0;
        checkUser(uid, false);
        if (type == 0) { // yuv
            buf = frame.yuv.buf;
            size = frame.yuv.bufSize;
            if (buf == null) {
                log.info("java demo videoFrameReceived null");
            }
        } else if (type == 1) { // h264
            buf = frame.h264.buf;
            size = frame.h264.bufSize;
        } else if (type == 2) { // jpg
            String path = storageDir + uid + System.currentTimeMillis() + ".jpg";
            buf = frame.jpg.buf;
            size = frame.jpg.bufSize;
            try {
                FileOutputStream channel = new FileOutputStream(path, true);
                channel.write(buf, 0, (int) size);
                channel.close();
            } catch (Exception e) {
                log.error("Error write to " + path, e);
            }
        }
        WriteBytesToFileClassic(uid, buf, size, false);
    }

    /*
     * Brief: Callback when call createChannel successfully
     *
     * @param path recording file directory
     */
    @Override
    public void recordingPathCallBack(String path) {
        storageDir = path;
    }

    private int SetVideoMixingLayout() {
        Common ei = new Common();
        Common.VideoMixingLayout layout = ei.new VideoMixingLayout();
        layout.keepLastFrame = keepLastFrame;
        int max_peers = profile_type == CHANNEL_PROFILE_TYPE.CHANNEL_PROFILE_COMMUNICATION ? 7 : 17;
        if (m_peers.size() > max_peers) {
            log.info("peers size is bigger than max m_peers:" + m_peers.size());
            return -1;
        }

        if (!IsMixMode()) {
            return -1;
        }

        long maxuid;
        if (userAccount.length() > 0) {
            maxuid = RecordingSDKInstance.getUidByUserAccount(maxResolutionUserAccount);
        } else {
            maxuid = maxResolutionUid;
        }

        Vector<Long> videoUids = new Vector<>();
        for (Long uid : m_peers) {
            if (!config.autoSubscribe && !subscribedVideoUids.contains(uid)) {
                continue;
            }
            if (layoutMode == VERTICALPRESENTATION_LAYOUT) {
                String uc = RecordingSDKInstance.getUserAccountByUid((int) (long) uid);
                if (uc.length() > 0 || maxuid != 0) {
                    videoUids.add(uid);
                }
            } else {
                videoUids.add(uid);
            }
        }

        layout.canvasHeight = height;
        layout.canvasWidth = width;
        layout.backgroundColor = "#23b9dc";
        layout.regionCount = videoUids.size();

        if (!videoUids.isEmpty()) {
            log.info("java setVideoMixingLayout videoUids is not empty, start layout");
            Common.VideoMixingLayout.Region[] regionList = new Common.VideoMixingLayout.Region[videoUids.size()];
            log.info("mixing layout mode:" + layoutMode);
            if (layoutMode == BESTFIT_LAYOUT) {
                adjustBestFitVideoLayout(regionList, layout, videoUids);
            } else if (layoutMode == VERTICALPRESENTATION_LAYOUT) {
                adjustVerticalPresentationLayout(maxuid, regionList, layout, videoUids);
            } else {
                adjustDefaultVideoLayout(regionList, layout, videoUids);
            }

            layout.regions = regionList;

        } else {
            layout.regions = null;
        }
        return RecordingSDKInstance.setVideoMixingLayout(layout);
    }

    private void adjustVerticalPresentationLayout(long maxResolutionUid, Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        log.info("begin adjust vertical presentation layout,peers size:" + m_peers.size() + ", maxResolutionUid:" + maxResolutionUid);
        if (m_peers.size()<= 5) {
            adjustVideo5Layout(maxResolutionUid, regionList, layout, videoUids);
        } else if (m_peers.size()<= 7) {
            adjustVideo7Layout(maxResolutionUid, regionList, layout, videoUids);
        } else if (m_peers.size()<= 9) {
            adjustVideo9Layout(maxResolutionUid, regionList, layout, videoUids);
        } else {
            adjustVideo17Layout(maxResolutionUid, regionList, layout, videoUids);
        }
    }

    private void adjustBestFitVideoLayout(Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        if (m_peers.size() == 1) {
            adjustBestFitLayout_Square(regionList, 1, layout, videoUids);
        } else if (m_peers.size() == 2) {
            adjustBestFitLayout_2(regionList, layout, videoUids);
        } else if (2<m_peers.size() && m_peers.size()<= 4) {
            adjustBestFitLayout_Square(regionList, 2, layout, videoUids);
        } else if (5<= m_peers.size() && m_peers.size()<= 9) {
            adjustBestFitLayout_Square(regionList, 3, layout, videoUids);
        } else if (10<= m_peers.size() && m_peers.size()<= 16) {
            adjustBestFitLayout_Square(regionList, 4, layout, videoUids);
        } else if (m_peers.size() == 17) {
            adjustBestFitLayout_17(regionList, layout, videoUids);
        } else {
            log.info("adjustBestFitVideoLayout is more than 17 users");
        }
    }

    private void adjustBestFitLayout_2(Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        int peersCount = videoUids.size();
        for (int i = 0; i<peersCount; i++) {
            regionList[i] = layout.new Region();
            regionList[i].uid = videoUids.get(i);
            regionList[i].x = (i + 1) % 2 == 0 ? 0 : 0.5;
            regionList[i].y = 0.f;
            regionList[i].width = 0.5f;
            regionList[i].height = 1.f;
            regionList[i].alpha = i + 1;
            regionList[i].renderMode = 0;
        }
    }
    private void adjustDefaultVideoLayout(Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        regionList[0] = layout.new Region();
        regionList[0].uid = videoUids.get(0);
        regionList[0].x = 0.f;
        regionList[0].y = 0.f;
        regionList[0].width = 1.f;
        regionList[0].height = 1.f;
        regionList[0].alpha = 1.f;
        regionList[0].renderMode = 0;
        float f_width = width;
        float viewWidth = 0.235f;
        float viewHEdge = 0.012f;
        float viewHeight = viewWidth * (f_width / height);
        float viewVEdge = viewHEdge * (f_width / height);
        for (int i = 1; i<videoUids.size(); i++) {
            regionList[i] = layout.new Region();

            regionList[i].uid = videoUids.get(i);
            float f_x = (i - 1) % 4;
            regionList[i].x = f_x * (viewWidth + viewHEdge) + viewHEdge;
            regionList[i].y = 1 - ((float) (i - 1) / 4 + 1) * (viewHeight + viewVEdge);
            regionList[i].width = viewWidth;
            regionList[i].height = viewHeight;
            regionList[i].alpha = i + 1;
            regionList[i].renderMode = 0;
        }
        layout.regions = regionList;
    }

    private void setMaxResolutionUid(int number, long maxResolutionUid, Common.VideoMixingLayout.Region[] regionList, double weight_ratio) {
        regionList[number].uid = maxResolutionUid;
        regionList[number].x = 0.f;
        regionList[number].y = 0.f;
        regionList[number].width = 1.f * weight_ratio;
        regionList[number].height = 1.f;
        regionList[number].alpha = 1.f;
        regionList[number].renderMode = 1;
    }
    private void changeToVideo7Layout(long maxResolutionUid, Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        log.info("changeToVideo7Layout");
        adjustVideo7Layout(maxResolutionUid, regionList, layout, videoUids);
    }
    private void changeToVideo9Layout(long maxResolutionUid, Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        log.info("changeToVideo9Layout");
        adjustVideo9Layout(maxResolutionUid, regionList, layout, videoUids);
    }
    private void changeToVideo17Layout(long maxResolutionUid, Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        log.info("changeToVideo17Layout");
        adjustVideo17Layout(maxResolutionUid, regionList, layout, videoUids);
    }
    private void adjustBestFitLayout_Square(Common.VideoMixingLayout.Region[] regionList, int nSquare, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        float viewWidth = (float)(1.f * 1.0 / nSquare);
        float viewHEdge = (float)(1.f * 1.0 / nSquare);
        int peersCount = videoUids.size();
        for (int i = 0; i<peersCount; i++) {
            regionList[i] = layout.new Region();
            float xIndex = i % nSquare;
            float yIndex = (float) i / nSquare;
            regionList[i].uid = videoUids.get(i);
            regionList[i].x = 1.f * 1.0 / nSquare * xIndex;
            regionList[i].y = 1.f * 1.0 / nSquare * yIndex;
            regionList[i].width = viewWidth;
            regionList[i].height = viewHEdge;
            regionList[i].alpha = i + 1;
            regionList[i].renderMode = 0;
        }
    }
    private void adjustBestFitLayout_17(Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        int n = 5;
        float viewWidth = (float)(1.f * 1.0 / n);
        float viewHEdge = (float)(1.f * 1.0 / n);
        int peersCount = videoUids.size();
        for (int i = 0; i<peersCount; i++) {
            regionList[i] = layout.new Region();
            float xIndex = i % (n - 1);
            float yIndex = (float) i / (n - 1);
            regionList[i].uid = videoUids.get(i);
            regionList[i].width = viewWidth;
            regionList[i].height = viewHEdge;
            regionList[i].alpha = i + 1;
            regionList[i].renderMode = 0;
            if (i == 16) {
                regionList[16].x = (1 - viewWidth) * (1.f / 2) * 1.f;
                log.info("special layout for 17 x is:" + regionList[16].x);
            } else {
                regionList[i].x = 0.5f * viewWidth + viewWidth * xIndex;
            }
            regionList[i].y = 1.0 / n * yIndex;
        }
    }
    private void adjustVideo5Layout(long maxResolutionUid, Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        boolean flag = false;

        int number = 0;

        int i = 0;
        for (; i<videoUids.size(); i++) {
            regionList[i] = layout.new Region();
            if (maxResolutionUid == videoUids.get(i)) {
                log.info("adjustVideo5Layout equal with configured user uid:" + maxResolutionUid);
                flag = true;
                setMaxResolutionUid(number, maxResolutionUid, regionList, 0.8);
                number++;
                continue;
            }
            regionList[number].uid = videoUids.get(i);
            //float xIndex = ;
            float yIndex = flag ? number - 1 % 4 : number % 4;
            regionList[number].x = 1.f * 0.8;
            regionList[number].y = 0.25 * yIndex;
            regionList[number].width = 1.f * (1 - 0.8);
            regionList[number].height = 1.f * 0.25;
            regionList[number].alpha = number;
            regionList[number].renderMode = 0;
            number++;
            if (i == 4 && !flag) {
                changeToVideo7Layout(maxResolutionUid, regionList, layout, videoUids);
            }
        }
    }

    private void adjustVideo7Layout(long maxResolutionUid, Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        boolean flag = false;
        int number = 0;

        int i = 0;
        for (; i<videoUids.size(); i++) {
            regionList[i] = layout.new Region();
            if (maxResolutionUid == videoUids.get(i)) {
                log.info("adjustVideo7Layout equal with configured user uid:" + maxResolutionUid);
                flag = true;
                setMaxResolutionUid(number, maxResolutionUid, regionList, 6.f / 7);
                number++;
                continue;
            }
            regionList[number].uid = videoUids.get(i);
            float yIndex = flag ? (float) number - 1 % 6 : number % 6;
            regionList[number].x = 6.f / 7;
            regionList[number].y = 1.f / 6 * yIndex;
            regionList[number].width = 1.f / 7;
            regionList[number].height = 1.f / 6;
            regionList[number].alpha = number;
            regionList[number].renderMode = 0;
            number++;
            if (i == 6 && !flag) {
                changeToVideo9Layout(maxResolutionUid, regionList, layout, videoUids);
            }
        }

    }
    private void adjustVideo9Layout(long maxResolutionUid, Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        boolean flag = false;

        int number = 0;

        int i = 0;
        for (; i<videoUids.size(); i++) {
            regionList[i] = layout.new Region();
            if (maxResolutionUid == videoUids.get(i)) {
                log.info("adjustVideo9Layout equal with configured user uid:" + maxResolutionUid);
                flag = true;
                setMaxResolutionUid(number, maxResolutionUid, regionList, 9.f / 5);
                number++;
                continue;
            }
            regionList[number].uid = videoUids.get(i);
            float yIndex = flag ? number - 1 % 8 : number % 8;
            regionList[number].x = 8.f / 9;
            regionList[number].y = 1.f / 8 * yIndex;
            regionList[number].width = 1.f / 9;
            regionList[number].height = 1.f / 8;
            regionList[number].alpha = number;
            regionList[number].renderMode = 0;
            number++;
            if (i == 8 && !flag) {
                changeToVideo17Layout(maxResolutionUid, regionList, layout, videoUids);
            }
        }
    }

    private void adjustVideo17Layout(long maxResolutionUid, Common.VideoMixingLayout.Region[] regionList, Common.VideoMixingLayout layout, Vector<Long> videoUids) {
        boolean flag = false;

        int number = 0;
        log.info("adjustVideo17Layoutenter videoUids size is:" + videoUids.size() + ", maxResolutionUid:" + maxResolutionUid);
        for (int i = 0; i<videoUids.size(); i++) {
            regionList[i] = layout.new Region();
            if (maxResolutionUid == videoUids.get(i)) {
                flag = true;
                setMaxResolutionUid(number, maxResolutionUid, regionList, 0.8);
                number++;
                continue;
            }
            if (!flag && i == 16) {
                log.info("Not the configured uid, and small regions is sixteen, so ignore this user:" + videoUids.get(16));
                break;
            }

            regionList[number].uid = videoUids.get(i);
            //float xIndex = 0.833f;
            float yIndex = flag ? (number - 1) % 8 : number % 8;
            regionList[number].x = flag && i > 8 || !flag && i >= 8 ? 9.f / 10 : 8.f / 10;
            regionList[number].y = 1.f / 8 * yIndex;
            regionList[number].width = 1.f / 10;
            regionList[number].height = 1.f / 8;
            regionList[number].alpha = number;
            regionList[number].renderMode = 0;
            number++;
        }
    }

    private void WriteBytesToFileClassic(long uid, byte[] byteBuffer, long size, boolean isAudio) {
        if (byteBuffer == null) {
            log.info("WriteBytesToFileClassic but byte buffer is null!");
            return;
        }

        synchronized(this) {
            try {
                UserInfo info = isAudio ? audioChannels.get(Long.toString(uid)) : videoChannels.get(Long.toString(uid));
                if (info != null) {
                    info.channel.write(byteBuffer, 0, (int) size);
                    info.channel.flush();
                    info.last_receive_time = System.currentTimeMillis();
                } else {
                    log.info("Channel is null");
                }
            } catch (IOException ignored) {
            }
        }
    }


    private static boolean checkEnumValue(int val, int max, String msg) {
        if (val<0 || val > max) {
            System.out.println(msg);
            return false;
        }
        return true;
    }

    @SuppressWarnings("DuplicatedCode")
    public void createChannel(String[] args) {
        int uid = 0;
        String appId;
        String channelKey = "";
        String name;
        int channelProfile = 0;

        String decryptionMode = "";
        String secret = "";
        String mixResolution = "360,640,15,500";

        int idleLimitSec = 5 * 60; // 300s

        String applitePath;
        String recordFileRootDir = "";
        String cfgFilePath = "";
        int proxyType = 1;
        String proxyServer = "";
        String defaultVideoBgPath = "";
        String defaultUserBgPath = "";
        String subscribeVideoUids = "";
        String subscribeAudioUids = "";

        int lowUdpPort = 0; // 40000;
        int highUdpPort = 0; // 40004;

        boolean isAudioOnly = false;
        boolean isVideoOnly = false;
        boolean isMixingEnabled = false;
        boolean autoSubscribe = true;
        boolean enableCloudProxy = false;
        int mixedVideoAudio = MIXED_AV_CODEC_TYPE.MIXED_AV_DEFAULT.ordinal();

        int getAudioFrame = AUDIO_FORMAT_TYPE.AUDIO_FORMAT_DEFAULT_TYPE.ordinal();
        int getVideoFrame = VIDEO_FORMAT_TYPE.VIDEO_FORMAT_DEFAULT_TYPE.ordinal();
        int streamType = REMOTE_VIDEO_STREAM_TYPE.REMOTE_VIDEO_STREAM_HIGH.ordinal();
        int captureInterval = 5;
        int triggerMode = 0;

        int audioIndicationInterval = 0;
        int logLevel = 5;

        int audioProfile = 0;

        // paser command line parameters
        if (args.length % 2 != 0) {
            log.info("command line parameters error, should be '--key value' format!");
            return;
        }
        String key;
        String value;
        Map<String, String> map = new HashMap<>();
        if (0<args.length) {
            for (int i = 0; i<args.length - 1; i++) {
                key = args[i];
                value = args[i + 1];
                map.put(key, value);
            }
        }
        // prefer to use CmdLineParser or annotation
        Object Appid = map.get("--appId");
        Object Uid = map.get("--uid");
        Object UserAccount = map.get("--userAccount");
        Object Channel = map.get("--channel");
        Object AppliteDir = map.get("--appliteDir");
        Object ChannelKey = map.get("--channelKey");
        Object ChannelProfile = map.get("--channelProfile");
        Object IsAudioOnly = map.get("--isAudioOnly");
        Object IsVideoOnly = map.get("--isVideoOnly");
        Object IsMixingEnabled = map.get("--isMixingEnabled");
        Object MixResolution = map.get("--mixResolution");
        Object MixedVideoAudio = map.get("--mixedVideoAudio");
        Object DecryptionMode = map.get("--decryptionMode");
        Object Secret = map.get("--secret");
        Object Idle = map.get("--idle");
        Object RecordFileRootDir = map.get("--recordFileRootDir");
        Object LowUdpPort = map.get("--lowUdpPort");
        Object HighUdpPort = map.get("--highUdpPort");
        Object GetAudioFrame = map.get("--getAudioFrame");
        Object GetVideoFrame = map.get("--getVideoFrame");
        Object CaptureInterval = map.get("--captureInterval");
        Object CfgFilePath = map.get("--cfgFilePath");
        Object StreamType = map.get("--streamType");
        Object TriggerMode = map.get("--triggerMode");
        Object ProxyType = map.get("--proxyType");
        Object ProxyServer = map.get("--proxyServer");
        Object AudioProfile = map.get("--audioProfile");
        Object DefaultVideoBg = map.get("--defaultVideoBg");
        Object DefaultUserBg = map.get("--defaultUserBg");
        Object LogLevel = map.get("--logLevel");
        Object AudioIndicationInterval = map.get("--audioIndicationInterval");
        Object LayoutMode = map.get("--layoutMode");
        Object MaxResolutionUid = map.get("--maxResolutionUid");
        Object MaxResolutionUserAccount = map.get("--maxResolutionUserAccount");
        Object AutoSubscribe = map.get("--autoSubscribe");
        Object EnableCloudProxy = map.get("--enableCloudProxy");
        Object SubscribeVideoUids = map.get("--subscribeVideoUids");
        Object SubscribeAudioUids = map.get("--subscribeAudioUids");
        Object KeepLastFrame = map.get("--keepLastFrame");

        if (Appid == null || Uid == null && UserAccount == null || Channel == null || AppliteDir == null) {
            // print usage
            String usage = "java RecordingSDK --appId STRING --uid UINTEGER32 --userAccount STRING --channel STRING --appliteDir STRING --channelKey STRING --channelProfile UINTEGER32 --isAudioOnly --isVideoOnly --isMixingEnabled --mixResolution STRING --mixedVideoAudio --decryptionMode STRING --secret STRING --idle INTEGER32 --recordFileRootDir STRING --lowUdpPort INTEGER32 --highUdpPort INTEGER32 --getAudioFrame UINTEGER32 --getVideoFrame UINTEGER32 --captureInterval INTEGER32 --cfgFilePath STRING --streamType UINTEGER32 --triggerMode INTEGER32 \r\n --appId     (App Id/must) \r\n --uid     (User Id default is 0/ one of uid and user account is must)  \r\n --channel     (Channel Id/must) \r\n --appliteDir     (directory of app lite 'AgoraCoreService', Must pointer to 'Agora_Server_SDK_for_Linux_FULL/bin/' folder/must) \r\n --channelKey     (channelKey/option)\r\n --channelProfile     (channel_profile:(0:COMMUNICATION),(1:broadcast) default is 0/option)  \r\n --isAudioOnly     (Default 0:A/V, 1:AudioOnly (0:1)/option) \r\n --isVideoOnly     (Default 0:A/V, 1:VideoOnly (0:1)/option)\r\n --isMixingEnabled     (Mixing Enable? (0:1)/option)\r\n --mixResolution     (change default resolution for vdieo mix mode/option)                 \r\n --mixedVideoAudio     (mixVideoAudio:(0:seperated Audio,Video) (1:mixed Audio & Video with legacy codec) (2:mixed Audio & Video with new codec) default is 0 /option)                 \r\n --decryptionMode     (decryption Mode, default is NULL/option)                 \r\n --secret     (input secret when enable decryptionMode/option)                 \r\n --idle     (Default 300s, should be above 3s/option)                 \r\n --recordFileRootDir     (recording file root dir/option)                 \r\n --lowUdpPort     (default is random value/option)                 \r\n --highUdpPort     (default is random value/option)                 \r\n --getAudioFrame     (default 0 (0:save as file, 1:aac frame, 2:pcm frame, 3:mixed pcm frame) (Can't combine with isMixingEnabled) /option)                 \r\n --getVideoFrame     (default 0 (0:save as file, 1:h.264, 2:yuv, 3:jpg buffer, 4:jpg file, 5:jpg file and video file) (Can't combine with isMixingEnabled) /option)              \r\n --captureInterval     (default 5 (Video snapshot interval (second)))                 \r\n --cfgFilePath     (config file path / option)                 \r\n --streamType     (remote video stream type(0:STREAM_HIGH,1:STREAM_LOW), default is 0/option)  \r\n --triggerMode     (triggerMode:(0: automatically mode, 1: manually mode) default is 0/option) \r\n --proxyType    proxyType:proxyServer format type, 0:self socks5 proxy server, 1:cloud proxy domain, 2:proxy LBS server list. default is 1/option \r\n --proxyServer     proxyServer:format ip:port, eg,\"127.0.0.1:1080\"/option \r\n --defaultVideoBg    (default user background image path/option) \r\n --defaultUserBg (default user background image path/option))  \r\n --audioProfile (audio profile(0: standard single channel, 1: high quality single channel, 2: high quality two channels) defualt is 0/option)   \r\n --logLevel (log level default INFO/option) \r\n --audioIndicationInterval(0: no indication, audio indication interval(ms) default is 0/option) \r\n --layoutMode    (mix video layout mode:(0: default layout, 1:bestFit Layout mode, 2:vertical presentation Layout mode, default is 0/option)(combine with isMixingEnabled)) \r\n --maxResolutionUid    (max resolution uid (uid with maxest resolution under vertical presentation Layout mode  ( default is -1 /option))\r\n --maxResolutionUserAccount (max resolution user account (user account with maxest resolution under vertical presentation layout mode )) --keepLastFrame (Whether to keep user last frame when user's video stream stop(0: not keep, 1: keep, default 0/option) \r\n --autoSubscribe (Auto subscribe video/audio streams of each uid. (0: false 1:true, default 1/option)) \r\n --subscribeVideoUids (subcsribe video stream of specified uids. seperated with commas, like 1234,2345 /option) \r\n --subscribeAudioUids (subscribe audio stream of specified uids seperated by commos, like 1234,2345 /option) \r\n --enableCloudProxy (Enable cloud proxy or not (0: not enable, 1 : enable, default 0/option)";
            log.info("Usage:" + usage);
            return;
        }
        if (Uid != null) {
            uid = Integer.parseInt(String.valueOf(Uid));
        }

        if (UserAccount != null) {
            userAccount = String.valueOf(UserAccount);
        }

        appId = String.valueOf(Appid);
        name = String.valueOf(Channel);
        applitePath = String.valueOf(AppliteDir);

        if (ChannelKey != null) {
            channelKey = String.valueOf(ChannelKey);
        }
        if (ChannelProfile != null) {
            channelProfile = Integer.parseInt(String.valueOf(ChannelProfile));
        }
        if (!checkEnumValue(channelProfile, 1, "Invalid channel profile value :" + channelProfile)) {
            return;
        }
        if (DecryptionMode != null) {
            decryptionMode = String.valueOf(DecryptionMode);
        }
        if (Secret != null) {
            secret = String.valueOf(Secret);
        }
        if (MixResolution != null) {
            mixResolution = String.valueOf(MixResolution);
        }
        if (Idle != null) {
            idleLimitSec = Integer.parseInt(String.valueOf(Idle));
        }
        if (RecordFileRootDir != null) {
            recordFileRootDir = String.valueOf(RecordFileRootDir);
        }
        if (CfgFilePath != null) {
            cfgFilePath = String.valueOf(CfgFilePath);
        }
        if (LowUdpPort != null) {
            lowUdpPort = Integer.parseInt(String.valueOf(LowUdpPort));
        }
        if (HighUdpPort != null) {
            highUdpPort = Integer.parseInt(String.valueOf(HighUdpPort));
        }
        if (IsAudioOnly != null && Integer.parseInt(String.valueOf(IsAudioOnly)) == 1) {
            isAudioOnly = true;
        }
        if (IsVideoOnly != null && Integer.parseInt(String.valueOf(IsVideoOnly)) == 1) {
            isVideoOnly = true;
        }
        if (IsMixingEnabled != null && Integer.parseInt(String.valueOf(IsMixingEnabled)) == 1) {
            isMixingEnabled = true;
        }
        if (MixedVideoAudio != null) {
            mixedVideoAudio = Integer.parseInt(String.valueOf(MixedVideoAudio));
        }
        if (!checkEnumValue(mixedVideoAudio, 2, "Invalid mixedVideoAudio :" + mixedVideoAudio)) {
            return;
        }
        if (GetAudioFrame != null) {
            getAudioFrame = Integer.parseInt(String.valueOf(GetAudioFrame));
        }
        if (!checkEnumValue(getAudioFrame, 3, "Invalid getAudioFrame value : " + getAudioFrame)) {
            return;
        }
        if (GetVideoFrame != null) {
            getVideoFrame = Integer.parseInt(String.valueOf(GetVideoFrame));
        }
        if (!checkEnumValue(getVideoFrame, 5, "Invalid getVideoFrame value : " + getVideoFrame)) {
            return;
        }
        if (StreamType != null) {
            streamType = Integer.parseInt(String.valueOf(StreamType));
        }
        if (!checkEnumValue(streamType, 1, "Invalid streamType value : " + streamType)) {
            return;
        }
        if (KeepLastFrame != null) {
            keepLastFrame = Integer.parseInt(String.valueOf(KeepLastFrame));
        }
        if (CaptureInterval != null) {
            captureInterval = Integer.parseInt(String.valueOf(CaptureInterval));
        }
        if (AudioIndicationInterval != null) {
            audioIndicationInterval = Integer.parseInt(String.valueOf(AudioIndicationInterval));
        }
        if (TriggerMode != null) {
            triggerMode = Integer.parseInt(String.valueOf(TriggerMode));
        }
        if (ProxyType != null) {
            proxyType = Integer.parseInt(String.valueOf(ProxyType));
        }
        if (ProxyServer != null) {
            proxyServer = String.valueOf(ProxyServer);
        }
        if (AudioProfile != null) {
            audioProfile = Integer.parseInt(String.valueOf(AudioProfile));
        }
        if (DefaultVideoBg != null) {
            defaultVideoBgPath = String.valueOf(DefaultVideoBg);
        }
        if (DefaultUserBg != null) {
            defaultUserBgPath = String.valueOf(DefaultUserBg);
        }
        if (LogLevel != null) {
            logLevel = Integer.parseInt(String.valueOf(LogLevel));
        }
        if (LayoutMode != null) {
            layoutMode = Integer.parseInt(String.valueOf(LayoutMode));
        }
        if (MaxResolutionUid != null) {
            maxResolutionUid = Long.parseLong(String.valueOf(MaxResolutionUid));
        }
        if (MaxResolutionUserAccount != null) {
            maxResolutionUserAccount = String.valueOf(MaxResolutionUserAccount);
        }

        if (userAccount.length() != 0 && maxResolutionUserAccount.length() == 0 && maxResolutionUid != -1 && layoutMode == 2) {
            log.info("maxResolutionUserAccount should be used when join channel with user account");
            return;
        }

        if (userAccount.length() == 0 && maxResolutionUid == 0 && maxResolutionUserAccount.length() != 0 && layoutMode == 2) {
            log.info("maxResolutionUid should be used when join channel with uid.");
            return;
        }

        if (EnableCloudProxy != null && Integer.parseInt(String.valueOf(EnableCloudProxy)) == 1) {
            enableCloudProxy = true;
        }

        if (AutoSubscribe != null && Integer.parseInt(String.valueOf(AutoSubscribe)) == 0) {
            autoSubscribe = false;
        }

        if (!autoSubscribe) {
            if (SubscribeVideoUids != null) {
                subscribeVideoUids = String.valueOf(SubscribeVideoUids);
                String[] struids = subscribeVideoUids.split(",");
                for (String struid : struids) {
                    if (userAccount.length() > 0) {
                        subscribeVideoUserAccount.add(struid);
                    } else {
                        try {
                            subscribedVideoUids.add(Long.parseLong(struid));
                        } catch (Exception e) {
                            //Ignore exception here.
                        }
                    }
                }
            }
            if (SubscribeAudioUids != null) {
                subscribeAudioUids = String.valueOf(SubscribeAudioUids);
            }
        }

        if (audioProfile > 2) {
            audioProfile = 2;
        }
        if (audioProfile<0) {
            audioProfile = 0;
        }

        RecordingConfig config = new RecordingConfig();
        config.channelProfile = CHANNEL_PROFILE_TYPE.values()[channelProfile];
        config.idleLimitSec = idleLimitSec;
        config.isVideoOnly = isVideoOnly;
        config.isAudioOnly = isAudioOnly;
        config.isMixingEnabled = isMixingEnabled;
        config.mixResolution = mixResolution;
        config.mixedVideoAudio = MIXED_AV_CODEC_TYPE.values()[mixedVideoAudio];
        config.appliteDir = applitePath;
        config.recordFileRootDir = recordFileRootDir;
        config.cfgFilePath = cfgFilePath;
        config.secret = secret;
        config.decryptionMode = decryptionMode;
        config.lowUdpPort = lowUdpPort;
        config.highUdpPort = highUdpPort;
        config.captureInterval = captureInterval;
        config.audioIndicationInterval = audioIndicationInterval;
        config.decodeAudio = AUDIO_FORMAT_TYPE.values()[getAudioFrame];
        config.decodeVideo = VIDEO_FORMAT_TYPE.values()[getVideoFrame];
        config.streamType = REMOTE_VIDEO_STREAM_TYPE.values()[streamType];
        config.triggerMode = triggerMode;
        config.proxyType = proxyType;
        config.proxyServer = proxyServer;
        config.audioProfile = audioProfile;
        config.defaultVideoBgPath = defaultVideoBgPath;
        config.defaultUserBgPath = defaultUserBgPath;
        config.enableCloudProxy = enableCloudProxy;
        config.autoSubscribe = autoSubscribe;
        config.subscribeVideoUids = subscribeVideoUids;
        config.subscribeAudioUids = subscribeAudioUids;

        if (config.decodeVideo == VIDEO_FORMAT_TYPE.VIDEO_FORMAT_ENCODED_FRAME_TYPE) {
            config.decodeVideo = VIDEO_FORMAT_TYPE.VIDEO_FORMAT_H264_FRAME_TYPE;
        }

        this.config = config;

        /*
         * change log_config Facility per your specific purpose like
         * agora::base::LOCAL5_LOG_FCLT Default:USER_LOG_FCLT.
         *
         * ars.setFacility(LOCAL5_LOG_FCLT);
         */

        if (logLevel < 1) {
            logLevel = 1;
        }
        if (logLevel > 6) {
            logLevel = 6;
        }

        isMixMode = isMixingEnabled;
        profile_type = CHANNEL_PROFILE_TYPE.values()[channelProfile];
        if (isMixingEnabled && !isAudioOnly) {
            String[] sourceStrArray = mixResolution.split(",");
            if (sourceStrArray.length != 4) {
                System.out.println("Illegal resolution:" + mixResolution);
                return;
            }
            width = Integer.valueOf(sourceStrArray[0]).intValue();
            height = Integer.valueOf(sourceStrArray[1]).intValue();
        }
        // run jni event loop , or start a new thread to do it
        if (userAccount.length() > 0) {
            RecordingSDKInstance.createChannelWithUserAccount(appId, channelKey, name, userAccount, config, logLevel);
        } else {
            RecordingSDKInstance.createChannel(appId, channelKey, name, uid, config, logLevel);
        }
        cleanTimer.cancel();
        log.info("jni layer has been exited...");
        stopped.compareAndSet(false, true);
    }

    public boolean leaveChannel() {
        return RecordingSDKInstance.leaveChannel();
    }
    public int startService() {
        return RecordingSDKInstance.startService();
    }
    public int stopService() {
        return RecordingSDKInstance.stopService();
    }

    public RecordingEngineProperties getProperties() {
        return RecordingSDKInstance.getProperties();
    }

    @Override
    public void onReceivingStreamStatusChanged(boolean receivingAudio, boolean receivingVideo) {
        log.info("pre receiving audio status is " + m_receivingAudio + ", now receiving audio status is " + receivingAudio);
        log.info("pre receiving video status is " + m_receivingVideo + ", now receiving video  status is " + receivingVideo);
        m_receivingAudio = receivingAudio;
        m_receivingVideo = receivingVideo;
    }

    @Override
    public void onConnectionLost() {
        log.info("connection is lost");
    }

    @Override
    public void onConnectionInterrupted() {
        log.info("connection is interrupted");
    }

    @Override
    public void onFirstRemoteAudioFrame(long uid, int elapsed) {
        log.info("onFirstRemoteAudioFrame User:" + uid + ", elapsed:" + elapsed);
    }

    @Override
    public void onFirstRemoteVideoDecoded(long uid, int width, int height, int elapsed) {
        log.info("onFirstRemoteVideoDecoded User:" + uid + ", width:" + width +
                           ", height:" + height + ", elapsed:" + elapsed);
    }
}