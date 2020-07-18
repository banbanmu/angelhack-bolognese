package gelato.riso.recorder;

public class RecordingWorkerThread extends Thread {
    private final RecordingSampleM ars;
    private final String[] args;

    RecordingWorkerThread(RecordingSampleM ars, String[] args) {
        this.ars = ars;
        this.args = args.clone();
    }

    @Override
    public void run() {
        ars.createChannel(args);
    }
}
