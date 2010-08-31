/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import tud.iir.classification.mio.MIOClassifier;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Fact;
import tud.iir.knowledge.FactValue;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.knowledge.Source;

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
    private final transient Entity entity;

    /** The trust limit. */
    // private static double trustLimit = 1;

    /** The knowledge manager. */
    private final transient KnowledgeManager knowledgeManager;

    /**
     * Instantiates a new entity MIOExtractionThread.
     * 
     * @param threadGroup the thread group
     * @param entityName the entityName
     * @param entity the entity
     * @param knowledgeManager the knowledge manager
     */
    public EntityMIOExtractionThread(final ThreadGroup threadGroup, final String entityName, final Entity entity,
            final KnowledgeManager knowledgeManager) {
        super(threadGroup, entityName);
        this.entity = entity;
        this.knowledgeManager = knowledgeManager;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        while (MIOExtractor.getInstance().getThreadCount() >= 3) {
            try {
                sleep(10000);
//                System.out.println("Thread " + entity.getName() + " schlafen geschickt!");
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        MIOExtractor.getInstance().increaseThreadCount();
        final long timeStamp1 = System.currentTimeMillis();
        // get MIO containing pages
        final MIOPageRetriever pageRetr = new MIOPageRetriever();
        final List<MIOPage> mioPages = pageRetr.retrieveMIOPages(entity);

        // System.out.println("GETTING MIO containing Pages: " + DateHelper.getRuntime(timeStamp1));
        // final long timeStamp2 = System.currentTimeMillis();

        // MIOAnalysis (content & context)
        final MIOPageAnalyzer mioPAnalyzer = new MIOPageAnalyzer();
      
        final Map<String, MIO> mios = mioPAnalyzer.extractMIOs(mioPages, entity);

        // System.out.println("EXTRACTING AND ANALYZING MIOs: " + DateHelper.getRuntime(timeStamp2));
        // final long timeStamp3 = System.currentTimeMillis();
        // System.out.println("Anzahl MIOs vor lowTrustRemoval: " + mios.size());

        // Calculate Trust and sort by trust with Comparator
        final MIOComparator mioComp = new MIOComparator();
        
        //load the trainedClassifier
        MIOClassifier mioClass = new MIOClassifier();
        mioClass.loadTrainedClassifier();
        
//        System.out.println("RMSE: "+mioClass.getRMSE());
        
        Set<MIO> sortedMIOs = new TreeSet<MIO>(mioComp);
        for (Entry<String, MIO> entry : mios.entrySet()) {
            final MIO mio = entry.getValue();
            mioClass.classify(mio);
//            calculateTrust(mio);
            sortedMIOs.add(mio);
        }
        // System.out.println("SORTING AND TRUST CALCULATION: " + DateHelper.getRuntime(timeStamp3));
        // final long timeStamp4 = System.currentTimeMillis();


        // remove MIOs that do not fulfill the trustlimit
        // sortedMIOs = removeLowTrustedMIOs(sortedMIOs, trustLimit);

        // System.out.println("Anzahl MIOs nach lowTrustRemoval: " + sortedMIOs.size());

        // detect and save rolePages
        detectRolePages(sortedMIOs, entity);
        // System.out.println("REMOVING LOW-TRUSTED AND DETECTING ROLEPAGES: " + DateHelper.getRuntime(timeStamp4));

        // print the MIOFeatures out
        printMIOFeaturesToFile(mios);
        printSetToHTMLFile(sortedMIOs);

        // System.out.println("PREPARE SAVING EXTRACTIONRESULTS! " + sortedMIOs.size() + " MIOs gefunden!");
        for (MIO mio : sortedMIOs) {
            // if (mio.getDirectURL().length() <= 767&& mio.getFindPageURL().length()<=767) {
            // prepareSavingResults(mio);
            // } else {
            if (mio.getDirectURL().length() >= 767) {
                String directURL = mio.getDirectURL().substring(0, 766);
                mio.setDirectURL(directURL);
            }
            if (mio.getFindPageURL().length() >= 767) {
                String findPageURL = mio.getFindPageURL().substring(0, 766);
                mio.setFindPageURL(findPageURL);
            }

            prepareSavingResults(mio);
            // LOGGER.error("DirectURL zu lang fuer DB!!!" + mio.getTrust());
            // }

        }

        LOGGER.info("Thread finished in " + DateHelper.getRuntime(timeStamp1) + "  " + mios.size() + " MIOs for \""
                + entity.getName() + "\" were found.");
        try {
            
            FileHelper.appendFile("f:/timeStamps.txt", DateHelper.getCurrentDatetime("yyyy-MM-dd HH:mm:ss") + "\r\n");
            FileHelper.appendFile("f:/timeStamps.txt", "Thread finished in " + DateHelper.getRuntime(timeStamp1) + "  "
                    + mios.size() + " MIOs for \"" + entity.getName() + "\" were found." + "\r\n");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // save Extraction-Results to database
        knowledgeManager.saveExtractions();

        MIOExtractor.getInstance().decreaseThreadCount();
    }

    /**
     * Detect rolePages.
     * 
     * @param sortedMIOs the sortedMIOs
     * @param entity the entity
     */
    private void detectRolePages(final Set<MIO> sortedMIOs, final Entity entity) {
        final RolePageDetector rpDetector = new RolePageDetector(entity);
        rpDetector.detectRolePages(sortedMIOs);
    }

    /**
     * Prepare the saving of extractionResults.
     * Associate Attribute, FactValue, Trust and Fact with Entity.
     * 
     * @param mio the MIO
     */
    private void prepareSavingResults(final MIO mio) {
        final Attribute attribute = new Attribute(mio.getInteractivityGrade() + " mio", Attribute.VALUE_STRING,
                entity.getConcept());
        attribute.setExtractedAt(new Date(System.currentTimeMillis()));

        final FactValue factValue = new FactValue(mio.getDirectURL(), new Source(mio.getFindPageURL()), 0);
        factValue.setTrust(mio.getTrust());
        factValue.setExtractedAt(new Date(System.currentTimeMillis()));

        entity.addFactAndValue(new Fact(attribute), factValue);

        // factString = StringHelper.trim(factString);
        // if (!(factString.length() > 250)) {
        // if (!(mio.getFindPageURL().length() > 500)) {
        //
        // counter++;
        // }
        //
        // } else {
        // System.out.println("FactString zu lang! " + mio.getDirectURL().length());
        // System.out.println("oder FindPageURL zu lang! " + mio.getFindPageURL().length());
        // }

    }

    /**
     * Removes the low-trusted MIOs.
     * 
     * @param sortedMIOs the sorted MIOs
     * @param trustLimit the trustLimit
     * @return the set of MIOs which fulfill the trustLimit
     */
    // private Set<MIO> removeLowTrustedMIOs(final Set<MIO> sortedMIOs, final double trustLimit) {
    // final MIOComparator mioComp = new MIOComparator();
    // final Set<MIO> resultSet = new TreeSet<MIO>(mioComp);
    // for (MIO mio : sortedMIOs) {
    // if (mio.getTrust() >= trustLimit) {
    // resultSet.add(mio);
    // }
    // }
    // return resultSet;
    //
    // }

    /**
     * Prints the set to HTML-File.
     * 
     * @param cleanedMIOs the cleanedMIOs
     */
    private void printSetToHTMLFile(final Set<MIO> cleanedMIOs) {

        try {
            FileHelper.appendFile("f:/test.html", "<html><body> <hr />");

            for (MIO mio : cleanedMIOs) {

                final String output = " TRUST: " + mio.getTrust()
                        + " Interactivity: " + mio.getInteractivityGrade() + " for Entity: " + mio.getEntity().getName() + " Type: "
                            + mio.getMIOType() +" <a href=\"" + mio.getDirectURL() + "\">"
                        + mio.getDirectURL() + "</a> founded on <a href=\"" + mio.getFindPageURL() + "\">"
                        + mio.getFindPageURL() + "</a> <br><br>";
                // System.out.println(output);
                FileHelper.appendFile("f:/test.html", output + "\r\n");

            }
            FileHelper.appendFile("f:/test.html", "</body></html>");
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Calculate the trust with own approach.
     * 
     * @param mio the MIO
     */
    private void calculateTrust(final MIO mio) {

        int factor = 4;

        final Map<String, Double> mioFeatures = mio.getFeatures();
        // System.out.println("Calculate Trust loading " + mioFeatures.size() + " Features.. "
        // + mioFeatures.keySet().toString());
        final double fileNameRelevance = mioFeatures.get("FileNameRelevance");
        final double filePathRelevance = mioFeatures.get("FilePathRelevance");
        final double badWordAbsence = mioFeatures.get("BadWordAbsence");
        final double headlineRelevance = mioFeatures.get("HeadlineRelevance");
        final double altTextRelevance = mioFeatures.get("ALTTextRelevance");
        final double surroundTextRelevance = mioFeatures.get("SurroundingTextRelevance");
        final double titleRelevance = mioFeatures.get("TitleRelevance");
        final double linkNameRelevance = mioFeatures.get("LinkNameRelevance");
        final double linkTitleRelevance = mioFeatures.get("LinkTitleRelevance");
        final double iframeParentTitleRelevance = mioFeatures.get("IFrameParentRelevance");
        final double urlRelevance = mioFeatures.get("PageURLRelevance");
        final double dpTrust = mioFeatures.get("DedicatedPageTrustRelevance");
//        final double xmlFileNameRelevance = mioFeatures.get("XMLFileNameRelevance");
//        final double xmlFileContentRelevance = mioFeatures.get("XMLFileContentRelevance");
        final double textContentRelevance = mioFeatures.get("TextContentRelevance");
        final double resolutionRelevance = mioFeatures.get("ResolutionRelevance");

        final double pageContextTrust = titleRelevance + linkNameRelevance + linkTitleRelevance
                + iframeParentTitleRelevance + urlRelevance + (dpTrust * factor);

        double mioTrust = pageContextTrust + (fileNameRelevance * factor) + (filePathRelevance) + altTextRelevance
                + headlineRelevance + surroundTextRelevance; //+ xmlFileNameRelevance + xmlFileContentRelevance;

        final double contentRelevance = (textContentRelevance + resolutionRelevance) * 2;

        mioTrust = mioTrust + contentRelevance;

        // if badWords are contained in the directLink-URL the whole trust is influenced
        if (badWordAbsence == 0) {
            mioTrust = mioTrust / 2;
        }

        mio.setTrust(mioTrust);

    }

    /**
     * Prints the featureMap to file.
     * 
     * @param cleanedMIOs the cleaned MIOs
     */
    private void printMIOFeaturesToFile(final Map<String, MIO> cleanedMIOs) {

        for (Entry<String, MIO> cMio : cleanedMIOs.entrySet()) {
            final MIO mio = cMio.getValue();
            final StringBuffer sBuffer = new StringBuffer();
            final Map<String, Double> mioFeatures = mio.getFeatures();

            sBuffer.append("# " + mio.getEntity().getName() + " Trust: " + mio.getTrust() + " " + mio.getDirectURL()
                    + "\r\n");
            sBuffer.append("# " + mio.getFindPageURL() + "\r\n");
//             sBuffer.append(mio.getFeatures().keySet().toString() + "\r\n");

            for (Entry<String, Double> feature : mioFeatures.entrySet()) {
                sBuffer.append(feature.getValue());
                sBuffer.append(";");

            }
            final String output = sBuffer.toString();
            // System.out.println(output);
            try {
                FileHelper.appendFile("f:/features.txt", output + "\r\n");
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

}
