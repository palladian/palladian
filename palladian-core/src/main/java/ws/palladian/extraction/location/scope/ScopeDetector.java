package ws.palladian.extraction.location.scope;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ws.palladian.helper.geo.GeoCoordinate;

/**
 * <p>
 * A {@link ScopeDetector} is used to determine a geographic scope for a given text. <b>Note:</b> The usual scope
 * detection algorithms implement {@link RankingScopeDetector} (see interface documentation for an explanation). This
 * interface is for some experimental purposes.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface ScopeDetector {

    /**
     * <p>
     * Detect the geographic scope for the given text.
     * </p>
     * 
     * @param text The text for which to determine the scope.
     * @return A coordinate representing the scope, or <code>null</code> in case no scope could be determined.
     */
    GeoCoordinate getScope(String text);

    /**
     * <p>
     * Detect the geographic scope for the given text.
     * </p>
     *
     * @param text The text for which to determine the scope.
     * @return Optional with a coordinate representing the scope if it could be determined.
     */
    default Optional<GeoCoordinate> getScopeOptional(String text) {
        return Optional.ofNullable(getScope(text));
    }

    /**
     * <p>
     * Detect the geographic scopes for the given text.
     * </p>
     *
     * @param text The text for which to determine the scope.
     * @return List with one or more detected scopes, or an empty list if no scope could be detected.
     */
    default List<? extends GeoCoordinate> getScopes(String text) {
        return getScopeOptional(text).map(Collections::singletonList).orElse(Collections.emptyList());
    }

}
