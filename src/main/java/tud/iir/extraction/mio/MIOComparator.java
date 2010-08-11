/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.io.Serializable;
import java.util.Comparator;

public class MIOComparator implements Comparator<Object>, Serializable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 5521460766618769610L;

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(final Object obj1, final Object obj2) {
        // for rank-comparison
        final double mioTrust1 = ((MIO) obj1).getTrust();
        final double mioTrust2 = ((MIO) obj2).getTrust();

        // for alphabetic comparison
        final String fileName1 = ((MIO) obj1).getFileName();
        final String fileName2 = ((MIO) obj1).getFileName();

        if (mioTrust1 > mioTrust2) {
            return -1;
        }

        else if (mioTrust1 < mioTrust2) {
            return 1;
        }

        else if (fileName1.equals(fileName2)) {
            return -1;
        }

        else {
            return 1;
        }

    }

}
