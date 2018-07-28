package ws.palladian.kaggle.fisheries.utils.hash;

import static org.imgscalr.Scalr.OP_GRAYSCALE;
import static org.imgscalr.Scalr.Mode.FIT_EXACT;

import java.awt.image.BufferedImage;

import org.imgscalr.Scalr;

/**
 * @see <a href=
 *      "http://www.hackerfactor.com/blog/index.php?/archives/529-Kind-of-Like-That.html">Kind
 *      of Like That</a>
 * @author pk
 *
 */
public class GradientHash implements ImageHash {

	private static final int WIDTH = 8;

	private static final int HEIGHT = 8;

	@Override
	public String hash(BufferedImage image) {

		BufferedImage processedImage = Scalr.resize(image, FIT_EXACT, WIDTH + 1, HEIGHT, OP_GRAYSCALE);

		StringBuilder bitString = new StringBuilder();

		for (int xIdx = 0; xIdx < WIDTH; xIdx++) {
			for (int yIdx = 0; yIdx < HEIGHT; yIdx++) {
				int currentRGB = processedImage.getRGB(xIdx, yIdx) & 0xFF;
				int rightRGB = processedImage.getRGB(xIdx + 1, yIdx) & 0xFF;
				bitString.append(currentRGB < rightRGB ? "1" : "0");
			}
		}

		return HashUtil.toHex(bitString, WIDTH * HEIGHT / 4);

	}

}
