/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.Locale;

import org.apache.log4j.Logger;

import tud.iir.knowledge.Entity;

/**
 * The Class MIOContextAnalyzer analyze the context and sets the features.
 */
public class MIOContextAnalyzer {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MIOContextAnalyzer.class);

    /** The entity. */
    private transient Entity entity;

    private transient double fileNameRelevance = 0;

    /** The file path relevance. */
    private transient double filePathRelevance = 0;

    /** The bad word absence. */
    private transient double badWordAbsence = 1;

    /** The headline relevance. */
    private transient double headlineRelevance = 0;

    /** The ALT-Ttext relevance. */
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

    /** A List of bad words. */
    final transient String[] badWords = { "banner", "tower", "titlefont", "newsletter", "cloud", "footer", "ticker",
            "ads", "expressinstall" };

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
     * Calculates features of the mio itself.
     * 
     * @param mio the mio
     */
    private void calcMIOFeatures(final MIO mio) {
        calcFileFeatures(mio);
        calcTagFeatures(mio);
    }

    /**
     * Calculate file features.
     * 
     * @param mio the mio
     */
    private void calcFileFeatures(final MIO mio) {
        calcFileNameRelevance(mio);
        calcFilePathRelevance(mio);
        calcBadWordAbsence(mio);
    }

    /**
     * Calculates the features of a relevant HTML-Tag.
     * 
     * @param mio the mio
     */
    private void calcTagFeatures(final MIO mio) {
        calcHeadlineRelevance(mio);
        calcALTTextRelevance(mio);
        calcSurroundingTextRelevance(mio);
    }

    /**
     * Calculates the features of the mioPage.
     * 
     * @param mioPage the mio page
     */
    private void calcPageFeatures(final MIOPage mioPage) {
        calcPageTitleRelevance(mioPage);
        calcLinkedPageRelevance(mioPage);
        calcIFrameRelevance(mioPage);
        calcPageURLRelevance(mioPage);
        this.dpTrust = mioPage.getDedicatedPageTrust();
    }

    /**
     * Calculates trust.
     * 
     * @param mio the mio
     */
    public void calculateTrust(final MIO mio) {
        this.entity = mio.getEntity();

        // calculate the MIOFeatures first
        calcMIOFeatures(mio);

        final double pageContextTrust = calcPageContextTrust();

        final double mioTrust = pageContextTrust + (fileNameRelevance * 4) + (filePathRelevance * 2) + altTextRelevance
                + headlineRelevance + surroundTextRelevance;

        mio.setTrust(mioTrust);
        mio.setTrust(checkForBadWords(mio));
    }

    /**
     * Initially calculate the trust for a mioPage which influences all its containing MIOs.
     * 
     * @return the double
     */
    private double calcPageContextTrust() {

        final double pcTrust = titleRelevance + linkNameRelevance + linkTitleRelevance + iframeParentTitleRelevance
                + urlRelevance + (dpTrust * 4);
        return pcTrust;

    }

    /**
     * Calculates string relevance.
     * 
     * @param inputString the input string
     * @param entity the entity
     * @return the double
     */
    private double calcStringRelevance(final String inputString, final Entity entity) {
        return calcStringRelevance(inputString, entity.getName());
    }

    /**
     * Calculates the relevance of a string by checking how many terms or morphs of the entityName are included in the
     * string. A special role play words like
     * d500 or x500i. Returns: a value from 0 to 1
     * 
     * @param inputString the string
     * @param entityName the entity name
     * @return the double
     */
    private double calcStringRelevance(final String inputString, final String entityName) {
        final SearchWordMatcher swm = new SearchWordMatcher(entityName);

        final String[] elements = entityName.split("\\s");
        // String input[] = inputString.split("\\s");
        // calculate the number of searchWord-Matches
        final double numOfMatches = (double) swm.getNumberOfSearchWordMatches(inputString);
        // System.out.println("number of swm: " + NumOfMatches);
        // calculate the number of searchWord-Matches with ignoring specialWords
        // like D500x
        final double numOfMatchesWithoutSW = (double) swm.getNumberOfSearchWordMatches(inputString, true,
                entityName.toLowerCase(Locale.ENGLISH));
        // System.out.println("number of swm without SW: " + NumOfMatchesWithoutSW);
        final double diff = (double) numOfMatches - numOfMatchesWithoutSW;

        double result = (double) ((numOfMatchesWithoutSW * 2) + (diff * 3)) / (double) (elements.length * 2);

        if (diff > 0) {

            result = (double) (numOfMatches - 1) / elements.length + (diff / elements.length)
                    + (diff / (2 * elements.length));
            //
            result = (double) numOfMatches / elements.length + (diff / (2 * elements.length));
            result = (double) ((numOfMatchesWithoutSW * 2) + (diff * 3)) / (double) (elements.length * 2);
        } else {
            result = (double) numOfMatches / elements.length;
        }

        // final JaroWinkler jaroWinkler = new JaroWinkler();
        // double result = (double) jaroWinkler.getSimilarity(entityName, inputString);

        if (result > 1) {
            // System.out.println("result groesser 1!");
            result = 1;
        }
        return result;
    }

    /**
     * Check for bad words that are not contained within the entityname.
     * 
     * @param mio the MIO
     * @return the double
     */
    private double checkForBadWords(final MIO mio) {

        for (String badWord : badWords) {
            if (!mio.getEntity().getName().contains(badWord) && mio.getDirectURL().contains(badWord)) {
                final double result = mio.getTrust();
                return (result / 2);
            }
        }
        return mio.getTrust();
    }

    /**
     * Calculates file name relevance.
     * 
     * @param mio the mio
     */
    private void calcFileNameRelevance(final MIO mio) {
        this.fileNameRelevance = (calcStringRelevance(mio.getFileName(), entity));
    }

    /**
     * Calculates file path relevance.
     * 
     * @param mio the mio
     */
    private void calcFilePathRelevance(final MIO mio) {
        this.filePathRelevance = calcStringRelevance(mio.getDirectURL(), entity);

    }

    /**
     * Calculates the absence of badWords.
     * Initially the badWordAbsence is 1. If a badWord was find it is set to 0.
     * 
     * @param mio the mio
     */
    private void calcBadWordAbsence(final MIO mio) {
        for (String badWord : badWords) {
            if (!mio.getEntity().getName().contains(badWord) && mio.getDirectURL().contains(badWord)) {
                this.badWordAbsence = 0;
            }
        }
    }

    /**
     * Calculates headline relevance.
     * 
     * @param mio the mio
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
                this.headlineRelevance = calcStringRelevance(headlines, entity);
            }
        }

    }

    /**
     * Calculates ALT-Text relevance.
     * 
     * @param mio the mio
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
                this.altTextRelevance = calcStringRelevance(altText, entity);
            }
        }
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
                surroundTextRelevance = calcStringRelevance(surroundingText, entity);
            }
        }
    }

    /**
     * Calculates the relevance of the mioPage-Title.
     * 
     * @param mioPage the mioPage
     */
    private void calcPageTitleRelevance(final MIOPage mioPage) {
        this.titleRelevance = calcStringRelevance(mioPage.getTitle(), entity);
    }

    /**
     * Calculates linked page relevance.
     * 
     * @param mioPage the mio page
     */
    private void calcLinkedPageRelevance(final MIOPage mioPage) {

        if (mioPage.isLinkedPage()) {
            this.linkNameRelevance = calcStringRelevance(mioPage.getLinkName(), entity);
            this.linkTitleRelevance = calcStringRelevance(mioPage.getLinkTitle(), entity);
        }
    }

    /**
     * Calculates iframe relevance.
     * 
     * @param mioPage the mio page
     */
    private void calcIFrameRelevance(final MIOPage mioPage) {

        if (mioPage.isIFrameSource()) {
            this.iframeParentTitleRelevance = calcStringRelevance(mioPage.getIframeParentPageTitle(), entity);
        }
    }

    /**
     * Calculates mioPageURL relevance.
     * 
     * @param mioPage the mioPage
     */
    private void calcPageURLRelevance(final MIOPage mioPage) {
        this.urlRelevance = calcStringRelevance(mioPage.getUrl(), entity);
    }

}
