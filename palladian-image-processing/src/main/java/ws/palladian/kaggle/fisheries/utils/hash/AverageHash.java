package ws.palladian.kaggle.fisheries.utils.hash;

import org.imgscalr.Scalr;
import ws.palladian.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.imgscalr.Scalr.Mode.FIT_EXACT;
import static org.imgscalr.Scalr.OP_GRAYSCALE;

/**
 * @author Philipp Katz
 * @see <a href=
 * "http://www.hackerfactor.com/blog/index.php?/archives/432-Looks-Like-It.html">Looks
 * Like It</a>
 * @see <a href=
 * "https://www.safaribooksonline.com/blog/2013/11/26/image-hashing-with-python/">Simple
 * Image Hashing With Python</a>
 */
public class AverageHash implements ImageHash {
    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;

    @Override
    public String hash(BufferedImage image) {
        BufferedImage processedImage = Scalr.resize(image, FIT_EXACT, WIDTH, HEIGHT, OP_GRAYSCALE);

        int[] rgb = ImageUtils.getRGB(processedImage);
        // image is greyscale, use value of the blue channel
        double meanValue = Arrays.stream(rgb).map(v -> v & 0xFF).average().getAsDouble();
		
		/* BufferedImage bitImage = new BufferedImage(WIDTH, HEIGHT, image.getType());
		for (int xIdx = 0; xIdx < WIDTH; xIdx++) {
			for (int yIdx = 0; yIdx < HEIGHT; yIdx++) {
				int value = processedImage.getRGB(xIdx, yIdx) & 0xFF;
				Color result = value > meanValue ? Color.WHITE : Color.BLACK;
				bitImage.setRGB(xIdx, yIdx, result.getRGB());
			}
		} */

        String bitString = Arrays.stream(ImageUtils.getRGB(processedImage)).mapToObj(v -> (v & 0xFF) > meanValue ? "1" : "0").collect(Collectors.joining());

        return HashUtil.toHex(bitString, WIDTH * HEIGHT / 4);
    }
}
