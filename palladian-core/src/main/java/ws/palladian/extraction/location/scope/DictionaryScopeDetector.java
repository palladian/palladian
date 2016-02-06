package ws.palladian.extraction.location.scope;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.BayesScorer;
import ws.palladian.classification.text.DictionaryBuilder;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.DictionaryTrieModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.geo.GeoCoordinate;

/**
 * Scope detection using the {@link PalladianTextClassifier}. We use a grid of the world (UTM) and use the cells as
 * potential classes. This approach is similar to the one described in
 * "Simple Supervised Document Geolocation with Geodesic Grids", Benjamin P. Wing and Jason Baldridge, 2011.
 * 
 * @author Philipp Katz
 */
public class DictionaryScopeDetector implements ScopeDetector {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryScopeDetector.class);

    /**
     * A scope model which contains a dictionary for the text classifier and grid information.
     * 
     * @author Philipp Katz
     */
    public static final class DictionaryScopeModel implements TextClassifierScopeModel, Serializable {
        private static final long serialVersionUID = 1L;

        public final double gridSize;
        public final DictionaryModel dictionaryModel;
        public final Map<String, GeoCoordinate> cellToCoordinate;

        public DictionaryScopeModel(double gridSize, DictionaryModel dictionaryModel,
                Map<String, GeoCoordinate> cellToCoordinate) {
            this.gridSize = gridSize;
            this.dictionaryModel = dictionaryModel;
            this.cellToCoordinate = cellToCoordinate;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + ", gridSize = " + gridSize + ", dictionary=" + dictionaryModel;
        }
    }

    /**
     * Conversion function from {@link LocationDocument} to a {@link Trainable} instance for the
     * {@link PalladianTextClassifier}. The function converts to the grid, as specified by the grid size in the
     * constructor.
     * 
     * @author Philipp Katz
     */
    static final class GridConverter implements Function<LocationDocument, Instance> {
        
        /** Placeholder, where no grid identifier could be applied. */
        public static final String UNDETERMINED = "## undetermined ##";

        private final GridCreator gridCreator;
        private final Map<String, CoordinateStats> cellStatsMap = LazyMap.create(CoordinateStats.FACTORY);

        GridConverter(GridCreator gridCreator) {
            this.gridCreator = gridCreator;
        }

        @Override
        public Instance compute(LocationDocument input) {
            Location mainLocation = input.getMainLocation();
            String gridIdentifier = UNDETERMINED;
            if (mainLocation != null && mainLocation.getCoordinate() != null) {
                GeoCoordinate coordinate = mainLocation.getCoordinate();
                gridIdentifier = gridCreator.getCell(coordinate).getIdentifier();
                cellStatsMap.get(gridIdentifier).add(coordinate);
            } else {
                LOGGER.warn("Encountered undetermined grid identifier");
            }
            return new InstanceBuilder().setText(input.getText()).create(gridIdentifier);
        }

        /**
         * Loop through the dataset and determine the center point of each cell, by calculating the center of minimum
         * distance of all occurring coordinates in a cell.
         * 
         * @return A {@link Map} with mappings from grid cell identifier to {@link GeoCoordinate} representing the most
         *         common coordinate of that specific cell.
         */
        public Map<String, GeoCoordinate> getMapping() {
            Map<String, GeoCoordinate> result = new HashMap<>();
            ProgressReporter progress = new ProgressMonitor();
            progress.startTask("Calculating cell to coordinate mapping", cellStatsMap.size());
            for (Entry<String, CoordinateStats> entry : cellStatsMap.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getCenterOfMinimumDistance());
                progress.increment();
            }
            return result;
        }

    }

    /**
     * Filter out such {@link LocationDocument}s which have no coordinates.
     * 
     * @author Philipp Katz
     */
    static final class CoordinateFilter implements Filter<LocationDocument> {
        @Override
        public boolean accept(LocationDocument item) {
            return item.getMainLocation() != null && item.getMainLocation().getCoordinate() != null;
        }

    }

    /** The default {@link Scorer} which is used for the text classifier. */
    public static final Scorer DEFAULT_SCORER = new BayesScorer();

    /** The model. */
    private final DictionaryScopeModel model;

    /** For mapping between grid cells and coordinates. */
    private final GridCreator gridCreator;

    /** The scorer for the text classification. */
    private final Scorer scorer;

    /** The text classifier. */
    private final PalladianTextClassifier classifier;

    /**
     * Create a new {@link DictionaryScopeDetector} with the provided model and scorer.
     * 
     * @param model The model, not <code>null</code>.
     * @param scorer The scorer for the text classifier, not <code>null</code>.
     */
    public DictionaryScopeDetector(DictionaryScopeModel model, Scorer scorer) {
        Validate.notNull(model, "model must not be null");
        Validate.notNull(scorer, "scorer must not be null");
        this.model = model;
        this.gridCreator = new GridCreator(model.gridSize);
        this.scorer = scorer;
        this.classifier = new PalladianTextClassifier(model.dictionaryModel.getFeatureSetting(), scorer);
    }

    /**
     * Create a new {@link DictionaryScopeDetector} with the provided model and the {@link BayesScorer} for the
     * text classification.
     * 
     * @param model The model, not <code>null</code>.
     */
    public DictionaryScopeDetector(DictionaryScopeModel model) {
        this(model, DEFAULT_SCORER);
    }

    public static final class DictionaryScopeDetectorLearner implements TextClassifierScopeDetectorLearner {

        private final FeatureSetting setting;
        private final DictionaryBuilder builder;
        private final double gridSize;

        public DictionaryScopeDetectorLearner(FeatureSetting setting, DictionaryBuilder builder, double gridSize) {
            Validate.notNull(setting, "setting must not be null");
            Validate.notNull(builder, "builder must not be null");
            Validate.isTrue(gridSize > 0, "gridSize must be greater zero");
            this.setting = setting;
            this.builder = builder;
            this.gridSize = gridSize;
        }

        public DictionaryScopeDetectorLearner(FeatureSetting setting, double gridSize) {
            this(setting, new DictionaryTrieModel.Builder(), gridSize);
        }

        @Override
        public DictionaryScopeModel train(Iterable<? extends LocationDocument> documentIterator) {
            PalladianTextClassifier classifier = new PalladianTextClassifier(setting, builder);
            GridCreator gridCreator = new GridCreator(gridSize);
            // remove entries without coordinates
            documentIterator = CollectionHelper.filter(documentIterator, new CoordinateFilter());
            GridConverter gridConverter = new GridConverter(gridCreator);
            Iterable<Instance> trainData = CollectionHelper.convert(documentIterator, gridConverter);
            LOGGER.info("Building dictionary");
            DictionaryModel dictionaryModel = classifier.train(trainData);
            LOGGER.info("Calculating cell to coordinate mapping");
            Map<String, GeoCoordinate> cellCoordinateMap = gridConverter.getMapping();
            LOGGER.info("Done.");
            return new DictionaryScopeModel(gridSize, dictionaryModel, cellCoordinateMap);
        }

    }

    @Override
    public GeoCoordinate getScope(String text) {
        StopWatch stopWatch = new StopWatch();
        CategoryEntries result = classifier.classify(text, model.dictionaryModel);
        String gridIdentifier = result.getMostLikely().getName();
        try {
            GeoCoordinate mappedCoordinate = model.cellToCoordinate.get(gridIdentifier);
            return mappedCoordinate != null ? mappedCoordinate : gridCreator.getCell(gridIdentifier).getCenter();
        } catch (IllegalArgumentException e) {
            return null;
        } finally {
            LOGGER.trace("Took {}", stopWatch);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", model=" + model + ", scorer=" + scorer;
    }

}
