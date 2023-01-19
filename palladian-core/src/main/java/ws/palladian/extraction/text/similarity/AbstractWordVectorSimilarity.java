package ws.palladian.extraction.text.similarity;

import ws.palladian.extraction.feature.StopWordRemover;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.nlp.AbstractStringMetric;

import java.util.List;

abstract class AbstractWordVectorSimilarity extends AbstractStringMetric {

    protected List<String> preprocess(String sentence) {
        sentence = sentence.toLowerCase();
        List<String> sentenceSplit = Tokenizer.tokenize(sentence);
        sentenceSplit = CollectionHelper.filterList(sentenceSplit, new StopWordRemover(Language.ENGLISH));
        return sentenceSplit;
    }

}
