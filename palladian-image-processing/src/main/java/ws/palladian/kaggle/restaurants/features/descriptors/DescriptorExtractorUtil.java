package ws.palladian.kaggle.restaurants.features.descriptors;

import ws.palladian.kaggle.restaurants.features.descriptors.DescriptorExtractor.Descriptor;
import ws.palladian.utils.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static ws.palladian.kaggle.restaurants.features.descriptors.MopsDescriptorExtractor.MOPS;
import static ws.palladian.kaggle.restaurants.features.descriptors.SiftDescriptorExtractor.SIFT;
import static ws.palladian.kaggle.restaurants.features.descriptors.SurfDescriptorExtractor.SURF;

public class DescriptorExtractorUtil {

    public static void drawDescriptors(BufferedImage image, DescriptorExtractor extractor, File outputFile) throws IOException {
        BufferedImage greyscaleImage = ImageUtils.getGrayscaleImage(image);
        Graphics2D graphics = greyscaleImage.createGraphics();
        List<Descriptor> descriptors = extractor.extractDescriptors(greyscaleImage);
        for (Descriptor descriptor : descriptors) {
            descriptor.draw(graphics);
        }
        ImageIO.write(greyscaleImage, "jpg", outputFile);
    }

    public static void main(String[] args) throws IOException {
        BufferedImage imageFile = ImageIO.read(new File("/Users/pk/temp/Yelp-Restaurants/train_photos/923.jpg"));
        drawDescriptors(imageFile, SURF, new File("/Users/pk/Desktop/test-surf.jpg"));
        drawDescriptors(imageFile, SIFT, new File("/Users/pk/Desktop/test-sift.jpg"));
        drawDescriptors(imageFile, MOPS, new File("/Users/pk/Desktop/test-mops.jpg"));
    }

}
