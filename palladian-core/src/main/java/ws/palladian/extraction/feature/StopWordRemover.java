package ws.palladian.extraction.feature;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Remove stop words from a text.
 * </p>
 */
public class StopWordRemover implements Filter<String> {

    private final Set<String> stopwords;

    /**
     * <p>
     * Default constructor for English texts.
     * </p>
     */
    public StopWordRemover() {
        this(Language.ENGLISH);
    }
    
    /**
     * <p>
     * Create a new {@link StopTokenRemover} with stop words from the specified {@link File}.
     * </p>
     * 
     * @param file The file which contains the stop words. Each line is treated as one stop word, lines starting with #
     *            are treated as comments and are therefore ignored.
     * @throws IllegalArgumentException If the supplied file cannot be found.
     */
    public StopWordRemover(File file) {
        Validate.notNull(file, "file must not be null");
        try {
            stopwords = loadStopwords(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File \"" + file + "\" not found.");
        }
    }

    /**
     * <p>
     * Choose a language (English or German).
     * </p>
     * 
     * @param language The language for which the stop words should be removed.
     */
    public StopWordRemover(Language language) {
        Validate.notNull(language, "language must not be null");
        switch (language) {
            case ENGLISH:
                stopwords = loadStopwordsResource("/stopwords_en.txt");
                break;
            case GERMAN:
                stopwords = loadStopwordsResource("/stopwords_de.txt");
                break;
            default:
                stopwords = Collections.emptySet();
                break;
        }
    }
    
    private Set<String> loadStopwordsResource(String resourcePath) {
        InputStream inputStream = StopWordRemover.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("Resource \"" + resourcePath + "\" not found.");
        }
        try {
            return loadStopwords(inputStream);
        } finally {
            FileHelper.close(inputStream);
        }
    }
    
    private Set<String> loadStopwords(InputStream fileInputStream) {
        final Set<String> result = new HashSet<String>();
        FileHelper.performActionOnEveryLine(fileInputStream, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String lineString = line.trim();
                // ignore comments and empty lines ...
                if (!lineString.startsWith("#") && !lineString.isEmpty()) {
                    result.add(line.toLowerCase());
                }
            }
        });
        return result;
    }

    public String removeStopWords(String text) {

        for (String stopWord : stopwords) {

            // skip comment lines
            if (stopWord.startsWith("#")) {
                continue;
            }

            text = StringHelper.removeWord(stopWord, text);
        }

        return text;
    }
    
    @Override
    public boolean accept(String item) {
        return !isStopWord(item);
    }

    public boolean isStopWord(String word) {
        return stopwords.contains(word);
    }

    public void addStopWord(String word) {
        stopwords.add(word);
    }

    public void removeStopWord(String word) {
        stopwords.remove(word);
    }

    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StopWordRemover [#stopwords=");
        builder.append(stopwords.size());
        builder.append("]");
        return builder.toString();
    }

    public static void main(String[] args) {
        StopWordRemover stopWordRemover = new StopWordRemover();
        System.out.println(stopWordRemover.removeStopWords("is the"));
    }


}
