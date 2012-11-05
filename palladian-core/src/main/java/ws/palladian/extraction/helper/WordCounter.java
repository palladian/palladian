package ws.palladian.extraction.helper;

import ws.palladian.extraction.feature.StringDocumentPipelineProcessor;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.NumericFeature;

public class WordCounter extends StringDocumentPipelineProcessor {

    @Override
    public void processDocument(PipelineDocument<String> document) {

        int wordCount = StringHelper.countWords(document.getContent());

        NumericFeature numericFeature = new NumericFeature("wordCount", (double)wordCount);

        document.getFeatureVector().add(numericFeature);

    }

}
