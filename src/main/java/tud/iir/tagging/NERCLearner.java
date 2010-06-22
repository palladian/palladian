package tud.iir.tagging;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;

/**
 * This class trains a NERCer on web texts.
 * 
 * @author David Urbansky
 * 
 */
public class NERCLearner {

    private static final Logger logger = Logger.getLogger(NERCLearner.class);

    private NERCer nercer = null;

    public NERCLearner() {
        nercer = new NERCer();
        nercer.setKbCommunicator(new TestKnowledgeBaseCommunicator());
    }

    public void learn(double trainingPercentage, int resultsPerEntity) {

        long t1 = System.currentTimeMillis();

        Crawler crawler = new Crawler();

        SourceRetriever sr = new SourceRetriever();
        sr.setSource(SourceRetrieverManager.GOOGLE);
        sr.setResultCount(resultsPerEntity);
        EntityList el = nercer.getTrainingEntities(trainingPercentage);

        logger.info("start learning NERCer with " + el.size() + " (" + trainingPercentage + "%) entities and " + resultsPerEntity + " web pages per entity");

        for (RecognizedEntity e : el) {
            logger.info("get URLs for traininig entity " + e.getName());
            ArrayList<String> urls = sr.getURLs(e.getName(), true);

            for (String url : urls) {
                nercer.trainRecognizer(crawler.download(url, true, true, true, false));
            }

        }

        logger.info("finish training");
        nercer.finishTraining();

        logger.info("finished learning in " + DateHelper.getRuntime(t1));
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        NERCLearner nl = new NERCLearner();
        nl.learn(3, 2);
    }

}