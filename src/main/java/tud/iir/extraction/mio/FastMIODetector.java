/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;

/**
 * The FastMIODetector simply analyze a MIOPageCandidate for pure MIO-Existence by some indicators.
 * 
 * @author Martin Werner
 */
public class FastMIODetector {

    List<String> mioIndicator = new ArrayList<String>();

    /**
     * Instantiates a new fast mio detector.
     */
    public FastMIODetector() {
        mioIndicator.add(".swf");
        mioIndicator.add(".SWF");
        mioIndicator.add(".mov");
        mioIndicator.add(".MOV");
        mioIndicator.add(".xap");
        mioIndicator.add(".XAP");
        mioIndicator.add(".jar");
        mioIndicator.add(".JAR");
        mioIndicator.add(".class");
        mioIndicator.add("<canvas>");
        mioIndicator.add("application/x-shockwave-flash");
        mioIndicator.add("flashvars");
        mioIndicator.add("swfobject");
        mioIndicator.add("SWFObject");
        mioIndicator.add("getflash");

    }

    /**
     * Gets the mio pages.
     * 
     * @param pageContent the page content
     * @param pageURL the page url
     * @return the mio pages
     */
    public List<MIOPage> getMioPages(String pageContent, String pageURL) {
        final List<MIOPage> mioPages = new ArrayList<MIOPage>();
        if (containsMIO(pageContent)) {

            final MIOPage mioPage = new MIOPage(pageURL, pageContent);
            mioPages.add(mioPage);
        }

        return mioPages;
    }

    /**
     * check if a MIO-Indicator is contained.
     * 
     * @param mioPageContent the mio page content
     * @return true, if successful
     */
    public boolean containsMIO(String mioPageContent) {

        for (String mioInd : mioIndicator) {
            if (mioPageContent.contains(mioInd)) {
                // break after a first indicator was detected
                return true;

            }
        }

        return false;

    }

}
