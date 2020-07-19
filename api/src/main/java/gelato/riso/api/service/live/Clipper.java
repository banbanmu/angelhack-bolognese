package gelato.riso.api.service.live;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import gelato.riso.api.service.live.LiveHandler.LiveStop.CookClipInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Clipper extends Thread {

    private static final String ROOT_PATH = "./recording/";
    private static final FFmpeg FFMPEG;
    private static final FFmpegExecutor FFMPEG_EXECUTOR;
    private static final String COMPLEX_FILTER_FORMAT = "fade=in:st=%f:d=1, fade=out:st=%f:d=1; afade=in:st=%f:d=1, afade=out:st=%f:d=1";
    private static final AmazonS3 S3 = AmazonS3ClientBuilder.standard().withRegion(Regions.AP_NORTHEAST_2).build();

    static {
        try {
            FFMPEG = new FFmpeg("/usr/bin/ffmpeg");
            FFMPEG_EXECUTOR = new FFmpegExecutor(FFMPEG);
        } catch (IOException e) {
            throw new RuntimeException("FFMPEG init failed", e);
        }
    }

    private final String startDate;
    private final String userId;
    private final String clipDirPath;
    private final List<CookClipInfo> clipInfos;
    private final String bucketName;

    private Clipper(String startDate, Integer userId, List<CookClipInfo> clipInfos, String bucketName) {
        this.startDate = startDate;
        this.userId = Integer.toString(userId);
        clipDirPath = ROOT_PATH + userId + "_clip";
        this.clipInfos = new ArrayList<>(clipInfos);
        this.bucketName = bucketName;

        log.info("clipDirPath: {}", clipDirPath);
    }

    public static void start(Long startTimestamp, Integer userId, List<CookClipInfo> clipInfos, String bucketName) {
        Clipper clipper = new Clipper(new SimpleDateFormat("yyyyMMdd").format(new Date(startTimestamp)),
                                      userId, clipInfos, bucketName);
        log.info("START. userId: {}, clipInfos: {}", userId, clipInfos);
        clipper.start();
    }

    @Override
    @SneakyThrows
    public void run() {
        File dateDir = new File(ROOT_PATH + startDate);
        log.info("DATE_DIR : {}", dateDir.getAbsolutePath());
        File clipDir = Arrays
                .stream(dateDir.listFiles())
                .filter(File::isDirectory)
                .filter(dir -> dir.getName().startsWith("channel_" + userId))
                .findFirst()
                .map(dir -> {
                    File dest = new File(clipDirPath);
                    if (dir.renameTo(dest)) {
                        return dest;
                    }
                    throw new RuntimeException(String.format("Rename fail. userId : %s", userId));
                }).orElseThrow(() -> new RuntimeException(String.format("Clipping fail. userId : %s", userId)));

        log.info("clipDir is {}", clipDir.getAbsolutePath());
        String video = null;
        String audio = null;
        for (File file : clipDir.listFiles()) {
            String fileName = file.getName();
            if (fileName.startsWith(userId)) {
                if (fileName.endsWith(".mp4")) {
                    video = file.getAbsolutePath();
                } else if (fileName.endsWith(".aac")) {
                    audio = file.getAbsolutePath();
                }
            }
        }

        if (video == null || audio == null) {
            log.info("There is no video or audio file");
            return;
        }

        String merge = clipDirPath + "/merge.mp4";

        log.info(">>> Start merge. video: {}, audio: {}", video, audio);
        FFMPEG_EXECUTOR.createJob(mergeFFmpegBuilder(video, audio, merge)).run();
        log.info("<<< Finish merge. video: {}, audio: {}", video, audio);


        log.info(">>> Start clip. video: {}, audio: {}", video, audio);
        for (CookClipInfo clipInfo : clipInfos) {
            String cut = clipDirPath + '/' + userId + '_' + clipInfo.getName() + ".mp4";
            FFMPEG_EXECUTOR.createJob(clipFFmpegBuilder(clipInfo, merge, cut)).run();

            String s3KeyName = userId + '_' + clipInfo.getName() + ".mp4";
            log.info(">>> Start upload S3. cut: {}, key: {}", cut, s3KeyName);
            S3.putObject(buildPutObjectRequest(bucketName, s3KeyName, new File(cut)));
            log.info("<<< Finish upload S3. cut: {}, key: {}", cut, s3KeyName);
        }
        log.info("<<< Finish clip. video: {}, audio: {}", video, audio);

        for (File f : clipDir.listFiles()) {
            f.delete();
        }
        clipDir.delete();
    }

    private static FFmpegBuilder mergeFFmpegBuilder(String video, String audio, String output) {
        return new FFmpegBuilder()
                .addInput(video)
                .addInput(audio)
                .overrideOutputFiles(true)
                .addOutput(output)
                .addExtraArgs("-shortest")
                .setAudioCodec("aac")
                .setVideoCodec("copy")
                .done();
    }

    private static FFmpegBuilder clipFFmpegBuilder(CookClipInfo clipInfo, String merge, String cut) {

        long startOffset = clipInfo.getStartMilli() - 1000;
        long duration = clipInfo.getDurationMilli() + 2000;
        if (startOffset < 0) {
            startOffset = 0;
        }

        float fadeInStart = startOffset / 1000f;
        float fadeOutStart = (startOffset + duration - 1000) / 1000f;

        log.info("Clipping, startOffset: {}, duration: {}, fadeInStart: {}, fadeOutStart: {}",
                 startOffset, duration, fadeInStart, fadeOutStart);

        return new FFmpegBuilder()
                .setInput(merge)
                .overrideOutputFiles(true)
                .setComplexFilter(String.format(COMPLEX_FILTER_FORMAT, fadeInStart, fadeOutStart, fadeInStart, fadeOutStart))
                .addOutput(cut)
                .setStartOffset(startOffset, TimeUnit.MILLISECONDS)
                .setDuration(duration, TimeUnit.MILLISECONDS)
                .setAudioCodec("aac")
                .setVideoCodec("libx264")
                .done();
    }

    private static PutObjectRequest buildPutObjectRequest(String bucketName, String s3KeyName, File cut) {
        return new PutObjectRequest(bucketName, s3KeyName, cut)
                .withCannedAcl(CannedAccessControlList.PublicRead);
    }

}
