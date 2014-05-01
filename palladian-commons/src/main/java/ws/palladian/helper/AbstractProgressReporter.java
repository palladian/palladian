package ws.palladian.helper;

public abstract class AbstractProgressReporter implements ProgressReporter {

    @Override
    public final void increment() {
        increment(1);
    }

    @Override
    public final ProgressReporter createSubProgress(double percentage) {
        return new SubProgressReporter(this, percentage);
    }

}
