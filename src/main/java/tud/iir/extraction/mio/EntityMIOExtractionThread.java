/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.knowledge.Entity;

/**
 * The EntityMIOExtractionThread extracts MIOs for one given entity. Therefore, extracting MIOs can be parallelized on
 * the entity level.
 * 
 * @author Martin Werner
 */

public class EntityMIOExtractionThread extends Thread {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EntityMIOExtractionThread.class);

    /** The central entity. */
    private Entity entity = null;

    /** The search-vocabulary. */
    private ConceptSearchVocabulary searchVoc = null;

    /**
     * Instantiates a new entity mio extraction thread.
     * 
     * @param threadGroup the thread group
     * @param entityName the entityName
     * @param entity the entity
     * @param searchVoc the searchVocabulary
     */
    public EntityMIOExtractionThread(ThreadGroup threadGroup, String entityName, Entity entity,
            ConceptSearchVocabulary searchVoc) {
        super(threadGroup, entityName);
        this.entity = entity;
        this.searchVoc = searchVoc;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        MIOExtractor.getInstance().increaseThreadCount();
        long t1 = System.currentTimeMillis();

        // DO SOMETHING
        MIOPageRetriever pageRetr = MIOPageRetriever.getInstance();
        List<MIOPage> MIOPages = pageRetr.retrieveMIOPages(entity, searchVoc);

        // TODO Detail MIOAnalysis (content&context & trust-calculation)
        MIOPageAnalyzer mioPAnalyzer = new MIOPageAnalyzer();
//        MIOComparator mioComp = new MIOComparator();
        Set<MIO> mios = mioPAnalyzer.extractMIOs(MIOPages, entity);

        // print the MIOFeatures out
//        printMIOFeaturesToFile(mios);

//        Set<MIO> mioResults = new TreeSet<MIO>(mioComp);
//        for (Entry<String, MIO> mio : mios.entrySet()) {
//            mioResults.add(mio.getValue());
//        }

        // MIOClassifier mioClass = new MIOClassifier();
        // mioClass.trainClassifier("f:/features - printer - allcontextfeat.txt");
        // for (MIO mio : mioResults) {
        //
        // mioClass.classify(mio);
        // }


        printSetToHTMLFile(mios);

        LOGGER.info("Thread finished in " + DateHelper.getRuntime(t1) + "  " + mios.size() + "s, MIOs for \""
                + entity.getName() + "\" were found.");

        MIOExtractor.getInstance().decreaseThreadCount();
    }

    /**
     * Prints the set to html file.
     * 
     * @param cleanedMIOs the cleaned MIOs
     */
    private void printSetToHTMLFile(Set<MIO> cleanedMIOs) {
        // FileHelper filehelper = new FileHelper();
        try {
            FileHelper.appendFile("f:/test.html", "<html><body>");

            for (MIO mio : cleanedMIOs) {
                // MIO mio = cleanedMIOs.get(mioURL);
                // StringBuffer sBuffer = new StringBuffer();
                // for (String info : mio.getInfos().keySet()) {
                // sBuffer.append(info);
                // sBuffer.append(" ---- ");
                // sBuffer.append(mio.getInfos().get(info).toString());
                //
                // }
                String output = " mlTrust: " + mio.getMlTrust() + " TRUST: " + mio.getTrust() + " Interactivity: " + mio.getInteractivityGrade() +" <a href=\""
                        + mio.getDirectURL() + "\">" + mio.getDirectURL() + "</a> founded on <a href=\""
                        + mio.getFindPageURL() + "\">" + mio.getFindPageURL() + "</a> for Entity: "
                        + mio.getEntity().getName() + "<br><br>";
                // System.out.println(output);
                FileHelper.appendFile("f:/test.html", output + "\r\n");

            }
            FileHelper.appendFile("f:/test.html", "</body></html>");
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Prints the map to file.
     * 
     * @param cleanedMIOs the cleaned mios
     */
    private void printMIOFeaturesToFile(Map<String, MIO> cleanedMIOs) {

        for (Entry<String, MIO> cMio : cleanedMIOs.entrySet()) {
            MIO mio = cMio.getValue();
            StringBuffer sBuffer = new StringBuffer();
            Map<String, Double> mioFeatures = mio.getFeatures();
            ;
            sBuffer.append("# " + mio.getEntity().getName() + " Trust: " + mio.getTrust() + " " + mio.getDirectURL()
                    + "\r\n");
            sBuffer.append("# " + mio.getFindPageURL() + "\r\n");

            for (Entry<String, Double> feature : mioFeatures.entrySet()) {
                sBuffer.append(feature.getValue());
                sBuffer.append(";");

            }
            String output = sBuffer.toString();
            // System.out.println(output);
            try {
                FileHelper.appendFile("f:/features.txt", output + "\r\n");
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }

        }

        // for (String mioURL : cleanedMIOs.keySet()) {
        // MIO mio = cleanedMIOs.get(mioURL);
        // StringBuffer sBuffer = new StringBuffer();
        // Map<String, Double> mioFeatures = cleanedMIOs.get(mioURL).getFeatures();
        // sBuffer.append("# " + mio.getEntity().getName()+" Trust: " + mio.getTrust() + " " + mio.getDirectURL() +
        // "\r\n");
        // sBuffer.append("# " + mio.getFindPageURL() + "\r\n");
        // for (Entry<String, Double> feature : mioFeatures.entrySet()) {
        // sBuffer.append(feature.getValue());
        // sBuffer.append(";");
        //
        // }
        // String output = sBuffer.toString();
        // // System.out.println(output);
        // FileHelper.appendToFile("f:/features.txt", output + "\r\n", false);
        //
        // }

    }

}
