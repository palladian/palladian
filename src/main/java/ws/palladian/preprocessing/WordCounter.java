package ws.palladian.preprocessing;

import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.model.features.NumericFeature;

public class WordCounter implements PipelineProcessor {

    @Override
    public void process(PipelineDocument document) {

        int wordCount = StringHelper.countWords(document.getModifiedContent());

        NumericFeature numericFeature = new NumericFeature("wordCount", (double) wordCount);

        document.getFeatures().add(numericFeature);

    }

}
