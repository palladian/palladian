package ws.palladian.extraction.multimedia;

import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.Serializable;

/**
 * An instance of an image vector containing the pixel values and the class name / identifier.
 *
 * @author David Urbansky
 * @since 21-Feb-22 at 11:21
 **/
public class ImageVector implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * We can store values between -127 and 128 (we have to normalize 0-255 greyscale values to this range). Using byte only takes 1 byte per pixel. Our KNN models use doubles with 8 bytes.
     */
    private byte[] values;

    /**
     * The image identifier (or the category if used for classification)
     */
    private String identifier;

    /**
     * We might want to tag the image, e.g. with a category so that we can later only consider images that match a certain tag id.
     */
    private IntSet tagIds;

    public byte[] getValues() {
        return values;
    }

    public void setValues(byte[] values) {
        this.values = values;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public IntSet getTagIds() {
        return tagIds;
    }

    public void setTagIds(IntSet tagIds) {
        this.tagIds = tagIds;
    }

    public boolean containsTagId(Integer tagId) {
        if (tagIds == null || tagId == null) {
            return false;
        }
        return tagIds.contains(tagId.intValue());
    }
}
