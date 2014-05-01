package ws.palladian.helper;

import org.apache.commons.lang3.Validate;

final class SubProgressReporter extends AbstractProgressReporter {

    private final ProgressReporter parent;

    private final double parentPercentage;

    private long totalSteps;

    private long steps;

    SubProgressReporter(ProgressReporter parent, double percentage) {
        Validate.notNull(parent, "parent must not be null");
        Validate.isTrue(0 <= percentage && percentage <= 1, "percentage must be in range [0,1]");
        this.parent = parent;
        this.parentPercentage = percentage;
        this.totalSteps = -1;
    }

    @Override
    public void startTask(String description, long totalSteps) {
        this.totalSteps = totalSteps;
    }

    @Override
    public void increment(long count) {
        steps += count;
        add(totalSteps > 0 ? (double)count / totalSteps : 1);
    }

    @Override
    public void add(double progress) {
        parent.add(progress * parentPercentage);
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

}
