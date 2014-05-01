package ws.palladian.helper;

/**
 * A progress reporter is used for monitoring the progress of long-running computations. Therefore, an instance of this
 * interface is handed to the computation method.
 * 
 * @author pk
 * @see ProgressMonitor
 */
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
     * Create a child progress reporter. This is used in cases where a longer operation is broken up into individual
     * parts, and each part reports separately.
     * 
     * @param percentage The percentage of work, which the child progress will contribute; in range of [0,1].
     * @return A new progress reporter for a sub progress.
     */
    ProgressReporter createSubProgress(double percentage);

}
