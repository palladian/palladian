package ws.palladian.extraction.multimedia;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Sort extracted images.
 * 
 * @author David Urbansky
 */
public class ExtractedImageComparator implements Comparator<ExtractedImage>, Serializable {

    /**
     * <p>
     * 
     * </p>
     */
    private static final long serialVersionUID = 7135495948588046912L;

    /**
     * Higher ranking first.
     * 
     * @param image1 Image1
     * @param image2 Image2
     */
    @Override
    public int compare(ExtractedImage image1, ExtractedImage image2) {
        return (int) (1000 * image2.getRanking() - 1000 * image1.getRanking());
    }
}