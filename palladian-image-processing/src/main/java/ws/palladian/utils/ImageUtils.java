package ws.palladian.utils;

import ws.palladian.features.color.ColorExtractor;
import ws.palladian.features.color.HSB;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
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

	public static BufferedImage flipVertically(BufferedImage image) {
		return flip(image, true);
	}

	public static BufferedImage flipHorizontally(BufferedImage image) {
		return flip(image, false);
	}

	private static BufferedImage flip(BufferedImage image, boolean vertically) {
		Objects.requireNonNull(image);
		AffineTransform tx;
		if (vertically) {
			tx = AffineTransform.getScaleInstance(1, -1);
			tx.translate(0, -image.getHeight(null));
		} else {
			tx = AffineTransform.getScaleInstance(-1, 1);
			tx.translate(-image.getWidth(null), 0);
		}
		return apply(image, tx);
	}

	private static BufferedImage apply(BufferedImage image, AffineTransform tx) {
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		return op.filter(image, null);
	}

	public static BufferedImage rotate180(BufferedImage image) {
		Objects.requireNonNull(image);
		return apply(image, AffineTransform.getRotateInstance(Math.PI, image.getWidth() / 2.0, image.getHeight() / 2.0));
	}

	/**
	 * Get RGB pixel array from the given image.
	 * 
	 * @param image
	 *            The image.
	 * @return An array with RGB pixel values.
	 */
	public static int[] getRGB(BufferedImage image) {
		Objects.requireNonNull(image, "image must not be null");
		return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
	}

	public static BufferedImage getGreyscale(ColorExtractor extractor, BufferedImage image) {
		Objects.requireNonNull(extractor);
		Objects.requireNonNull(image);
		WritableRaster raster = image.copyData(null);
		BufferedImage result = new BufferedImage(image.getColorModel(), raster, image.getColorModel().isAlphaPremultiplied(), null);
		int[] rgbImage = getRGB(image);
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
		ImageIO.write(flipHorizontally(image), "jpg", new File("/Users/pk/Desktop/horizontal-flip.jpg"));
		ImageIO.write(flipVertically(image), "jpg", new File("/Users/pk/Desktop/vertical-flip.jpg"));
		ImageIO.write(rotate180(image), "jpg", new File("/Users/pk/Desktop/rotated.jpg"));
		for (ColorExtractor extractor : HSB.values()) {
			BufferedImage hImage = ImageUtils.getGreyscale(extractor, image);
			ImageIO.write(hImage, "jpg", new File("/Users/pk/Desktop/"+extractor+".jpg"));
		}
	}

}
