package ws.palladian.features;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Codify a picture into words. These words can then be used for a normal text classifier.
 * Idea: Let the picture talk about it's contents using a more limited dictionary than 24 bit pixels. Multiple pictures must use a common vocabulary to talk to each other.
 */
public enum BlockCodeExtractor implements FeatureExtractor {
    BLOCK_CODE;

    private List<Color> palette = new ArrayList<>();

    BlockCodeExtractor() {
        palette.add(Color.BLACK);
        palette.add(Color.WHITE);
//        palette.add(Color.GRAY);
        // chromatic circle colors
        palette.add(new Color(7, 139, 91));
//        palette.add(new Color(134, 185, 53));
        palette.add(new Color(234, 227, 49));
//        palette.add(new Color(245, 194, 46));
        palette.add(new Color(235, 139, 47));
//        palette.add(new Color(229, 95, 45));
        palette.add(new Color(221, 38, 44));
//        palette.add(new Color(190, 0, 121));
        palette.add(new Color(107, 51, 133));
//        palette.add(new Color(71, 71, 145));
//        palette.add(new Color(53, 104, 169));
        palette.add(new Color(36, 143, 181));
    }

    @Override
    public FeatureVector extract(BufferedImage image) {
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        int pixelSize = 4;
        BufferedImage pixelatedImage = ImageHandler.pixelate(image, pixelSize, palette);
        instanceBuilder.set("block_code", codeImage(pixelatedImage, pixelSize));
        return instanceBuilder.create();
    }

    private String codeImage(BufferedImage image, int pixelationSize) {
        // number of blocks for each dimension, 2 = 2x2 block
        int blockSize = 2;

        // word size
        int wordSize = blockSize * pixelationSize;

        String entireCode = "";

        int i1 = (int) ((double) image.getWidth() / wordSize);
        int i2 = (int) ((double) image.getHeight() / wordSize);
        for (int j = 0; j < i2; j++) {
            for (int i = 0; i < i1; i++) {
                int blockX = wordSize * i;
                int blockY = wordSize * j;
                Color[] block = new Color[4];
                block[0] = new Color(image.getRGB(blockX, blockY));
                block[1] = new Color(image.getRGB(blockX + pixelationSize, blockY));
                block[2] = new Color(image.getRGB(blockX, blockY + pixelationSize));
                block[3] = new Color(image.getRGB(blockX + pixelationSize, blockY + pixelationSize));
                String code = codeBlock(block);
                entireCode += code + " ";
            }
        }

        return entireCode.trim();
    }

    private String codeBlock(Color[] block) {
        int numberOfColors;
        String mainColorCode = "";
        int shapeCode = 0;

        Bag<Color> colorCounter = Bag.create();
        for (int x = 0; x < block.length; x++) {
            colorCounter.add(block[x]);
        }
        Bag<Color> sorted = colorCounter.createSorted(CollectionHelper.Order.DESCENDING);
        LinkedHashMap<Color, Integer> frequencies = new LinkedHashMap<>();
        Color mainColor = null;
        for (Color s : sorted.uniqueItems()) {
            if (mainColor == null) {
                mainColor = s;
            }
            int pos = 97;
            for (Color color : palette) {
                if (s.equals(color)) {
                    mainColorCode += Character.toString((char) pos);
                    break;
                }
                pos++;
            }
            frequencies.put(s, sorted.count(s));
            break;
        }

        numberOfColors = sorted.uniqueItems().size();

        // shape one of 4 possibilities
        if (numberOfColors == 1) {
            shapeCode = 1;
        } else if (block[0].equals(block[2]) && block[1].equals(block[3])) {
            shapeCode = 2;
        } else if (block[0].equals(block[1]) && block[2].equals(block[3])) {
            shapeCode = 3;
        } else if (block[0].equals(block[3]) && block[1].equals(block[2])) {
            shapeCode = 4;
        }
//        if (numberOfColors == 1) {
//            shapeCode = 1;
//        } else if (block[0].equals(block[2])) {
//            shapeCode = 2;
//        } else if (block[1].equals(block[3])) {
//            shapeCode = 3;
//        } else if (block[0].equals(block[1])) {
//            shapeCode = 4;
//        } else if (block[2].equals(block[3])) {
//            shapeCode = 5;
//        } else if (block[0].equals(block[3])) {
//            shapeCode = 6;
//        } else if (block[1].equals(block[2])) {
//            shapeCode = 7;
//        }

        return numberOfColors + mainColorCode + shapeCode;
    }
}
