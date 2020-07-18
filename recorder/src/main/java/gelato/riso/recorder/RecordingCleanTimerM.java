package gelato.riso.recorder;

import java.util.TimerTask;

class RecordingCleanTimerM extends TimerTask {
    private final RecordingSampleM rs;

    RecordingCleanTimerM(RecordingSampleM rs) {
        this.rs = rs;
    }

    @Override
    public void run() {
        rs.clean();
    }
}
