package ws.palladian.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.StringOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * This class should provide convenience methods for interacting with the OS functionality.
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class ProcessHelper {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessHelper.class);

    /**
     * Default stuck window for {@link #waitForThreadPool(ExecutorService, StopWatch)}: a pool that completes no task at
     * all for this long has a wedged task and its remainder is abandoned. Generous on purpose — this must never fire on
     * a slow-but-working pool, only on one that has stopped making progress entirely.
     */
    public static final long DEFAULT_STUCK_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(10);

    /** Returned by {@code completedTaskCount} when the executor does not expose a completed-task counter. */
    private static final long PROGRESS_UNOBSERVABLE = -1;

    /**
     * How often the wait loop wakes up to re-check termination and progress. The loop never sleeps longer than the
     * stuck window itself, so a small window stays responsive instead of being rounded up to this interval.
     */
    private static final long POLL_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);

    private ProcessHelper() {
        // utility, no instances.
    }

    /**
     * <p>
     * Run a command on the console/terminal.
     * </p>
     *
     * @param consoleCommand The command to run.
     * @return The console output that was read after executing the command.
     */
    public static String runCommand(String consoleCommand) {
        StringBuilder result = new StringBuilder();

        StringOutputStream stringOutputStream = new StringOutputStream();
        Process p = null;
        InputStream in = null;
        try {
            p = Runtime.getRuntime().exec(consoleCommand);
            in = p.getInputStream();
            byte[] buffer = new byte[4096];

            int n;
            while ((n = in.read(buffer)) != -1) {
                stringOutputStream.write(buffer, 0, n);
            }

            result.append(stringOutputStream.toString());

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            FileHelper.close(in, stringOutputStream);
            if (p != null) {
                p.destroy();
            }
        }

        return result.toString();
    }

    /**
     * <p>
     * Get the amount of free/usable heap memory.
     * </p>
     *
     * @return Free memory in bytes.
     */
    public static long getFreeMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
    }

    public static String getHeapUtilization() {
        String log = "";

        long mb = SizeUnit.MEGABYTES.toBytes(1);

        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();

        log += "##### Heap utilization statistics [MB] #####\n";

        // used memory
        log += "Used Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / mb + "\n";

        // free memory
        log += "Free Memory: " + runtime.freeMemory() / mb + "\n";

        // total available memory
        log += "Total Memory: " + runtime.totalMemory() / mb + "\n";

        // maximum available memory
        log += "Max. Memory: " + runtime.maxMemory() / mb + "\n";

        return log;
    }

    public static <E> void threadedIteration(int numThreads, int threadBatchSize, Iterator<E> iterator, Consumer<E> consumer) {
        StopWatch stopWatch = new StopWatch();

        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // keep track of the number of submitted threads as we can't load all millions of tasks in memory
        final AtomicInteger threadCounter = new AtomicInteger(0);

        boolean wedged = false;
        while (iterator.hasNext() && !wedged) {
            final E obj = iterator.next();
            Thread thread = new Thread(() -> {
                try {
                    consumer.accept(obj);
                } catch (Throwable t) {
                    LOGGER.error("error while iterating", t);
                } finally {
                    threadCounter.decrementAndGet();
                }
            });
            if (!executor.isShutdown()) {
                executor.submit(thread);
                threadCounter.incrementAndGet();
            }

            // Check if we have too many threads running. This throttle has to give up on a wedged task for the same
            // reason waitForThreadPool does: a task that never finishes never decrements threadCounter, so an unbounded
            // wait here would park the caller forever and never even reach waitForThreadPool's stuck detection.
            long lastProgressNanos = System.nanoTime();
            int countAtLastProgress = threadCounter.get();
            while (threadCounter.get() > threadBatchSize) {
                ThreadHelper.deepSleep(1000);
                int running = threadCounter.get();
                if (running < countAtLastProgress) {
                    countAtLastProgress = running;
                    lastProgressNanos = System.nanoTime();
                } else if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastProgressNanos) > DEFAULT_STUCK_TIMEOUT_MS) {
                    LOGGER.error("no task finished for {}s while throttled at {} running tasks - a task is wedged; "
                                    + "stopping submission of further items so the caller can continue",
                            DEFAULT_STUCK_TIMEOUT_MS / 1000, running);
                    wedged = true;
                    break;
                }
            }
        }

        waitForThreadPool(executor, stopWatch);
    }

    /**
     * Wait for {@code executorService} to finish, giving up on tasks that are wedged.
     *
     * <p>See {@link #waitForThreadPool(ExecutorService, StopWatch, long)} for why the give-up exists; the stuck window
     * defaults to {@link #DEFAULT_STUCK_TIMEOUT_MS}. Callers that need to know whether the pool actually terminated
     * must use the three-argument form, which returns that.</p>
     */
    public static void waitForThreadPool(ExecutorService executorService, StopWatch stopWatch) {
        waitForThreadPool(executorService, stopWatch, DEFAULT_STUCK_TIMEOUT_MS);
    }

    /**
     * Wait for {@code executorService} to finish, giving up once it has completed no task for
     * {@code stuckTimeoutMillis}.
     *
     * <p>This method used to be an unconditional {@code while (!awaitTermination(5s))} loop, which turned any single
     * wedged task into a permanent hang of the calling thread. That is not theoretical: it froze gamebrain's update
     * service twice — once via a hostile {@code Retry-After} sleep (2026-06-23), once via an HTTP response read that
     * never completed (2026-08-02). In the second case this method appeared <b>twice</b> in the same stack (an inner
     * per-item pool nested inside an outer worker pool), so one stuck image download froze the entire service.</p>
     *
     * <p>The give-up condition is deliberately <b>progress-based, not wall-clock-based</b>: a pool is considered stuck
     * only when {@link ThreadPoolExecutor#getCompletedTaskCount()} has not moved for {@code stuckTimeoutMillis}. A
     * legitimately long-running pool that keeps completing tasks is never interrupted, however long it takes in total
     * — an absolute deadline here would truncate healthy multi-hour phases (a wall-clock heartbeat already false-killed
     * a healthy phase once, 2026-06-25).</p>
     *
     * <p>On giving up, {@link ExecutorService#shutdownNow()} is called and this method returns so the caller can make
     * progress. Note that {@code shutdownNow()} interrupts tasks but <b>cannot</b> abort an uninterruptible blocking
     * call such as a socket read, so the wedged thread is abandoned rather than killed; it dies when its own timeout
     * fires. Abandoning it is the point — the caller is no longer held hostage by it.</p>
     *
     * <p><b>Two consequences of abandoning that callers must handle</b>, which is why this method returns a flag rather
     * than nothing: the queued tasks that never ran are <b>dropped</b>, and the wedged task is <b>still running</b> and
     * may still be writing to whatever the caller passed it. A caller that collects results into a shared collection
     * must not read that collection unsynchronised after a {@code false} return, and a caller that cannot afford to lose
     * queued work must re-submit it.</p>
     *
     * <p>Because the signal is a gap between completions, {@code stuckTimeoutMillis} has to exceed the longest
     * <i>legitimate</i> single-task duration: a pool whose every task honestly takes longer than the window is
     * indistinguishable from a wedged one and gets abandoned. Size the window per call site rather than relying on the
     * default when tasks can be genuinely long-running.</p>
     *
     * <p>Progress can only be observed on a {@link ThreadPoolExecutor} (what {@link Executors#newFixedThreadPool(int)}
     * returns). For any other {@link ExecutorService} — e.g. the wrapper returned by
     * {@link Executors#newSingleThreadExecutor()} — no deadline can be enforced and the wait stays unbounded; that is
     * logged at WARN when the wait starts. Pass {@code stuckTimeoutMillis <= 0} to disable the deadline explicitly.</p>
     *
     * @param executorService    the pool to shut down and wait for.
     * @param stopWatch          used only for the completion log message.
     * @param stuckTimeoutMillis how long the pool may complete no task at all before its remainder is abandoned;
     *                           {@code <= 0} waits forever (the old behaviour).
     * @return {@code true} if the pool terminated on its own, {@code false} if it was abandoned (stuck, or the wait was
     * interrupted) — in which case queued tasks were dropped and a task may still be running.
     */
    public static boolean waitForThreadPool(ExecutorService executorService, StopWatch stopWatch, long stuckTimeoutMillis) {
        LOGGER.debug("waiting for all threads to finish...");
        executorService.shutdown();
        boolean abandoned = false;
        long completedAtLastProgress = completedTaskCount(executorService);
        // nanoTime, not currentTimeMillis: a wall-clock step (NTP correction, VM/host resume) must not be mistaken for
        // a pool that stopped making progress, which would abandon a healthy pool and silently drop its queued work
        long lastProgressNanos = System.nanoTime();
        boolean progressObservable = completedAtLastProgress != PROGRESS_UNOBSERVABLE;
        if (!progressObservable && stuckTimeoutMillis > 0) {
            LOGGER.warn("cannot observe task completion on {} - waiting without a stuck-task deadline",
                    executorService.getClass().getName());
        }
        // never sleep past the deadline we are meant to enforce, or a small window silently becomes the poll interval
        long pollMillis = stuckTimeoutMillis > 0 ? Math.min(POLL_INTERVAL_MS, stuckTimeoutMillis) : POLL_INTERVAL_MS;
        try {
            while (!executorService.awaitTermination(pollMillis, TimeUnit.MILLISECONDS)) {
                if (!progressObservable) {
                    LOGGER.debug("wait");
                    continue;
                }
                long completed = completedTaskCount(executorService);
                if (completed > completedAtLastProgress) {
                    completedAtLastProgress = completed;
                    lastProgressNanos = System.nanoTime();
                    LOGGER.debug("wait");
                    continue;
                }
                long idleMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastProgressNanos);
                if (isStuck(completed, idleMillis, stuckTimeoutMillis)) {
                    int neverStarted = executorService.shutdownNow().size();
                    LOGGER.error("thread pool completed no task for {}s (completed {} in total, {} never started) - "
                                    + "abandoning it so the caller can continue; a task is wedged in an uninterruptible "
                                    + "call and is left to time out on its own. Raise the stuck timeout if a single task "
                                    + "can legitimately run this long.", idleMillis / 1000, completed, neverStarted);
                    abandoned = true;
                    break;
                }
                // heartbeat while stalled: without this the whole stuck window passes with no output at all, which is
                // exactly the stretch an operator needs to see
                LOGGER.debug("wait - no task completed for {}s of a {}s stuck window", idleMillis / 1000, stuckTimeoutMillis / 1000);
            }
        } catch (InterruptedException e) {
            // the caller wants out - hand the interrupt on instead of swallowing it (getMessage() is null here anyway)
            LOGGER.warn("interrupted while waiting for the thread pool - abandoning the wait");
            Thread.currentThread().interrupt();
            abandoned = true;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            abandoned = true;
        }
        // do not claim completion we did not get - an abandoned pool still has a task running
        LOGGER.debug((abandoned ? "...gave up on a stuck thread pool after " : "...all threads finished in ")
                + stopWatch.getTotalElapsedTimeString());
        return !abandoned;
    }

    /**
     * Decide whether a pool is wedged. Pure so it can be unit-tested without a real pool.
     *
     * @param completedTaskCount       tasks completed so far, or {@link #PROGRESS_UNOBSERVABLE}.
     * @param millisSinceLastCompletion how long ago {@code completedTaskCount} last increased.
     * @param stuckTimeoutMillis       the stuck window; {@code <= 0} disables the check.
     * @return {@code true} only when progress is observable, the deadline is enabled, and the window has elapsed.
     */
    static boolean isStuck(long completedTaskCount, long millisSinceLastCompletion, long stuckTimeoutMillis) {
        if (stuckTimeoutMillis <= 0) {
            return false;
        }
        // never guess at a pool whose progress we cannot see - a false "stuck" silently drops queued work
        if (completedTaskCount == PROGRESS_UNOBSERVABLE) {
            return false;
        }
        return millisSinceLastCompletion > stuckTimeoutMillis;
    }

    /** Tasks completed so far, or {@link #PROGRESS_UNOBSERVABLE} if this executor does not expose the counter. */
    private static long completedTaskCount(ExecutorService executorService) {
        if (executorService instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) executorService).getCompletedTaskCount();
        }
        return PROGRESS_UNOBSERVABLE;
    }
}
