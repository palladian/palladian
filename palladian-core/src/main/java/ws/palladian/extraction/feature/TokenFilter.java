package ws.palladian.extraction.feature;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * Remove tokens based on a vocabulary list, i.e. remove those tokens which are not in the supplied vocabulary.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class TokenFilter extends AbstractTokenRemover {

    private final Set<String> vocabulary = new HashSet<String>();

    public TokenFilter(File vocabularyFile) {
        FileHelper.performActionOnEveryLine(vocabularyFile.getAbsolutePath(), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                vocabulary.add(line);
            }
        });
    }

    public TokenFilter(Collection<String> vocabulary) {
        this.vocabulary.addAll(vocabulary);
    }

    @Override
    protected boolean remove(Annotation<String> annotation) {
        return !vocabulary.contains(annotation.getValue());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TokenFilter [#vocabulary=");
        builder.append(vocabulary.size());
        builder.append("]");
        return builder.toString();
    }

}
