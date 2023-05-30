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

        while (iterator.hasNext()) {
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

            // check if we have too many threads running
            while (threadCounter.get() > threadBatchSize) {
                ThreadHelper.deepSleep(1000);
            }
        }

        waitForThreadPool(executor, stopWatch);
    }

    public static void waitForThreadPool(ExecutorService executorService, StopWatch stopWatch) {
        LOGGER.info("waiting for all threads to finish...");
        executorService.shutdown();
        try {
            while (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.debug("wait");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        LOGGER.info("...all threads finished in " + stopWatch.getTotalElapsedTimeString());
    }
}
