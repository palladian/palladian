package ws.palladian.extraction.multimedia;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.color.CMMException;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.swing.ImageIcon;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.search.images.WebImageResult;

/**
 * <p>
 * A handler for images.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class ImageHandler {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageHandler.class);

    /** Image similarity mean square error. */
    public static final int MSE = 1;

    /** Image similarity with Minkowsi. */
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
        BufferedImage bufferedImage = null;
        bufferedImage = ImageIO.read(imageFile);

        return bufferedImage;
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
        BufferedImage bufferedImage = null;

        try {
            url = url.trim();
            if (url.startsWith("http:") || url.startsWith("https:")) {
                HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
                HttpResult httpResult = retriever.httpGet(url);
                bufferedImage = ImageIO.read(new ByteArrayInputStream(httpResult.getContent()));
            } else {
                bufferedImage = ImageIO.read(new File(url));
            }
        } catch (MalformedURLException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error(url + ", " + e.getMessage());
        }

        return bufferedImage;
    }

    public static String getMatchingImageUrl(Collection<WebImageResult> images) {
        String[] matchingImages = getMatchingImageUrls(images, 1);
        if (matchingImages.length > 0) {
            return matchingImages[0];
        }
        return "";
    }

    public static String[] getMatchingImageUrls(Collection<WebImageResult> images, int matchingNumber) {

        try {

            // normalize all images to fixed width
            List<ExtractedImage> normalizedImages = new ArrayList<ExtractedImage>();

            for (WebImageResult image : images) {
                BufferedImage bufferedImage = null;
                try {
                    bufferedImage = load(image.getUrl());
                    if (bufferedImage != null) {
                        bufferedImage = rescaleImage(bufferedImage, 200);
                        image.setImageContent(bufferedImage);
                        normalizedImages.add(new ExtractedImage(image));
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    LOGGER.error(image.getUrl());
                } catch (IllegalArgumentException e) {
                    LOGGER.error(image.getUrl());
                } catch (CMMException e) {
                    LOGGER.error(image.getUrl());
                } catch (Exception e) {
                    LOGGER.error(image.getUrl());
                }
            }
            images.clear();

            // compare images with almost or exactly the same width height ratio
            Set<String> duplicateImages = new HashSet<String>();
            for (int i = 0; i < normalizedImages.size() - 1; i++) {
                ExtractedImage image1 = normalizedImages.get(i);

                for (int j = i + 1; j < normalizedImages.size(); j++) {
                    try {
                        ExtractedImage image2 = normalizedImages.get(j);
                        if (duplicateImages.contains(image2.getUrl())) {
                            continue;
                        }

                        if (!MathHelper
                                .isWithinMargin(image1.getWidthHeightRatio(), image2.getWidthHeightRatio(), 0.05)) {
                            continue;
                        }
                        if (isDuplicate(image1.getImageContent(), image2.getImageContent())) {
                            image1.addDuplicate();
                            image1.addRanking(image2.getRankCount());
                            duplicateImages.add(image2.getUrl());
                        }
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            }
            duplicateImages.clear();

            // order images by ranking and collect urls
            Collections.sort(normalizedImages, new Comparator<ExtractedImage>() {
                @Override
                public int compare(ExtractedImage image1, ExtractedImage image2) {
                    return (int)(1000 * image2.getRanking() - 1000 * image1.getRanking());
                }
            });
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
        double scale = Math.max((double)boxWidth / (double)image.getWidth(),
                (double)boxHeight / (double)image.getHeight());

        int targetWidth = Math.max((int)(image.getWidth() * scale), boxWidth);
        int targetHeight = Math.max((int)(image.getHeight() * scale), boxHeight);

        image = boxFit(image, targetWidth, targetHeight, false);

        // saveImage(image, "jpg", "test.jpg");

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

    private static BufferedImage rescaleImage(BufferedImage bufferedImage, int boxWidth, int boxHeight, boolean toFit) {

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

        RenderingHints qualityHints1 = new RenderingHints(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        // results in exactly the same as above (tested only for downscaling)
        // RenderingHints qualityHints1 = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
        // RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        RenderedOp resizedImage = null;

        resizedImage = JAI.create("SubsampleAverage", pb, qualityHints1);

        return resizedImage.getAsBufferedImage();
    }

    private static BufferedImage rescaleImage(BufferedImage bufferedImage, double scale) {

        // "SubsampleAverage" is smooth but does only work for downscaling. If upscaling, we need to use "Scale".
        boolean upscale = false;
        if (scale > 1.0) {
            upscale = true;
        }

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(bufferedImage); // The source image
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

        RenderingHints qualityHints1 = new RenderingHints(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        // results in exactly the same as above (tested only for downscaling)
        // RenderingHints qualityHints1 = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
        // RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        RenderedOp resizedImage = null;

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
        Image resizedImage = null;

        int width = (int)(Math.round(scaleX * bufferedImage.getWidth()));
        int height = (int)(Math.round(scaleY * bufferedImage.getHeight()));
        resizedImage = image.getScaledInstance(width,
                height, Image.SCALE_SMOOTH);

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

    public static boolean downloadAndSave(String url, String savePath) {

        boolean success = false;

        try {

            BufferedImage bi = load(url);

            // get file extension
            String fileExtension = FileHelper.getFileType(url);
            if (fileExtension.length() == 0) {
                fileExtension = "png";
            }

            if (!savePath.toLowerCase().endsWith(fileExtension.toLowerCase())) {
                savePath += "." + fileExtension;
            }

            // save image
            LOGGER.debug("write " + savePath + " with " + fileExtension);
            FileHelper.createDirectoriesAndFile(savePath);
            ImageIO.write(bi, fileExtension, new File(savePath));

            success = true;
        } catch (MalformedURLException e) {
            LOGGER.error(url, e);
        } catch (IOException e) {
            LOGGER.error(url, e);
        } catch (NullPointerException e) {
            LOGGER.error(url, e);
        } catch (IllegalArgumentException e) {
            LOGGER.error(url, e);
        }

        return success;
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

        float averageGray = grayCount / (float)pixelCount;
        return averageGray;
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

        double meanSquareError = 1 / (double)(image1.getWidth() * image1.getHeight()) * squaredError;
        return meanSquareError;
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
        if (similarity > 0.82) {
            return true;
        }

        return false;
    }

    /**
     * Save an image to disk. This methods wraps the ImageIO.write method and does error handling.
     * 
     * @param image The image to save.
     * @param fileType The image type (e.g. "jpg")
     * @param filePath The path where the image should be saved.
     * @return True if the image was saved successfully, false otherwise.
     */
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
                    LOGGER.warn("compression is not supported for " + fileType + " files, " + filePath);
                }

                File outFile = new File(filePath);
                FileImageOutputStream output = new FileImageOutputStream(outFile);
                writer.setOutput(output);
                IIOImage newImage = new IIOImage(image, null, null);
                writer.write(null, newImage, iwp);
                output.close();
            }

            return true;

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return false;
    }

    /**
     * Save an image to disk. This methods wraps the ImageIO.write method and does error handling.
     * 
     * @param image The image to save.
     * @param fileType The image type (e.g. "jpg")
     * @param filePath The path where the image should be saved.
     * @return True if the image was saved successfully, false otherwise.
     */
    @Deprecated
    public static boolean saveImage2(BufferedImage image, String fileType, String filePath) {
        try {
            ImageIO.write(image, fileType, new File(filePath));
            return true;
        } catch (IOException e) {
            LOGGER.error("saving of image failed, " + e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error("saving of image failed, " + e.getMessage());
        }
        return false;
    }

    /**
     * <p>
     * Save an image to disk. This methods wraps the JAI.create method and does error handling.
     * </p>
     * 
     * @param image The image to save.
     * @param fileType The image type (e.g. "jpg")
     * @param filePath The path where the image should be saved.
     * @return True if the image was saved successfully, false otherwise.
     */
    @Deprecated
    public static boolean saveImage3(BufferedImage bufferedImage, String fileType, String filePath) {

        try {
            JAI.create("encode", bufferedImage, new FileOutputStream(new File(filePath)), fileType.toUpperCase(), null);
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

        Set<String> selectedImages = new HashSet<String>();

        // keep the index of the loaded image and tie it to the image URL
        Map<Integer, String> indexUrlMap = new HashMap<Integer, String>();

        // load all images
        List<BufferedImage> loadedImages = new ArrayList<BufferedImage>();
        for (String imageUrl : imageUrls) {
            indexUrlMap.put(loadedImages.size(), imageUrl);
            BufferedImage loadedImage = load(imageUrl);
            loadedImages.add(loadedImage);
        }

        // hold the representatives per cluster, <clusterId,[imageIds]>
        Map<Integer, List<Integer>> representatives = new HashMap<Integer, List<Integer>>();

        // remember all image ids that are in a cluster already
        Set<Integer> clusteredImageIds = new HashSet<Integer>();

        for (int i = 0; i < loadedImages.size(); i++) {

            BufferedImage image1 = loadedImages.get(i);

            boolean image1ClusteredAlready = !clusteredImageIds.add(i);
            if (!image1ClusteredAlready) {
                List<Integer> newList = new ArrayList<Integer>();
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

    public static void main(String[] args) throws Exception {

        // BufferedImage testImg = ImageHandler.load("data/temp/img/testImage.jpg");
        BufferedImage testImg = ImageHandler.load("http://162.61.226.249/PicOriginal/ChocolatePecanPie8917.jpg");
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

        Collection<String> imageUrls = new ArrayList<String>();
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

        BufferedImage duplicate1 = ImageHandler
                .load("http://static0.cinefreaks.com/application/frontend/images/movies/Brautalarm_2.jpg");

        BufferedImage duplicate2 = ImageHandler
                .load("http://static0.cinefreaks.com/application/frontend/images/movies/Brautalarm_5.jpg");

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