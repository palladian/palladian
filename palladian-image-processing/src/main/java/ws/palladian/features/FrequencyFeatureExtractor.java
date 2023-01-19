package ws.palladian.features;

//import edu.emory.mathcs.jtransforms.dct.DoubleDCT_2D;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.helper.math.FatStats;
import ws.palladian.helper.math.SlimStats;

import java.awt.image.BufferedImage;

/**
 * Simple frequency features, high frequency means lots of things going on in the image while low frequencies are bigger surfaces and smooth gradients.
 * Idea behind this feature: Detecting how much "action" is going on in a picture.
 */
public enum FrequencyFeatureExtractor implements FeatureExtractor {
    FREQUENCY;

    @Override
    public FeatureVector extract(BufferedImage image) {
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        FatStats fatStats = ImageHandler.detectFrequencies(image);
        instanceBuilder.set("frequency-mean", fatStats.getMean());
        instanceBuilder.set("frequency-median", fatStats.getMedian());

        fatStats = ImageHandler.detectFrequencies(toDct(image));
        instanceBuilder.set("frequency-dct-mean", fatStats.getMean());
        return instanceBuilder.create();
    }

    private BufferedImage toDct(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        BufferedImage img = new BufferedImage(w, h, BufferedImage.OPAQUE);
        img.getGraphics().drawImage(image, 0, 0, null);

        int[] rgb1 = new int[w * h];
        img.getRaster().getDataElements(0, 0, w, h, rgb1);
        double[] array = new double[w * h];

        for (int i = 0; i < w * h; i++) {
            array[i] = (double) (rgb1[i] & 0xFF);
        }

        // TODO requires spark dependency?
        //        DoubleDCT_2D tr = new DoubleDCT_2D(w, h);
        //        tr.forward(array, true);

        SlimStats stat = new SlimStats();
        for (int i = 0; i < w * h; i++) {
            // Grey levels
            int val = Math.min((int) (array[i] + 128), 255);
            rgb1[i] = (val << 16) | (val << 8) | val;
            //            Color color = new Color(rgb1[i]);
            //stat.add(csc.rgbToHsb(color)[2]);
            stat.add(val);
        }

        img.getRaster().setDataElements(0, 0, w, h, rgb1);

        return img;
    }

    public static void main(String[] args) {
        FeatureVector extract = FREQUENCY.extract(ImageHandler.load("gradient.png"));
        System.out.println(extract);
    }
}
