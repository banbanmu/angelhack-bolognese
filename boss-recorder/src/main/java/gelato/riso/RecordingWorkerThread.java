package gelato.riso;

class RecordingWorkerThread extends Thread {
    private RecordingSampleM ars;
    private String[] args;

    public RecordingWorkerThread(RecordingSampleM ars, String[] args) {
        this.ars = ars;
        this.args = args;
    }

    @Override
    public void run() {
        ars.createChannel(args);
    }
}
