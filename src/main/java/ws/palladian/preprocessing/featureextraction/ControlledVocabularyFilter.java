package ws.palladian.preprocessing.featureextraction;

import gnu.trove.THashSet;

import java.util.Collection;

import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.LineAction;

public class ControlledVocabularyFilter extends TokenRemover {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ControlledVocabularyFilter.class);
    
    // private HashSet<String> vocabulary = new HashSet<String>();
    private THashSet vocabulary = new THashSet();
    
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
    protected boolean remove(Token token) {
        return !vocabulary.contains(token.getValue().toLowerCase());
    }

}
