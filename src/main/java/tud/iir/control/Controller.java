package tud.iir.control;

//import knowledge.KnowledgeManager;
//import extraction.fact.FactExtractor;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.extraction.fact.FactExtractor;
import tud.iir.gui.GUIManager;
import tud.iir.persistence.DatabaseManager;

/**
 * This class is the entry point to the WebKnox Core application.
 * 
 * @author David Urbansky
 */
public class Controller {

    /**
     * benchmark settings
     */
    private static Controller instance = null;

    private static PropertiesConfiguration config = null;

    public static final String NAME = "WebKnox";
    public static final String ID = "WebKnox";
    public static final double VERSION = 0.12;

    public static final int WEB = 1;
    public static final int SELECTION = 2;
    public static final int SELECTION_HALF = 3;

    public static final int EXTRACTION_SOURCES = WEB; // determines where to run the extraction, normally from all available sources (WEB) but for benchmarking

    // reasons maybe only on a selection

    /**
     * DELETE FROM `entities_sources` WHERE extractionType > 10; DELETE FROM sources WHERE id NOT IN (SELECT sourceID FROM entities_sources); DELETE FROM
     * entities WHERE id NOT IN (SELECT entityID FROM entities_sources);
     */
    private Controller() {
        getConfig();
    }

    /**
     * Get the instance of the class.
     * 
     * @return
     */
    public static Controller getInstance() {
        if (instance == null) {
            instance = new Controller();
        }
        return instance;
    }

    public static PropertiesConfiguration getConfig() {
        if (config == null) {
            try {
                config = new PropertiesConfiguration("config/general.conf");
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        }

        return config;
    }

    /**
     * WebKnox Core application entry point.
     * 
     * @param args No arguments are read.
     */
    public static void main(String[] args) throws Exception {

        // version
        Logger.getRootLogger().info("starting WebKnox version " + Controller.VERSION);
        System.out.println("starting WebKnox version " + Controller.VERSION);

        // instantiate the controller
        Controller.getInstance();

        // command line mode
        if (args.length > 0) {

            // start extraction processes
            if (args[0].equalsIgnoreCase("entityextraction")) {

                ExtractionProcessManager.startEntityExtraction();

            } else if (args[0].equalsIgnoreCase("factextraction")) {

                ExtractionProcessManager.startFactExtraction();

            } else if (args[0].equalsIgnoreCase("qaextraction")) {

                ExtractionProcessManager.startQAExtraction();

            } else if (args[0].equalsIgnoreCase("loop")) {

                ExtractionProcessManager.startFullExtractionLoop();

            } else if (args[0].equalsIgnoreCase("clean")) {

                DatabaseManager.getInstance().clearCompleteDatabase();

            }

            // enter interactive mode
            else if (args[0].equalsIgnoreCase("interactive")) {
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

                System.out.println("starting interactive mode:");
                while (true) {

                    System.out.println("command:");
                    String line = in.readLine();

                    if (line == null || line.length() <= 0 || line.equals("quit")) {
                        break;
                    }

                    if (line.equalsIgnoreCase("extract entities")) {

                        System.out.println("start entity extraction process...");
                        ExtractionProcessManager.startEntityExtraction();

                    } else if (line.equalsIgnoreCase("extract facts")) {

                        System.out.println("start fact extraction process...");
                        ExtractionProcessManager.startFactExtraction();

                    } else if (line.equalsIgnoreCase("extract qas")) {

                        System.out.println("start Q/A extraction process...");
                        ExtractionProcessManager.startQAExtraction();

                    } else if (line.equalsIgnoreCase("evaluate benchmark facts")) {

                        System.out.println("start evaluation process...");
                        FactExtractor.getInstance().getKnowledgeManager().evaluateBenchmarkExtractions();

                    } else if (line.equalsIgnoreCase("gui")) {

                        GUIManager.getInstance();

                    } else if (line.equalsIgnoreCase("loop")) {

                        ExtractionProcessManager.startFullExtractionLoop();

                    }
                }
            }

            // graphic mode
        } else {
            GUIManager.getInstance();
        }

        // interactive mode

        // new GUIManager();

        // Thread thread1 = new Thread(new GUIManager(), "thread1");
        // thread1.start();

    }
}