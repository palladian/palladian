package ws.palladian.preprocessing;

import java.util.List;

import ws.palladian.helper.FileHelper;

public class StopWordRemover implements PipelineProcessor {

    private static final long serialVersionUID = 5014188120999997379L;

    public String removeStopWords(String text) {
    	List<String> stopWords = FileHelper.readFileToArray(StopWordRemover.class.getResource("/stopwords_en.txt"));

    	for (String stopWord : stopWords) {

            // skip comment lines
            if (stopWord.startsWith("#")) {
                continue;
            }

            text = text.replaceAll("\\W" + stopWord + "\\W", " ");
        }
    	
    	return text;
    }
    
    @Override
    public void process(PipelineDocument document) {       
        String content = document.getModifiedContent();

        content = removeStopWords(content);

        document.setModifiedContent(content);
    }

}
