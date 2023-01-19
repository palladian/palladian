package ws.palladian.extraction.multimedia;

import org.apache.commons.math3.util.FastMath;

import java.awt.Color;

/**
 * Convert an image to a different color space.
 */
public class ColorSpaceConverter {

    /**
     * Reference white in XYZ coordinates
     */
    public double[] D50 = {96.4212, 100.0, 82.5188};
    public double[] D55 = {95.6797, 100.0, 92.1481};
    public double[] D65 = {95.0429, 100.0, 108.8900};
    public double[] D75 = {94.9722, 100.0, 122.6394};
    public double[] whitePoint = D65;

    /**
     * Reference white in xyY coordinates
     */
    public double[] chromaD50 = {0.3457, 0.3585, 100.0};
    public double[] chromaD55 = {0.3324, 0.3474, 100.0};
    public double[] chromaD65 = {0.3127, 0.3290, 100.0};
    public double[] chromaD75 = {0.2990, 0.3149, 100.0};
    public double[] chromaWhitePoint = chromaD65;

    /**
     * sRGB to XYZ conversion matrix
     */
    public double[][] M = {{0.4124, 0.3576, 0.1805}, {0.2126, 0.7152, 0.0722}, {0.0193, 0.1192, 0.9505}};

    /**
     * XYZ to sRGB conversion matrix
     */
    public double[][] Mi = {{3.2406, -1.5372, -0.4986}, {-0.9689, 1.8758, 0.0415}, {0.0557, -0.2040, 1.0570}};

    /**
     * Default constructor, uses D65 for the white point
     */
    public ColorSpaceConverter() {
        whitePoint = D65;
        chromaWhitePoint = chromaD65;
    }

    /**
     * Constructor for setting a non-default white point
     *
     * @param white String specifying the white point to use.
     */
    public ColorSpaceConverter(String white) {
        whitePoint = D65;
        chromaWhitePoint = chromaD65;
        if (white.equalsIgnoreCase("d50")) {
            whitePoint = D50;
            chromaWhitePoint = chromaD50;
        } else if (white.equalsIgnoreCase("d55")) {
            whitePoint = D55;
            chromaWhitePoint = chromaD55;
        } else if (white.equalsIgnoreCase("d65")) {
            whitePoint = D65;
            chromaWhitePoint = chromaD65;
        } else if (white.equalsIgnoreCase("d75")) {
            whitePoint = D75;
            chromaWhitePoint = chromaD75;
        }
    }

    /**
     * @param H Hue angle/360 (0..1)
     * @param S Saturation (0..1)
     * @param B Value (0..1)
     * @return RGB values
     */
    public static int[] hsbToRrb(double H, double S, double B) {
        int[] result = new int[3];
        int rgb = Color.HSBtoRGB((float) H, (float) S, (float) B);
        result[0] = (rgb >> 16) & 0xff;
        result[1] = (rgb >> 8) & 0xff;
        result[2] = (rgb) & 0xff;
        return result;
    }

    public static int[] hsbToRrb(double[] HSB) {
        return hsbToRrb(HSB[0], HSB[1], HSB[2]);
    }

    /**
     * Convert LAB to RGB.
     *
     * @param L
     * @param a
     * @param b
     * @return RGB values
     */
    public int[] labToRgb(double L, double a, double b) {
        return xyzToRgb(labToXyz(L, a, b));
    }

    public int[] labToRgb(double[] Lab) {
        return xyzToRgb(labToXyz(Lab));
    }

    /**
     * Convert LAB to XYZ.
     *
     * @param L
     * @param a
     * @param b
     * @return XYZ values
     */
    public double[] labToXyz(double L, double a, double b) {
        double[] result = new double[3];

        double y = (L + 16.0) / 116.0;
        double y3 = FastMath.pow(y, 3.0);
        double x = (a / 500.0) + y;
        double x3 = FastMath.pow(x, 3.0);
        double z = y - (b / 200.0);
        double z3 = FastMath.pow(z, 3.0);

        if (y3 > 0.008856) {
            y = y3;
        } else {
            y = (y - (16.0 / 116.0)) / 7.787;
        }
        if (x3 > 0.008856) {
            x = x3;
        } else {
            x = (x - (16.0 / 116.0)) / 7.787;
        }
        if (z3 > 0.008856) {
            z = z3;
        } else {
            z = (z - (16.0 / 116.0)) / 7.787;
        }

        result[0] = x * whitePoint[0];
        result[1] = y * whitePoint[1];
        result[2] = z * whitePoint[2];

        return result;
    }

    public double[] labToXyz(double[] Lab) {
        return labToXyz(Lab[0], Lab[1], Lab[2]);
    }

    /**
     * @param R Red in range 0..255
     * @param G Green in range 0..255
     * @param B Blue in range 0..255
     * @return HSB values: H is 0..360 degrees / 360 (0..1), S is 0..1, B is 0..1
     */
    public static double[] rgbToHsb(Color color) {
        return rgbToHsb(color.getRed(), color.getGreen(), color.getBlue());
    }

    public static double[] rgbToHsb(int R, int G, int B) {
        double[] result = new double[3];
        float[] hsb = new float[3];
        Color.RGBtoHSB(R, G, B, hsb);
        result[0] = hsb[0];
        result[1] = hsb[1];
        result[2] = hsb[2];
        return result;
    }

    public double[] rgbToHsb(int[] RGB) {
        return rgbToHsb(RGB[0], RGB[1], RGB[2]);
    }

    /**
     * Convert RGB to LAB.
     *
     * @param r
     * @param g
     * @param b
     * @return Lab values
     */
    public double[] rgbToLab(int r, int g, int b) {
        return xyzToLab(rgbToXyz(r, g, b));
    }

    public double[] rgbToLab(int[] rgb) {
        return xyzToLab(rgbToXyz(rgb));
    }

    /**
     * Convert RGB to XYZ.
     *
     * @param red
     * @param green
     * @param blue
     * @return XYZ in double array.
     */
    public double[] rgbToXyz(int red, int green, int blue) {
        double[] result = new double[3];

        // convert 0..255 into 0..1
        double r = red / 255.0;
        double g = green / 255.0;
        double b = blue / 255.0;

        // assume sRGB
        if (r <= 0.04045) {
            r = r / 12.92;
        } else {
            r = FastMath.pow(((r + 0.055) / 1.055), 2.4);
        }
        if (g <= 0.04045) {
            g = g / 12.92;
        } else {
            g = FastMath.pow(((g + 0.055) / 1.055), 2.4);
        }
        if (b <= 0.04045) {
            b = b / 12.92;
        } else {
            b = FastMath.pow(((b + 0.055) / 1.055), 2.4);
        }

        r *= 100.0;
        g *= 100.0;
        b *= 100.0;

        // [X Y Z] = [r g b][M]
        result[0] = (r * M[0][0]) + (g * M[0][1]) + (b * M[0][2]);
        result[1] = (r * M[1][0]) + (g * M[1][1]) + (b * M[1][2]);
        result[2] = (r * M[2][0]) + (g * M[2][1]) + (b * M[2][2]);

        return result;
    }

    /**
     * Convert RGB to XYZ.
     *
     * @param rgb
     * @return XYZ in double array.
     */
    public double[] rgbToXyz(int[] rgb) {
        return rgbToXyz(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Convert xyY to XYZ.
     *
     * @param x
     * @param y
     * @param Y
     * @return XYZ values
     */
    public double[] xyYToXyz(double x, double y, double Y) {
        double[] result = new double[3];
        if (y == 0) {
            result[0] = 0;
            result[1] = 0;
            result[2] = 0;
        } else {
            result[0] = (x * Y) / y;
            result[1] = Y;
            result[2] = ((1 - x - y) * Y) / y;
        }
        return result;
    }

    public double[] xyYToXyz(double[] xyY) {
        return xyYToXyz(xyY[0], xyY[1], xyY[2]);
    }

    /**
     * Convert XYZ to LAB.
     *
     * @param X
     * @param Y
     * @param Z
     * @return Lab values
     */
    public double[] xyzToLab(double X, double Y, double Z) {

        double x = X / whitePoint[0];
        double y = Y / whitePoint[1];
        double z = Z / whitePoint[2];

        if (x > 0.008856) {
            x = FastMath.pow(x, 1.0 / 3.0);
        } else {
            x = (7.787 * x) + (16.0 / 116.0);
        }
        if (y > 0.008856) {
            y = FastMath.pow(y, 1.0 / 3.0);
        } else {
            y = (7.787 * y) + (16.0 / 116.0);
        }
        if (z > 0.008856) {
            z = FastMath.pow(z, 1.0 / 3.0);
        } else {
            z = (7.787 * z) + (16.0 / 116.0);
        }

        double[] result = new double[3];

        result[0] = (116.0 * y) - 16.0;
        result[1] = 500.0 * (x - y);
        result[2] = 200.0 * (y - z);

        return result;
    }

    public double[] xyzToLab(double[] xyz) {
        return xyzToLab(xyz[0], xyz[1], xyz[2]);
    }

    /**
     * Convert XYZ to RGB.
     *
     * @param X
     * @param Y
     * @param Z
     * @return RGB in int array.
     */
    public int[] xyzToRgb(double X, double Y, double Z) {
        int[] result = new int[3];

        double x = X / 100.0;
        double y = Y / 100.0;
        double z = Z / 100.0;

        // [r g b] = [X Y Z][Mi]
        double r = (x * Mi[0][0]) + (y * Mi[0][1]) + (z * Mi[0][2]);
        double g = (x * Mi[1][0]) + (y * Mi[1][1]) + (z * Mi[1][2]);
        double b = (x * Mi[2][0]) + (y * Mi[2][1]) + (z * Mi[2][2]);

        // assume sRGB
        if (r > 0.0031308) {
            r = ((1.055 * FastMath.pow(r, 1.0 / 2.4)) - 0.055);
        } else {
            r = (r * 12.92);
        }
        if (g > 0.0031308) {
            g = ((1.055 * FastMath.pow(g, 1.0 / 2.4)) - 0.055);
        } else {
            g = (g * 12.92);
        }
        if (b > 0.0031308) {
            b = ((1.055 * FastMath.pow(b, 1.0 / 2.4)) - 0.055);
        } else {
            b = (b * 12.92);
        }

        r = (r < 0) ? 0 : r;
        g = (g < 0) ? 0 : g;
        b = (b < 0) ? 0 : b;

        // convert 0..1 into 0..255
        result[0] = (int) Math.round(r * 255);
        result[1] = (int) Math.round(g * 255);
        result[2] = (int) Math.round(b * 255);

        return result;
    }

    /**
     * Convert XYZ to RGB,
     *
     * @param xyz in a double array.
     * @return RGB in int array.
     */
    public int[] xyzToRgb(double[] xyz) {
        return xyzToRgb(xyz[0], xyz[1], xyz[2]);
    }

    public double[] xyzToxyY(double X, double Y, double Z) {
        double[] result = new double[3];
        if ((X + Y + Z) == 0) {
            result[0] = chromaWhitePoint[0];
            result[1] = chromaWhitePoint[1];
            result[2] = chromaWhitePoint[2];
        } else {
            result[0] = X / (X + Y + Z);
            result[1] = Y / (X + Y + Z);
            result[2] = Y;
        }
        return result;
    }

    public double[] xyzToxyY(double[] XYZ) {
        return xyzToxyY(XYZ[0], XYZ[1], XYZ[2]);
    }

}
