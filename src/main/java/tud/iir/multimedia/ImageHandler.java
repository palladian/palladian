package tud.iir.multimedia;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.color.CMMException;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ScaleDescriptor;
import javax.swing.ImageIcon;

import org.apache.log4j.Logger;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;

/**
 * A handler for images.
 * 
 * @author David Urbansky
 */
public class ImageHandler {

    // image similarity mean square error
    public static final int MSE = 1;

    // image similarity with minkowsi
    public static final int MINKOWSKI = 2;

    // image similarity with image difference and average gray values
    public static final int DIFFG = 3;

    private static final Logger logger = Logger.getLogger(ImageHandler.class);

    static {
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    public static BufferedImage load(String url) {
        BufferedImage bufferedImage = null;

        try {
            if (url.indexOf("http:") > -1) {
                URL urlLocation = new URL(url);
                bufferedImage = ImageIO.read(urlLocation);
            } else {
                bufferedImage = ImageIO.read(new File(url));
            }
        } catch (MalformedURLException e) {
            Logger.getRootLogger().error(url, e);
        } catch (IOException e) {
            Logger.getRootLogger().error(url, e);
        }

        return bufferedImage;
    }

    public static String getMatchingImageURL(ArrayList<ExtractedImage> images) {
        String[] matchingImages = getMatchingImageURLs(images, 1);
        if (matchingImages.length > 0) {
            return matchingImages[0];
        }
        return "";
    }

    public static String[] getMatchingImageURLs(ArrayList<ExtractedImage> images, int matchingNumber) {

        URL urlLocation;
        try {

            // normalize all images to fixed width
            ArrayList<ExtractedImage> normalizedImages = new ArrayList<ExtractedImage>();
            Iterator<ExtractedImage> imageIterator = images.iterator();
            while (imageIterator.hasNext()) {
                ExtractedImage image = imageIterator.next();
                urlLocation = new URL(image.getURL());
                BufferedImage bufferedImage = null;
                try {
                    bufferedImage = ImageIO.read(urlLocation);
                    if (bufferedImage != null) {
                        bufferedImage = rescaleImage(bufferedImage, 200);
                        image.setImageContent(bufferedImage);
                        normalizedImages.add(image);
                    }
                } catch (IOException e) {
                    Logger.getRootLogger().error(image.getURL(), e);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Logger.getRootLogger().error(image.getURL(), e);
                } catch (IllegalArgumentException e) {
                    Logger.getRootLogger().error(image.getURL(), e);
                } catch (CMMException e) {
                    Logger.getRootLogger().error(image.getURL(), e);
                } catch (Exception e) {
                    Logger.getRootLogger().error(image.getURL(), e);
                }
            }
            images.clear();

            // compare images with almost or exactly the same width height ratio
            HashSet<String> duplicateImages = new HashSet<String>();
            for (int i = 0; i < normalizedImages.size() - 1; i++) {
                ExtractedImage image1 = normalizedImages.get(i);

                for (int j = i + 1; j < normalizedImages.size(); j++) {
                    ExtractedImage image2 = normalizedImages.get(j);
                    if (duplicateImages.contains(image2.getURL())) {
                        continue;
                    }

                    if (!MathHelper.isWithinMargin(image1.getWidthHeightRatio(), image2.getWidthHeightRatio(), 0.05)) {
                        continue;
                    }
                    if (isDuplicate(image1.getImageContent(), image2.getImageContent())) {
                        image1.addDuplicate();
                        image1.addRanking(image2.getRankCount());
                        duplicateImages.add(image2.getURL());
                    }
                }
            }
            duplicateImages.clear();

            // order images by ranking and collect urls
            Collections.sort(normalizedImages, new ExtractedImageComparator());
            CollectionHelper.print(normalizedImages);

            int matchingImages = Math.min(normalizedImages.size(), matchingNumber);
            String[] matchingImageURLs = new String[matchingImages];
            for (int i = 0; i < matchingImages; i++) {
                matchingImageURLs[i] = normalizedImages.get(i).getURL();
            }
            normalizedImages.clear();

            return matchingImageURLs;

        } catch (MalformedURLException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (OutOfMemoryError e) {
            Logger.getRootLogger().error(e.getMessage());
        }

        return new String[0];
    }

    public static BufferedImage rescaleImage(String imageURL, int width) {
        BufferedImage bufferedImage = null;
        try {
            /*
             * URL urlObject = new URL(imageURL); URLConnection urlConnection = null; urlConnection = urlObject.openConnection(); InputStream fis =
             * urlConnection.getInputStream(); JpegFilterInputStream jfis = new JpegFilterInputStream(fis); bufferedImage = ImageIO.read(jfis); jfis.close();
             */

            bufferedImage = ImageIO.read(new URL(imageURL));

        } catch (MalformedURLException e) {
            Logger.getRootLogger().error(e.getMessage() + " for image " + imageURL + " (target width: " + width + ")");
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage() + " for image " + imageURL + " (target width: " + width + ")");
        }

        return rescaleImage(bufferedImage, width);
    }

    /**
     * Rescaling an image using JAI SubsampleAverage for downscaling and getScaledInstance for upscaling. This produces smooth images but upscaling is slightly
     * slower.
     * 
     * @param bufferedImage The input image.
     * @param newWidth The desired new width (size) of the image.
     * @param fit If true, the newWidth will be the maximum side length of the image. Default is false.
     * @return The scaled image.
     */
    public static BufferedImage rescaleImageOptimal(BufferedImage bufferedImage, int newWidth, boolean fit) {

        int iWidth = bufferedImage.getWidth();
        int iHeight = bufferedImage.getHeight();

        double scale = (double) newWidth / (double) iWidth;

        if (fit && iWidth < iHeight) {
            scale = (double) newWidth / (double) iHeight;
        }

        if (scale > 1.0) {
            return rescaleImage3(bufferedImage, newWidth, fit);
        } else {
            return rescaleImage(bufferedImage, newWidth, fit);
        }

    }

    /**
     * Rescaling an image using JAI SubsampleAverage. The image looks smooth after rescaling.
     * 
     * @param bufferedImage The input image.
     * @param newWidth The desired new width (size) of the image.
     * @param fit If true, the newWidth will be the maximum side length of the image. Default is false.
     * @return The scaled image.
     */
    public static BufferedImage rescaleImage(BufferedImage bufferedImage, int newWidth, boolean fit) {

        if (bufferedImage == null) {
            logger.warn("given image was NULL");
            return null;
        }

        int iWidth = bufferedImage.getWidth();
        int iHeight = bufferedImage.getHeight();

        double scale = (double) newWidth / (double) iWidth;

        if (fit && iWidth < iHeight) {
            scale = (double) newWidth / (double) iHeight;
        }

        // "SubsampleAverage" is smooth but does only work for downscaling. If upscaling, we need to use "Scale".
        boolean upscale = false;
        if (scale > 1.0) {
            upscale = true;
        }

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(bufferedImage); // The source image
        // x scale
        if (upscale) {
            pb.add((float) scale);
        } else {
            pb.add(scale);
        }
        // y scale
        if (upscale) {
            pb.add((float) scale);
        } else {
            pb.add(scale);
        }
        // x translation
        pb.add(0.0f);
        // y translation
        pb.add(0.0f);
        pb.add(new InterpolationBicubic(4));
        pb.add(bufferedImage);

        RenderingHints qualityHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        RenderedOp resizedImage = null;

        if (upscale) {
            resizedImage = JAI.create("scale", pb, qualityHints);
        } else {
            resizedImage = JAI.create("SubsampleAverage", pb, qualityHints);
        }

        return resizedImage.getAsBufferedImage();
    }

    public static BufferedImage rescaleImage(BufferedImage bufferedImage, int newWidth) {
        return rescaleImage(bufferedImage, newWidth, false);
    }

    /**
     * Rescaling an image using JAI Scale descriptor. The image does not look smooth after rescaling.
     * 
     * @param bufferedImage The input image.
     * @param newWidth The desired new width (size) of the image.
     * @param fit If true, the newWidth will be the maximum side length of the image. Default is false.
     * @return The scaled image.
     */
    public static BufferedImage rescaleImage2(BufferedImage bufferedImage, int newWidth, boolean fit) {

        if (bufferedImage == null) {
            logger.warn("given image was NULL");
            return null;
        }

        RenderedOp renderedOp = null;

        int iWidth = bufferedImage.getWidth();
        int iHeight = bufferedImage.getHeight();

        float scale = (float) newWidth / (float) iWidth;

        if (fit && iWidth < iHeight) {
            scale = (float) newWidth / (float) iHeight;
        }

        RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        renderingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // renderingHints.put(RenderingHints.KEY_DITHERING,
        // RenderingHints.VALUE_DITHER_DISABLE);
        // renderingHints.put(RenderingHints.KEY_STROKE_CONTROL,
        // RenderingHints.VALUE_STROKE_PURE);
        // renderingHints.put(RenderingHints.KEY_FRACTIONALMETRICS,
        // RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        // renderingHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
        // RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        renderedOp = ScaleDescriptor.create(bufferedImage, new Float(scale), new Float(scale), new Float(0.0f), new Float(0.0f), Interpolation
                .getInstance(Interpolation.INTERP_BICUBIC), renderingHints);

        return renderedOp.getAsBufferedImage();
    }

    public static BufferedImage rescaleImage2(BufferedImage bufferedImage, int newWidth) {
        return rescaleImage2(bufferedImage, newWidth, false);
    }

    /**
     * Rescaling an image using java.awt.Image.getScaledInstance. The image looks smooth after rescaling.
     * 
     * @param bufferedImage The input image.
     * @param newWidth The desired new width (size) of the image.
     * @param fit If true, the newWidth will be the maximum side length of the image. Default is false.
     * @return The scaled image.
     */
    public static BufferedImage rescaleImage3(BufferedImage bufferedImage, int newWidth, boolean fit) {

        if (bufferedImage == null) {
            logger.warn("given image was NULL");
            return null;
        }

        ImageIcon imageIcon = new ImageIcon(bufferedImage);
        Image image = imageIcon.getImage();
        Image resizedImage = null;

        int iWidth = image.getWidth(null);
        int iHeight = image.getHeight(null);

        double scale = (double) newWidth / (double) iWidth;

        if (fit && iWidth < iHeight) {
            scale = (double) newWidth / (double) iHeight;
        }

        resizedImage = image.getScaledInstance((int) (scale * iWidth), (int) (scale * iHeight), Image.SCALE_SMOOTH);

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

        // Encodes image as a JPEG data stream
        // FileOutputStream out = new FileOutputStream(resizedFile);
        // com.sun.image.codec.jpeg.JPEGImageEncoder encoder = com.sun.image.codec.jpeg.JPEGCodec.createJPEGEncoder(out);
        // com.sun.image.codec.jpeg.JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(bufferedImage);
        // param.setQuality(1.0f, true);
        // encoder.setJPEGEncodeParam(param);
        // encoder.encode(bufferedImage);
        //        
        // out.close();

        return bufferedImage;
    }

    public static BufferedImage rescaleImage3(BufferedImage bufferedImage, int newWidth) {
        return rescaleImage3(bufferedImage, newWidth, false);
    }

    /**
     * @deprecated
     * @param bufferedImage
     * @param width
     * @return
     */
    @Deprecated
    public static BufferedImage rescaleImage_broken(BufferedImage bufferedImage, int width) {

        if (bufferedImage == null) {
            logger.error("image was null and could not be rescaled");
            return null;
        }

        double factor = (double) width / (double) bufferedImage.getWidth();
        int newHeight = (int) (factor * bufferedImage.getHeight());

        AffineTransform tx = new AffineTransform();
        tx.scale(factor, factor);
        AffineTransform tx1 = AffineTransform.getScaleInstance(factor, factor);

        BufferedImage rescaledImage = new BufferedImage(width, newHeight, bufferedImage.getType());

        AffineTransformOp op = new AffineTransformOp(tx1, AffineTransformOp.TYPE_BICUBIC);
        op.filter(bufferedImage.getRaster(), rescaledImage.getRaster());

        return rescaledImage;
    }

    /*
     * public static void resizeVeryHigh(File originalFile, File resizedFile, int newWidth) throws IOException { ImageIcon imageIcon = new
     * ImageIcon(originalFile.getCanonicalPath()); Image image = imageIcon.getImage(); Image resizedImage = null; int iWidth = image.getWidth(null); int iHeight
     * = image.getHeight(null); if (iWidth > iHeight) { resizedImage = image.getScaledInstance(newWidth, (newWidth * iHeight) / iWidth, Image.SCALE_SMOOTH); }
     * else { resizedImage = image.getScaledInstance((newWidth * iWidth) / iHeight, newWidth, Image.SCALE_SMOOTH); } // This code ensures that all the pixels in
     * the image are loaded. Image temp = new ImageIcon(resizedImage).getImage(); // Create the buffered image. BufferedImage bufferedImage = new
     * BufferedImage(temp.getWidth(null), temp.getHeight(null), BufferedImage.TYPE_INT_RGB); // Copy image to buffered image. Graphics g =
     * bufferedImage.createGraphics(); // Clear background and paint the image. g.setColor(Color.white); g.fillRect(0, 0, temp.getWidth(null),
     * temp.getHeight(null)); g.drawImage(temp, 0, 0, null); g.dispose(); // Encodes image as a JPEG data stream FileOutputStream out = new
     * FileOutputStream(resizedFile); com.sun.image.codec.jpeg.JPEGImageEncoder encoder = com.sun.image.codec.jpeg.JPEGCodec.createJPEGEncoder(out);
     * com.sun.image.codec.jpeg.JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(bufferedImage); param.setQuality(1.0f, true);
     * encoder.setJPEGEncodeParam(param); encoder.encode(bufferedImage); out.close(); }
     */

    public static void downloadAndSave(String url, String savePath) {
        URL urlLocation;
        try {

            urlLocation = new URL(url);
            BufferedImage bi = ImageIO.read(urlLocation);

            // get file extension
            String fileExtension = FileHelper.getFileType(url);
            if (fileExtension.length() == 0) {
                fileExtension = "png";
            }

            // save image
            Logger.getRootLogger().info("write " + savePath + " with " + fileExtension);
            ImageIO.write(bi, fileExtension, new File(savePath));

        } catch (MalformedURLException e) {
            Logger.getRootLogger().error(url, e);
        } catch (IOException e) {
            Logger.getRootLogger().error(url, e);
        } catch (NullPointerException e) {
            Logger.getRootLogger().error(url, e);
        } catch (IllegalArgumentException e) {
            Logger.getRootLogger().error(url, e);
        }
    }

    private static BufferedImage substractImages(BufferedImage image1, BufferedImage image2) {
        int pixelCount = image1.getWidth() * image1.getHeight();
        int grayCount = 0;

        if (image1.getWidth() != image2.getWidth()) {
            logger.warn("Images do not have the same size.");
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

                int gray = (int) (redNormalized + greenNormalized + blueNormalized);
                Color cg = new Color(gray, gray, gray);
                substractedImage.setRGB(i, j, cg.getRGB());
                grayCount += gray;
            }
        }

        float averageGray = grayCount / (float) pixelCount;

        logger.debug(averageGray);

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

                int gray = (int) (redNormalized + greenNormalized + blueNormalized);
                grayCount += gray;
            }
        }

        float averageGray = grayCount / (float) pixelCount;
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

    public static double getMeanSquareError(BufferedImage image1, BufferedImage image2) {
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

        double meanSquareError = 1 / (double) (image1.getWidth() * image1.getHeight()) * squaredError;
        return meanSquareError;
    }

    public static double getMinkowskiSimilarity(BufferedImage image1, BufferedImage image2) {
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
                squaredError += Math.pow((color1.getRed() - color2.getRed()) / (double) 255, r);
            }
        }

        double meanSquareError = 1.0 / (image1.getWidth() * image1.getHeight()) * squaredError;
        meanSquareError = Math.pow(meanSquareError, 1.0 / r);

        return 1 - meanSquareError;
    }

    public static double getGrayDifference(BufferedImage image1, BufferedImage image2) {
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
                int gray = (int) (0.3 * color.getRed() + 0.59 * color.getGreen() + 0.11 * color.getBlue());
                Color cg = new Color(gray, gray, gray);
                bufferedImage.setRGB(i, j, cg.getRGB());
            }
        }

        return bufferedImage;
    }

    public static boolean isDuplicate(BufferedImage image1, BufferedImage image2) {
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
                    logger.warn("compression is not supported for " + fileType + " files, " + filePath);
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
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
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
    public static boolean saveImage2(BufferedImage image, String fileType, String filePath) {
        try {
            ImageIO.write(image, fileType, new File(filePath));
            return true;
        } catch (IOException e) {
            logger.error("saving of image failed, " + e.getMessage());
        } catch (NullPointerException e) {
            logger.error("saving of image failed, " + e.getMessage());
        }
        return false;
    }

    /**
     * Save an image to disk. This methods wraps the JAI.create method and does error handling.
     * 
     * @param image The image to save.
     * @param fileType The image type (e.g. "jpg")
     * @param filePath The path where the image should be saved.
     * @return True if the image was saved successfully, false otherwise.
     */
    public static boolean saveImage3(BufferedImage bufferedImage, String fileType, String filePath) {

        try {
            JAI.create("encode", bufferedImage, new FileOutputStream(new File(filePath)), fileType.toUpperCase(), null);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return false;
    }

    public static void main(String[] args) {

        // String url = "http://entimg.msn.com/i/gal/ScaryCelebs/JimCarrey_400.jpg";
        // url = "http://www.thehollywoodnews.com/artman2/uploads/1/jim-carrey_1.jpg";
        // URL urlLocation;
        try {

            // urlLocation = new URL(url);
            BufferedImage bufferedImage = ImageIO.read(new File("data/test/images/tdk5.jpg"));
            // bufferedImage = rescaleImage(bufferedImage, 200);
            // saveImage(bufferedImage, "jpg", "data/test/images/1_tdk1.jpg");
            // System.exit(0);

            // measure performance of rescaling algorithms
            long t1 = System.currentTimeMillis();
            for (int i = 0; i < 20; i++) {
                rescaleImage3(bufferedImage, 200);
            }
            DateHelper.getRuntime(t1, System.currentTimeMillis(), true);
            System.exit(0);

            saveImage(bufferedImage, "jpg", "data/test/images/1_tdk1.jpg");

            bufferedImage = rescaleImage(bufferedImage, 200);
            System.out.println(bufferedImage.getWidth());
            saveImage(bufferedImage, "jpg", "data/test/images/1_tdk1_rescaled.jpg");

            System.exit(0);

            bufferedImage.getScaledInstance(200, -1, Image.SCALE_SMOOTH);

            bufferedImage = rescaleImage2(ImageIO.read(new File("data/test/images/tdk5.jpg")), 200);
            saveImage(bufferedImage, "jpg", "data/test/images/1_tdk1_rescaled2.jpg");

            bufferedImage = rescaleImage3(ImageIO.read(new File("data/test/images/tdk5.jpg")), 200);
            saveImage(bufferedImage, "jpg", "data/test/images/1_tdk1_rescaled3.jpg");

            System.exit(0);

            // RenderedImage ri = JAI.create("fileload", "data/test/images/tdk1.jpg");
            // RenderedOp op = JAI.create("filestore", ri, "data/multimedia/tdk1_rescaled.jpg", "JPEG");

            // BufferedImage image1 = Sanselan.getBufferedImage(new File("data/test/images/tdk1.jpg"));

            // Sanselan.writeImage(image1, new File("data/multimedia/tdk1_rescaled.jpg"), ImageFormat.IMAGE_FORMAT_JPEG, new HashMap());
            // jigl.image.io.ImageInputStreamJAI is = new jigl.image.io.ImageInputStreamJAI("data/multimedia/images/test.jpg");
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

        } catch (MalformedURLException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (Exception e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }
}