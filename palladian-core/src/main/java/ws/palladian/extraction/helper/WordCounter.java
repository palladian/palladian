package ws.palladian.extraction.helper;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.model.features.NumericFeature;

public class WordCounter implements PipelineProcessor {

    private static final long serialVersionUID = 4592668328026315402L;

    @Override
    public void process(PipelineDocument document) {

        int wordCount = StringHelper.countWords(document.getModifiedContent());

        NumericFeature numericFeature = new NumericFeature("wordCount", (double) wordCount);
        
        document.getFeatureVector().add(numericFeature);

    }

}
