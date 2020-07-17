package gelato.riso;

import java.util.TimerTask;

class RecordingCleanTimerM extends TimerTask {
    RecordingSampleM rs;
    public RecordingCleanTimerM(RecordingSampleM rs) {
        this.rs = rs;
    }
    @Override
    public void run() {
        rs.clean();
    }
}
