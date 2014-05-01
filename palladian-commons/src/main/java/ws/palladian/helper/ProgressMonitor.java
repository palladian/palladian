package ws.palladian.helper;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.date.DateHelper;

/**
 * <p>
 * The ProgressMonitor eases the progress visualization needed in many long-running processes. Usage example:
 * 
 * <pre>
 * ProgressMonitor pm = new ProgressMonitor(10);
 * for (int i = 0; i &lt; 10; i++) {
 *     performSophisticatedCalculations(i);
 *     pm.incrementAndPrintProgress();
 * }
 * </pre>
 * 
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class ProgressMonitor implements ProgressReporter {

    // private final static char PROGRESS_CHAR = '■';
    private final static char PROGRESS_CHAR = '=';
    
    private final double showEveryPercent;
    
    private final StopWatch stopWatch = new StopWatch();
    
    private String description;
    
    private double progress;
    
    private final AtomicLong currentCount = new AtomicLong(0);
    
    private long totalSteps = -1;
    
    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * 
     * @param showEveryPercent Step size for outputting the progress in range [0,100].
     */
    public ProgressMonitor(double showEveryPercent) {
        Validate.inclusiveBetween(0., 100., showEveryPercent, "showEveryPercent must be in range [0,100]");
        this.showEveryPercent = showEveryPercent;
    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor} which updates the status every one percent.
     */
    public ProgressMonitor() {
        this(1.);
    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor} showing the current progress with each percent.
     * </p>
     * 
     * @param totalCount The total iterations to perform, greater/equal zero.
     * @deprecated Use {@link #ProgressMonitor(double)} instead.
     */
    @Deprecated
    public ProgressMonitor(long totalCount) {
        this(totalCount, 1);
    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * </p>
     * 
     * @param totalCount The total iterations to perform, greater/equal zero.
     * @param showEveryPercent Step size for outputting the progress in range [0,100].
     * @deprecated Use {@link #ProgressMonitor(double)} instead.
     */
    @Deprecated
    public ProgressMonitor(long totalCount, double showEveryPercent) {
        this(totalCount, showEveryPercent, null);
    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * </p>
     * 
     * @param totalCount The total iterations to perform, greater/equal zero.
     * @param showEveryPercent Step size for outputting the progress in range [0,100].
     * @param processName The name of the process, for identification purposes when outputting the bar.
     * @deprecated Use {@link #ProgressMonitor(double)} instead.
     */
    @Deprecated
    public ProgressMonitor(long totalCount, double showEveryPercent, String processName) {
        Validate.isTrue(totalCount >= 0, "totalCount must be greater/equal zero");
        Validate.inclusiveBetween(0., 100., showEveryPercent, "showEveryPercent must be in range [0,100]");
        this.totalSteps = totalCount;
        this.showEveryPercent = showEveryPercent;
        this.description = processName;
    }


    private static String createProgressBar(double progress) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        int scaledPercent = (int)Math.round(progress * 50);
        stringBuilder.append(StringUtils.repeat(PROGRESS_CHAR, scaledPercent));
        stringBuilder.append(StringUtils.repeat(' ', Math.max(50 - scaledPercent, 0)));
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    /**
     * <p>
     * Increments the counter by one and prints the current progress to the System's standard output.
     * 
     * @deprecated Use {@link #increment()} instead.
     */
    @Deprecated
    public void incrementAndPrintProgress() {
        incrementByAndPrintProgress(1);
    }

    /**
     * <p>
     * Increments the counter by the step size and prints the current progress to the System's standard output.
     * 
     * @param steps The number of steps to increment the counter with.
     * @deprecated Use {@link #increment(long)} instead.
     */
    @Deprecated
    public void incrementByAndPrintProgress(long steps) {
        set((double)currentCount.addAndGet(steps) / totalSteps);
    }

    /**
     * Returns the current progress.
     * 
     * @param counter Counter for current iteration in a loop.
     */
    private String getProgress(long counter, double progress) {
        try {
            if (showEveryPercent == 0 || totalSteps < 0 || counter % (showEveryPercent * totalSteps / 100.0) < 1) {
                StringBuilder progressString = new StringBuilder();
                if (description != null) {
                    progressString.append(description).append(" ");
                }
                NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
//                if (totalSteps >= 0) {
                    progressString.append(createProgressBar(progress));
                    progressString.append(" ").append(format.format(100 * progress)).append("% (");
//                }
                if (counter > 0) {
                    progressString.append(totalSteps - counter).append(" remaining, ");
                }
                if (progress > 0) {
                    long msRemaining = (long)((1 - progress) * stopWatch.getTotalElapsedTime() / progress);
                    // if elapsed not possible (timer started long before progress helper used) =>
                    // long msRemaining = (long)((100 - percent) * stopWatch.getElapsedTime() / 10); => in case total
                    progressString.append("elapsed: ").append(stopWatch.getTotalElapsedTimeString());
                    progressString.append(", iteration: ").append(stopWatch.getElapsedTimeString());
                    if (counter < totalSteps) {
                        progressString.append(", ~remaining: ").append(DateHelper.formatDuration(0, msRemaining, true));
                    }
                    stopWatch.start();
                }
                progressString.append(")");
                return progressString.toString();
            }
        } catch (ArithmeticException e) {
    e.printStackTrace();
        } catch (Exception e) {
    e.printStackTrace();
        }
        return StringUtils.EMPTY;
    }

    public String getTotalElapsedTimeString() {
        return stopWatch.getTotalElapsedTimeString();
    }

    @Override
    public void startTask(String description, long totalSteps) {
        this.totalSteps = totalSteps;
        this.description = description;
    }

    @Override
    public void increment() {
        incrementAndPrintProgress();
    }

    @Override
    public void increment(long by) {
        incrementByAndPrintProgress(by);
    }

    private void set(double progress) {
        this.progress = progress;
        String progressString = getProgress(currentCount.get(), progress);
        if (!progressString.isEmpty()) {
            System.out.println(progressString);
        }
    }

    @Override
    public void add(double progress) {
        set(Math.min(this.progress + progress, 1));
    }

    @Override
    public void finishTask() {
        set(1);
    }
    
    @Override
    public double getProgress() {
        return progress;
    }

    @Override
    public ProgressReporter createSubProgress(double percentage) {
        return new SubProgressReporter(this, percentage);
    }

}
