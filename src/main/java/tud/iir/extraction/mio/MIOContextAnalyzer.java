/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import tud.iir.knowledge.Entity;
import tud.iir.web.Crawler;

/**
 * The Class MIOContextAnalyzer analyze the context and sets the features.
 */
public class MIOContextAnalyzer {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MIOContextAnalyzer.class);

    /** The entity. */
    private final transient Entity entity;
    
    /** The mio page. */
    private final transient MIOPage mioPage;
    
    /** The mio. */
    private transient MIO mio;

    /** The title relevance. */
    private transient double titleRelevance = 0;

    /** The link name relevance. */
    private transient double linkNameRelevance = 0;

    /** The link title relevance. */
    private transient double linkTitleRelevance = 0;

    /** The iframe-parent-title relevance. */
    private transient double iframeParentTitleRelevance = 0;

    /** The URL relevance. */
    private transient double urlRelevance = 0;

    /** The DedicatedPageTrust (Relevance). */
    private transient double dpTrust = 0;
    
    private final transient List<String> badWords = InCoFiConfiguration.getInstance().getBadWords();

    /**
     * Instantiates a new mioContextAnalyzer.
     * 
     * @param entity the entity
     * @param mioPage the mioPage
     */
    public MIOContextAnalyzer(final Entity entity, final MIOPage mioPage) {
        this.entity = entity;
        this.mioPage = mioPage;
        //calculate the features of this MIOPage
        calcPageFeatures();
    }

    /**
     * Sets the features.
     * 
     * @param mio the new features
     */
    public void setFeatures(final MIO mio) {        
        this.mio=mio;

        setFileFeatures();
        setTagFeatures();
        setPageFeatures();
    }

    /**
     * Sets the file features.
     *
     */
    private void setFileFeatures() {
       
        mio.setFeature("FileNameRelevance", calcFileNameRelevance());
        mio.setFeature("FilePathRelevance", calcFilePathRelevance());
        mio.setFeature("BadWordAbsence", calcBadWordAbsence());
    }

    /**
     * Sets the tag features.
     *
     */
    private void setTagFeatures() {
        
        mio.setFeature("ALTTextRelevance", calcALTTextRelevance());
        mio.setFeature("HeadlineRelevance", calcHeadlineRelevance());
        mio.setFeature("SurroundingTextRelevance", calcSurroundingTextRelevance());
//        mio.setFeature("XMLFileNameRelevance", calcXMLNameRelevance());
//        mio.setFeature("XMLFileContentRelevance", calcXMLContentRelevance());
    }

    /**
     * Sets the features which were calculated for the MIOPage.
     *
     */
    private void setPageFeatures() {

        mio.setFeature("TitleRelevance", titleRelevance);
        mio.setFeature("LinkNameRelevance", linkNameRelevance);
        mio.setFeature("LinkTitleRelevance", linkTitleRelevance);
        mio.setFeature("IFrameParentRelevance", iframeParentTitleRelevance);
        mio.setFeature("PageURLRelevance", urlRelevance);
        mio.setFeature("DedicatedPageTrustRelevance", dpTrust);
    }


    /**
     * Calculates the features of the mioPage.
     *
     */
    private void calcPageFeatures() {
        calcPageTitleRelevance();
        calcLinkedPageRelevance();
        calcIFrameRelevance();
        calcPageURLRelevance();
        this.dpTrust = mioPage.getDedicatedPageTrust();
    }

    /**
     * Checks for resolution information like 120x200 which is often an indicator for an advertisement.
     * 
     * @param mio the MIO
     * @return true, if successful
     */
    private boolean hasResolution(final MIO mio) {
        boolean returnValue;
        if (mio.getFileName().toLowerCase(Locale.ENGLISH).matches(".*\\d+x\\d+.*")) {
            returnValue = true;
        } else {
            returnValue = false;
        }
        return returnValue;
    }

    /**
     * Calculates file name relevance.
     *
     * @return the double
     */
    private double calcFileNameRelevance() {
        return (RelevanceCalculator.calcStringRelevance(mio.getFileName(), entity));
    }

    /**
     * Calculates file path relevance.
     *
     * @return the double
     */
    private double calcFilePathRelevance() {
        return RelevanceCalculator.calcStringRelevance(mio.getDirectURL(), entity);

    }

    /**
     * Calculates the absence of badWords.
     * Initially the badWordAbsence is 1. If a badWord was find it is set to 0.
     *
     * @return the double
     */
    private double calcBadWordAbsence() {
        double returnValue=1;
        for (String badWord : badWords) {
            if (!mio.getEntity().getName().toLowerCase(Locale.ENGLISH).contains(badWord)
                    && mio.getDirectURL().toLowerCase(Locale.ENGLISH).contains(badWord)) {
                returnValue = 0;
            }
            // check for Resolution Information
            if ((returnValue >= 0) && hasResolution(mio)) {
               returnValue = 0;
            }
        }
        return returnValue;
    }

    /**
     * Calculates headline relevance.
     *
     * @return the double
     */
    private double calcHeadlineRelevance() {
        
        double returnValue=0;
        String headlines = mio.getPreviousHeadlines();
//        if (mio.getInfos().containsKey("previousHeadlines")) {
//            try {
//                // get the nearest previousHeadline
//                headlines = (String) mio.getInfos().get("previousHeadlines").get(0);
//            } catch (Exception e) {
//                // its not relevant, so info is enough
//                LOGGER.info(e.getMessage());
//            }

            if (headlines.length() > 1) {
                returnValue = RelevanceCalculator.calcStringRelevance(headlines, entity);
            }
//        }
        return returnValue;

    }

    /**
     * Calculates ALT-Text relevance.
     *
     * @return the double
     */
    private double calcALTTextRelevance() {
        double returnValue=0;

        String altText = mio.getAltText();
//        if (mio.getInfos().containsKey("altText")) {
//            try {
//                altText = (String) mio.getInfos().get("altText").get(0);
//            } catch (Exception e) {
//                LOGGER.info("NO ERROR: " + e.getMessage());
//            }

            if (altText.length() > 1) {
                returnValue = RelevanceCalculator.calcStringRelevance(altText, entity);
            }
//        }
        return returnValue;
    }

    /**
     * Calculates XMLFileNameRelevance.
     *
     * @return the double
     */
    private double calcXMLNameRelevance() {
        
        double returnValue=0;
        // if (mio.getInfos().containsKey("xmlFileName")) {
//        final List<String> xmlFileNames = mio.getInfos().get("xmlFileName");
//
//        if (xmlFileNames != null) {
//            double highestRelevance = 0;
//            // check every xmlFileName for Relevance, the most relevanced file dominates the result
//            for (String xmlfileName : xmlFileNames) {
//                final double tempRelevance = RelevanceCalculator.calcStringRelevance(xmlfileName, entity);
//                if (tempRelevance > highestRelevance) {
//                    highestRelevance = tempRelevance;
//                }
//
//            }
//            returnValue = highestRelevance;
//        }
        
        return returnValue;
    }

    // }

    /**
     * Calculates XMLFileContentRelevance.
     *
     * @return the double
     */
    private double calcXMLContentRelevance() {
        
        double returnValue=0;

//        final List<String> xmlFileURLs = mio.getInfos().get("xmlFileURL");
//        if (xmlFileURLs != null) {
//            for (String xmlFileURL : xmlFileURLs) {
//                final String xmlContent = extractXMLContent(xmlFileURL);
//
//                final double tempContentRelevance = RelevanceCalculator.calcStringRelevance(xmlContent, entity);
//                if (tempContentRelevance > returnValue) {
//                    returnValue = tempContentRelevance;
//                }
//            }
//        }
        return returnValue;

    }

    /**
     * Extract the content of an XML-File.
     * 
     * @param xmlFileURL the XML-File-URL
     * @return the complete Content(incl. tags) as String
     */
    public static String extractXMLContent(final String xmlFileURL) {
        String result = "";

        try {
            final String downloadPath = InCoFiConfiguration.getInstance().tempDirPath;
            final File contentFile = Crawler.downloadBinaryFile(xmlFileURL, downloadPath);
            final FileReader fReader = new FileReader(contentFile);
            final BufferedReader bufReader = new BufferedReader(fReader);
            final StringBuffer output = new StringBuffer();
            String tempString;
            while ((tempString = bufReader.readLine()) != null) {
                output.append(tempString.trim());
            }
            result = output.toString();
            bufReader.close();
        } catch (Exception fx) {
            LOGGER.error(fx.getMessage());
        }

        return result;
    }

    /**
     * Calculates surrounding text relevance.
     *
     * @return the double
     */
    private double calcSurroundingTextRelevance() {
        double returnValue=0;
        String surroundingText = mio.getSurroundingText();
//        if (mio.getInfos().containsKey("surroundingText")) {
//            try {
//                surroundingText = (String) mio.getInfos().get("surroundingText").get(0);
//            } catch (Exception e) {
//                // its not relevant, so info is enough
//                LOGGER.info(e.getMessage());
//            }

            if (surroundingText.length() > 1) {
                returnValue = RelevanceCalculator.calcStringRelevance(surroundingText, entity);
            }
//        }
        return returnValue;
    }

    /**
     * Calculates the relevance of the mioPage-Title.
     *
     */
    private void calcPageTitleRelevance() {
        this.titleRelevance = RelevanceCalculator.calcStringRelevance(mioPage.getTitle(), entity);
    }

    /**
     * Calculates linked page relevance.
     *
     */
    private void calcLinkedPageRelevance() {

        if (mioPage.isLinkedPage()) {
            this.linkNameRelevance = RelevanceCalculator.calcStringRelevance(mioPage.getLinkName(), entity);
            this.linkTitleRelevance = RelevanceCalculator.calcStringRelevance(mioPage.getLinkTitle(), entity);
        }
    }

    /**
     * Calculates iframe relevance.
     *
     */
    private void calcIFrameRelevance() {

        if (mioPage.isIFrameSource()) {
            this.iframeParentTitleRelevance = RelevanceCalculator.calcStringRelevance(
                    mioPage.getIframeParentPageTitle(), entity);
        }
    }

    /**
     * Calculates mioPageURL relevance.
     *
     */
    private void calcPageURLRelevance() {
        this.urlRelevance = RelevanceCalculator.calcStringRelevance(mioPage.getUrl(), entity);
    }

}
