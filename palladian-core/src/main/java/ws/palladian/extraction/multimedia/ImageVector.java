package ws.palladian.extraction.multimedia;

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
}
