package ws.palladian.extraction.content.evaluation;

import java.io.File;

public interface ContentExtractionDataset extends Iterable<ContentExtractionDataset.ContentExtractionPage> {
    
    interface ContentExtractionPage {

        File getHtmlFile();

        String getExpectedText();

    }
    
    int size();

}
