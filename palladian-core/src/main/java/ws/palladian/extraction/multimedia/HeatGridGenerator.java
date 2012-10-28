package ws.palladian.extraction.multimedia;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.math.NumericMatrix;

public class HeatGridGenerator {
    
    /** Use only one color with different intensities. */
    public static int TRANSPARENCY = 1;
    
    /** Use a color palette for coloring the grid. */
    public static int COLOR = 2;
    
    /** The mode to use. */
    private int mode = TRANSPARENCY;
    
    /** The intensity color to use in TRANSPARENCY mode. */
    private Color color = new Color(0,0,0);
    
    /** The color palette to use in color mode. */
    private Map<Integer, Color> colorPalette;
    
    public HeatGridGenerator() {
        
        initStandardPalette();
        
    }
    
    /**
     * <p>Colors from http://cdn3.sbnation.com/fan_shot_images/26552/090104_knicks.png</p>
     */
    private void initStandardPalette() {
        colorPalette = new HashMap<Integer, Color>();
        
        int colorAlpha = 255;
        
        // 16 colors and 255 intensity values => ~16 steps per color
        colorPalette.put(0, new Color(0, 4, 114, colorAlpha));
        colorPalette.put(1, new Color(46, 49, 146, colorAlpha));
        colorPalette.put(2, new Color(69, 85, 163, colorAlpha));
        colorPalette.put(3, new Color(90, 118, 181, colorAlpha));
        colorPalette.put(4, new Color(115, 154, 200, colorAlpha));
        colorPalette.put(5, new Color(138, 170, 211, colorAlpha));
        colorPalette.put(6, new Color(163, 187, 225, colorAlpha));
        colorPalette.put(7, new Color(188, 204, 237, colorAlpha));
        colorPalette.put(8, new Color(97, 220, 104, colorAlpha));
        colorPalette.put(9, new Color(170, 234, 0, colorAlpha));
        colorPalette.put(10, new Color(255, 222, 0, colorAlpha));
        colorPalette.put(11, new Color(255, 192, 0, colorAlpha));
        colorPalette.put(12, new Color(248, 170, 0, colorAlpha));
        colorPalette.put(13, new Color(246, 124, 000, colorAlpha));
        colorPalette.put(14, new Color(226, 91, 36, colorAlpha));
        colorPalette.put(15, new Color(212, 0, 0, colorAlpha));
        
    }
    
    private Color getColor(int intensity) {
        Color color = getColor();
        
        if (mode == COLOR) {
            int bucket = intensity / 16;
            color = colorPalette.get(bucket);
        } else {
            color = new Color(color.getRed(),color.getGreen(),color.getBlue(), intensity);
        }

        return color;
    }
    
    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
    
    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * <p>Generate heat grid from the given data matrix M(n,m). The grid will contain n*m squares with intensity values in the range [0,1] depending on the value at M(n,m) = intensity.</p>
     * @param data The data matrix with intensity values in the range [0,1].
     * @param imagePath The path where the image should be saved to.
     */
    public void generateHeatGrid(NumericMatrix data, String imagePath) {
        
        //final int IMAGE_WIDTH = 400;
        //final int IMAGE_HEIGHT = 400;
        
        Map<String, Map<String, Number>> matrix = data.getMatrix();
        //final int tileWidth = IMAGE_WIDTH / matrix.size();
        //final int tileHeight = IMAGE_HEIGHT / matrix.entrySet().size();
        
        final int tileWidth = 30;
        final int tileHeight = 30;
        final int IMAGE_WIDTH = matrix.size() * tileWidth;
        final int IMAGE_HEIGHT = matrix.entrySet().iterator().next().getValue().size() * tileHeight;
        
        BufferedImage bufferedImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = bufferedImage.createGraphics();
        g2.setPaint(Color.WHITE);
        g2.fill(new Rectangle(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT));
        g2.setPaint(Color.RED);
        
        int columnNumber = 0;
        for (Entry<String, Map<String, Number>> column : matrix.entrySet()) {
            //System.out.println("column " + columnNumber + " : " + column.getKey());
            int rowNumber = 0;
            for (Entry<String, Number> row : column.getValue().entrySet()) {
            	//System.out.println("row " + rowNumber + " : " + row.getKey());
                double intensity = (Double) row.getValue();
                int intensityScaled = (int) (intensity * 255); 
                g2.setColor(getColor(intensityScaled));              
                
                g2.fill(new Rectangle(columnNumber * tileWidth, rowNumber * tileHeight, tileWidth, tileHeight));
                
                rowNumber++;
            }
            columnNumber++;            
        }
        
        ImageHandler.saveImage(bufferedImage, "png", imagePath);        
    }
    
    
    public static void main(String[] args) {
        
        NumericMatrix data = new NumericMatrix();
        data.set("0", "0", 0.1);
        data.set("1", "0", 0.3);
        data.set("0", "1", 0.5);
        data.set("1", "1", 0.7);
        System.out.println(data);
        System.out.println(data.get("0", "0"));
        
        // generate random heat grid data
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                data.set(i+"", j+"", Math.random());
            }
        }
        
        HeatGridGenerator heatGridGenerator = new HeatGridGenerator();
//        heatGridGenerator.setMode(TRANSPARENCY);
//        heatGridGenerator.setColor(new Color(10,40,200));
        heatGridGenerator.setMode(COLOR);
        heatGridGenerator.generateHeatGrid(data, "heatgrid.png");
        
    }
    
}