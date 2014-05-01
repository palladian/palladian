package ws.palladian.helper;

public final class NoProgress implements ProgressReporter {

    public static final NoProgress INSTANCE = new NoProgress();

    private NoProgress() {
        // singleton
    }

    @Override
    public void startTask(String description, long steps) {
    }

    @Override
    public void increment() {
    }

    @Override
    public void increment(long steps) {
    }

    @Override
    public void add(double progress) {
    }

    @Override
    public void finishTask() {
    }

    @Override
    public double getProgress() {
        return -1;
    }

    @Override
    public ProgressReporter createSubProgress(double percentage) {
        return INSTANCE;
    }

}
