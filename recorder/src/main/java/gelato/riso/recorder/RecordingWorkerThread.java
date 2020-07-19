package gelato.riso.recorder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class RecordingWorkerThread extends Thread {
    private final RecordingSampleM ars;
    private final String appId;
    private final String channelId;

    @Override
    public void run() {
        ars.createChannel(appId, channelId);
    }
}
