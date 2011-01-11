package tud.iir.extraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.knowledge.KnowledgeManager;

/**
 * The abstract Extractor from which other singleton extractors inherit.
 * 
 * @author David Urbansky
 * 
 */
public abstract class Extractor {

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(Extractor.class);

    /** 5 minutes wait in case no entities were found in the database. */
    protected static final int WAIT_FOR_ENTITIES_TIMEOUT = (int) (30 * DateHelper.SECOND_MS);

    /** Number of parallel extraction threads. */
    protected static final int MAX_EXTRACTION_THREADS = 10;

    /** Wait 10 seconds if all extraction threads are currently running. */
    protected static final int WAIT_FOR_FREE_THREAD_SLOT = (int) (10 * DateHelper.SECOND_MS);

    /** Wait a maximum of 30 iterations with {@link WAIT_FOR_FREE_THREAD_SLOT} for extraction threads to finish. */
    protected static final int WAIT_FOR_FREE_THREAD_SLOT_COUNT = 30;

    /** The number of iterations waited for threads to finish. */
    protected int iterationsWaited = 0;

    /** The knowledge manager. */
    protected KnowledgeManager knowledgeManager;

    /** Count number of active extraction threads. */
    private int threadCount = 0;

    /** The thread group of all extraction threads. */
    protected ThreadGroup extractionThreadGroup;

    /** Whether the stop command has been called. */
    private boolean stopped = false;

    /** If true some statistics for benchmarking are gathered. */
    private boolean benchmark = false;

    /** List of binary file extensions. */
    public static final String[] URL_BINARY_BLACKLIST = { "pdf", "doc", "ppt", "xls", "png", "jpg", "jpeg", "gif", "ai", "svg", "zip", "avi", "exe", "msi",
        "wav", "mp3", "wmv" };

    /** List of textual file extensions. */
    public static final String[] URL_TEXTUAL_BLACKLIST = { "cfm", "db", "svg", "txt" };

    /** List of file extensions that should be ignored for the extractor. */
    private Set<String> blackList = new HashSet<String>();

    public KnowledgeManager getKnowledgeManager() {
        return knowledgeManager;
    }

    public void setKnowledgeManager(KnowledgeManager knowledgeManager) {
        this.knowledgeManager = knowledgeManager;
    }

    protected void resetThreadCount() {
        this.threadCount = 0;
    }

    public int getThreadCount() {
        return this.threadCount;
    }

    public void increaseThreadCount() {
        this.threadCount++;
        System.out.println("increase thread count to " + threadCount);
    }

    public void decreaseThreadCount() {
        this.threadCount--;
        System.out.println("decrease thread count to " + threadCount);
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public boolean isBenchmark() {
        return benchmark;
    }

    /**
     * Check whether a crawled URL has a suffix which is on the black list.
     * 
     * @param url The URL to check.
     * @return True, if the file extension is not on the black list.
     */
    protected boolean isURLallowed(String url) {

        for (String forbiddenSuffix : blackList) {

            url=url.trim();
            if (url.endsWith("." + forbiddenSuffix)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns for a given list of URLs these which are not blacklisted (be sure to set a blacklist first)
     * 
     */
    public List<String> filterURLs(List<String> urls) {
        List<String> filteredURLs = new ArrayList<String>();

        for (String url : urls) {
            if (isURLallowed(url)) {
                filteredURLs.add(url);
            }
        }

        return filteredURLs;
    }

    public boolean stopExtraction(boolean saveResults) {
        setStopped(true);

        ThreadHelper.deepSleep(2 * DateHelper.SECOND_MS);

        // let all current threads finish
        int waitCount = 0;
        while (getThreadCount() > 0 && waitCount < 25) {
            LOGGER.info("stop extraction but wait for " + getThreadCount() + " threads to finish (wait count: " + waitCount + "/25)");

            try {
                Thread.sleep(WAIT_FOR_FREE_THREAD_SLOT);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                break;
            }

            waitCount++;
        }

        // stop all threads that are still running
        if (extractionThreadGroup != null) {
            LOGGER.info("interrupting " + getThreadCount() + " threads now");
            extractionThreadGroup.interrupt();
        }
        resetThreadCount();

        LOGGER.info("stop extraction (save results: " + saveResults + ")");

        saveExtractions(saveResults);

        return true;
    }

    protected boolean waitForFreeThreadSlot(Logger logger) {

        logger.info("NEED TO WAIT FOR FREE THREAD SLOT (count: " + getThreadCount() + ", allowed: "
                + MAX_EXTRACTION_THREADS + ") " + iterationsWaited + "/" + WAIT_FOR_FREE_THREAD_SLOT_COUNT + ", "
                + extractionThreadGroup.activeCount() + "," + extractionThreadGroup.activeGroupCount());

        if (extractionThreadGroup.activeCount() + extractionThreadGroup.activeGroupCount() == 0) {
            logger.warn("apparently " + getThreadCount()
                    + " threads have not finished correctly but thread group is empty, continuing...");
            resetThreadCount();
            iterationsWaited = 0;
            return false;
        }

        try {
            Thread.sleep(WAIT_FOR_FREE_THREAD_SLOT);
        } catch (InterruptedException e) {
            LOGGER.warn(e.getMessage());
            setStopped(true);
            iterationsWaited = 0;
            return false;
        }

        if (isStopped()) {
            iterationsWaited++;
        }

        if (iterationsWaited > WAIT_FOR_FREE_THREAD_SLOT_COUNT) {
            logger.info("waited " + WAIT_FOR_FREE_THREAD_SLOT_COUNT
                    + " iterations after stop has been called, breaking now");
            iterationsWaited = 0;
            return false;
        }

        return true;
    }

    abstract protected void saveExtractions(boolean saveExtractions);

    public void setBenchmark(boolean benchmark) {
        this.benchmark = benchmark;
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public Set<String> getBlackList() {
        return blackList;
    }

    /**
     * Allows to define the SuffixBlackList
     * 
     */
    public void addSuffixesToBlackList(String[] nBlackList) {
        for (String suffix : nBlackList) {
            if (!blackList.contains(suffix)) {
                blackList.add(suffix);
            }
        }

    }
}