package ws.palladian.extraction.multimedia;

import org.apache.commons.lang3.tuple.Pair;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.EntryValueComparator;
import ws.palladian.helper.collection.FixedSizePriorityQueue;
import ws.palladian.helper.io.FileHelper;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.*;

/**
 * An image optimized KNN implementation. This class indexes image vectors and allows for finding similar to a given one.
 *
 * @author David Urbansky
 * @since 21-Feb-22 at 11:02
 **/
public class SimilarImageSearcher {
    protected final File folder;
    protected static final String PROCESSED_PREFIX = "prcssd-";
    protected static final String UUID_PREFIX = "uuid-";
    protected final List<ImageVector> imageVectors = new ArrayList<>();

    public SimilarImageSearcher(File folder) {
        this.folder = folder;
        buildIndex();
    }

    public boolean index(String imageUrl, String identifier) {
        return index(ImageHandler.load(imageUrl), identifier);
    }

    public boolean index(BufferedImage image, String identifier) {
        if (image == null) {
            return false;
        }
        ImageVector imageVector = createImageVector(image, identifier);
        imageVectors.add(imageVector);
        return true;
    }

    public void buildIndex() {
        File[] files = FileHelper.getFiles(folder.getPath(), PROCESSED_PREFIX);

        ProgressMonitor pm = new ProgressMonitor(files.length, 0.1, "Building Index");
        for (File file : files) {
            String identifier = file.getName().replace(PROCESSED_PREFIX, "");
            if (identifier.startsWith(UUID_PREFIX)) {
                continue;
            }
            identifier = FileHelper.getFileName(identifier);
            BufferedImage image = ImageHandler.load(file.getAbsolutePath());
            ImageVector imageVector = createImageVector(image, identifier);
            imageVectors.add(imageVector);
            pm.incrementAndPrintProgress();
        }
    }

    protected ImageVector createImageVector(BufferedImage image) {
        return createImageVector(image, null);
    }

    protected ImageVector createImageVector(BufferedImage image, String identifier) {
        String tempLinkPath = getImagePath(identifier, false);
        BufferedImage existingImage = findImageByIdentifier(identifier);

        if (existingImage != null) {
            image = existingImage;
        } else {
            ImageHandler.saveImage(image, tempLinkPath);
        }

        if (image == null) {
            return null;
        }

        String imageIdentifier = identifier;
        if (identifier == null) {
            imageIdentifier = UUID_PREFIX;
        }
        // process the image
        //        BufferedImage smallImage = ImageHandler.boxCrop(image, 100, 100);
        String greyImagePath = tempLinkPath.replace("/" + imageIdentifier, "/" + PROCESSED_PREFIX + imageIdentifier);

        BufferedImage grayscaleImage;
        if (!FileHelper.fileExists(greyImagePath)) {
            BufferedImage smallImage = ImageHandler.rescaleImage(image, 20, 20, false);
            grayscaleImage = ImageHandler.toGrayScale(smallImage);
            ImageHandler.saveImage(grayscaleImage, greyImagePath);
        } else {
            grayscaleImage = ImageHandler.load(greyImagePath);
        }

        return createImageVectorFromNormalizedImage(grayscaleImage, identifier);
    }

    private BufferedImage findProcessedImageByIdentifier(String identifier) {
        String tempLinkPath = getImagePath(identifier, true);

        // check whether we have the image already
        if (FileHelper.fileExists(tempLinkPath)) {
            return ImageHandler.load(tempLinkPath);
        }

        return null;
    }

    private BufferedImage findImageByIdentifier(String identifier) {
        String tempLinkPath = getImagePath(identifier, false);

        // check whether we have the image already
        if (FileHelper.fileExists(tempLinkPath)) {
            return ImageHandler.load(tempLinkPath);
        }

        return null;
    }

    private String getImagePath(String identifier, boolean processed) {
        if (identifier == null) {
            identifier = UUID_PREFIX + UUID.randomUUID();
        }
        if (processed) {
            identifier = PROCESSED_PREFIX + identifier;
        }
        return folder.getPath() + "/" + identifier + ".jpg";
    }

    private ImageVector createImageVectorFromNormalizedImage(BufferedImage image, String identifier) {
        ImageVector imageVector = new ImageVector();

        int vectorPosition = 0;
        byte[] values = new byte[image.getWidth() * image.getHeight()];
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                int rgb = image.getRGB(i, j);
                java.awt.Color color = new Color(rgb);
                int gray = color.getRed();
                values[vectorPosition] = (byte) (gray - 128);
                vectorPosition++;
            }
        }

        imageVector.setValues(values);
        imageVector.setIdentifier(identifier);

        return imageVector;
    }

    public List<String> search(String identifier, int num) {
        BufferedImage existingImage = findProcessedImageByIdentifier(identifier);
        return search(existingImage, num);
    }

    public List<String> search(BufferedImage image, int num) {
        if (image == null) {
            return new ArrayList<>();
        }
        ImageVector imageVector = createImageVector(image);
        LinkedHashSet<String> similarProductIdentifiers = new LinkedHashSet<>(getNeighbors(imageVector, num));
        return new ArrayList<>(CollectionHelper.getSubset(similarProductIdentifiers, 0, num));
    }

    private List<String> getNeighbors(ImageVector imageVector, int numNeighbors) {
        List<String> categoryNames = new ArrayList<>();
        CategoryEntries classify = classify(imageVector, numNeighbors);
        for (Category category : classify) {
            categoryNames.add(category.getName());
        }
        return CollectionHelper.getSublist(categoryNames, 0, numNeighbors);
    }

    private CategoryEntries classify(ImageVector imageVectorToClassify, int numNeighbors) {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();

        // find k nearest neighbors, compare instance to every known instance
        FixedSizePriorityQueue<Pair<String, Double>> neighbors = new FixedSizePriorityQueue<>(numNeighbors, new EntryValueComparator<>(CollectionHelper.Order.DESCENDING));

        for (ImageVector imageVector : imageVectors) {
            double distance = computeDistance(imageVector.getValues(), imageVectorToClassify.getValues());
            neighbors.add(Pair.of(imageVector.getIdentifier(), distance));
        }

        for (Pair<String, Double> neighbor : neighbors.asList()) {
            double distance = neighbor.getValue();
            double weight = 1.0 / (distance + 0.000000001);
            String targetClass = neighbor.getKey();
            builder.add(targetClass, weight);
        }

        return builder.create();
    }

    private double computeDistance(byte[] values1, byte[] values2) {
        Objects.requireNonNull(values1, "values1 must not be null");
        Objects.requireNonNull(values2, "values2 must not be null");
        double distance = 0;
        for (int idx = 0; idx < values1.length; idx++) {
            double value = values2[idx] - values1[idx];
            distance += value * value;
        }
        return distance;
    }

    public int getNumberOfIndexedImages() {
        return imageVectors.size();
    }
}
