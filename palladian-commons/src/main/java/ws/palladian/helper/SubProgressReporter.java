package ws.palladian.helper;

import org.apache.commons.lang3.Validate;

public final class SubProgressReporter implements ProgressReporter {

    private final ProgressReporter parent;

    private final double percentageFraction;

    private long totalSteps;

    private long steps;

    SubProgressReporter(ProgressReporter progress, double percentage) {
        Validate.notNull(progress, "progress must not be null");
        Validate.isTrue(0 <= percentage && percentage <= 1, "percentage must be in range [0,1]");
        this.parent = progress;
        this.percentageFraction = percentage;
        this.totalSteps = -1;
    }

    @Override
    public void startTask(String description, long totalSteps) {
        this.totalSteps = totalSteps;
    }

    @Override
    public void increment() {
        increment(1);
    }

    @Override
    public void increment(long count) {
        steps += count;
        add(totalSteps > 0 ? (double)count / totalSteps : 1);
    }

    @Override
    public void add(double progress) {
        parent.add(progress * percentageFraction);
    }

    @Override
    public void finishTask() {
        if (totalSteps <= 0 || steps < totalSteps) {
            // add the remaining steps
            increment(totalSteps - steps);
        }
    }

    @Override
    public double getProgress() {
        return (double)steps / totalSteps;
    }

    @Override
    public ProgressReporter createSubProgress(double percentage) {
        return new SubProgressReporter(this, percentage);
    }

}
