/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The FastMIODetector simply analyze a MIOPageCandidate for pure MIO-Existence by some indicators.
 * 
 * @author Martin Werner
 */
public class FastMIODetector {

    /** A List of MIOIndicators. */
    private List<String> mioIndicator;

    /**
     * Instantiates a new fast MIODetector.
     */
    public FastMIODetector() {
        mioIndicator = new ArrayList<String>();

        mioIndicator.add(".swf");
        mioIndicator.add(".mov");
        mioIndicator.add(".xap");
        mioIndicator.add(".jar");
        mioIndicator.add(".class");
        mioIndicator.add("<canvas>");
        mioIndicator.add("application/x-shockwave-flash");
        mioIndicator.add("flashvars");
        mioIndicator.add("swfobject");
        mioIndicator.add("getflash");

    }

    /**
     * Gets the MIOPage.
     * 
     * @param pageContent the pageContent
     * @param pageURL the page URL
     * @return the MIOpages
     */
    public MIOPage getMioPage(final String pageContent, final String pageURL) {

        MIOPage mioPage = null;

        final String lowerPageContent = pageContent.toLowerCase(Locale.ENGLISH);
        if (containsMIO(lowerPageContent)) {

            mioPage = new MIOPage(pageURL);

        }
        return mioPage;
    }

    /**
     * check if a MIO-Indicator is contained.
     * 
     * @param mioPageContent the MIOPageContent
     * @return true, if successful
     */
    public boolean containsMIO(final String mioPageContent) {
        boolean returnValue = false;
        for (String mioInd : mioIndicator) {
            if (mioPageContent.contains(mioInd)) {
                // break after a first indicator was detected
                returnValue = true;

            }
        }

        return returnValue;

    }

}
