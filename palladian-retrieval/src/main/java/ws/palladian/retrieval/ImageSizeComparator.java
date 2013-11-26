package ws.palladian.retrieval;

import java.util.Comparator;

import ws.palladian.retrieval.resources.WebImage;

/**
 * <p>
 * Sort WebImages by size.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class ImageSizeComparator implements Comparator<WebImage> {

    @Override
    public int compare(WebImage o1, WebImage o2) {
        return o2.getSize() - o1.getSize();
    }

}
