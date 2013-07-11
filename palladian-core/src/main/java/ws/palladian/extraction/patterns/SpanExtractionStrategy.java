/**
 * Created on: 19.06.2012 19:19:13
 */
package ws.palladian.extraction.patterns;

import java.util.List;

import ws.palladian.processing.features.SequentialPattern;

/**
 * <p>
 * Provides an extraction strategy for the {@link SequentialPatternAnnotator}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public interface SpanExtractionStrategy {
    /**
     * <p>
     * Extracts all {@link SequentialPattern}s from the provided list of token.
     * </p>
     * 
     * @param tokenList The tokens to extract patterns from.
     * @param minPatternSize The minimum size of the patterns.
     * @param maxPatternSize The maximum size of the pattern.
     * @return A {@code List} of the {@code SequentialPattern}s from the provided token.
     */
    List<SequentialPattern> extract(final String[] tokenList,
            final Integer minPatternSize, final Integer maxPatternSize);
}
