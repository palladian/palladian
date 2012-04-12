package ws.palladian.extraction.feature;

import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

public class ControlledVocabularyFilter extends TokenRemover {

    private static final long serialVersionUID = 1L;

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ControlledVocabularyFilter.class);
    
    private HashSet<String> vocabulary = new HashSet<String>();
    
    public ControlledVocabularyFilter(String vocabularyFile) {

        final int numLines = FileHelper.getNumberOfLines(vocabularyFile);
        
        LineAction la = new LineAction() {
            
            @Override
            public void performAction(String line, int lineNumber) {
                String word = line.replace("_", " ").toLowerCase();
                vocabulary.add(word);
                if (lineNumber % 10000 == 0) {
                    LOGGER.trace("read " + lineNumber + "/" + numLines);
                }
            }
        };
        FileHelper.performActionOnEveryLine(vocabularyFile, la);
    }
    
    public ControlledVocabularyFilter(Collection<String> vocabulary) {
        this.vocabulary.addAll(vocabulary);
    }

    @Override
    protected boolean remove(Annotation annotation) {
        return !vocabulary.contains(annotation.getValue().toLowerCase());
    }

}
