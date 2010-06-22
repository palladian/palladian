package tud.iir.extraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.knowledge.KnowledgeManager;

public abstract class Extractor {

    /** the logger for this class */
    protected static final Logger LOGGER = Logger.getLogger(Extractor.class);

    /** 5 minutes wait in case no entities were found in the database */
    protected static final int WAIT_FOR_ENTITIES_TIMEOUT = 300000;

    /** number of parallel extraction threads */
    protected static final int MAX_EXTRACTION_THREADS = 10;

    /** wait 10 seconds if all extraction threads are currently running */
    protected static final int WAIT_FOR_FREE_THREAD_SLOT = 10000;

    /** the knowledge manager */
    protected KnowledgeManager knowledgeManager;

    /** count number of active extraction threads */
    private int threadCount = 0;

    /** the thread group of all extraction threads */
    protected ThreadGroup extractionThreadGroup;

    /** whether the stop command has been called */
    private boolean stopped = false;

    /** if true some statistics for benchmarking are gathered */
    private boolean benchmark = false;

    /** list of binary file extensions */
    public static final String[] URL_BINARY_BLACKLIST = { "pdf", "doc", "ppt", "xls", "png", "jpg", "jpeg", "gif", "ai", "svg", "zip", "avi", "exe", "msi",
            "wav", "mp3", "wmv" };

    /** list of textual file extensions */
    public static final String[] URL_TEXTUAL_BLACKLIST = { "cfm", "db", "svg", "txt" };

    /** list of file extensions that should be ignored for the extractor */
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
            if (url.endsWith("." + forbiddenSuffix))
                return false;
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

        ThreadHelper.sleep(2 * DateHelper.SECOND_MS);

        // let all current threads finish
        int waitCount = 0;
        while (getThreadCount() > 0 && waitCount < 25) {
            LOGGER.info("stop extraction but wait for " + getThreadCount() + " threads to finish (wait count: " + waitCount + "/25)");
            ThreadHelper.sleep(WAIT_FOR_FREE_THREAD_SLOT);
            waitCount++;
        }

        // stop all threads that are still running
        if (extractionThreadGroup != null) {
            extractionThreadGroup.interrupt();
        }
        resetThreadCount();

        LOGGER.info("stop extraction (save results: " + saveResults + ")");

        saveExtractions(saveResults);

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