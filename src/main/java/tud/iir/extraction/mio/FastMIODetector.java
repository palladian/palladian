/**
 * The FastMIODetector simply analyze a MIOPageCandidate for pure MIO-Existence by some indicators.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FastMIODetector {

    /** A List of MIOIndicators. */
    private final transient List<String> mioIndicators;

    /**
     * Instantiates a new fast MIODetector.
     */
    public FastMIODetector() {
        final List<String> relevantMIOTypes = InCoFiConfiguration.getInstance().getMIOTypes();
        mioIndicators = new ArrayList<String>();

        if (relevantMIOTypes.contains("flash")) {
            mioIndicators.add(".swf");
            mioIndicators.add("application/x-shockwave-flash");
            mioIndicators.add("flashvars");
            mioIndicators.add("swfobject");
            mioIndicators.add("getflash");
        }

        if (relevantMIOTypes.contains("silverlight")) {
            mioIndicators.add(".xap");
        }

        if (relevantMIOTypes.contains("applet")) {
            mioIndicators.add(".jar");
            mioIndicators.add(".class");
        }

        if (relevantMIOTypes.contains("quicktime")) {
            mioIndicators.add(".mov");
        }
        if (relevantMIOTypes.contains("html5canvas")) {
            mioIndicators.add("<canvas>");
        }
    }

    /**
     * check if a MIO-Indicator is contained.
     * 
     * @param mioPageContent the MIOPageContent
     * @return true, if successful
     */
    public boolean containsMIO(final String mioPageContent) {
        final String modPageContent = mioPageContent.toLowerCase(Locale.ENGLISH);
        boolean returnValue = false;
        for (String mioInd : mioIndicators) {
            if (modPageContent.contains(mioInd)) {
                // break after a first indicator was detected
                returnValue = true;
            }
        }
        return returnValue;
    }
}
