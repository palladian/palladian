package ws.palladian.extraction.multimedia;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFImageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * The PdfToImageConverter uses Apache PDFbox for conversion.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class PdfToImageConverter {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfToImageConverter.ImageFormat.class);

    public enum ImageFormat {
        JPG, PNG;

        public String asString() {
            return this.toString().toLowerCase();
        }
    }

    /**
     * private constructor.
     */
    private PdfToImageConverter() {
        // static class
    }

    public static File convertPdfToImage(String pdfFilePath, String targetFileName) {

        int resolution;
        try {
            resolution = Toolkit.getDefaultToolkit().getScreenResolution();
        } catch (HeadlessException e) {
            resolution = 96;
        }
        ImageFormat imageFormat = ImageFormat.valueOf(FileHelper.getFileType(targetFileName.toUpperCase()));
        
        return convertPdfToImage(pdfFilePath, targetFileName, imageFormat, resolution, BufferedImage.TYPE_INT_ARGB);
    }

    public static File convertPdfToImage(String pdfFilePath, String targetFileName, ImageFormat imageFormat,
            int resolution, int bitsPerPixel) {

        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFilePath);

            // String color = "rgb";
            // if ("bilevel".equalsIgnoreCase(color)) {
            // bitsPerPixel = BufferedImage.TYPE_BYTE_BINARY;
            // } else if ("indexed".equalsIgnoreCase(color)) {
            // bitsPerPixel = BufferedImage.TYPE_BYTE_INDEXED;
            // } else if ("gray".equalsIgnoreCase(color)) {
            // bitsPerPixel = BufferedImage.TYPE_BYTE_GRAY;
            // } else if ("rgb".equalsIgnoreCase(color)) {
            // bitsPerPixel = BufferedImage.TYPE_INT_RGB;
            // } else if ("rgba".equalsIgnoreCase(color)) {
            // bitsPerPixel = BufferedImage.TYPE_INT_ARGB;
            // } else {
            // //bitsPerPixel = 24;
            // throw new IllegalArgumentException("The number of bits per pixel must be 1, 8 or 24.");
            // }

            String targetFolder = FileHelper.getFolderName(targetFileName);
            PDFImageWriter imageWriter = new PDFImageWriter();
            boolean success = imageWriter.writeImage(document, imageFormat.asString(), "", 1, 1,
 targetFolder,
                    bitsPerPixel, resolution);

            if (!success) {
                LOGGER.error("no writer found for image format '" + imageFormat.asString() + "'");
            } else {

                // rename output image
                FileHelper.renameFile(new File(targetFolder + "1." + imageFormat.asString()), targetFileName);

            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }

        return new File(targetFileName);
    }

    /**
     * Infamous main method.
     * 
     * @param args Command line arguments, should be one and a reference to a file.
     * 
     * @throws Exception If there is an error parsing the document.
     */
    public static void main(String[] args) throws Exception {
        PdfToImageConverter.convertPdfToImage("documentation/book/img/architecture.pdf", "test.png", ImageFormat.PNG,
                300, BufferedImage.TYPE_INT_RGB);
    }

}