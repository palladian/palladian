/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.io.Serializable;
import java.util.Comparator;

public class MIOComparator implements Comparator<Object>, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 5521460766618769610L;

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object obj1, Object obj2) {
        // for rank-comparison
        double mioTrust1 = ((MIO) obj1).getTrust();
        double mioTrust2 = ((MIO) obj2).getTrust();

        // for alphabetic comparison
        String labelTitle1 = ((MIO) obj1).getFileName();
        String labelTitle2 = ((MIO) obj1).getFileName();

        if (mioTrust1 > mioTrust2) {
            return -1;
        }

        else if (mioTrust1 < mioTrust2) {
            return 1;
        }

        else if (labelTitle1.equals(labelTitle2)) {
            return -1;
        }

        else {
            return 1;
        }

    }

}
