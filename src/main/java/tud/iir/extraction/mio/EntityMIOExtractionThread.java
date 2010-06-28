/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

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
    // private static final Logger mio_logger = Logger.getLogger("mio");

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
        List<MIOPage> MIOPages = pageRetr.retrieveMIOs(entity, searchVoc);

        // TODO Detail MIOAnalysis (content&context & trust-calculation)
        MIOPageAnalyzer mioPAnalyzer = new MIOPageAnalyzer();
        MIOComparator mioComp = new MIOComparator();
        Map<String, MIO> mios = mioPAnalyzer.extractMIOs(MIOPages, entity);

        Set<MIO> mioResults = new TreeSet<MIO>(mioComp);
        // for (String mioURL : mios.keySet()) {
        // mioResults.add(mios.get((mioURL)));
        //
        // }

        for (Entry<String, MIO> mio : mios.entrySet()) {
            mioResults.add(mio.getValue());
        }

        // printMapToFile(mios);
        printSetToHTMLFile(mioResults);

        LOGGER.info("Thread finished in " + DateHelper.getRuntime(t1) + "  " + mios.size() + "s, MIOs for \""
                + entity.getName() + "\" were found.");

        MIOExtractor.getInstance().decreaseThreadCount();
    }

    /**
     * Prints the set to html file.
     * 
     * @param cleanedMIOs the cleaned mi os
     */
    private void printSetToHTMLFile(Set<MIO> cleanedMIOs) {
        // FileHelper filehelper = new FileHelper();
        FileHelper.appendToFile("f:/test.html", "<html><body>", false);
        for (MIO mio : cleanedMIOs) {
            // MIO mio = cleanedMIOs.get(mioURL);
            StringBuffer sBuffer = new StringBuffer();
            for (String info : mio.getInfos().keySet()) {
                sBuffer.append(info);
                sBuffer.append(" ---- ");
                sBuffer.append(mio.getInfos().get(info).toString());

            }
            String output = " TRUST: " + mio.getTrust() + " <a href=\"" + mio.getDirectURL() + "\">"
                    + mio.getDirectURL() + "</a> founded on <a href=\"" + mio.getFindPageURL() + "\">"
                    + mio.getFindPageURL() + "</a> for Entity: " + mio.getEntity().getName() + " Infos: "
                    + sBuffer.toString() + "<br><br>";
            // System.out.println(output);
            FileHelper.appendToFile("f:/test.html", output + "\r\n", false);

        }
        FileHelper.appendToFile("f:/test.html", "</body></html>", false);

    }

    /**
     * Prints the map to file.
     * 
     * @param cleanedMIOs the cleaned mi os
     */
    // private void printMapToFile(Map<String, MIO> cleanedMIOs) {
    // FileHelper filehelper = new FileHelper();
    //
    // for (String mioURL : cleanedMIOs.keySet()) {
    // MIO mio = cleanedMIOs.get(mioURL);
    // StringBuffer sBuffer = new StringBuffer();
    // for (String info : mio.getInfos().keySet()) {
    // sBuffer.append(info);
    // sBuffer.append(" ---- ");
    // sBuffer.append(mio.getInfos().get(info).toString());
    //
    // }
    // String output = " TRUST: " + mio.getTrust() + " " + mio.getDirectURL() + " founded on "
    // + mio.getFindPageURL() + " for Entity: " + mio.getEntity().getName() + " Infos: "
    // + sBuffer.toString();
    // // System.out.println(output);
    // filehelper.appendToFile("f:/test.txt", output + "\r\n", false);
    //
    // }
    //
    // }

}
