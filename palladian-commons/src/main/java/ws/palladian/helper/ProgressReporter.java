package ws.palladian.helper;

public interface ProgressReporter {

    /**
     * Initialize the progress reporter for a specific task.
     * 
     * @param description A short description of the task.
     * @param totalSteps The number of steps which will be performed.
     */
    void startTask(String description, long totalSteps);

    /**
     * Increments the progress counter by one.
     */
    void increment();

    /**
     * Increments the counter by the step size.
     * 
     * @param steps The number of steps to increment the counter with.
     */
    void increment(long steps);

    /**
     * Increments the percentage by the given value.
     * 
     * @param progress The progress in range of [0,1].
     */
    void add(double progress);

    /**
     * Indicate, that the task has completed.
     */
    void finishTask();
    
    /**
     * @return The current progress in range [0,1].
     */
    double getProgress();

    /**
     * Create a child progress reporter.
     * 
     * @param percentage The percentage of work, which the child progress will contribute in range of [0,1].
     * @return A new progress reporter for a sub progress.
     */
    ProgressReporter createSubProgress(double percentage);

}
