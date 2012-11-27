package ws.palladian.extraction.feature;

import java.io.InputStream;
import java.util.List;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.TextDocument;

/**
 * @deprecated Use {@link StopTokenRemover} instead.
 */
@Deprecated
public class StopWordRemover extends TextDocumentPipelineProcessor {

    private List<String> stopWords;

    public StopWordRemover() {
        InputStream inputStream = null;
        try {
            inputStream = this.getClass().getResourceAsStream("/stopwords_en.txt");
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
