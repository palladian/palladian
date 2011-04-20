package ws.palladian.preprocessing;

import java.util.List;

import ws.palladian.helper.FileHelper;

public class StopWordRemover implements PipelineProcessor {

    private static final long serialVersionUID = 5014188120999997379L;

    @Override
    public void process(PipelineDocument document) {

        List<String> stopWords = FileHelper.readFileToArray(StopWordRemover.class.getResource("/stopwords_en.txt"));

        String content = document.getModifiedContent();

        for (String stopWord : stopWords) {

            // skip comment lines
            if (stopWord.startsWith("#")) {
                continue;
            }

            content = content.replaceAll("\\W" + stopWord + "\\W", " ");
        }

        document.setModifiedContent(content);
    }

}
