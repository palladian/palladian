package ws.palladian.extraction.multimedia;

import static java.lang.Math.PI;
import static java.lang.Math.sqrt;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.WritableRaster;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.functional.Function;

public final class GaussianBlur implements Function<BufferedImage, BufferedImage> {

    private final int width;

    public GaussianBlur(int width) {
        Validate.isTrue(width >= 1, "width must be greater/equal one");
        this.width = width;
    }

    @Override
    public BufferedImage compute(BufferedImage input) {
        return apply(input, width);
    }

    private static Kernel createGaussianKernel(int width, boolean horizontal) {
        Validate.isTrue(width >= 1, "width must be greater/equal one");
        // http://www.blitzbasic.com/Community/posts.php?topic=84166
        float[] matrix = new float[width * 2 + 1];
        float sigma = width / 3f;
        float norm = (float)(1f / (sqrt(2 * PI) * sigma));
        float coeff = 2f * sigma * sigma;
        float total = 0;
        for (int x = -width; x <= width; x++) {
            float g = (float)(norm * Math.exp(-x * x / coeff));
            matrix[x + width] = g;
            total += g;
        }
        for (int x = 0; x < matrix.length; x++) {
            matrix[x] /= total;
        }
        return new Kernel(horizontal ? matrix.length : 1, horizontal ? 1 : matrix.length, matrix);
    }

    static BufferedImage apply(BufferedImage image, int width) {
        BufferedImage resultImage = image;
        resultImage = extend(resultImage, width);
        resultImage = new ConvolveOp(createGaussianKernel(width, true)).filter(resultImage, null);
        resultImage = new ConvolveOp(createGaussianKernel(width, false)).filter(resultImage, null);
        resultImage = resultImage.getSubimage(width, width, image.getWidth(), image.getHeight());
        return resultImage;
    }

    private static BufferedImage extend(BufferedImage oImage, int by) {
        int oWidth = oImage.getWidth();
        int oHeight = oImage.getHeight();
        int nWidth = oWidth + 2 * by;
        int nHeight = oHeight + 2 * by;
        BufferedImage nImage = new BufferedImage(nWidth, nHeight, oImage.getType());
        WritableRaster oRaster = oImage.getRaster();
        WritableRaster nRaster = nImage.getRaster();
        int nBands = oRaster.getNumBands();
        // 1) expand up/down
        int vArrayLen = oWidth * nBands;
        double[] topRow = oRaster.getPixels(0, 0, oWidth, 1, new double[vArrayLen]);
        double[] bottomRow = oRaster.getPixels(0, oHeight - 1, oWidth, 1, new double[vArrayLen]);
        for (int y = 0; y < by; y++) {
            nRaster.setPixels(by, y, oWidth, 1, topRow);
            nRaster.setPixels(by, nHeight - 1 - y, oWidth, 1, bottomRow);
        }
        // 2) expand left/right
        int hArrayLen = oHeight * nBands;
        double[] leftRow = oRaster.getPixels(0, 0, 1, oHeight, new double[hArrayLen]);
        double[] rightRow = oRaster.getPixels(oWidth - 1, 0, 1, oHeight, new double[hArrayLen]);
        for (int x = 0; x < by; x++) {
            nRaster.setPixels(x, by, 1, oHeight, leftRow);
            nRaster.setPixels(nWidth - 1 - x, by, 1, oHeight, rightRow);
        }
        // 3) expand corners
        fillRect(nRaster, 0, 0, by, by, oRaster.getPixel(0, 0, new double[nBands])); // TL
        fillRect(nRaster, nWidth - by, 0, by, by, oRaster.getPixel(oWidth - 1, 0, new double[nBands])); // TR
        fillRect(nRaster, 0, nHeight - by, by, by, oRaster.getPixel(0, oHeight - 1, new double[nBands])); // BL
        fillRect(nRaster, nWidth - by, nHeight - by, by, by,
                oRaster.getPixel(oWidth - 1, oHeight - 1, new double[nBands])); // BR
        // 4) copy original image
        double[] oRect = oRaster.getPixels(0, 0, oWidth, oHeight, new double[oWidth * oHeight * nBands]);
        nRaster.setPixels(by, by, oWidth, oHeight, oRect);

        return nImage;
    }

    private static void fillRect(WritableRaster r, int x, int y, int w, int h, double[] dArray) {
        for (int cx = x; cx < x + w; cx++) {
            for (int cy = y; cy < y + h; cy++) {
                r.setPixel(cx, cy, dArray);
            }
        }
    }

}
