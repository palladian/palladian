package tud.iir.extraction.qa;

import java.util.HashSet;

/**
 * A stack from which qa URLs are taken.
 * 
 * @author David Urbansky
 * 
 */
public class QAUrlStack extends HashSet<QAUrl> {

    private static final long serialVersionUID = -2104572656534821733L;

    @Override
    public boolean contains(Object o) {
        QAUrl newQA = (QAUrl) o;
        for (QAUrl qaURL : this) {
            if (qaURL.getUrl().equals(newQA.getUrl())) {
                return true;
            }
        }
        return false;
    }
}