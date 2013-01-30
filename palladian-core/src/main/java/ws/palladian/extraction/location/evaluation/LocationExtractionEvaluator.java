package ws.palladian.extraction.location.evaluation;

import java.util.List;

import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.location.AlchemyLocationExtractor;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.OpenCalaisLocationExtractor;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.helper.collection.CollectionHelper;

public class LocationExtractionEvaluator {

    public void evaluate(String goldStandardTaggedTextPath) {

        List<LocationExtractor> extractors = CollectionHelper.newArrayList();
        extractors.add(new AlchemyLocationExtractor("FIXME"));
        extractors.add(new OpenCalaisLocationExtractor("FIXME"));
        extractors.add(new PalladianLocationExtractor());

        for (LocationExtractor locationExtractor : extractors) {
            locationExtractor.evaluate(goldStandardTaggedTextPath, TaggingFormat.XML);
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        LocationExtractionEvaluator evaluator = new LocationExtractionEvaluator();
        evaluator.evaluate("text5.txt");
    }

}
