package ws.palladian.extraction.content.evaluation;

import java.io.File;

public interface ContentExtractionDataset extends Iterable<ContentExtractionDataset.ContentExtractionPage> {

    /**
     * An evaluation page within the dataset.
     * 
     * @author pk
     */
    interface ContentExtractionPage {

        /**
         * @return The path to the HTML file.
         */
        File getHtmlFile();

        /**
         * @return The expected text as given in the dataset.
         */
        String getExpectedText();

        /**
         * @return A web URL pointing to that page, or <code>null</code> in case this information is not available in
         *         the dataset.
         */
        String getUrl();

    }

    /**
     * @return The number of pages within the dataset.
     */
    int size();

}
