package ws.palladian.extraction.location.disambiguation;

import java.util.Arrays;
import java.util.List;

import ws.palladian.extraction.location.scope.ScopeDetector;
import ws.palladian.retrieval.search.Searcher;

final class ImmutableFeatureExtractorSetting implements FeatureExtractorSetting {

    private final int equalDistance;
    private final int[] distanceValues;
    private final int[] populationValues;
    private final List<ScopeDetector> scopeDetectors;
    private final List<Searcher<?>> indexSearchers;
    private final String[] locationMarkers;
    private final boolean debug;

    public ImmutableFeatureExtractorSetting(Builder builder) {
        this.equalDistance = builder.equalDistance;
        this.distanceValues = builder.distanceValues;
        this.populationValues = builder.populationValues;
        this.scopeDetectors = builder.scopeDetectors;
        this.indexSearchers = builder.indexSearchers;
        this.locationMarkers = builder.locationMarkers;
        this.debug = builder.debug;
    }

    @Override
    public int getEqualDistance() {
        return equalDistance;
    }

    @Override
    public int[] getDistanceValues() {
        return distanceValues;
    }

    @Override
    public int[] getPopulationValues() {
        return populationValues;
    }

    @Override
    public List<ScopeDetector> getScopeDetectors() {
        return scopeDetectors;
    }

    @Override
    public List<Searcher<?>> getIndexSearchers() {
        return indexSearchers;
    }

    @Override
    public String[] getLocationMarkers() {
        return locationMarkers;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ImmutableFeatureExtractorSetting [equalDistance=");
        builder.append(equalDistance);
        builder.append(", distanceValues=");
        builder.append(Arrays.toString(distanceValues));
        builder.append(", populationValues=");
        builder.append(Arrays.toString(populationValues));
        if (scopeDetectors.size() > 0) {
            builder.append(", scopeDetectors=");
            builder.append(scopeDetectors);
        }
        if (indexSearchers.size() > 0) {
            builder.append(", indexSearchers=");
            builder.append(indexSearchers);
        }
        if (locationMarkers.length > 0) {
            builder.append(", locationMarkers=");
            builder.append(Arrays.toString(locationMarkers));
        }
        if (debug) {
            builder.append(", debug");
        }
        builder.append("]");
        return builder.toString();
    }

}
