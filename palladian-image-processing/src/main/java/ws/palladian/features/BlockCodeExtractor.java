package ws.palladian.features;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.value.ImmutableTextValue;
import ws.palladian.extraction.multimedia.ColorSpaceConverter;
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
 * Idea: Let the picture talk about its contents using a more limited dictionary than 24 bit pixels. Multiple pictures
 * must use a common vocabulary to talk to each other.
 */
public class BlockCodeExtractor implements FeatureExtractor {

    public enum Colors {
        GREY_SCALE_5, EIGHT, FOURTEEN, TWENTY_EIGHT, FIFTY_ONE, TWENTY_EIGHT_AND_BRIGHTNESS_2, TWENTY_EIGHT_AND_BRIGHTNESS_3, TWENTY_EIGHT_AND_BRIGHTNESS_4, TWENTY_EIGHT_AND_BRIGHTNESS_6, TWENTY_EIGHT_AND_BRIGHTNESS_8, TWENTY_EIGHT_AND_BRIGHTNESS_10
    }

    public enum BlockSize {
        // FIXME should be (1) but for evaluation purposes this was a dirty fix
        ONE_BY_ONE(2), TWO_BY_TWO(2), THREE_BY_THREE(3), FOUR_BY_FOUR(4), FIVE_BY_FIVE(5);

        int length;

        BlockSize(int length) {
            this.length = length;
        }

        public int getLength() {
            return length;
        }
    }

    private List<Color> palette = new ArrayList<>();

    /** Number of colors we want to normalize the image to. */
    private Colors numberOfColors = Colors.GREY_SCALE_5;

    /** Number of pixels to cluster when pixelating the image. */
    private int pixelationSize = 4;

    /** Block size in pixels. This is basically the word size. */
    private BlockSize blockSize = BlockSize.TWO_BY_TWO;

    /** Image sections. Has to be a square number starting with 4. */
    private BlockSize imageSections = BlockSize.TWO_BY_TWO;

    /** Whether to include the number of colors in a certain pixel block in the block code. */
    private boolean numberOfColorsInCode = true;

    public BlockCodeExtractor(Colors numberOfColors, int pixelationSize, BlockSize blockSize, BlockSize imageSections,
            boolean numberOfColorsInCode) {
        this.numberOfColors = numberOfColors;
        this.pixelationSize = pixelationSize;
        this.blockSize = blockSize;
        this.imageSections = imageSections;
        this.numberOfColorsInCode = numberOfColorsInCode;
        init();
    }

    public BlockCodeExtractor() {
        init();
    }

    private void init() {

        palette.add(Color.BLACK);
        palette.add(Color.WHITE);

        switch (numberOfColors) {
            case GREY_SCALE_5:
                palette.add(Color.LIGHT_GRAY);
                palette.add(Color.GRAY);
                palette.add(Color.DARK_GRAY);
                break;
            case EIGHT:
                // chromatic circle colors:
                // https://thesisterisadoraknocking.files.wordpress.com/2014/06/chromatic-circle.png inner part
                palette.add(new Color(7, 139, 91));
                palette.add(new Color(234, 227, 49));
                palette.add(new Color(235, 139, 47));
                palette.add(new Color(221, 38, 44));
                palette.add(new Color(107, 51, 133));
                palette.add(new Color(36, 143, 181));
                break;
            case FOURTEEN:
                // chromatic circle colors:
                // https://thesisterisadoraknocking.files.wordpress.com/2014/06/chromatic-circle.png all outer colors
                palette.add(new Color(7, 139, 91));
                palette.add(new Color(134, 185, 53));
                palette.add(new Color(234, 227, 49));
                palette.add(new Color(245, 194, 46));
                palette.add(new Color(235, 139, 47));
                palette.add(new Color(229, 95, 45));
                palette.add(new Color(221, 38, 44));
                palette.add(new Color(190, 0, 121));
                palette.add(new Color(107, 51, 133));
                palette.add(new Color(71, 71, 145));
                palette.add(new Color(53, 104, 169));
                palette.add(new Color(36, 143, 181));
                break;
            case TWENTY_EIGHT:
            case FIFTY_ONE:
            case TWENTY_EIGHT_AND_BRIGHTNESS_2:
            case TWENTY_EIGHT_AND_BRIGHTNESS_3:
            case TWENTY_EIGHT_AND_BRIGHTNESS_4:
            case TWENTY_EIGHT_AND_BRIGHTNESS_6:
            case TWENTY_EIGHT_AND_BRIGHTNESS_8:
            case TWENTY_EIGHT_AND_BRIGHTNESS_10:
                palette.add(Color.GRAY);
                palette.add(Color.LIGHT_GRAY);
                palette.add(Color.DARK_GRAY);
                // chromatic circle colors + average rgb steps in between:
                // https://thesisterisadoraknocking.files.wordpress.com/2014/06/chromatic-circle.png all outer colors
                palette.add(new Color(7, 139, 91));
                palette.add(new Color(70, 162, 71)); // averaged
                palette.add(new Color(134, 185, 53));
                palette.add(new Color(184, 206, 51)); // averaged
                palette.add(new Color(234, 227, 49));
                palette.add(new Color(240, 210, 47)); // averaged
                palette.add(new Color(245, 194, 46));
                palette.add(new Color(240, 166, 46)); // averaged
                palette.add(new Color(235, 139, 47));
                palette.add(new Color(232, 117, 46)); // averaged
                palette.add(new Color(229, 95, 45));
                palette.add(new Color(225, 67, 45)); // averaged
                palette.add(new Color(221, 38, 44));
                palette.add(new Color(206, 19, 82)); // averaged
                palette.add(new Color(190, 0, 121));
                palette.add(new Color(149, 26, 127)); // averaged
                palette.add(new Color(107, 51, 133));
                palette.add(new Color(94, 61, 139)); // averaged
                palette.add(new Color(71, 71, 145));
                palette.add(new Color(62, 88, 157)); // averaged
                palette.add(new Color(53, 104, 169));
                palette.add(new Color(45, 124, 175));// averaged
                palette.add(new Color(36, 143, 181));
                break;
        }

        if (numberOfColors == Colors.FIFTY_ONE) {
            // use two different brightnesses 30% and 70% of the of the 28 colors (skip the 5 grey values
            int c = 0;
            List<Color> colorsToAdd = new ArrayList<>();
            for (Color color : palette) {
                c++;
                if (c <= 5) {
                    continue;
                }
                double[] doubles = ColorSpaceConverter.rgbToHsb(color);
                doubles[2] = 0.25 * 256;
                int[] ints = ColorSpaceConverter.hsbToRrb(doubles);
                Color shiftedColor1 = new Color(ints[0],ints[1],ints[2]);
                doubles[2] = 0.75 * 256;
                ints = ColorSpaceConverter.hsbToRrb(doubles);
                Color shiftedColor2 = new Color(ints[0],ints[1],ints[2]);
                colorsToAdd.add(shiftedColor1);
                colorsToAdd.add(shiftedColor2);
            }
            palette.addAll(colorsToAdd);
        }

    }

    @Override
    public FeatureVector extract(BufferedImage image) {
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        BufferedImage pixelatedImage = ImageHandler.pixelate(image, pixelationSize, palette);
        instanceBuilder.set("text", new ImmutableTextValue(codeImage(pixelatedImage, pixelationSize)));
        return instanceBuilder.create();
    }

    private String codeImage(BufferedImage image, int pixelationSize) {
        // number of blocks for each dimension, 2 = 2x2 block
        // int blockSize = 2;

        // each image is divided into sections and when creating a block, we need to know which section it belongs to
        int sectionWidth = (int)((double)image.getWidth() / imageSections.getLength());
        int sectionHeight = (int)((double)image.getHeight() / imageSections.getLength());

        // word length, number of pixels (in one dimension, e.g. blocksize 2x2 and pixelation size of 4 makes 2*4=8
        // pixel word length
        int wordLength = blockSize.getLength() * pixelationSize;

        String entireCode = "";

        int i1 = (int)((double)image.getWidth() / wordLength);
        int i2 = (int)((double)image.getHeight() / wordLength);

        switch (blockSize) {
            // FIXME, this is confusing, ONE_BY_ONE means only 2x2 but discard the shape code later
            case ONE_BY_ONE:
            case TWO_BY_TWO:
                for (int j = 0; j < i2; j++) {
                    for (int i = 0; i < i1; i++) {
                        int blockX = wordLength * i;
                        int blockY = wordLength * j;

                        // which section does the block fall into?
                        int sx = (int)((double)blockX / sectionWidth) + 1;
                        int sy = (int)((double)blockY / sectionHeight);
                        int sectionNumber = sy * imageSections.getLength() + sx;

                        Color[] block = new Color[4];
                        block[0] = new Color(image.getRGB(blockX, blockY));
                        block[1] = new Color(image.getRGB(blockX + pixelationSize, blockY));
                        block[2] = new Color(image.getRGB(blockX, blockY + pixelationSize));
                        block[3] = new Color(image.getRGB(blockX + pixelationSize, blockY + pixelationSize));
                        String code = codeBlock(block, sectionNumber);
                        entireCode += code + " ";
                    }
                }
                break;
            case THREE_BY_THREE:
                for (int j = 0; j < i2 - 1; j++) {
                    for (int i = 0; i < i1 - 1; i++) {
                        int blockX = wordLength * i;
                        int blockY = wordLength * j;

                        // which section does the block fall into?
                        int sx = (int)((double)blockX / sectionWidth) + 1;
                        int sy = (int)((double)blockY / sectionHeight);
                        int sectionNumber = sy * imageSections.getLength() + sx;

                        Color[] block = new Color[9];
                        block[0] = new Color(image.getRGB(blockX, blockY));
                        block[1] = new Color(image.getRGB(blockX + pixelationSize, blockY));
                        block[2] = new Color(image.getRGB(blockX + 2 * pixelationSize, blockY));
                        block[3] = new Color(image.getRGB(blockX, blockY + pixelationSize));
                        block[4] = new Color(image.getRGB(blockX + pixelationSize, blockY + pixelationSize));
                        block[5] = new Color(image.getRGB(blockX + 2 * pixelationSize, blockY + pixelationSize));
                        block[6] = new Color(image.getRGB(blockX, blockY + 2 * pixelationSize));
                        block[7] = new Color(image.getRGB(blockX + pixelationSize, blockY + 2 * pixelationSize));
                        block[8] = new Color(image.getRGB(blockX + 2 * pixelationSize, blockY + 2 * pixelationSize));
                        String code = codeBlock(block, sectionNumber);
                        entireCode += code + " ";
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("block size must be 2x2 or 3x3");
        }

        return entireCode.trim();
    }

    /**
     * Create the actual block code word from the colored block.
     * Each word contains of 4 parts:
     * 1. numberOfColors
     * 2. mainColorCode
     * 3. shapeCode
     * 4. sectionCode
     * NOTE: we end each word with ! to prevent the stemmer from taking our "s"
     * An example word would be 3a5p!
     * 
     * @param block
     * @param imageSection The number of the section where we found the
     * @return
     */
    private String codeBlock(Color[] block, int imageSection) {
        int numberOfColors;
        String mainColorCode = "";
        int shapeCode = 0;

        Bag<Color> colorCounter = Bag.create();
        double averageBlockBrightness = 0;
        for (Color aBlock : block) {
            colorCounter.add(aBlock);
            double[] doubles = ColorSpaceConverter.rgbToHsb(aBlock);
            averageBlockBrightness += doubles[2];
        }
        averageBlockBrightness /= block.length;

        Bag<Color> sorted = colorCounter.createSorted(CollectionHelper.Order.DESCENDING);
        LinkedHashMap<Color, Integer> frequencies = new LinkedHashMap<>();
        for (Color s : sorted.uniqueItems()) {
            int pos = 97;
            for (Color color : palette) {
                if (s.equals(color)) {
                    mainColorCode = Character.toString((char)pos);
                    break;
                }
                pos++;
            }
            frequencies.put(s, sorted.count(s));
            break;
        }

        numberOfColors = sorted.uniqueItems().size();

        switch (blockSize) {
            case ONE_BY_ONE:
                shapeCode = 1;
                break;
            case TWO_BY_TWO:
                // shape one of 4 possibilities
                if (numberOfColors == 1) {
                    // all the same
                    shapeCode = 1;
                } else if (block[0].equals(block[2]) && block[1].equals(block[3])) {
                    // ||
                    shapeCode = 2;
                } else if (block[0].equals(block[1]) && block[2].equals(block[3])) {
                    // =
                    shapeCode = 3;
                } else if (block[0].equals(block[3]) && block[1].equals(block[2])) {
                    // X
                    shapeCode = 4;
                }
                break;
            case THREE_BY_THREE:
                // shape one of 4 possibilities
                if (numberOfColors == 1) {
                    // all the same
                    shapeCode = 1;
                } else if ((block[0].equals(block[3]) && block[0].equals(block[6]))
                        || (block[1].equals(block[4]) && block[1].equals(block[7]))
                        || (block[2].equals(block[5]) && block[2].equals(block[8]))) {
                    // |
                    shapeCode = 2;
                } else if ((block[0].equals(block[1]) && block[0].equals(block[2]))
                        || (block[3].equals(block[4]) && block[3].equals(block[5]))
                        || (block[6].equals(block[7]) && block[6].equals(block[8]))) {
                    // -
                    shapeCode = 3;
                } else if ((block[0].equals(block[4]) && block[0].equals(block[8]))
                        || (block[2].equals(block[4]) && block[2].equals(block[6]))) {
                    // X
                    shapeCode = 4;
                }
                break;
            default:
                throw new UnsupportedOperationException("block size must be 2x2 or 3x3");
        }

        String brightness = "";
        int quantificationLevel = 0;

        switch (this.numberOfColors) {
            case TWENTY_EIGHT_AND_BRIGHTNESS_2:
                quantificationLevel = 2;
                break;
            case TWENTY_EIGHT_AND_BRIGHTNESS_3:
                quantificationLevel = 3;
                break;
            case TWENTY_EIGHT_AND_BRIGHTNESS_4:
                quantificationLevel = 4;
                break;
            case TWENTY_EIGHT_AND_BRIGHTNESS_6:
                quantificationLevel = 6;
                break;
            case TWENTY_EIGHT_AND_BRIGHTNESS_8:
                quantificationLevel = 8;
                break;
            case TWENTY_EIGHT_AND_BRIGHTNESS_10:
                quantificationLevel = 10;
                break;
            default:
        }
        if (quantificationLevel > 0) {
            int brightnessNormalized = (int)(quantificationLevel * averageBlockBrightness / 256);
            brightness = String.valueOf(brightnessNormalized);
        }

        // if (numberOfColors == 1) {
        // shapeCode = 1;
        // } else if (block[0].equals(block[2])) {
        // shapeCode = 2;
        // } else if (block[1].equals(block[3])) {
        // shapeCode = 3;
        // } else if (block[0].equals(block[1])) {
        // shapeCode = 4;
        // } else if (block[2].equals(block[3])) {
        // shapeCode = 5;
        // } else if (block[0].equals(block[3])) {
        // shapeCode = 6;
        // } else if (block[1].equals(block[2])) {
        // shapeCode = 7;
        // }

        String blockCode = "";
        if (numberOfColorsInCode) {
            blockCode += numberOfColors;
        }
        if (!brightness.isEmpty()) {
            mainColorCode = brightness;
        }

        blockCode += mainColorCode + shapeCode + Character.toString((char)(96 + imageSection)) + "!";

        return blockCode;
    }
}