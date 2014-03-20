package ws.palladian.extraction.multimedia;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.NumericMatrix;
import ws.palladian.helper.math.NumericMatrix.NumericMatrixVector;

/**
 * The {@link HeatGridGenerator} visualizes numeric matrices with intensity values [0,1].
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class HeatGridGenerator {

    /**
     * A ColorCoder maps a given intensity to a Color.
     * 
     * @author pk
     */
    public static interface ColorCoder {
        /**
         * Transform the given intensity to a Color.
         * 
         * @param intensity The intensity in range [0,1].
         * @return The Color for the given intensity.
         */
        Color getColor(double intensity);
    }

    /** Use a color palette for coloring the grid. */
    public static final class PaletteColorCoder implements ColorCoder {

        private final List<Color> palette;

        public PaletteColorCoder(List<Color> palette) {
            Validate.notNull(palette, "palette must not be null");
            this.palette = CollectionHelper.newArrayList(palette);
        }

        public PaletteColorCoder() {
            // colors from: http://cdn3.sbnation.com/fan_shot_images/26552/090104_knicks.png
            this.palette = CollectionHelper.newArrayList();
            final int colorAlpha = 255;
            this.palette.add(new Color(0, 4, 114, colorAlpha));
            this.palette.add(new Color(46, 49, 146, colorAlpha));
            this.palette.add(new Color(69, 85, 163, colorAlpha));
            this.palette.add(new Color(90, 118, 181, colorAlpha));
            this.palette.add(new Color(115, 154, 200, colorAlpha));
            this.palette.add(new Color(138, 170, 211, colorAlpha));
            this.palette.add(new Color(163, 187, 225, colorAlpha));
            this.palette.add(new Color(188, 204, 237, colorAlpha));
            this.palette.add(new Color(97, 220, 104, colorAlpha));
            this.palette.add(new Color(170, 234, 0, colorAlpha));
            this.palette.add(new Color(255, 222, 0, colorAlpha));
            this.palette.add(new Color(255, 192, 0, colorAlpha));
            this.palette.add(new Color(248, 170, 0, colorAlpha));
            this.palette.add(new Color(246, 124, 000, colorAlpha));
            this.palette.add(new Color(226, 91, 36, colorAlpha));
            this.palette.add(new Color(212, 0, 0, colorAlpha));
        }

        @Override
        public Color getColor(double intensity) {
            int bucket = (int)Math.round(intensity * (palette.size() - 1));
            return palette.get(bucket);
        }

    }

    /** Use only one color with different intensities. */
    public static final class TransparencyColorCoder implements ColorCoder {

        private final Color baseColor;

        public TransparencyColorCoder(Color baseColor) {
            this.baseColor = baseColor;
        }

        public TransparencyColorCoder() {
            this(new Color(0, 0, 0));
        }

        @Override
        public Color getColor(double intensity) {
            int alpha = (int)Math.round(intensity * 255);
            return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
        }

    }

    /** The mode to use. */
    private final ColorCoder colorCoder;

    private final int tileSize;

    public HeatGridGenerator(ColorCoder colorCoder, int tileSize) {
        Validate.notNull(colorCoder, "colorCoder must not be null");
        Validate.isTrue(tileSize > 0, "tileSize must be greater zero");
        this.colorCoder = colorCoder;
        this.tileSize = tileSize;
    }

    /**
     * <p>
     * Generate a heat grid from the given data matrix <code>M(n,m)</code>. The grid will contain <code>n*m</code>
     * squares with intensity values in the range [0,1] depending on the value at M(n,m) = intensity.
     * </p>
     * 
     * @param data The data matrix with intensity values in the range [0,1], not <code>null</code>.
     * @param imagePath The path where the image should be saved to, not <code>null</code> or empty.
     */
    public void generateHeatGrid(NumericMatrix<String> data, String imagePath) {
        Validate.notNull(data, "data must not be null");
        Validate.notEmpty(imagePath, "imagePath must not be empty");

        int imageWidth = data.columnCount() * tileSize;
        int imageHeight = data.rowCount() * tileSize;

        BufferedImage bufferedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = bufferedImage.createGraphics();
        g2.setPaint(Color.WHITE);
        g2.fill(new Rectangle(0, 0, imageWidth, imageHeight));
        g2.setPaint(Color.RED);

        int columnNumber = 0;
        Set<String> columnKeys = data.getColumnKeys();
        for (NumericMatrixVector<String> row : data.rows()) {
            int rowNumber = 0;
            for (String columnKey : columnKeys) {
                double value = row.get(columnKey);
                if (value < 0 || value > 1) {
                    throw new IllegalArgumentException("The values must be in range [0,1].");
                }
                Color color = colorCoder.getColor(value);
                g2.setColor(color);
                g2.fill(new Rectangle(columnNumber * tileSize, rowNumber * tileSize, tileSize, tileSize));
                rowNumber++;
            }
            columnNumber++;
        }

        ImageHandler.saveImage(bufferedImage, "png", imagePath);
    }

    public static void main(String[] args) {

        NumericMatrix<String> data = new NumericMatrix<String>();
        // data.set("0", "0", 0.1);
        // data.set("1", "0", 0.3);
        // data.set("0", "1", 0.5);
        // data.set("1", "1", 0.7);
        // System.out.println(data);
        // System.out.println(data.get("0", "0"));

        // generate random heat grid data
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                data.set(i + "", j + "", Math.random());
            }
        }

        HeatGridGenerator heatGridGenerator = new HeatGridGenerator(new PaletteColorCoder(), 30);
        heatGridGenerator.generateHeatGrid(data, "heatgrid__.png");

    }

}