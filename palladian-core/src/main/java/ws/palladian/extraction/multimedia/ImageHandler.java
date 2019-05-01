package ws.palladian.extraction.multimedia;

import com.sun.media.jai.codec.SeekableStream;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.math.FatStats;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;

import javax.imageio.*;
import javax.imageio.stream.FileImageOutputStream;
import javax.media.jai.*;
import javax.media.jai.operator.ColorQuantizerDescriptor;
import javax.media.jai.operator.ErodeDescriptor;
import javax.media.jai.operator.GradientMagnitudeDescriptor;
import javax.swing.*;
import java.awt.Color;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * <p>
 * A handler for images.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public class ImageHandler {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageHandler.class);

    public static final List<ws.palladian.extraction.multimedia.Color> COLORS;

    static {
        InputStream inputStream = ImageHandler.class.getResourceAsStream("/colors.csv");
        final List<ws.palladian.extraction.multimedia.Color> colors = new ArrayList<>();
        FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split(";");
                colors.add(new ws.palladian.extraction.multimedia.Color(split[0], split[1], split[2]));
            }
        });
        COLORS = Collections.unmodifiableList(colors);
    }

    /**
     * <p>
     * An extracted image.
     * </p>
     *
     * @author David Urbansky
     */
    private static final class ExtractedImage extends BasicWebImage {
        private int rankCount = 1;
        private int duplicateCount = 0;
        private final BufferedImage imageContent;

        public ExtractedImage(WebImage image, BufferedImage imageContent) {
            super(image);
            this.imageContent = imageContent;
        }

        public void addRanking(int ranking) {
            this.rankCount += ranking;
        }

        public double getRanking() {
            return duplicateCount + 1. / rankCount;
        }

        public double getWidthHeightRatio() {
            return getWidth() / getHeight();
        }
    }

    /**
     * <p>
     * A color cluster.
     * </p>
     */
    private static final class ColorCluster {
        long totalRed = 0L;
        long totalGreen = 0L;
        long totalBlue = 0L;
        int population;

        public Color getCenterColor() {
            return new Color((int)((double)totalRed / population), (int)((double)totalGreen / population), (int)((double)totalBlue / population));
        }
    }

    /**
     * Image similarity mean square error.
     */
    public static final int MSE = 1;

    /**
     * Image similarity with Minkowsi.
     */
    public static final int MINKOWSKI = 2;

    // image similarity with image difference and average gray values
    public static final int DIFFG = 3;

    static {
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    /**
     * <p>
     * Load an image from disk.
     * </p>
     *
     * @param imageFile The image file on disk.
     * @return The buffered image.
     * @throws IOException
     */
    public static BufferedImage load(File imageFile) throws IOException {
        return ImageIO.read(imageFile);
    }

    /**
     * <p>
     * Load an image from an URL.
     * </p>
     *
     * @param url The url of the image.
     * @return The buffered image.
     */
    public static BufferedImage load(String url) {
        return load(url, new HashSet<>());
    }

    public static BufferedImage load(String url, Set<String> detectedContentTypes) {
        BufferedImage bufferedImage = null;

        // get file extension from URL if possible
        String fileExtension = FileHelper.getFileType(url);
        if (!fileExtension.isEmpty()) {
            detectedContentTypes.add(fileExtension);
        }

        try {
            url = url.trim();
            if (url.startsWith("http:") || url.startsWith("https:")) {
                HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
                url = url.replace(" ", "%20");
                HttpResult httpResult = retriever.httpGet(url);
                try {
                    // let's try to guess the actual content type from the stream, if we find something, this must be more accurate than the file extension
                    String detectedContentType = Optional.ofNullable(URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(httpResult.getContent()))).orElse("");
                    detectedContentType = StringHelper.getSubstringBetween(detectedContentType, "/", null);
                    detectedContentType = detectedContentType.replace("jpeg", "jpg");
                    if (!detectedContentType.isEmpty()) {
                        detectedContentTypes.clear();
                        detectedContentTypes.add(detectedContentType);
                    }
                } catch (Exception e) {
                    // ccl
                }
                try {
                    bufferedImage = ImageIO.read(new ByteArrayInputStream(httpResult.getContent()));

                    if (bufferedImage == null) {
                        bufferedImage = ImageIO.read(new URL(url));
                    }

                } catch (Exception e) {
                    bufferedImage = JAI.create("stream", SeekableStream.wrapInputStream(new ByteArrayInputStream(httpResult.getContent()), true)).getAsBufferedImage();
                }
            } else {
                try {
                    bufferedImage = ImageIO.read(new File(url));
                } catch (Exception e) {
                    bufferedImage = JAI.create("stream", SeekableStream.wrapInputStream(new FileInputStream(new File(url)), true)).getAsBufferedImage();
                }
            }
        } catch (Exception e) {
            LOGGER.error(url + ", " + e.getMessage());
        }

        return bufferedImage;
    }

    public static String getMatchingImageUrl(Collection<WebImage> images) {
        String[] matchingImages = getMatchingImageUrls(images, 1);
        if (matchingImages.length > 0) {
            return matchingImages[0];
        }
        return "";
    }

    public static String[] getMatchingImageUrls(Collection<WebImage> images, int matchingNumber) {

        try {

            // normalize all images to fixed width
            List<ExtractedImage> normalizedImages = new ArrayList<>();

            for (WebImage image : images) {
                BufferedImage bufferedImage;
                try {
                    bufferedImage = load(image.getUrl());
                    if (bufferedImage != null) {
                        bufferedImage = rescaleImage(bufferedImage, 200);
                        normalizedImages.add(new ExtractedImage(image, bufferedImage));
                    }
                } catch (Exception e) {
                    LOGGER.error(image.getUrl());
                }
            }
            images.clear();

            // compare images with almost or exactly the same width height ratio
            Set<String> duplicateImages = new HashSet<>();
            for (int i = 0; i < normalizedImages.size() - 1; i++) {
                ExtractedImage image1 = normalizedImages.get(i);

                for (int j = i + 1; j < normalizedImages.size(); j++) {
                    try {
                        ExtractedImage image2 = normalizedImages.get(j);
                        if (duplicateImages.contains(image2.getUrl())) {
                            continue;
                        }

                        if (!MathHelper.isWithinMargin(image1.getWidthHeightRatio(), image2.getWidthHeightRatio(), 0.05)) {
                            continue;
                        }
                        if (isDuplicate(image1.imageContent, image2.imageContent)) {
                            image1.duplicateCount++;
                            image1.addRanking(image2.rankCount);
                            duplicateImages.add(image2.getUrl());
                        }
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            }
            duplicateImages.clear();

            // order images by ranking and collect urls
            Collections.sort(normalizedImages, (image1, image2) -> Double.compare(image2.getRanking(), image1.getRanking()));
            // CollectionHelper.print(normalizedImages);

            int matchingImages = Math.min(normalizedImages.size(), matchingNumber);
            String[] matchingImageURLs = new String[matchingImages];
            for (int i = 0; i < matchingImages; i++) {
                matchingImageURLs[i] = normalizedImages.get(i).getUrl();
            }
            normalizedImages.clear();

            return matchingImageURLs;

        } catch (OutOfMemoryError e) {
            LOGGER.error(e.getMessage());
        }

        return new String[0];
    }

    /**
     * <p>
     * Rescale an image to fit a in a given bounding box. The image will <b>not</b> be stretched so the rest of the box
     * might be empty.
     * </p>
     * <p>
     * Example 1: a 600x120 image is transformed to 200x40 to fit a 200x100 box.
     * </p>
     * <p>
     * Example 2: a 100x400 image is transformed to 25x100 to fit a 200x100 box.
     * </p>
     *
     * @param image The buffered image which should be transformed.
     * @param boxWidth The width of the box in which the image should be positioned.
     * @param boxHeight The height of the box in which the image should be positioned.
     * @return The transformed buffered image.
     */
    public static BufferedImage boxFit(BufferedImage image, int boxWidth, int boxHeight) {
        return boxFit(image, boxWidth, boxHeight, true);
    }

    private static BufferedImage boxFit(BufferedImage image, int boxWidth, int boxHeight, boolean toFit) {
        Validate.notNull(image);
        return rescaleImage(image, boxWidth, boxHeight, toFit);
    }

    /**
     * <p>
     * Rescale an image to completely so that the image's shorter side fills the box's side. The image is then centered
     * and everything outside of the box is cropped.
     * </p>
     * <p>
     * Example 1: a 600x200 image is transformed to 300x100 to fit a 200x100 box. 50px to the left and right will be
     * cropped (300-200/2).
     * </p>
     * <p>
     * Example 2: a 100x400 image is transformed to 200x800 to fit a 200x100 box. 200px at the top and bottom will be
     * cropped (800-400/2).
     * </p>
     *
     * @param image The buffered image which should be transformed.
     * @param boxWidth The width of the box in which the image should be positioned.
     * @param boxHeight The height of the box in which the image should be positioned.
     * @return The transformed buffered image.
     */
    public static BufferedImage boxCrop(BufferedImage image, int boxWidth, int boxHeight) {
        Validate.notNull(image);

        // scale to fill the target box completely
        double scale = Math.max((double)boxWidth / (double)image.getWidth(), (double)boxHeight / (double)image.getHeight());

        int targetWidth = Math.max((int)(image.getWidth() * scale), boxWidth);
        int targetHeight = Math.max((int)(image.getHeight() * scale), boxHeight);

        image = boxFit(image, targetWidth, targetHeight, false);

        int iWidth = image.getWidth();
        int iHeight = image.getHeight();

        // vertically center the image in the box if the height is greater than the box height
        double yOffset = (iHeight - boxHeight) / 2.0;

        // horizontally center the image in the box if the width is greater than the box width
        double xOffset = (iWidth - boxWidth) / 2.0;

        // nothing to crop
        if (yOffset <= 0 && xOffset <= 0) {
            return image;
        }

        return image.getSubimage((int)xOffset, (int)yOffset, Math.min(boxWidth, iWidth), Math.min(boxHeight, iHeight));
    }

    /**
     * <p>
     * Batch rescale images in a folder.
     * </p>
     *
     * @param imageFolder The folder with the images to rescale.
     * @param imageWidth The target image width.
     * @param imageHeight The target image height.
     * @param fit Whether images should be fit to the box or cropped to match the imageWidth and imageHeight.
     * @throws IOException
     */
    public static void rescaleAllImages(String imageFolder, int imageWidth, int imageHeight, boolean fit) throws IOException {
        File[] imageFiles = FileHelper.getFiles(imageFolder);
        for (File file : imageFiles) {
            BufferedImage image = load(file);
            if (fit) {
                image = boxFit(image, imageWidth, imageHeight);
            } else {
                image = boxCrop(image, imageWidth, imageHeight);
            }
            saveImage(image, FileHelper.getFileType(file.getAbsolutePath()), file.getAbsolutePath());
        }

    }

    public static BufferedImage rescaleImage(BufferedImage bufferedImage, int boxWidth, int boxHeight, boolean toFit) {
        if (bufferedImage == null) {
            LOGGER.warn("given image was NULL");
            return null;
        }

        int iWidth = bufferedImage.getWidth();
        int iHeight = bufferedImage.getHeight();

        double scaleX = (double)boxWidth / (double)iWidth;
        double scaleY = (double)boxHeight / (double)iHeight;
        double scale = Math.min(scaleX, scaleY);

        if (toFit) {
            scaleX = scale;
            scaleY = scale;
        }

        BufferedImage rescaledImage;

        if (scale >= 1.0) {
            rescaledImage = scaleUp(bufferedImage, scaleX, scaleY);
        } else {
            rescaledImage = scaleDown(bufferedImage, scaleX, scaleY);
        }

        return rescaledImage;
    }

    private static BufferedImage scaleDown(BufferedImage bufferedImage, double scaleX, double scaleY) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(bufferedImage); // The source image
        // x scale
        pb.add(scaleX);
        // y scale
        pb.add(scaleY);
        // x translation
        pb.add(0.0f);
        // y translation
        pb.add(0.0f);
        pb.add(new InterpolationBicubic(4));
        pb.add(bufferedImage);

        RenderingHints qualityHints1 = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // results in exactly the same as above (tested only for downscaling)
        // RenderingHints qualityHints1 = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
        // RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        RenderedOp resizedImage = JAI.create("SubsampleAverage", pb, qualityHints1);

        return resizedImage.getAsBufferedImage();
    }

    private static BufferedImage rescaleImage(BufferedImage bufferedImage, double scale) {
        // "SubsampleAverage" is smooth but does only work for downscaling. If upscaling, we need to use "Scale".
        boolean upscale = false;
        if (scale > 1.0) {
            upscale = true;
        }

        ParameterBlock pb = new ParameterBlock();
        // the source image
        pb.addSource(bufferedImage);
        // x scale
        if (upscale) {
            pb.add((float)scale);
        } else {
            pb.add(scale);
        }
        // y scale
        if (upscale) {
            pb.add((float)scale);
        } else {
            pb.add(scale);
        }
        // x translation
        pb.add(0.0f);
        // y translation
        pb.add(0.0f);
        pb.add(new InterpolationBicubic(4));
        pb.add(bufferedImage);

        RenderingHints qualityHints1 = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // results in exactly the same as above (tested only for downscaling)
        // RenderingHints qualityHints1 = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
        // RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        RenderedOp resizedImage;

        if (upscale) {
            resizedImage = JAI.create("scale", pb, qualityHints1);
        } else {
            resizedImage = JAI.create("SubsampleAverage", pb, qualityHints1);
        }

        return resizedImage.getAsBufferedImage();
    }

    /**
     * <p>
     * Rescaling an image using JAI SubsampleAverage. The image looks smooth after rescaling.
     * </p>
     *
     * @param bufferedImage The input image.
     * @param newWidth The desired new width (size) of the image.
     * @param fit If true, the newWidth will be the maximum side length of the image. Default is false.
     * @return The scaled image.
     */
    private static BufferedImage rescaleImage(BufferedImage bufferedImage, int newWidth, boolean fit) {
        if (bufferedImage == null) {
            LOGGER.warn("given image was NULL");
            return null;
        }

        int iWidth = bufferedImage.getWidth();
        int iHeight = bufferedImage.getHeight();

        double scale = (double)newWidth / (double)iWidth;

        if (fit && iWidth < iHeight) {
            scale = (double)newWidth / (double)iHeight;
        }

        return rescaleImage(bufferedImage, scale);
    }

    private static BufferedImage rescaleImage(BufferedImage bufferedImage, int newWidth) {
        return rescaleImage(bufferedImage, newWidth, false);
    }

    /**
     * <p>
     * Rescaling an image using java.awt.Image.getScaledInstance. The image looks smooth after rescaling.
     * </p>
     *
     * @param bufferedImage The input image.
     * @param boxWidth The desired new width (size) of the image.
     * @param fit If true, the newWidth will be the maximum side length of the image. Default is false.
     * @return The scaled image.
     */
    private static BufferedImage scaleUp(BufferedImage bufferedImage, double scaleX, double scaleY) {
        ImageIcon imageIcon = new ImageIcon(bufferedImage);
        Image image = imageIcon.getImage();
        Image resizedImage;

        int width = (int)(Math.round(scaleX * bufferedImage.getWidth()));
        int height = (int)(Math.round(scaleY * bufferedImage.getHeight()));
        resizedImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        // ensure that all the pixels in the image are loaded.
        Image temp = new ImageIcon(resizedImage).getImage();

        bufferedImage = new BufferedImage(temp.getWidth(null), temp.getHeight(null), BufferedImage.TYPE_INT_RGB);

        // copy image to buffered image.
        Graphics g = bufferedImage.createGraphics();

        // clear background and paint the image.
        g.setColor(Color.white);
        g.fillRect(0, 0, temp.getWidth(null), temp.getHeight(null));
        g.drawImage(temp, 0, 0, null);
        g.dispose();

        return bufferedImage;
    }

    /**
     * Download and save a picture from a URL. The image extension will automatically be added to the save path depending on the the image format.
     * 
     * @param url The URL of the image.
     * @param savePath The path to which the image should be saved.
     * @return The path (including the detected file type) where the image was saved or null if there was an error.
     */
    public static String downloadAndSave(String url, String savePath) {
        return downloadAndSave(url, savePath, 1.f);
    }
    public static String downloadAndSave(String url, String savePath, float quality) {
        try {
            Set<String> detectedContentTypes = new HashSet<>();
            BufferedImage bi = load(url, detectedContentTypes);

            String fileExtension = Optional.ofNullable(CollectionHelper.getFirst(detectedContentTypes)).orElse("jpg");

            if (!savePath.toLowerCase().endsWith(fileExtension.toLowerCase())) {
                savePath += "." + fileExtension;
            }

            // save image
            LOGGER.debug("write " + savePath + " with " + fileExtension);
            FileHelper.createDirectoriesAndFile(savePath);

            // gifs lose animation when saved using the save method, let us download them as files
            if (fileExtension.equalsIgnoreCase("gif")) {
                HttpRetrieverFactory.getHttpRetriever().downloadAndSave(url, savePath);
            } else {
                saveImage(bi, fileExtension, savePath, quality);
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            LOGGER.error("problem with URL:" + url + ", " + e.getMessage());
            return null;
        }

        return savePath;
    }

    private static BufferedImage substractImages(BufferedImage image1, BufferedImage image2) {
        int pixelCount = image1.getWidth() * image1.getHeight();
        int grayCount = 0;

        if (image1.getWidth() != image2.getWidth()) {
            LOGGER.warn("Images do not have the same size.");
            return image1;
        }

        BufferedImage substractedImage = new BufferedImage(image1.getWidth(), image1.getHeight(), image1.getType());

        for (int i = 0; i < image1.getWidth(); i++) {
            for (int j = 0; j < Math.min(image1.getHeight(), image2.getHeight()); j++) {

                Color c1 = new Color(image1.getRGB(i, j));
                Color c2 = new Color(image2.getRGB(i, j));

                double redNormalized = 0.3 * Math.abs(c1.getRed() - c2.getRed());
                double greenNormalized = 0.59 * Math.abs(c1.getGreen() - c2.getGreen());
                double blueNormalized = 0.11 * Math.abs(c1.getBlue() - c2.getBlue());

                int gray = (int)(redNormalized + greenNormalized + blueNormalized);
                Color cg = new Color(gray, gray, gray);
                substractedImage.setRGB(i, j, cg.getRGB());
                grayCount += gray;
            }
        }

        float averageGray = grayCount / (float)pixelCount;

        LOGGER.debug("{}", averageGray);

        return substractedImage;
    }

    public static float getAverageGray(BufferedImage bufferedImage) {
        int pixelCount = bufferedImage.getWidth() * bufferedImage.getHeight();
        int grayCount = 0;

        for (int i = 0; i < bufferedImage.getWidth(); i++) {
            for (int j = 0; j < bufferedImage.getHeight(); j++) {

                Color c1 = new Color(bufferedImage.getRGB(i, j));

                double redNormalized = 0.3 * c1.getRed();
                double greenNormalized = 0.59 * c1.getGreen();
                double blueNormalized = 0.11 * c1.getBlue();

                int gray = (int)(redNormalized + greenNormalized + blueNormalized);
                grayCount += gray;
            }
        }

        return grayCount / (float)pixelCount;
    }

    public static double getAverageRed(BufferedImage bufferedImage, boolean ignoreWhite) {
        int pixelCount = bufferedImage.getWidth() * bufferedImage.getHeight();
        int redCount = 0;

        for (int i = 0; i < bufferedImage.getWidth(); i++) {
            for (int j = 0; j < bufferedImage.getHeight(); j++) {

                Color c1 = new Color(bufferedImage.getRGB(i, j));

                if (ignoreWhite && c1.getRed() == 255 && c1.getGreen() == 255 && c1.getBlue() == 255) {
                    continue;
                }
                redCount += c1.getRed();

            }
        }

        return 0.3 * redCount / pixelCount;
    }

    public static double getAverageGreen(BufferedImage bufferedImage, boolean ignoreWhite) {
        int pixelCount = bufferedImage.getWidth() * bufferedImage.getHeight();
        int greenCount = 0;

        for (int i = 0; i < bufferedImage.getWidth(); i++) {
            for (int j = 0; j < bufferedImage.getHeight(); j++) {

                Color c1 = new Color(bufferedImage.getRGB(i, j));

                if (ignoreWhite && c1.getRed() == 255 && c1.getGreen() == 255 && c1.getBlue() == 255) {
                    continue;
                }
                greenCount += c1.getGreen();

            }
        }

        return 0.59 * greenCount / pixelCount;
    }

    public static double getAverageBlue(BufferedImage bufferedImage, boolean ignoreWhite) {
        int pixelCount = bufferedImage.getWidth() * bufferedImage.getHeight();
        int blueCount = 0;

        for (int i = 0; i < bufferedImage.getWidth(); i++) {
            for (int j = 0; j < bufferedImage.getHeight(); j++) {

                Color c1 = new Color(bufferedImage.getRGB(i, j));

                if (ignoreWhite && c1.getRed() == 255 && c1.getGreen() == 255 && c1.getBlue() == 255) {
                    continue;
                }
                blueCount += c1.getBlue();

            }
        }

        return 0.11 * blueCount / pixelCount;
    }

    public static double getSimilarity(BufferedImage image1, BufferedImage image2, int measure) {
        switch (measure) {
            case MSE:
                return getMeanSquareError(image1, image2);
            case MINKOWSKI:
                return getMinkowskiSimilarity(image1, image2);
            case DIFFG:
                return getGrayDifference(image1, image2);
        }

        return 0.0;
    }

    private static double getMeanSquareError(BufferedImage image1, BufferedImage image2) {
        // normalize size if not done already
        if (image1.getWidth() != image2.getWidth()) {
            image1 = rescaleImage(image1, 200);
            image2 = rescaleImage(image2, 200);
        }

        image1 = toGrayScale(image1);
        image2 = toGrayScale(image2);

        double squaredError = 0;
        for (int i = 0; i < image1.getWidth(); i++) {
            for (int j = 0; j < Math.min(image1.getHeight(), image2.getHeight()); j++) {
                Color color1 = new Color(image1.getRGB(i, j));
                Color color2 = new Color(image2.getRGB(i, j));
                squaredError += Math.pow((color1.getRed() - color2.getRed()) / 255, 2);
            }
        }

        return 1 / (double)(image1.getWidth() * image1.getHeight()) * squaredError;
    }

    private static double getMinkowskiSimilarity(BufferedImage image1, BufferedImage image2) {
        // normalize size if not done already
        if (image1.getWidth() != image2.getWidth()) {
            image1 = rescaleImage(image1, 200);
            image2 = rescaleImage(image2, 200);
        }

        image1 = toGrayScale(image1);
        image2 = toGrayScale(image2);

        int r = 2;
        double squaredError = 0;
        for (int i = 0; i < image1.getWidth(); i++) {
            for (int j = 0; j < Math.min(image1.getHeight(), image2.getHeight()); j++) {
                Color color1 = new Color(image1.getRGB(i, j));
                Color color2 = new Color(image2.getRGB(i, j));
                squaredError += Math.pow((color1.getRed() - color2.getRed()) / (double)255, r);
            }
        }

        double meanSquareError = 1.0 / (image1.getWidth() * image1.getHeight()) * squaredError;
        meanSquareError = Math.pow(meanSquareError, 1.0 / r);

        return 1 - meanSquareError;
    }

    private static double getGrayDifference(BufferedImage image1, BufferedImage image2) {
        // normalize size if not done already
        if (image1.getWidth() != image2.getWidth()) {
            image1 = rescaleImage(image1, 200);
            image2 = rescaleImage(image2, 200);
        }

        BufferedImage substractedImage = substractImages(image1, image2);
        double averageGray = getAverageGray(substractedImage);

        return 1 - averageGray / 255.0;
    }

    public static BufferedImage toGrayScale(BufferedImage bufferedImage) {

        for (int i = 0; i < bufferedImage.getWidth(); i++) {
            for (int j = 0; j < bufferedImage.getHeight(); j++) {
                Color color = new Color(bufferedImage.getRGB(i, j));
                int gray = (int)(0.3 * color.getRed() + 0.59 * color.getGreen() + 0.11 * color.getBlue());
                Color cg = new Color(gray, gray, gray);
                bufferedImage.setRGB(i, j, cg.getRGB());
            }
        }

        return bufferedImage;
    }

    public static boolean isDuplicate(BufferedImage image1, BufferedImage image2) {

        if (image1 == null || image2 == null) {
            return true;
        }

        // normalize size if not done already
        // if (image1.getWidth() != image2.getWidth()) {
        // image1 = rescaleImage(image1, 200);
        // image2 = rescaleImage(image2, 200);
        // }
        //
        // BufferedImage substractedImage = substractImages(image1, image2);
        // float averageGray = getAverageGray(substractedImage);
        //
        // if (averageGray < 50) return true;

        double similarity = getSimilarity(image1, image2, DIFFG);
        // System.out.println(similarity);
        return similarity > 0.82;
    }

    /**
     * Save an image to disk. This methods wraps the ImageIO.write method and does error handling.
     *
     * @param image The image to save.
     * @param fileType The image type (e.g. "jpg")
     * @param filePath The path where the image should be saved.
     * @return True if the image was saved successfully, false otherwise.
     */
    public static boolean saveImage(BufferedImage image, String filePath) {
        return saveImage(image, FileHelper.getFileType(filePath), filePath);
    }

    public static boolean saveImage(BufferedImage image, String fileType, String filePath) {
        return saveImage(image, fileType, filePath, 1.0f);
    }

    public static boolean saveImage(BufferedImage image, String fileType, String filePath, float quality) {
        try {
            Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(fileType.toUpperCase());
            if (iter.hasNext()) {
                ImageWriter writer = iter.next();
                ImageWriteParam iwp = writer.getDefaultWriteParam();

                if (fileType.equalsIgnoreCase("jpg")) {
                    iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    iwp.setCompressionQuality(quality);
                } else if (quality < 1) {
                    LOGGER.debug("compression is not supported for " + fileType + " files, " + filePath);
                }

                FileHelper.createDirectoriesAndFile(filePath);

                File outFile = new File(filePath);
                FileImageOutputStream output = new FileImageOutputStream(outFile);
                writer.setOutput(output);
                IIOImage newImage = new IIOImage(image, null, null);
                writer.write(null, newImage, iwp);
                output.close();
            }

            return true;

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return false;
    }

    /**
     * <p>
     * Given a set of images, we cluster them by similarity in order to remove duplicates. In each cluster with more
     * than one image, we pick the one with the highest resolution. We return a set of image URLs with the highest
     * resolving pictures.
     * </p>
     *
     * @param imageUrls A collection of image URLs.
     * @return A set of image URLs that all represent different images (the highest resolving images per cluster).
     */
    public static Set<String> clusterImagesAndPickRepresentatives(Collection<String> imageUrls) {

        Set<String> selectedImages = new HashSet<>();

        // keep the index of the loaded image and tie it to the image URL
        Map<Integer, String> indexUrlMap = new HashMap<>();

        // load all images
        List<BufferedImage> loadedImages = new ArrayList<>();
        for (String imageUrl : imageUrls) {
            indexUrlMap.put(loadedImages.size(), imageUrl);
            BufferedImage loadedImage = load(imageUrl);
            loadedImages.add(loadedImage);
        }

        // hold the representatives per cluster, <clusterId,[imageIds]>
        Map<Integer, List<Integer>> representatives = new HashMap<>();

        // remember all image ids that are in a cluster already
        Set<Integer> clusteredImageIds = new HashSet<>();

        for (int i = 0; i < loadedImages.size(); i++) {

            BufferedImage image1 = loadedImages.get(i);

            boolean image1ClusteredAlready = !clusteredImageIds.add(i);
            if (!image1ClusteredAlready) {
                List<Integer> newList = new ArrayList<>();
                newList.add(i);
                representatives.put(i, newList);
            } else {
                continue;
            }

            // int image1PixelCount = getPixelCount(image1);

            for (int j = i + 1; j < loadedImages.size(); j++) {

                boolean image2ClusteredAlready = clusteredImageIds.contains(j);
                if (image2ClusteredAlready) {
                    continue;
                }

                BufferedImage image2 = loadedImages.get(j);
                boolean isDuplicate = isDuplicate(image1, image2);

                if (isDuplicate) {
                    representatives.get(i).add(j);
                    clusteredImageIds.add(j);
                }
            }
        }

        // pick highest resolving representative
        for (Entry<Integer, List<Integer>> cluster : representatives.entrySet()) {

            // System.out.println(cluster.getKey());
            // CollectionHelper.print(cluster.getValue());

            int highestPixelCount = 0;
            String highestResolutionImageUrl = "";
            for (Integer imageId : cluster.getValue()) {

                BufferedImage loadedImage = loadedImages.get(imageId);

                int pixelCount = getPixelCount(loadedImage);
                if (pixelCount > highestPixelCount) {
                    highestResolutionImageUrl = indexUrlMap.get(imageId);
                    highestPixelCount = pixelCount;
                }

            }

            selectedImages.add(highestResolutionImageUrl);

        }

        return selectedImages;
    }

    private static int getPixelCount(BufferedImage image) {
        return image.getWidth() * image.getHeight();
    }

    public static Color hexToRgb(String colorStr) {
        if (colorStr.startsWith("#")) {
            colorStr = colorStr.substring(1);
        }
        return new Color(Integer.valueOf(colorStr.substring(0, 2), 16), Integer.valueOf(colorStr.substring(2, 4), 16), Integer.valueOf(colorStr.substring(4, 6), 16));
    }

    public static String rgbToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static List<ws.palladian.extraction.multimedia.Color> detectColors(BufferedImage bufferedImage) {
        final double maxClusterDistance = 50;

        final int upperBound = 245;
        final int lowerBound = 10;

        List<ColorCluster> clusters = new ArrayList<>();

        for (int i = 0; i < bufferedImage.getWidth(); i++) {
            ol: for (int j = 0; j < bufferedImage.getHeight(); j++) {

                Color c1 = new Color(bufferedImage.getRGB(i, j));

                // discard too white and too dark
                if ((c1.getRed() > upperBound && c1.getGreen() > upperBound && c1.getBlue() > upperBound)
                        || (c1.getRed() < lowerBound && c1.getGreen() < lowerBound && c1.getBlue() < lowerBound)) {
                    continue;
                }

                // check whether we can add the color to an existing cluster or to open a new one
                for (ColorCluster cluster : clusters) {
                    double distance = colorDistance(cluster.getCenterColor(), c1);
                    if (distance < maxClusterDistance) {
                        cluster.totalRed += c1.getRed();
                        cluster.totalGreen += c1.getGreen();
                        cluster.totalBlue += c1.getBlue();
                        cluster.population++;
                        continue ol;
                    }
                }

                ColorCluster newCluster = new ColorCluster();
                newCluster.totalRed += c1.getRed();
                newCluster.totalGreen += c1.getGreen();
                newCluster.totalBlue += c1.getBlue();
                newCluster.population++;
                clusters.add(newCluster);
            }
        }

        Collections.sort(clusters, (o1, o2) -> o2.population - o1.population);

        List<ws.palladian.extraction.multimedia.Color> colors = new ArrayList<>();
        Set<String> seenMainColors = new HashSet<>();

        // go through clusters and get top 3 main colors
        for (ColorCluster cluster : clusters) {
            Color imageColor = cluster.getCenterColor();
            String hex = rgbToHex(imageColor);

            Double bestMatchScore = null;
            ws.palladian.extraction.multimedia.Color bestMatchColor = null;

            for (ws.palladian.extraction.multimedia.Color currentColor : COLORS) {
                Color color = hexToRgb(currentColor.getHexCode());
                double distance = colorDistance(imageColor, color);
                if (bestMatchColor == null || distance < bestMatchScore) {
                    bestMatchColor = new ws.palladian.extraction.multimedia.Color(hex, currentColor.getSpecificColorName(), currentColor.getMainColorName());
                    bestMatchScore = distance;
                }

            }

            if (seenMainColors.add(bestMatchColor.getMainColorName())) {
                colors.add(bestMatchColor);
            }

            if (seenMainColors.size() >= 3) {
                break;
            }
        }

        return colors;
    }

    /**
     * Compute the perceptual distance of two colors. We use the CIE Lab color space as that is closer to how humans
     * perceive colors.
     *
     * @param color1 First color.
     * @param color2 Second color.
     * @return A distance between the colors.
     */
    private static double colorDistance(Color color1, Color color2) {
        double[] lab1 = new ColorSpaceConverter().rgbToLab(color1.getRed(), color1.getGreen(), color1.getBlue());
        double[] lab2 = new ColorSpaceConverter().rgbToLab(color2.getRed(), color2.getGreen(), color2.getBlue());

        double lDistance = Math.pow(lab1[0] - lab2[0], 2);
        double aDistance = Math.pow(lab1[1] - lab2[1], 2);
        double bDistance = Math.pow(lab1[2] - lab2[2], 2);

        return Math.sqrt(lDistance + aDistance + bDistance);
    }

    public static BufferedImage reduceColors(BufferedImage image, int numberOfColors) {

        // NOTE: 256 seems to be a good number for OCTTREE as upper bound, default would be 65536, this parameter is
        // ColorQuantizerType dependent!
        final RenderedOp cqImage = ColorQuantizerDescriptor.create(image, ColorQuantizerDescriptor.OCTTREE, numberOfColors, 256, null, null, null, null);

        return cqImage.getAsBufferedImage();
    }

    public static Color getNearestColor(Color color, Collection<Color> colorPalette) {
        Color nearestColor = null;
        Double nearestDistance = Double.MAX_VALUE;
        for (Color color1 : colorPalette) {
            Double distance = colorDistance(color, color1);
            if (nearestColor == null || nearestDistance > distance) {
                nearestColor = color1;
                nearestDistance = distance;
            }
        }

        return nearestColor;
    }

    public static BufferedImage pixelate(BufferedImage image, int boxSize) {
        return pixelate(image, boxSize, null);
    }

    public static BufferedImage pixelate(BufferedImage image, int boxSize, Collection<Color> colorPalette) {

        for (int w = 0; w < image.getWidth(); w += boxSize) {
            for (int h = 0; h < image.getHeight(); h += boxSize) {
                Color color = new Color(image.getRGB(w, h));

                if (colorPalette != null) {
                    color = getNearestColor(color, colorPalette);
                }

                Graphics imageGraphics = image.getGraphics();

                imageGraphics.setColor(color);
                imageGraphics.fillRect(w, h, boxSize, boxSize);
            }
        }

        return image;
    }

    public static LinkedHashMap<Color, Integer> getColorFrequencies(BufferedImage image) {

        Bag<Color> colorCounter = new Bag<>();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                colorCounter.add(new Color(rgb));
            }
        }
        Bag<Color> sorted = colorCounter.createSorted(CollectionHelper.Order.DESCENDING);
        LinkedHashMap<Color, Integer> frequencies = new LinkedHashMap<>();
        for (Color s : sorted.uniqueItems()) {
            frequencies.put(s, sorted.count(s));
        }

        return frequencies;
    }

    public static Color getRandomColor() {
        return new Color(MathHelper.getRandomIntBetween(0, 255), MathHelper.getRandomIntBetween(0, 255), MathHelper.getRandomIntBetween(0, 255));
    }

    /**
     * Fill a region with the same color with a new color.
     * NOTE: This is recursive and might require to increase stack size (-Xss) for larger images.
     *
     * @param image The image.
     * @param x Starting x for the flood fill.
     * @param y Starting y for the flood fill.
     * @param followColor The color to follow.
     * @param replacementColor The replacement color.
     * @param pixels A collection of pixels that were changed.
     */
    public static void floodFill(BufferedImage image, int x, int y, Color followColor, Color replacementColor, Collection<Point> pixels) {
        if (image.getRGB(x, y) != followColor.getRGB()) {
            return;
        }
        image.setRGB(x, y, replacementColor.getRGB());
        pixels.add(new Point(x, y));
        floodFill(image, x - 1, y - 1, followColor, replacementColor, pixels);
        floodFill(image, x, y - 1, followColor, replacementColor, pixels);
        floodFill(image, x + 1, y - 1, followColor, replacementColor, pixels);
        floodFill(image, x - 1, y, followColor, replacementColor, pixels);
        floodFill(image, x + 1, y, followColor, replacementColor, pixels);
        floodFill(image, x - 1, y + 1, followColor, replacementColor, pixels);
        floodFill(image, x, y + 1, followColor, replacementColor, pixels);
        floodFill(image, x + 1, y + 1, followColor, replacementColor, pixels);
    }

    /**
     * Use a simple 3x3 kernel on a gray scale image to detect frequencies (quick changes in brightness).
     *
     * @param image The image
     * @return Statistics about brightness differences.
     */
    public static FatStats detectFrequencies(BufferedImage image) {
        BufferedImage grayImage = toGrayScale(image);

        FatStats frequencyStats = new FatStats();
        for (int i = 1; i < grayImage.getWidth() - 1; i++) {
            for (int j = 1; j < grayImage.getHeight() - 1; j++) {
                Color thisColor = new Color(grayImage.getRGB(i, j));
                Set<Color> colors = new HashSet<>();
                Color color1 = new Color(grayImage.getRGB(i - 1, j - 1));
                Color color2 = new Color(grayImage.getRGB(i, j - 1));
                Color color3 = new Color(grayImage.getRGB(i, j + 1));
                Color color4 = new Color(grayImage.getRGB(i - 1, j));
                Color color5 = new Color(grayImage.getRGB(i + 1, j));
                Color color6 = new Color(grayImage.getRGB(i - 1, j + 1));
                Color color7 = new Color(grayImage.getRGB(i, j + 1));
                Color color8 = new Color(grayImage.getRGB(i + 1, j + 1));
                colors.add(color1);
                colors.add(color2);
                colors.add(color3);
                colors.add(color4);
                colors.add(color5);
                colors.add(color6);
                colors.add(color7);
                colors.add(color8);
                double[] doubles = ColorSpaceConverter.rgbToHsb(thisColor);
                double v = 0.;
                for (Color color : colors) {
                    double[] doubles2 = ColorSpaceConverter.rgbToHsb(color);
                    v += Math.abs(doubles2[2] - doubles[2]);
                }
                frequencyStats.add(v / colors.size());
            }
        }

        return frequencyStats;
    }

    /**
     * Detect edges in the given image using SOBEL.
     *
     * @param image The image in which we want to detect edges.
     * @return The image with detected edges.
     */
    public static BufferedImage detectEdges(BufferedImage image) {

        // erode image first to get rid of noise around real edges
        float[] floats = new float[25];
        for (int i = 0; i < 25; i++) {
            floats[i] = 1;
        }
        RenderedOp erodeOp = ErodeDescriptor.create(image, new KernelJAI(5, 5, floats), null);
        BufferedImage erodedImage = erodeOp.getAsBufferedImage();

        PlanarImage temp = GradientMagnitudeDescriptor.create(erodedImage, KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL, KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL, null).createInstance();

        return temp.getAsBufferedImage();
    }

    public static void main(String[] args) throws Exception {
        // BufferedImage loadedImage =
        // load("http://de.mathworks.com/help/releases/R2015b/examples/images/DetectEdgesInImagesExample_01.png");
        // BufferedImage loadedImage = load("D:\\yelp\\train_photos\\170350.jpg");
        // BufferedImage loadedImage =
        // load("https://www.baskinrobbins.com/content/dam/baskinrobbins/Product%20Images/Beverages,%20Mix-Ins,%20Novelties,%20Parfaits,%20Quarts%20and%20Sundaes/Novelties/Clown_Cone_000l.jpg");
        // BufferedImage loadedImage =
        // load("https://www.baskinrobbins.com/content/dam/baskinrobbins/Product%20Images/Beverages,%20Mix-Ins,%20Novelties,%20Parfaits,%20Quarts%20and%20Sundaes/Sundaes/Sundae_Enlarged_2scoop2.jpg");
//        BufferedImage loadedImage = load("https://media.tenor.com/images/c72d51692045c85b0f5f213f113ebfc4/tenor.gif");
        ImageHandler.downloadAndSave("https://media.tenor.com/images/c72d51692045c85b0f5f213f113ebfc4/tenor.gif", "tenor-downloaded.gif");
        System.exit(0);

//        InputStream in = new FileInputStream("tenor-src.gif");
//        Image image = Toolkit.getDefaultToolkit().createImage(org.apache.commons.io.IOUtils.toByteArray(in));
//
//        Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
//        if (!iter.hasNext()) {
//            throw new IIOException("no GIF Image Writers exist");
//        } else {
//            ImageWriter writer = iter.next();
//            ImageWriteParam iwp = writer.getDefaultWriteParam();
//
//            FileHelper.createDirectoriesAndFile("tenor-dest.gif");
//
//            File outFile = new File("tenor-dest.gif");
//            FileImageOutputStream output = new FileImageOutputStream(outFile);
//            writer.setOutput(output);
//            IIOImage newImage = new IIOImage(loadedImage, null, null);
//            writer.write(null, newImage, iwp);
//            output.close();
//        }

//        File outFile = new File("tenor-dest.gif");
//        Graphics2D g2 = image.createGraphics();
//        g2.drawImage(img, 0, 0, null);
//        g2.dispose();
//        ImageIO.write(bi, "jpg", new File("img.jpg"));
//        ImageIcon image = new ImageIcon(loadedImage);
//        int width = 100;
//        int height = 100;
//        image.setImage(image.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT));
//        image.

        // BufferedImage loadedImage = load("GrilledChickenCaesarSalad_579x441.jpg");
//        System.out.println(loadedImage.getWidth());
//        ImageHandler.saveImage(loadedImage, "tenor-dest.gif");
        // saveImage(detectEdges(loadedImage), "gradient.png");
        // saveImage(detectEdges(toGrayScale(loadedImage)), "gradient-grey.png");
        // System.out.println(ImageHandler.detectEdginess(load("gradient-grey.png")));
        System.exit(0);

        // spaghetti 0.49
        // balloons 124871: 0.375
        // menu 170357: 0.206
        // plate 170350: 0.234

        // ParameterBlock param = new ParameterBlock();
        // BufferedImage load = load("D:\\yelp\\train_photos\\248344.jpg");
        // int w = load.getWidth();
        // int h = load.getHeight();
        //
        // GraphicsDevice gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        // GraphicsConfiguration gc = gs.getDefaultConfiguration();
        //
        // BufferedImage img = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
        // img.getGraphics().drawImage(load, 0, 0, null);
        //
        //// System.out.println(load.getColorModel());
        //// param.addSource(load);
        ////// param.add(DFTDescriptor.SCALING_NONE);
        ////// param.add(DFTDescriptor.REAL_TO_COMPLEX);
        ////// param.add(DCTDescriptor.NO_PARAMETER_DEFAULT);
        //// RenderedOp dft = JAI.create("DCT", param);
        ////
        //// PlanarImage dct = JAI.create("dct", param, null);
        //// int w = dct.getWidth();
        //// int h = dct.getHeight();
        //
        // // obtain information in frequency domain
        //// int dctData[] = dct.getData().getPixels(0, 0, w, h, (int[])null);
        //// double[] pixels = new double[dctData.length];
        //
        // int[] rgb1 = new int[w*h];
        // img.getRaster().getDataElements(0, 0, w, h, rgb1);
        // double[] array = new double[w*h];
        //
        // for (int i=0; i<w*h; i++) {
        // array[i] = (double) (rgb1[i] & 0xFF);
        // }
        //
        // DoubleDCT_2D tr = new DoubleDCT_2D(w, h);
        // tr.forward(array, true);
        //
        // SlimStats stat = new SlimStats();
        // for (int i=0; i<w*h; i++)
        // {
        // // Grey levels
        // int val= Math.min((int) (array[i]+128), 255);
        // rgb1[i] = (val <<16) | (val << 8) | val;
        // stat.add(rgb1[i]);
        //
        // }
        //
        // img.getRaster().setDataElements(0, 0, w, h, rgb1);
        //
        // System.out.println(stat);
        // saveImage(img, "data/temp/pics/dct3.jpg");

        System.out.println("=== LOW");
        detectFrequencies(load("D:\\yelp\\train_photos\\266414.jpg")); // should be low
        detectFrequencies(load("D:\\yelp\\train_photos\\266895.jpg")); // should be low
        detectFrequencies(load("D:\\yelp\\train_photos\\266876.jpg")); // should be low
        detectFrequencies(load("D:\\yelp\\train_photos\\266991.jpg")); // should be low
        System.out.println("=== HIGH");
        detectFrequencies(load("D:\\yelp\\train_photos\\266453.jpg")); // should be high
        detectFrequencies(load("D:\\yelp\\train_photos\\266921.jpg")); // should be high
        detectFrequencies(load("D:\\yelp\\train_photos\\266966.jpg")); // should be high
        detectFrequencies(load("D:\\yelp\\train_photos\\266958.jpg")); // should be high
        System.exit(0);

        List<Color> palette = new ArrayList<>();
        palette.add(Color.BLACK);
        palette.add(Color.WHITE);
        palette.add(Color.GRAY);
        // chromatic circle
        palette.add(new Color(7, 139, 91));
        palette.add(new Color(134, 185, 53));
        palette.add(new Color(234, 227, 49));
        palette.add(new Color(245, 194, 46));
        palette.add(new Color(235, 139, 47));
        palette.add(new Color(229, 95, 45));
        palette.add(new Color(221, 38, 44));
        palette.add(new Color(190, 0, 121));
        palette.add(new Color(107, 51, 133));
        palette.add(new Color(71, 71, 145));
        palette.add(new Color(53, 104, 169));
        palette.add(new Color(36, 143, 181));
        int pixelSize = 10;
        String iName = "264505";
        BufferedImage pixelated = ImageHandler.pixelate(ImageHandler.load("D:\\yelp\\train_photos\\" + iName + ".jpg"), 10);
        ImageHandler.saveImage(pixelated, "data/temp/pics/pixelated-nopalette.jpg");
        pixelated = ImageHandler.pixelate(ImageHandler.load("D:\\yelp\\train_photos\\" + iName + ".jpg"), pixelSize, palette);
        ImageHandler.saveImage(pixelated, "data/temp/pics/pixelated-palette.jpg");
        System.exit(0);

        // BufferedImage testImg = ImageHandler.load("data/temp/img/testImage.jpg");
        // BufferedImage testImg = ImageHandler.load("http://162.61.226.249/PicOriginal/ChocolatePecanPie8917.jpg");
        BufferedImage testImg = ImageHandler.load("https://res.svh24.de/images2/720/2/257/1012007099_1.jpg");
        CollectionHelper.print(ImageHandler.detectColors(testImg));
        testImg = ImageHandler.load("https://res.svh24.de/images2/720/4/119/1000000095_1.jpg");
        CollectionHelper.print(ImageHandler.detectColors(testImg));
        testImg = ImageHandler.load("https://res.svh24.de/images2/720/1/271/1011038627_1.jpg");
        CollectionHelper.print(ImageHandler.detectColors(testImg));
        testImg = ImageHandler.load("http://cdn1-www.webecoist.momtastic.com/assets/uploads/2008/12/8-green-camera.jpg");
        CollectionHelper.print(ImageHandler.detectColors(testImg));
        testImg = ImageHandler.load("http://www.fotokoch.de/bilddaten/bildklein/samsung-wb50f-rot_60185.jpg");
        CollectionHelper.print(ImageHandler.detectColors(testImg));
        testImg = ImageHandler.load("http://cdn.itechnews.net/wp-content/uploads/2012/09/HTC-8X-Windows-Phone-8-Smartphone-flaming-red.jpg");
        CollectionHelper.print(ImageHandler.detectColors(testImg));
        testImg = ImageHandler.load("http://image01.bonprix.de/bonprixbilder/460x644/1427714799/15023250-ujoMmNo0.jpg");
        CollectionHelper.print(ImageHandler.detectColors(testImg));
        System.exit(0);

        StopWatch sw = new StopWatch();
        // BufferedImage testImage = ImageHandler.boxCrop(testImg, 800, 500);
        BufferedImage testImage = ImageHandler.boxCrop(testImg, 500, 100);
        // BufferedImage testImage = ImageHandler.rescaleImageSmooth(testImg, 500, 500);
        // BufferedImage testImage = ImageHandler.rescaleImage(testImg, 350, 233);
        System.out.println(sw.getElapsedTimeString());
        ImageHandler.saveImage(testImage, "jpg", "testBoxCrop.jpg");
        System.exit(0);

        // String url = "http://entimg.msn.com/i/gal/ScaryCelebs/JimCarrey_400.jpg";
        // url = "http://www.thehollywoodnews.com/artman2/uploads/1/jim-carrey_1.jpg";
        // URL urlLocation;

        Collection<String> imageUrls = new ArrayList<>();
        imageUrls.add("imageA1.jpg");
        imageUrls.add("imageA2.jpg");
        imageUrls.add("imageB1.jpg");
        imageUrls.add("imageA3.jpg");
        imageUrls.add("imageC1.jpg");
        imageUrls.add("imageB2.jpg");
        Set<String> representatives = ImageHandler.clusterImagesAndPickRepresentatives(imageUrls);
        CollectionHelper.print(representatives);

        System.exit(0);
        // BufferedImage i0 = ImageHandler
        // .load("http://static0.cinefreaks.com/application/frontend/images/movies/Brautalarm_1.jpg");
        // ImageHandler.saveImage(i0, "jpg", "testOriginal.jpg");
        // BufferedImage i1 = ImageHandler.rescaleImageAndCrop(i0, 400, 100);
        // ImageHandler.saveImage(i1, "jpg", "testCrop.jpg");
        // i1 = ImageHandler.rescaleImage(i0, 500, 500);
        // ImageHandler.saveImage(i1, "jpg", "testRescale.jpg");
        // i1 = ImageHandler.rescaleImageOptimal(i0, 500, 500);
        // ImageHandler.saveImage(i1, "jpg", "testRescaleOptimal.jpg");

        BufferedImage duplicate1 = ImageHandler.load("http://static0.cinefreaks.com/application/frontend/images/movies/Brautalarm_2.jpg");

        BufferedImage duplicate2 = ImageHandler.load("http://static0.cinefreaks.com/application/frontend/images/movies/Brautalarm_5.jpg");

        System.out.println(ImageHandler.isDuplicate(duplicate1, duplicate2));

        System.exit(0);

        // urlLocation = new URL(url);
        BufferedImage bufferedImage = ImageIO.read(new File("data/test/images/tdk5.jpg"));
        // bufferedImage = rescaleImage(bufferedImage, 200);
        // saveImage(bufferedImage, "jpg", "data/test/images/1_tdk1.jpg");
        // System.exit(0);

        // measure performance of rescaling algorithms
        // long t1 = System.currentTimeMillis();
        // for (int i = 0; i < 20; i++) {
        // rescaleImage3(bufferedImage, 200);
        // }
        // DateHelper.getRuntime(t1, System.currentTimeMillis(), true);
        // System.exit(0);

        saveImage(bufferedImage, "jpg", "data/test/images/1_tdk1.jpg");

        bufferedImage = rescaleImage(bufferedImage, 200);
        System.out.println(bufferedImage.getWidth());
        saveImage(bufferedImage, "jpg", "data/test/images/1_tdk1_rescaled.jpg");

        System.exit(0);

        bufferedImage.getScaledInstance(200, -1, Image.SCALE_SMOOTH);

        // bufferedImage = rescaleImage3(ImageIO.read(new File("data/test/images/tdk5.jpg")), 200);
        saveImage(bufferedImage, "jpg", "data/test/images/1_tdk1_rescaled3.jpg");

        System.exit(0);

        // RenderedImage ri = JAI.create("fileload", "data/test/images/tdk1.jpg");
        // RenderedOp op = JAI.create("filestore", ri, "data/multimedia/tdk1_rescaled.jpg", "JPEG");

        // BufferedImage image1 = Sanselan.getBufferedImage(new File("data/test/images/tdk1.jpg"));

        // Sanselan.writeImage(image1, new File("data/multimedia/tdk1_rescaled.jpg"), ImageFormat.IMAGE_FORMAT_JPEG, new
        // HashMap());
        // jigl.image.io.ImageInputStreamJAI is = new
        // jigl.image.io.ImageInputStreamJAI("data/multimedia/images/test.jpg");
        // ColorImage ci = (ColorImage) is.read();
        // ci.add(10,10,10);
        // ImageOutputStreamJAI outputJPEG=new ImageOutputStreamJAI("data/multimedia/images/test2.jpg");
        // outputJPEG.writeJPEG(ci);

        // urlLocation = new URL("http://entimg.msn.com/i/gal/ScaryCelebs/JimCarrey_400.jpg");
        BufferedImage bufferedImage2 = ImageIO.read(new File("data/multimedia/jc2g.jpg"));
        bufferedImage2 = rescaleImage(bufferedImage2, 200);

        BufferedImage substractedImage = ImageHandler.substractImages(bufferedImage, bufferedImage2);
        ImageHandler.getAverageGray(substractedImage);

        ImageIO.write(substractedImage, "jpg", new File("data/multimedia/test.jpg"));
    }
}