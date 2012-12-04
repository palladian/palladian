package ws.palladian.extraction.feature;

import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.NumericFeature;

public class WordCounter extends TextDocumentPipelineProcessor {

    @Override
    public void processDocument(TextDocument document) {
        int wordCount = StringHelper.countWords(document.getContent());
        document.getFeatureVector().add(new NumericFeature("wordCount", wordCount));
    }

}
