package ws.palladian.dataset;

import ws.palladian.core.value.AbstractValue;
import ws.palladian.core.value.Value;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageValue extends AbstractValue {
    private final File imageFile;

    public ImageValue(File imageFile) {
        this.imageFile = imageFile;
    }

    @Override
    public int hashCode() {
        return imageFile.hashCode();
    }

    @Override
    protected boolean equalsValue(Value value) {
        ImageValue imageValue = (ImageValue) value;
        return imageFile.equals(imageValue.imageFile);
    }

    @Override
    public String toString() {
        return imageFile.toString();
    }

    public BufferedImage getImage() {
        try {
            return ImageIO.read(imageFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public File getFile() {
        return imageFile;
    }

}
