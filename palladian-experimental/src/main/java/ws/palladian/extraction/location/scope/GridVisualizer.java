package ws.palladian.extraction.location.scope;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeModel;
import ws.palladian.extraction.multimedia.HeatGridGenerator;
import ws.palladian.extraction.multimedia.HeatGridGenerator.ColorCoder;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.NumericMatrix;

public final class GridVisualizer {

    // private static ColorCoder colorCoder = new HeatGridGenerator.PaletteColorCoder();
    
    private static ColorCoder colorCoder = new HeatGridGenerator.TransparencyColorCoder();

    private GridVisualizer() {
        // utility class
    }

    /**
     * Visualize the spatial classification of a given text.
     * 
     * @param model The model to use, not <code>null</code>.
     * @param text The text to classify, not <code>null</code>.
     * @param outputFile The path to the file where to write the visualized grid.
     * @param normalization The scaling factor (probabilities are normalized to maximum, then we take p^normalization).
     */
    public static void createVisualization(DictionaryModel model, String text, File outputFile, double normalization) {
        Validate.notNull(model, "model must not be null");
        Validate.notNull(text, "text must not be null");
        Validate.notNull(outputFile, "outputFile must not be null");
        PalladianTextClassifier classifier = new PalladianTextClassifier(model.getFeatureSetting(),
                DictionaryScopeDetector.DEFAULT_SCORER);
        CategoryEntries result = classifier.classify(text, model);
        NumericMatrix<String> matrix = convertToMatrix(result, normalization);
        HeatGridGenerator heatGridGenerator = new HeatGridGenerator(colorCoder, 5);
        heatGridGenerator.generateHeatGrid(matrix, outputFile.getAbsolutePath());
    }

    /**
     * Visualize the prior probabilities in a model.
     * 
     * @param model The model to use, not <code>null</code>.
     * @param outputFile The path to the file where to write the visualized grid.
     * @param normalization The scaling factor (probabilities are normalized to maximum, then we take p^normalization).
     */
    public static void createPriorVisualization(DictionaryModel model, File outputFile, double normalization) {
        Validate.notNull(model, "model must not be null");
        Validate.notNull(outputFile, "outputFile must not be null");
        NumericMatrix<String> matrix = convertToMatrix(model.getDocumentCounts(), normalization);
        HeatGridGenerator heatGridGenerator = new HeatGridGenerator(colorCoder, 5);
        heatGridGenerator.generateHeatGrid(matrix, outputFile.getAbsolutePath());
    }

    private static NumericMatrix<String> convertToMatrix(CategoryEntries categoryEntries, double normalization) {
        CategoryEntries temp = new CategoryEntriesBuilder().add(categoryEntries).create();
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        double maxProbability = temp.getMostLikely().getProbability();
        for (Category category : temp) {
            String[] identifier = GridCreator.split(category.getName());
            maxX = Math.max(maxX, Integer.parseInt(identifier[0]));
            maxY = Math.max(maxY, Integer.parseInt(identifier[1]));
        }
        NumericMatrix<String> matrix = new NumericMatrix<String>();
        for (int x = 0; x <= maxX; x++) {
            for (int y = maxY; y >= 0; y--) {
                matrix.set(String.valueOf(x), String.valueOf(y), 0.);
            }
        }
        for (Category category : temp) {
            String[] identifier = GridCreator.split(category.getName());
            String x = identifier[0];
            String y = identifier[1];
            matrix.set(x, y, FastMath.pow(category.getProbability() / maxProbability, normalization));
        }
        return matrix;
    }

    public static void main(String[] args) throws IOException {
        String text = FileHelper.readFileToString("src/test/resources/location/text2_stripped.txt");
        DictionaryScopeModel model = FileHelper
                .deserialize("/Volumes/iMac HD/temp/wikipediaLocationGridModel_0.1.ser.gz");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/text.txt");
//        createVisualization(model.dictionaryModel, text, new File("classification_0.25.png"));
//        createVisualization(new CoarseDictionaryDecorator(model, 0.5), text, new File("classification_0.5.png"));
//        createVisualization(new CoarseDictionaryDecorator(model, 1.0), text, new File("classification_1.0.png"));
//        createVisualization(new CoarseDictionaryDecorator(model, 2.5), text, new File("classification_2.5.png"));
        
//        createPriorVisualization(model.dictionaryModel, new File("priors.png"));
        
        createPriorVisualization(model.dictionaryModel, new File("priors_0.5.png"),0.5);
        createPriorVisualization(model.dictionaryModel, new File("priors_0.2.png"),0.2);
        createPriorVisualization(model.dictionaryModel, new File("priors_0.1.png"),0.1);
        createPriorVisualization(model.dictionaryModel, new File("priors_0.05.png"),0.05);
        createPriorVisualization(model.dictionaryModel, new File("priors_0.01.png"),0.01);
    }

}
