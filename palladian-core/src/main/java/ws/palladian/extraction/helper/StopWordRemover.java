package ws.palladian.extraction.helper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

public class StopWordRemover implements PipelineProcessor {

    private static final long serialVersionUID = 5014188120999997379L;

    public String removeStopWords(String text) {

        InputStream stream = this.getClass().getResourceAsStream("/stopwords_en.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        List<String> stopWords = FileHelper.readFileToArray(br);

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
    public void process(PipelineDocument document) {
        String content = document.getModifiedContent();

        content = removeStopWords(content);

        document.setModifiedContent(content);
    }

    public static void main(String[] args) {
        StopWordRemover stopWordRemover = new StopWordRemover();
        System.out.println(stopWordRemover.removeStopWords("is the"));
    }
}
