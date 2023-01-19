package ws.palladian.utils;

import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.features.color.ColorExtractor;
import ws.palladian.features.color.HSB;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Objects;

import static java.awt.color.ColorSpace.CS_GRAY;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

// FIXME merge with ImageHandler
public final class ImageUtils {
    private ImageUtils() {
        // no instance me
    }

    public static BufferedImage getGrayscaleImage(BufferedImage image) {
        BufferedImageOp grayscaleConv = new ColorConvertOp(ColorSpace.getInstance(CS_GRAY), null);
        BufferedImage greyscaleImage = new BufferedImage(image.getWidth(), image.getHeight(), TYPE_BYTE_GRAY);
        grayscaleConv.filter(image, greyscaleImage);
        return greyscaleImage;
    }

    public static BufferedImage getGreyscale(ColorExtractor extractor, BufferedImage image) {
        Objects.requireNonNull(extractor);
        Objects.requireNonNull(image);
        WritableRaster raster = image.copyData(null);
        BufferedImage result = new BufferedImage(image.getColorModel(), raster, image.getColorModel().isAlphaPremultiplied(), null);
        int[] rgbImage = ImageHandler.getRGB(image);
        for (int xIdx = 0; xIdx < image.getWidth(); xIdx++) {
            for (int yIdx = 0; yIdx < image.getHeight(); yIdx++) {
                Color color = new Color(rgbImage[xIdx * image.getWidth() + yIdx]);
                int value = extractor.extractValue(color);
                int rgbValue = new Color(value, value, value).getRGB();
                result.setRGB(xIdx, yIdx, rgbValue);

            }
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        BufferedImage image = ImageIO.read(new File("/Users/pk/Documents/workspace_4.5/kaggle-restaurants/src/test/resources/51612.jpg"));
        ImageIO.write(ImageHandler.flipHorizontally(image), "jpg", new File("/Users/pk/Desktop/horizontal-flip.jpg"));
        ImageIO.write(ImageHandler.flipVertically(image), "jpg", new File("/Users/pk/Desktop/vertical-flip.jpg"));
        ImageIO.write(ImageHandler.rotate180(image), "jpg", new File("/Users/pk/Desktop/rotated.jpg"));
        for (ColorExtractor extractor : HSB.values()) {
            BufferedImage hImage = ImageUtils.getGreyscale(extractor, image);
            ImageIO.write(hImage, "jpg", new File("/Users/pk/Desktop/" + extractor + ".jpg"));
        }
    }

}
