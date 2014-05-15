package ws.palladian.extraction.content.evaluation;

import java.io.File;

public interface ContentExtractionDatasetItem {

    File getHtmlFile();

    String getExpectedText();

}
