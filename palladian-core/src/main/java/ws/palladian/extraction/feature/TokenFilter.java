package ws.palladian.extraction.feature;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.model.features.Annotation;

public final class TokenFilter extends AbstractTokenRemover {

    private static final long serialVersionUID = 1L;

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
    protected boolean remove(Annotation annotation) {
        return !vocabulary.contains(annotation.getValue());
    }

}
