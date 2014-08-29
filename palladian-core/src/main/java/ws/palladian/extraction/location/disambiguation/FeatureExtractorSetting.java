package ws.palladian.extraction.location.disambiguation;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.scope.ScopeDetector;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Factory;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.Searcher;

public interface FeatureExtractorSetting {

    /**
     * The default setting which worked well during our experiments. Use a value of 50km for distance-based features,
     * 100.000 for population-based features and treat equally-named locations within 50km as identical.
     */
    FeatureExtractorSetting DEFAULT = new Builder() //
            .setDistanceValues(50) //
            .setPopulationValues(100000) //
            .setEqualDistance(50).create();

    /**
     * @return Maximum distance, when two locations with same name are regarded equal.
     */
    int getEqualDistance();

    /**
     * @return Values for the distance, for which distance-based features should be extracted.
     */
    int[] getDistanceValues();

    /**
     * @return Values for the population, for which population-based features should be extracted.
     */
    int[] getPopulationValues();

    /**
     * @return Scope detectors to apply for determining the assumed main location from text.
     */
    List<ScopeDetector> getScopeDetectors();

    /**
     * @return Searchers to use, for determining how common a term is.
     */
    List<Searcher<?>> getIndexSearchers();

    /**
     * @return Word markers, for which explicit features will be created (e.g. <code>river</code>).
     */
    String[] getLocationMarkers();

    /**
     * @return <code>true</code> to create additional debugging features, should usually be set to <code>false</code>.
     */
    boolean isDebug();

    class Builder implements Factory<FeatureExtractorSetting> {

        public static final int DEFAULT_EQUAL_DISTANCE = 50;

        int equalDistance = DEFAULT_EQUAL_DISTANCE;
        int[] distanceValues;
        int[] populationValues;
        List<ScopeDetector> scopeDetectors = CollectionHelper.newArrayList();
        List<Searcher<? extends WebContent>> indexSearchers = CollectionHelper.newArrayList();
        String[] locationMarkers = new String[0];
        boolean debug = false;

        public Builder setEqualDistance(int equalDistance) {
            this.equalDistance = equalDistance;
            return this;
        }

        public Builder setDistanceValues(int... distanceValues) {
            this.distanceValues = distanceValues;
            return this;
        }

        public Builder setPopulationValues(int... populationValues) {
            this.populationValues = populationValues;
            return this;
        }

        public Builder addScopeDetector(ScopeDetector scopeDetector) {
            this.scopeDetectors.add(scopeDetector);
            return this;
        }

        public Builder addIndexSearcher(Searcher<? extends WebContent> indexSearcher) {
            this.indexSearchers.add(indexSearcher);
            return this;
        }

        public Builder setLocationMarkers(String... locationMarkers) {
            this.locationMarkers = locationMarkers;
            return this;
        }

        public Builder setDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder setFeatureExtractorSetting(FeatureExtractorSetting setting) {
            Validate.notNull(setting, "setting must not be null");
            this.equalDistance = setting.getEqualDistance();
            this.distanceValues = Arrays.copyOf(setting.getDistanceValues(), setting.getDistanceValues().length);
            this.populationValues = Arrays.copyOf(setting.getPopulationValues(), setting.getPopulationValues().length);
            this.scopeDetectors = CollectionHelper.newArrayList(setting.getScopeDetectors());
            this.indexSearchers = CollectionHelper.newArrayList(setting.getIndexSearchers());
            this.locationMarkers = Arrays.copyOf(setting.getLocationMarkers(), setting.getLocationMarkers().length);
            this.debug = setting.isDebug();
            return this;
        }

        @Override
        public FeatureExtractorSetting create() {
            return new ImmutableFeatureExtractorSetting(this);
        }

    }

}
