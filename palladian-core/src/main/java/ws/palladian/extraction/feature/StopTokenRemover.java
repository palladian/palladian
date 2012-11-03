package ws.palladian.extraction.feature;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * A {@link PipelineProcessor} for removing stop words from a pre-tokenized text. This means, the documents to be
 * processed by this class must be processed by a {@link BaseTokenizer} in advance, supplying
 * {@link BaseTokenizer#PROVIDED_FEATURE} annotations. Stop words are determined case-insensitively.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public final class StopTokenRemover extends AbstractTokenRemover {

    private final Set<String> stopwords;

    /**
     * <p>
     * Create a new {@link StopTokenRemover} for the specified {@link Language}. If no stop words are available for the
     * specified language, a no-operation {@link StopTokenRemover} is created.
     * </p>
     * 
     * @param language
     */
    public StopTokenRemover(Language language) {
        Validate.notNull(language, "languae must not be null");
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

    /**
     * <p>
     * Create a new {@link StopTokenRemover} with stop words from the specified {@link File}.
     * </p>
     * 
     * @param file The file which contains the stop words. Each line is treated as one stop word, lines starting with #
     *            are treated as comments and are therefore ignored.
     * @throws IllegalArgumentException If the supplied file cannot be found.
     */
    public StopTokenRemover(File file) {
        Validate.notNull(file, "file must not be null");
        try {
            stopwords = loadStopwords(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File \"" + file + "\" not found.");
        }
    }

    private Set<String> loadStopwordsResource(String resourcePath) {
        InputStream inputStream = StopTokenRemover.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("Resource \"" + resourcePath + "\" not found.");
        }
        try {
            return loadStopwords(inputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
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

    /**
     * <p>
     * Determine whether the specified word is a stop word.
     * </p>
     * 
     * @param word The word to check.
     * @return <code>true</code> if word is a stop word, <code>false</code> otherwise.
     */
    public boolean isStopword(String word) {
        return stopwords.contains(word.toLowerCase());
    }

    @Override
    protected boolean remove(Annotation<String> annotation) {
        return isStopword(annotation.getValue());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StopTokenRemover [#stopwords=");
        builder.append(stopwords.size());
        builder.append("]");
        return builder.toString();
    }

}
