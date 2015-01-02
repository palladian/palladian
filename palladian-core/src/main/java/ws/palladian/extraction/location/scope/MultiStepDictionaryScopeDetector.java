package ws.palladian.extraction.location.scope;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeModel;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.wiki.WikiPage;

/**
 * <p>
 * A multi-level text-classification-based {@link ScopeDetector}. Conceptually, it uses several coarse grids first to do
 * a broad classification of the area, and finally the fine grid given by the supplied model for the exact place
 * determination. This class does basically the same what the {@link TwoLevelTextClassifierScopeDetector} does, but
 * simulates the coarse grid during runtime, so that only the finer model needs to be built. This saves time, and, more
 * important, memory.
 * <p>
 * Use {@link DictionaryScopeDetector#train(Iterable, ws.palladian.classification.text.FeatureSetting, double)} to
 * create a model.
 * 
 * @author pk
 */
public class MultiStepDictionaryScopeDetector implements ScopeDetector {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiStepDictionaryScopeDetector.class);

    private final Map<String, GeoCoordinate> cellToCoordinate;

    private final double[] gridSizes;

    private final GridCreator[] gridCreators;

    private final DictionaryModel[] dictionaryModels;

    private final FeatureSetting featureSetting;

    private final Scorer scorer;

    /**
     * Create a {@link MultiStepDictionaryScopeDetector} using the
     * {@link DictionaryScopeDetector#DEFAULT_SCORER}.
     * 
     * @param model The model to use, not <code>null</code>.
     * @param coarserGridSizes The simulated coarser grid sizes; must be sorted in descending order, starting with the
     *            biggest grid size. Each grid size must be at least twice as big as its successor, and the last given
     *            grid size must be at least twice as big as the grid size supplied by the used model.
     * @throws IllegalArgumentException In case any of the given conditions is not fulfilled.
     */
    public MultiStepDictionaryScopeDetector(DictionaryScopeModel model, double... coarserGridSizes) {
        this(model, DictionaryScopeDetector.DEFAULT_SCORER, coarserGridSizes);
    }

    /**
     * Create a {@link MultiStepDictionaryScopeDetector}.
     * 
     * @param model The model to use, not <code>null</code>.
     * @param scorer The scorer to use for text classification, not <code>null</code>.
     * @param coarserGridSizes The simulated coarser grid sizes; must be sorted in descending order, starting with the
     *            biggest grid size. Each grid size must be at least twice as big as its successor, and the last given
     *            grid size must be at least twice as big as the grid size supplied by the used model.
     * @throws IllegalArgumentException In case any of the given conditions is not fulfilled.
     */
    public MultiStepDictionaryScopeDetector(DictionaryScopeModel model, Scorer scorer,
            double... coarserGridSizes) {
        validateParameters(model, scorer, coarserGridSizes);
        int numSteps = coarserGridSizes.length + 1;
        this.dictionaryModels = new DictionaryModel[numSteps];
        this.gridCreators = new GridCreator[numSteps];
        this.gridSizes = new double[numSteps];
        this.featureSetting = model.dictionaryModel.getFeatureSetting();
        this.cellToCoordinate = model.cellToCoordinate;
        this.scorer = scorer;

        GridCreator fineCreator = new GridCreator(model.gridSize);
        for (int i = 0; i < coarserGridSizes.length; i++) {
            GridCreator gridCreator = new GridCreator(coarserGridSizes[i]);
            this.gridCreators[i] = gridCreator;
            this.dictionaryModels[i] = new CoarseDictionaryDecorator(model.dictionaryModel, gridCreator, fineCreator);
            this.gridSizes[i] = coarserGridSizes[i];
        }
        this.gridCreators[numSteps - 1] = fineCreator;
        this.dictionaryModels[numSteps - 1] = model.dictionaryModel;
        this.gridSizes[numSteps - 1] = model.gridSize;
    }

    private static void validateParameters(DictionaryScopeModel model, Scorer scorer, double... coarserGridSizes) {
        Validate.notNull(model, "model must not be null");
        Validate.notNull(scorer, "scorer must not be null");
        Validate.notNull(coarserGridSizes, "coarserGridSizes must not be null");
        for (int i = 1; i < coarserGridSizes.length; i++) {
            Validate.isTrue(coarserGridSizes[i - 1] >= 2 * coarserGridSizes[i],
                    "coarser grid size must be given in descending order, each grid size must be twice as big as its successor");
        }
        if (coarserGridSizes.length > 0) {
            Validate.isTrue(model.gridSize * 2 <= coarserGridSizes[coarserGridSizes.length - 1],
                    "size of smallest coarse grid should at least be twice as much as fine grid; (coarse="
                            + coarserGridSizes[coarserGridSizes.length - 1] + ",fine=" + model.gridSize + ")");
        } else {
            LOGGER.warn("No coarser grid sizes given, using only the original size of {}°", model.gridSize);
        }
    }

    @Override
    public GeoCoordinate getScope(String text) {

        GridCell gridCell = null;
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting, scorer);

        for (int i = 0; i < gridSizes.length; i++) {
            if (gridCell == null) { // first iteration
                CategoryEntries coarseResult = classifier.classify(text, dictionaryModels[i]);
                String currentCellIdentifier = coarseResult.getMostLikely().getName();
                gridCell = gridCreators[i].getCell(currentCellIdentifier);
            } else { // we already have a coarser prediction; we limit this prediction to this area
                Set<String> categories = new HashSet<>();
                for (GridCell finerCell : gridCreators[i].getCells(gridCell)) {
                    categories.add(finerCell.getIdentifier());
                }
                // LOGGER.debug("# finer cells {}", categories.size());
                DictionaryModel focusedModel = new FocusDictionaryDecorator(dictionaryModels[i], categories);
                CategoryEntries finerResult = classifier.classify(text, focusedModel);
                Category mostLikely = finerResult.getMostLikely();
                if (mostLikely == null) {
                    // something went wrong, this should not happen, but it does for very few cases in the Wikipedia
                    // evaluation; we did not get a finer prediction here, break loop and return current cell
                    LOGGER.debug("did not get a finer classification, returning last result");
                    break;
                }
                gridCell = gridCreators[i].getCell(mostLikely.getName());
            }
            if (LOGGER.isDebugEnabled()) {
                String gridIdentifier = null;
                if (gridCell != null) {
                    gridIdentifier = gridCell.getIdentifier() + ": " + gridCell.getCenter();
                }
                LOGGER.debug("prediction @ {}° = {}", gridSizes[i], gridIdentifier);
            }
        }

        if (gridCell == null) {
            LOGGER.debug("no scope detected");
            return null;
        }

        GeoCoordinate mappedCoordinate = cellToCoordinate.get(gridCell.getIdentifier());
        return mappedCoordinate != null ? mappedCoordinate : gridCell.getCenter();

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " gridSizes=" + Arrays.toString(gridSizes) + ", scorer=" + scorer + ", "
                + dictionaryModels[dictionaryModels.length - 1];
    }

    public static void main(String[] args) throws IOException {
        DictionaryScopeModel model = FileHelper.deserialize("/Users/pk/Code/newsseecr/newsseecr/data/temp/textClassifierScopeModel_0.25.ser");
        ScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(model, 5, 2.5, 1, 0.5);
        // ScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(model);
        // ScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(model, 0.1);
        
        String content = FileHelper.readFileToString("/Users/pk/Desktop/WikipediaScopeDataset-2014/split-2/Ballantyne_Park.mediawiki");
        String text = new WikiPage(0, 0, null, content).getCleanText();
        System.out.println(scopeDetector.getScope(text));

        content = FileHelper.readFileToString("/Users/pk/Desktop/WikipediaScopeDataset-2014/split-2/Airdrop_Peak.mediawiki");
        text = new WikiPage(0, 0, null, content).getCleanText();
        System.out.println(scopeDetector.getScope(text));

        content = FileHelper.readFileToString("/Users/pk/Desktop/text.txt");
        System.out.println(scopeDetector.getScope(content));

        content = FileHelper.readFileToString("/Users/pk/Desktop/text_stripped.txt");
        System.out.println(scopeDetector.getScope(content));
        
        content = FileHelper.readFileToString("/Users/pk/Desktop/text2.txt");
        System.out.println(scopeDetector.getScope(content));
        
        content = FileHelper.readFileToString("/Users/pk/Desktop/text2_stripped.txt");
        System.out.println(scopeDetector.getScope(content));
    }

}
