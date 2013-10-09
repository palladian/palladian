package ws.palladian.extraction.feature;

import java.io.InputStream;
import java.util.List;

import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.TextDocument;

/**
 * <p>
 * Remove stop words from a text.
 * </p>
 */
public class StopWordRemover extends TextDocumentPipelineProcessor {

    private List<String> stopWords;

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
     * Choose a language (English or German).
     * </p>
     * 
     * @param language The language for which the stop words should be removed.
     */
    public StopWordRemover(Language language) {
        InputStream inputStream = null;
        try {
            if (language.equals(Language.ENGLISH)) {
                inputStream = this.getClass().getResourceAsStream("/stopwords_en.txt");
            } else if (language.equals(Language.GERMAN)) {
                inputStream = this.getClass().getResourceAsStream("/stopwords_de.txt");
            }
            stopWords = FileHelper.readFileToArray(inputStream);
        } finally {
            FileHelper.close(inputStream);
        }
    }

    public String removeStopWords(String text) {

        for (String stopWord : stopWords) {

            // skip comment lines
            if (stopWord.startsWith("#")) {
                continue;
            }

            text = StringHelper.removeWord(stopWord, text);
        }

        return text;
    }

    public boolean isStopWord(String word) {
        return stopWords.contains(word);
    }

    @Override
    public void processDocument(TextDocument document) {
        String content = document.getContent();

        content = removeStopWords(content);

        document.setContent(content);
    }

    public static void main(String[] args) {
        StopWordRemover stopWordRemover = new StopWordRemover();
        System.out.println(stopWordRemover.removeStopWords("is the"));
    }
}
