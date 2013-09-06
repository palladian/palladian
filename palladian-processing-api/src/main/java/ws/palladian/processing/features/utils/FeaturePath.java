/**
 * Created on: 22.05.2013 09:37:55
 */
package ws.palladian.processing.features.utils;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * A path to a {@link Feature} in a {@link FeatureVector}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 2.1.0
 */
public final class FeaturePath {
    /**
     * <p>
     * The feature paths segments beginning with the root running to the leaf.
     * </p>
     */
    private final String[] pathSegments;

    /**
     * <p>
     * Creates a new completely initialized {@link FeaturePath}.
     * </p>
     * 
     * @param pathSegments The path segments of the new feature path beginning with the root and running to the leaf.
     */
    public FeaturePath(String... pathSegments) {
        this.pathSegments = Arrays.copyOf(pathSegments, pathSegments.length);
    }

    /**
     * <p>
     * Creates a new {@link FeaturePath} as child of an existing {@link FeaturePath}.
     * </p>
     * 
     * @param parentPath The parent path.
     * @param leafSegment The leaf of the new path appended at the end of the parent path.
     */
    public FeaturePath(FeaturePath parentPath, String leafSegment) {
        int newLength = parentPath.pathSegments.length + 1;
        this.pathSegments = Arrays.copyOf(parentPath.pathSegments, newLength);
        this.pathSegments[newLength - 1] = leafSegment;
    }

    /**
     * @param depth The depth to get the path segment from starting with 0. This value must be between inclusive 0 and
     *            exclusive the length of the path.
     * @return The path segment at the specified depth.
     */
    public String get(int depth) {
        Validate.isTrue(depth < pathSegments.length);
        Validate.isTrue(depth > 0);

        return pathSegments[depth];
    }
}
