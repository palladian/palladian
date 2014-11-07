package ws.palladian.extraction.keyphrase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.keyphrase.temp.Dataset2;
import ws.palladian.extraction.keyphrase.temp.DatasetItem;
import ws.palladian.helper.io.FileHelper;

public abstract class KeyphraseExtractor {

    /** Maximum number of keyphrases to assign. */
    private int keyphraseCount = 10;

    public final void train(Dataset2 dataset) {
        startTraining();
        int i = 0;
        for (DatasetItem item : dataset) {
            i++;
            System.out.println(i + "/" + dataset.size() + ":" + item.getFile().getAbsolutePath());
            String[] categories = item.getCategories();
            String text;
            //            try {
            text = FileHelper.tryReadFileToString(item.getFile());
            // in case we have HTML files, strip HTML tags and unescape. Added to allow easy processing of HTML
            // files, would be better to let the extractors decide how to work with the supplied data though, e.g.
            // special HTML feature extraction in the future.
            //                if (item.getFile().getName().endsWith(".html")) {
            //                    text = HtmlHelper.stripHtmlTags(text);
            //                    text = StringEscapeUtils.unescapeHtml(text);
            //                }
            //            } catch (IOException e) {
            //                throw new IllegalStateException(e);
            //            }
            train(text, new HashSet<String>(Arrays.asList(categories)));
        }
        endTraining();
    }

    /**
     * Hook method which is called before the training begins. Can be overridden by subclasses as necessary.
     */
    public void startTraining() {
        // override if necessary
    }

    /**
     * Train the keyphrase extractor with the specified text and the assigned keyphrases.
     * 
     * @param inputText
     * @param keyphrases
     * @param index
     */
    public void train(String inputText, Set<String> keyphrases) {
        // override if this extractor needs training
    }

    /**
     * Hook method which is called after the training is finished. Can be overridden by subclasses as necessary.
     */
    public void endTraining() {
        // override if necessary
    }

    /**
     * Indicate whether this KeyphraseExtractor needs to be trained.
     * 
     * @return
     */
    public abstract boolean needsTraining();

    public abstract List<Keyphrase> extract(String inputText);

    /**
     * @return the keyphraseCount
     */
    public int getKeyphraseCount() {
        return keyphraseCount;
    }

    /**
     * Specify the maximum number of keyphrases to extract.
     * 
     * @param keyphraseCount the keyphraseCount to set
     */
    public void setKeyphraseCount(int keyphraseCount) {
        this.keyphraseCount = keyphraseCount;
    }

    /**
     * Hook method which is called before extraction. Can be overridden by subclasses as necessary.
     */
    public void startExtraction() {

    }

    public abstract String getExtractorName();

    public void reset() {
        // TODO Auto-generated method stub

    }

}