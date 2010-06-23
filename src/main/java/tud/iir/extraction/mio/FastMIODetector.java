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

    ArrayList<String> MIOIndicator = new ArrayList<String>();

    /**
     * Instantiates a new fast mio detector.
     */
    public FastMIODetector() {
        MIOIndicator.add(".swf");
        MIOIndicator.add(".SWF");
        MIOIndicator.add(".mov");
        MIOIndicator.add(".MOV");
        MIOIndicator.add(".xap");
        MIOIndicator.add(".XAP");
        MIOIndicator.add(".jar");
        MIOIndicator.add(".JAR");
        MIOIndicator.add(".class");
        MIOIndicator.add("<canvas>");
        MIOIndicator.add("application/x-shockwave-flash");
        MIOIndicator.add("flashvars");
        MIOIndicator.add("swfobject");
        MIOIndicator.add("SWFObject");
        MIOIndicator.add("getflash");

    }

    /**
     * Gets the mio pages.
     *
     * @param pageContent the page content
     * @param pageURL the page url
     * @return the mio pages
     */
    public List<MIOPage> getMioPages(String pageContent, String pageURL) {
        List<MIOPage> mioPages = new ArrayList<MIOPage>();
        if (containsMIO(pageContent)) {

            MIOPage mioPage = new MIOPage(pageURL, pageContent);
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

        for (String mioInd : MIOIndicator) {
            if (mioPageContent.contains(mioInd)) {
                // break after a first indicator was detected
                return true;

            }
        }

        return false;

    }

}
