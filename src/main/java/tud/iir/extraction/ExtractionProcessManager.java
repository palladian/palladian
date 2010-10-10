package tud.iir.extraction;

import org.apache.log4j.Logger;

import tud.iir.control.Controller;
import tud.iir.extraction.entity.EntityExtractionProcess;
import tud.iir.extraction.fact.FactExtractionProcess;
import tud.iir.extraction.mio.MIOExtractionProcess;
import tud.iir.extraction.qa.QAExtractionProcess;
import tud.iir.extraction.snippet.SnippetExtractionProcess;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.persistence.DatabaseManager;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetrieverManager;

/**
 * The ExtractionProcessManager manages the entity and the fact extraction process.
 * 
 * @author David Urbansky
 */
public class ExtractionProcessManager {

    public static boolean entityExtractionIsRunning = false;
    public static boolean factExtractionIsRunning = false;
    public static boolean qaExtractionIsRunning = false;
    public static boolean snippetExtractionIsRunning = false;
    public static boolean mioExtractionIsRunning = false;
    private static EntityExtractionProcess ep;
    private static FactExtractionProcess fp;
    private static MIOExtractionProcess mp;
    private static SnippetExtractionProcess sp;
    private static QAExtractionProcess qp;

    public static LiveStatus liveStatus = new LiveStatus();

    private static boolean useConceptSynonyms = false;
    private static boolean useAttributeSynonyms = false;
    private static boolean findNewAttributesAndValues = true;
    private static boolean continueQAExtraction = true;

    // fact trust calculation
    public static final int QUANTITY_TRUST = 1;
    public static final int SOURCE_TRUST = 2;
    public static final int EXTRACTION_TYPE_TRUST = 3;
    public static final int COMBINED_TRUST = 4;
    public static final int CROSS_TRUST = 5;
    private static int trustFormula = CROSS_TRUST;

    // benchmark settings
    public static final int BENCHMARK_FULL_SET = 1;
    public static final int BENCHMARK_HALF_SET = 2;
    private static int benchmarkSetSize = BENCHMARK_HALF_SET;
    public static final int MICROSOFT_8 = 1;
    public static final int YAHOO_8 = 2;
    public static final int HAKIA_8 = 3;
    public static final int GOOGLE_8 = 4;
    private static int benchmarkSet = YAHOO_8;
    public static String BENCHMARK_FACT_EXTRACTION = "facts";
    public static String BENCHMARK_ENTITY_EXTRACTION = "entities";
    private static String benchmarkType = BENCHMARK_FACT_EXTRACTION;

    public static void startEntityExtraction() {
        if (!entityExtractionIsRunning) {
            entityExtractionIsRunning = true;
            ep = new EntityExtractionProcess();
            ep.start();
        } else {
            System.out.println("already running");
        }
    }

    public static boolean stopEntityExtraction() {
        if (entityExtractionIsRunning) {
            boolean stopped = ep.stopExtraction();
            entityExtractionIsRunning = false;
            return stopped;
        } else {
            System.out.println("not running");
        }
        return false;
    }

    public static void startFactExtraction() {
        if (!factExtractionIsRunning) {
            factExtractionIsRunning = true;
            fp = new FactExtractionProcess();
            fp.start();
        } else {
            System.out.println("already running");
        }
    }

    public static boolean stopFactExtraction() {
        if (factExtractionIsRunning) {
            boolean stopped = fp.stopExtraction();
            factExtractionIsRunning = false;
            return stopped;
        } else {
            System.out.println("not running");
        }
        return false;
    }

    public static void runFactExtractionBenchmark() {
        if (!factExtractionIsRunning) {
            factExtractionIsRunning = true;
            fp = new FactExtractionProcess(true);
            fp.start();
        } else {
            System.out.println("already running");
        }
    }

    public static void startQAExtraction() {
        if (!qaExtractionIsRunning) {
            qaExtractionIsRunning = true;
            qp = new QAExtractionProcess();
            qp.start();
        } else {
            System.out.println("already running");
        }
    }

    public static boolean stopQAExtraction() {
        if (qaExtractionIsRunning) {
            boolean stopped = qp.stopExtraction();
            qaExtractionIsRunning = false;
            return stopped;
        } else {
            System.out.println("not running");
        }
        return false;
    }

    public static void startSnippetExtraction() {
        if (!snippetExtractionIsRunning) {
            snippetExtractionIsRunning = true;
            sp = new SnippetExtractionProcess();
            sp.start();
        } else {
            System.out.println("already running");
        }
    }

    public static boolean stopSnippetExtraction() {
        if (snippetExtractionIsRunning) {
            boolean stopped = sp.stopExtraction();
            snippetExtractionIsRunning = false;
            return stopped;
        } else {
            System.out.println("not running");
        }
        return false;
    }

    public static void startMIOExtraction() {
        if (!mioExtractionIsRunning) {
            mioExtractionIsRunning = true;
            mp = new MIOExtractionProcess();
            mp.start();
        } else {
            System.out.println("already running");
        }
    }

    public static boolean stopMIOExtraction() {
        if (mioExtractionIsRunning) {
            boolean stopped = mp.stopExtraction();
            mioExtractionIsRunning = false;
            return stopped;
        } else {
            System.out.println("not running");
        }
        return false;
    }

    public static void startFullExtractionLoop() {

        // SourceRetrieverManager.getInstance().setSource(SourceRetrieverManager.GOOGLE);
        // SourceRetrieverManager.getInstance().setResultCount(100);

        // get the current number of downloaded bytes from the database
        Crawler.sessionDownloadedBytes = DatabaseManager.getInstance().getExtractionStatusDownloadedBytes();

        System.out.println(Crawler.sessionDownloadedBytes);

        SourceRetrieverManager.getInstance().setSource(Controller.getConfig().getInt("extraction.source"));
        SourceRetrieverManager.getInstance().setResultCount(Controller.getConfig().getInt("extraction.resultCount"));

        // the interval in seconds in which the live_status table should be updated
        int extractionStatusUpdateInteraval = Controller.getConfig().getInt("extraction.statusUpdate") * 1000;

        while (true) {

            if (Controller.getConfig().getInt("extraction.entitySlot") > 0) {
                startEntityExtraction();
                waitingLoop(Controller.getConfig().getInt("extraction.entitySlot") * DateHelper.MINUTE_MS,
                        extractionStatusUpdateInteraval);
                Logger.getRootLogger().info("stop entity extraction now...");
                stopEntityExtraction();
                Logger.getRootLogger().info("entity extraction stopped, continue");
            }

            if (Controller.getConfig().getInt("extraction.factSlot") > 0) {
                startFactExtraction();
                waitingLoop(Controller.getConfig().getInt("extraction.factSlot") * DateHelper.MINUTE_MS,
                        extractionStatusUpdateInteraval);
                Logger.getRootLogger().info("stop fact extraction now...");
                stopFactExtraction();
                Logger.getRootLogger().info("fact extraction stopped, continue");
            }

            if (Controller.getConfig().getInt("extraction.mioSlot") > 0) {
                startMIOExtraction();
                waitingLoop(Controller.getConfig().getInt("extraction.mioSlot") * DateHelper.MINUTE_MS,
                        extractionStatusUpdateInteraval);
                Logger.getRootLogger().info("stop mio extraction now...");
                stopMIOExtraction();
                Logger.getRootLogger().info("mio extraction stopped, continue");
            }

            if (Controller.getConfig().getInt("extraction.snippetSlot") > 0) {
                startSnippetExtraction();
                waitingLoop(Controller.getConfig().getInt("extraction.snippetSlot") * DateHelper.MINUTE_MS,
                        extractionStatusUpdateInteraval);
                Logger.getRootLogger().info("stop snippet extraction now...");
                stopSnippetExtraction();
                Logger.getRootLogger().info("snippet extraction stopped, continue");
            }

            if (Controller.getConfig().getInt("extraction.qaSlot") > 0) {
                startQAExtraction();
                waitingLoop(Controller.getConfig().getInt("extraction.qaSlot") * DateHelper.MINUTE_MS,
                        extractionStatusUpdateInteraval);
                Logger.getRootLogger().info("stop qa extraction now...");
                stopQAExtraction();
                Logger.getRootLogger().info("qa extraction stopped, continue");
            }

        }
    }

    /**
     * Wait for a certain time. This is used to not run different extraction phases in parallel.
     * 
     * @param totalTimeMS The total pause time in milliseconds.
     * @param intervalMS The interval in milliseconds when the loop should write live status updates.
     */
    private static void waitingLoop(int totalTimeMS, int intervalMS) {
        int steps = totalTimeMS / intervalMS;
        for (int i = 0; i < steps; i++) {
            ThreadHelper.sleep(intervalMS);
            int progress = (int) Math.floor((double) 100 * i / steps);
            System.out.println("progress: " + progress + "%");

            liveStatus.setPercent(progress);
            liveStatus.setTimeLeft(DateHelper.getTimeString(totalTimeMS - i * intervalMS));
            liveStatus.setCurrentPhase(getExtractionPhaseName());
            liveStatus.setLogExcerpt(getCurrentPhaseLoggerExcerpt().toString());
            liveStatus.setDownloadedBytes(Crawler.sessionDownloadedBytes);

            DatabaseManager.getInstance().checkCleanExtractionStatus();
            DatabaseManager.getInstance().updateExtractionStatus(liveStatus);

            // liveStatus = new LiveStatus();
        }
    }

    private static int getExtractionPhase() {
        if (entityExtractionIsRunning) {
            return 1;
        }
        if (factExtractionIsRunning) {
            return 2;
        }
        if (mioExtractionIsRunning) {
            return 3;
        }
        if (snippetExtractionIsRunning) {
            return 4;
        }
        if (qaExtractionIsRunning) {
            return 5;
        }
        return -1;
    }
    /**
     * Find out in which extraction phase the extraction loop is at the moment.
     * 
     * @return The phase number.
     */
    private static String getExtractionPhaseName() {
        switch (getExtractionPhase()) {
            case 1:
                return "Entity Extraction";
            case 2:
                return "Fact Extraction";
            case 3:
                return "MIO Extraction";
            case 4:
                return "Snippet Extraction";
            case 5:
                return "QA Extraction";
            default:
                return "Unknown";
        }
    }

    /**
     * Get an excerpt of the logger of the phase that is currently active.
     * 
     * @return An excerpt of the logs.
     */
    private static String getCurrentPhaseLoggerExcerpt() {
        return FileHelper.readFileToString(Controller.getConfig().getString("extraction.liveStatusLogPath"));
    }

    public static int getSourceRetrievalSite() {
        return SourceRetrieverManager.getInstance().getSource();
    }

    public static int getSourceRetrievalCount() {
        return SourceRetrieverManager.getInstance().getResultCount();
    }

    public static boolean isUseConceptSynonyms() {
        return useConceptSynonyms;
    }

    public static void setUseConceptSynonyms(boolean useConceptSynonyms) {
        ExtractionProcessManager.useConceptSynonyms = useConceptSynonyms;
    }

    public static boolean isUseAttributeSynonyms() {
        return useAttributeSynonyms;
    }

    public static void setUseAttributeSynonyms(boolean useAttributeSynonyms) {
        ExtractionProcessManager.useAttributeSynonyms = useAttributeSynonyms;
    }

    public static boolean isFindNewAttributesAndValues() {
        return findNewAttributesAndValues;
    }

    public static void setFindNewAttributesAndValues(boolean findNewAttributesAndValues) {
        ExtractionProcessManager.findNewAttributesAndValues = findNewAttributesAndValues;
    }

    public static boolean isContinueQAExtraction() {
        return continueQAExtraction;
    }

    public static void setContinueQAExtraction(boolean continueQAExtraction) {
        ExtractionProcessManager.continueQAExtraction = continueQAExtraction;
    }

    public static int getBenchmarkSetSize() {
        return benchmarkSetSize;
    }

    public static void setBenchmarkSetSize(int benchmarkSetSize) {
        ExtractionProcessManager.benchmarkSetSize = benchmarkSetSize;
    }

    public static int getBenchmarkSet() {
        return benchmarkSet;
    }

    public static void setBenchmarkSet(int benchmarkSet) {
        ExtractionProcessManager.benchmarkSet = benchmarkSet;
    }

    public static String getBenchmarkType() {
        return benchmarkType;
    }

    public static void setBenchmarkType(String benchmarkType) {
        ExtractionProcessManager.benchmarkType = benchmarkType;
    }

    public static int getTrustFormula() {
        return trustFormula;
    }

    public static void setTrustFormula(int trustFormula) {
        ExtractionProcessManager.trustFormula = trustFormula;
    }

}