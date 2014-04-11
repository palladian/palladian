package ws.palladian.extraction.feature;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Remove tokens based on a vocabulary list, i.e. remove those tokens which are not in the supplied vocabulary.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class TokenFilter extends AbstractTokenRemover {

    /**
     * <p>
     * The vocabulary containing all tokens this filter filters out.
     * </p>
     */
    private final Set<String> vocabulary = new HashSet<String>();

    /**
     * <p>
     * Creates a new {@code TokenFilter} based on the filter tokens from a vocabulary file.
     * </p>
     * 
     * @param vocabularyFile The vocabulary file containing the tokens to filter. This file is a text file with one
     *            token per line.
     */
    public TokenFilter(File vocabularyFile) {
        FileHelper.performActionOnEveryLine(vocabularyFile.getAbsolutePath(), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                vocabulary.add(line);
            }
        });
    }

    /**
     * <p>
     * Creates a new {@code TokenFilter} based on the filter tokens from a {@link Collection} of vocabulary tokens.
     * </p>
     * 
     * @param vocabulary The tokens to filter, when applying this filter.
     */
    public TokenFilter(Collection<String> vocabulary) {
        this.vocabulary.addAll(vocabulary);
    }

    @Override
    protected boolean remove(PositionAnnotation annotation) {
        return !vocabulary.contains(annotation.getValue());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TokenFilter [#vocabulary=");
        builder.append(vocabulary.size());
        builder.append("]");
        return builder.toString();
    }

}
