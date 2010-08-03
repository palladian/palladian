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
    private transient Entity entity;

    /** The file name relevance. */
    private transient double fileNameRelevance = 0;

    /** The file path relevance. */
    private transient double filePathRelevance = 0;

    /** The bad word absence. */
    private transient double badWordAbsence = 1;

    /** The headline relevance. */
    private transient double headlineRelevance = 0;

    /** The ALT-Text relevance. */
    private transient double altTextRelevance = 0;

    /** The surround text relevance. */
    private transient double surroundTextRelevance = 0;

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

    /** The xml file name relevance. */
    private transient double xmlFileNameRelevance = 0;

    /** The xml file content relevance. */
    private transient double xmlFileContentRelevance = 0;

    /** A List of bad words. */
    final transient String[] badWords = { "banner", "tower", "titlefont", "newsletter", "cloud", "footer", "ticker",
            "ads", "expressinstall", "header", "advertise", "logo" };

    /**
     * Instantiates a new mioContextAnalyzer.
     * 
     * @param entity the entity
     * @param mioPage the mioPage
     */
    public MIOContextAnalyzer(final Entity entity, final MIOPage mioPage) {
        this.entity = entity;
        // this.mioPage = mioPage;
        calcPageFeatures(mioPage);
    }

    /**
     * Sets the features.
     * 
     * @param mio the new features
     */
    public void setFeatures(final MIO mio) {

        // calculate the MIOFeatures first
        calcMIOFeatures(mio);

        setFileFeatures(mio);
        setTagFeatures(mio);
        setPageFeatures(mio);
    }

    /**
     * Sets the file features.
     * 
     * @param mio the new file features
     */
    private void setFileFeatures(final MIO mio) {
        this.entity = mio.getEntity();

        mio.setFeature("FileNameRelevance", fileNameRelevance);
        mio.setFeature("FilePathRelevance", filePathRelevance);
        mio.setFeature("BadWordAbsence", badWordAbsence);
    }

    /**
     * Sets the tag features.
     * 
     * @param mio the new tag features
     */
    private void setTagFeatures(final MIO mio) {
        this.entity = mio.getEntity();

        mio.setFeature("ALTTextRelevance", altTextRelevance);
        mio.setFeature("HeadlineRelevance", headlineRelevance);
        mio.setFeature("SurroundingTextRelevance", surroundTextRelevance);
        mio.setFeature("XMLFileNameRelevance", xmlFileNameRelevance);
        mio.setFeature("XMLFileContentRelevance", xmlFileContentRelevance);
    }

    /**
     * Sets the page features.
     * 
     * @param mio the new page features
     */
    private void setPageFeatures(final MIO mio) {

        mio.setFeature("TitleRelevance", titleRelevance);
        mio.setFeature("LinkNameRelevance", linkNameRelevance);
        mio.setFeature("LinkTitleRelevance", linkTitleRelevance);
        mio.setFeature("IFrameParentRelevance", iframeParentTitleRelevance);
        mio.setFeature("PageURLRelevance", urlRelevance);
        mio.setFeature("DedicatedPageTrustRelevance", dpTrust);
    }

    /**
     * Calculates features of the MIO itself.
     * 
     * @param mio the MIO
     */
    private void calcMIOFeatures(final MIO mio) {
        calcFileFeatures(mio);
        calcTagFeatures(mio);
    }

    /**
     * Calculate file features.
     * 
     * @param mio the MIO
     */
    private void calcFileFeatures(final MIO mio) {
        calcFileNameRelevance(mio);
        calcFilePathRelevance(mio);
        calcBadWordAbsence(mio);
        calcXMLNameRelevance(mio);
        calcXMLContentRelevance(mio);
    }

    /**
     * Calculates the features of a relevant HTML-Tag.
     * 
     * @param mio the MIO
     */
    private void calcTagFeatures(final MIO mio) {
        calcHeadlineRelevance(mio);
        calcALTTextRelevance(mio);
        calcSurroundingTextRelevance(mio);
    }

    /**
     * Calculates the features of the mioPage.
     * 
     * @param mioPage the MIOPage
     */
    private void calcPageFeatures(final MIOPage mioPage) {
        calcPageTitleRelevance(mioPage);
        calcLinkedPageRelevance(mioPage);
        calcIFrameRelevance(mioPage);
        calcPageURLRelevance(mioPage);
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
     * @param mio the MIO
     */
    private void calcFileNameRelevance(final MIO mio) {
        this.fileNameRelevance = (RelevanceCalculator.calcStringRelevance(mio.getFileName(), entity));
    }

    /**
     * Calculates file path relevance.
     * 
     * @param mio the MIO
     */
    private void calcFilePathRelevance(final MIO mio) {
        this.filePathRelevance = RelevanceCalculator.calcStringRelevance(mio.getDirectURL(), entity);

    }

    /**
     * Calculates the absence of badWords.
     * Initially the badWordAbsence is 1. If a badWord was find it is set to 0.
     * 
     * @param mio the MIO
     */
    private void calcBadWordAbsence(final MIO mio) {
        for (String badWord : badWords) {
            if (!mio.getEntity().getName().toLowerCase(Locale.ENGLISH).contains(badWord)
                    && mio.getDirectURL().toLowerCase(Locale.ENGLISH).contains(badWord)) {
                this.badWordAbsence = 0;
            }
            // check for Resolution Information
            if ((badWordAbsence >= 0) && hasResolution(mio)) {
                this.badWordAbsence = 0;
            }
        }
    }

    /**
     * Calculates headline relevance.
     * 
     * @param mio the MIO
     */
    private void calcHeadlineRelevance(final MIO mio) {

        String headlines = "";
        if (mio.getInfos().containsKey("previousHeadlines")) {
            try {
                // get the nearest previousHeadline
                headlines = (String) mio.getInfos().get("previousHeadlines").get(0);
            } catch (Exception e) {
                // its not relevant, so info is enough
                LOGGER.info(e.getMessage());
            }

            if (headlines.length() > 1) {
                this.headlineRelevance = RelevanceCalculator.calcStringRelevance(headlines, entity);
            }
        }

    }

    /**
     * Calculates ALT-Text relevance.
     * 
     * @param mio the MIO
     */
    private void calcALTTextRelevance(final MIO mio) {

        String altText = "";
        if (mio.getInfos().containsKey("altText")) {
            try {
                altText = (String) mio.getInfos().get("altText").get(0);
            } catch (Exception e) {
                LOGGER.info("NO ERROR: " + e.getMessage());
            }

            if (altText.length() > 1) {
                this.altTextRelevance = RelevanceCalculator.calcStringRelevance(altText, entity);
            }
        }
    }

    /**
     * Calculates XMLFileNameRelevance.
     * 
     * @param mio the MIO
     */
    private void calcXMLNameRelevance(final MIO mio) {

        // if (mio.getInfos().containsKey("xmlFileName")) {
        final List<String> xmlFileNames = mio.getInfos().get("xmlFileName");

        if (xmlFileNames != null) {
            double highestRelevance = 0;
            // check every xmlFileName for Relevance, the most relevanced file dominates the result
            for (String xmlfileName : xmlFileNames) {
                final double tempRelevance = RelevanceCalculator.calcStringRelevance(xmlfileName, entity);
                if (tempRelevance > highestRelevance) {
                    highestRelevance = tempRelevance;
                }

            }
            this.xmlFileNameRelevance = highestRelevance;
        }
    }

    // }

    /**
     * Calculates XMLFileContentRelevance.
     * 
     * @param mio the MIO
     */
    private void calcXMLContentRelevance(final MIO mio) {

        final List<String> xmlFileURLs = mio.getInfos().get("xmlFileURL");
        if (xmlFileURLs != null) {
            for (String xmlFileURL : xmlFileURLs) {
                final String xmlContent = extractXMLContent(xmlFileURL);

                final double tempContentRelevance = RelevanceCalculator.calcStringRelevance(xmlContent, entity);
                if (tempContentRelevance > xmlFileContentRelevance) {
                    xmlFileContentRelevance = tempContentRelevance;
                }
            }
        }

    }

    /**
     * Extract the content of an XML-File.
     * 
     * @param xmlFileURL the XML-File-URL
     * @return the complete Content(incl. tags) as String
     */
    public static String extractXMLContent(final String xmlFileURL) {
        String result = "";
        // Document document = null;
        // SAXBuilder builder = new SAXBuilder(false);
        // try {
        // URL url = new URL(xmlFileURL);
        // document = builder.build(url);
        // DocType docType = new DocType("html", "-//W3C...", "http://...");
        // document.setDocType(docType);
        //
        // } catch (Exception e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // if (document != null) {
        // // List<Comment> contentList = document.getContent();
        // System.out.println(document.toString());
        // // for(Comment content:contentList){
        // // System.out.println(content.toString());
        // // }
        //
        // }

        try {
            final File contentFile = Crawler.downloadBinaryFile(xmlFileURL, "F:Temp");
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
     * @param mio the mio
     */
    private void calcSurroundingTextRelevance(final MIO mio) {
        String surroundingText = "";
        if (mio.getInfos().containsKey("surroundingText")) {
            try {
                surroundingText = (String) mio.getInfos().get("surroundingText").get(0);
            } catch (Exception e) {
                // its not relevant, so info is enough
                LOGGER.info(e.getMessage());
            }

            if (surroundingText.length() > 1) {
                surroundTextRelevance = RelevanceCalculator.calcStringRelevance(surroundingText, entity);
            }
        }
    }

    /**
     * Calculates the relevance of the mioPage-Title.
     * 
     * @param mioPage the mioPage
     */
    private void calcPageTitleRelevance(final MIOPage mioPage) {
        this.titleRelevance = RelevanceCalculator.calcStringRelevance(mioPage.getTitle(), entity);
    }

    /**
     * Calculates linked page relevance.
     * 
     * @param mioPage the mio page
     */
    private void calcLinkedPageRelevance(final MIOPage mioPage) {

        if (mioPage.isLinkedPage()) {
            this.linkNameRelevance = RelevanceCalculator.calcStringRelevance(mioPage.getLinkName(), entity);
            this.linkTitleRelevance = RelevanceCalculator.calcStringRelevance(mioPage.getLinkTitle(), entity);
        }
    }

    /**
     * Calculates iframe relevance.
     * 
     * @param mioPage the mio page
     */
    private void calcIFrameRelevance(final MIOPage mioPage) {

        if (mioPage.isIFrameSource()) {
            this.iframeParentTitleRelevance = RelevanceCalculator.calcStringRelevance(
                    mioPage.getIframeParentPageTitle(), entity);
        }
    }

    /**
     * Calculates mioPageURL relevance.
     * 
     * @param mioPage the mioPage
     */
    private void calcPageURLRelevance(final MIOPage mioPage) {
        this.urlRelevance = RelevanceCalculator.calcStringRelevance(mioPage.getUrl(), entity);
    }

    // /**
    // * The main method.
    // *
    // * @param args the arguments
    // */
    // public static void main(String[] args) {
    // MIOContextAnalyzer.extractXMLContent("http://wave.samsungmobile.com/flash/bin/xml/config.xml");
    // }

}
