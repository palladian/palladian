package ws.palladian.extraction.multimedia;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.commons.lang3.StringUtils;
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
import java.io.Serializable;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

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
    protected final Map<String, IntSet> tagMap = new HashMap<>();
    protected final int boxSize;

    public SimilarImageSearcher(File folder) {
        this(folder, 20, true);
    }

    public SimilarImageSearcher(File folder, int boxSize, boolean buildIndex) {
        this.folder = folder;
        this.boxSize = boxSize;
        boolean loaded = loadIndex();
        if (!loaded && buildIndex) {
            buildIndex();
        }
    }

    public boolean index(String imageUrl, String identifier) {
        return index(imageUrl, identifier, null);
    }

    public boolean index(String imageUrl, String identifier, IntSet tagIds) {
        return index(ImageHandler.load(imageUrl), identifier, tagIds);
    }

    public boolean index(BufferedImage image, String identifier) {
        return index(image, identifier, null);
    }

    public boolean index(BufferedImage image, String identifier, IntSet tagIds) {
        if (image == null) {
            return false;
        }
        ImageVector imageVector = createImageVector(image, identifier, tagIds);
        imageVectors.add(imageVector);
        return true;
    }

    protected boolean loadIndex() {
        String imageVectorsPath = folder.getPath() + "/image-vectors.gz";
        if (FileHelper.fileExists(imageVectorsPath)) {
            List<ImageVector> loadedImageVectors = FileHelper.tryDeserialize(imageVectorsPath);
            if (loadedImageVectors != null) {
                imageVectors.addAll(loadedImageVectors);
                String tagMapPath = folder.getPath() + "/tag-map.txt";
                if (FileHelper.fileExists(tagMapPath)) {
                    List<String> strings = FileHelper.readFileToArray(tagMapPath);
                    // tag-map.txt has lines like this:
                    // identifier,1,3,4,5
                    for (String string : strings) {
                        String[] split = string.split("§");
                        IntSet tagIds = new IntOpenHashSet();
                        for (int i = 1; i < split.length; i++) {
                            tagIds.add(Integer.parseInt(split[i]));
                        }
                        tagMap.put(split[0], tagIds);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void saveTagMap() {
        String tagMapPath = folder.getPath() + "/tag-map.txt";
        StringBuilder content = new StringBuilder();

        for (Map.Entry<String, IntSet> stringIntSetEntry : tagMap.entrySet()) {
            content.append(stringIntSetEntry.getKey()).append("§");
            content.append(StringUtils.join(stringIntSetEntry.getValue(), "§"));
            content.append("\n");
        }

        FileHelper.writeToFile(tagMapPath, content.toString());
        FileHelper.removeDuplicateLines(tagMapPath);
    }

    protected boolean saveIndex() {
        String imageVectorsPath = folder.getPath() + "/image-vectors.gz";
        saveTagMap();
        return FileHelper.trySerialize((Serializable) imageVectors, imageVectorsPath);
    }

    public void buildIndex() {
        File[] files = FileHelper.getFiles(folder.getPath(), PROCESSED_PREFIX);

        ProgressMonitor pm = new ProgressMonitor(files.length, 0.1, "Building Index (" + folder.getPath() + ")");
        for (File file : files) {
            String identifier = file.getName().replace(PROCESSED_PREFIX, "");
            if (identifier.startsWith(UUID_PREFIX)) {
                continue;
            }
            identifier = FileHelper.getFileName(identifier);
            BufferedImage image = ImageHandler.load(file.getAbsolutePath());
            ImageVector imageVector = createImageVector(image, identifier, null);

            imageVector.setTagIds(tagMap.get(identifier));

            imageVectors.add(imageVector);
            pm.incrementAndPrintProgress();
        }

        if (!imageVectors.isEmpty()) {
            saveIndex();
        }
    }

    protected ImageVector createImageVector(BufferedImage image) {
        return createImageVector(image, null, null);
    }

    protected ImageVector createImageVector(BufferedImage image, String identifier, IntSet tagIds) {
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
            BufferedImage smallImage = ImageHandler.rescaleImage(image, boxSize, boxSize, false);
            grayscaleImage = ImageHandler.toGrayScale(smallImage);
            ImageHandler.saveImage(grayscaleImage, greyImagePath);
        } else {
            grayscaleImage = ImageHandler.load(greyImagePath);
        }

        ImageVector imageVectorFromNormalizedImage = createImageVectorFromNormalizedImage(grayscaleImage, identifier);

        if (tagIds != null) {
            tagMap.put(identifier, tagIds);
            imageVectorFromNormalizedImage.setTagIds(tagIds);
        }

        return imageVectorFromNormalizedImage;
    }

    protected BufferedImage findProcessedImageByIdentifier(String identifier) {
        String tempLinkPath = getImagePath(identifier, true);

        // check whether we have the image already
        if (FileHelper.fileExists(tempLinkPath)) {
            return ImageHandler.load(tempLinkPath);
        }

        return null;
    }

    protected BufferedImage findImageByIdentifier(String identifier) {
        String tempLinkPath = getImagePath(identifier, false);

        // check whether we have the image already
        if (FileHelper.fileExists(tempLinkPath)) {
            return ImageHandler.load(tempLinkPath);
        }

        return null;
    }

    protected String getImagePath(String identifier, boolean processed) {
        if (identifier == null) {
            identifier = UUID_PREFIX + UUID.randomUUID();
        }
        if (processed) {
            identifier = PROCESSED_PREFIX + identifier;
        }
        return folder.getPath() + "/" + identifier + ".jpg";
    }

    protected ImageVector createImageVectorFromNormalizedImage(BufferedImage image, String identifier) {
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
        return search(identifier, num, null);
    }

    public List<String> search(String identifier, int num, Integer tagId) {
        BufferedImage existingImage = findProcessedImageByIdentifier(identifier);
        return search(existingImage, num, tagId);
    }

    public List<String> search(BufferedImage image, int num) {
        return search(image, num, null);
    }

    public List<String> search(BufferedImage image, int num, Integer tagId) {
        if (image == null) {
            return new ArrayList<>();
        }
        ImageVector imageVector = createImageVector(image);
        LinkedHashSet<String> similarProductIdentifiers = new LinkedHashSet<>(getNeighbors(imageVector, num, tagId));
        return new ArrayList<>(CollectionHelper.getSubset(similarProductIdentifiers, 0, num));
    }

    private List<String> getNeighbors(ImageVector imageVector, int numNeighbors, Integer tagId) {
        List<String> categoryNames = new ArrayList<>();
        CategoryEntries classify = classify(imageVector, numNeighbors, tagId);
        for (Category category : classify) {
            categoryNames.add(category.getName());
        }
        return CollectionHelper.getSublist(categoryNames, 0, numNeighbors);
    }

    protected CategoryEntries classify(ImageVector imageVectorToClassify, int numNeighbors) {
        return classify(imageVectorToClassify, numNeighbors, null);
    }

    protected CategoryEntries classify(ImageVector imageVectorToClassify, int numNeighbors, Integer tagId) {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();

        // find k nearest neighbors, compare instance to every known instance
        FixedSizePriorityQueue<Pair<String, Double>> neighbors = new FixedSizePriorityQueue<>(numNeighbors, new EntryValueComparator<>(CollectionHelper.Order.DESCENDING));

        List<ImageVector> filteredList = imageVectors;
        if (tagId != null) {
            filteredList = imageVectors.stream().filter(v -> v.containsTagId(tagId)).collect(Collectors.toList());
        }

        for (ImageVector imageVector : filteredList) {
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

    protected double computeDistance(byte[] values1, byte[] values2) {
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

    public void clearIndex() {
        // clear processed images, otherwise they would come back when building the index but that might be old out of date images
        File[] files = FileHelper.getFiles(folder.getPath(), PROCESSED_PREFIX);
        ProgressMonitor pm = new ProgressMonitor(files.length, 1., "Clearing index");
        for (File file : files) {
            FileHelper.delete(file);
            pm.incrementAndPrintProgress();
        }

        FileHelper.delete(folder.getPath() + "/image-vectors.gz");
        FileHelper.delete(folder.getPath() + "/tag-map.txt");

        // clear in memory
        this.imageVectors.clear();
        this.tagMap.clear();
    }
}
