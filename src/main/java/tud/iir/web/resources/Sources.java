package tud.iir.web.resources;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A set of sources.
 * 
 * @author David Urbansky
 * 
 * @param <S>
 */
public class Sources<S> extends HashSet<Source> implements Serializable {

    private static final long serialVersionUID = -5357351871493757860L;

    @Override
    public final boolean contains(Object o) {
        Source s = (Source) o;
        Iterator<Source> sI = this.iterator();
        while (sI.hasNext()) {
            Source s1 = sI.next();
            if (s.equals(s1)) {
                return true;
            }
        }
        return false;
    }
}