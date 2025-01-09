package ws.palladian.extraction.entity.tagger;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.store.FSDirectory;

import ws.palladian.core.ClassifyingTagger;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator;
import ws.palladian.extraction.location.persistence.lucene.LuceneLocationSource;

public class BertNerLocationExtraction {

    public static void main(String[] args) throws IOException {
        var evaluator = new LocationExtractionEvaluator();
        evaluator.addDataset("/Users/pk/temp/tud-loc-2013/1-training");
//        evaluator.addDataset("/Users/pk/temp/LGL-converted/1-train");
//        evaluator.addDataset("/Users/pk/temp/CLUST-converted/1-train");

        var luceneLocationDb = Paths.get("/Users/pk/temp/Palladian_Location_Database_2024-09-11_23-55-10");
        var database = new LuceneLocationSource(FSDirectory.open(luceneLocationDb));

        // (I) heuristic with default tagger
//        evaluator.addExtractor(new PalladianLocationExtractor(database));

        // (II) heuristic with BERT NER
        var modelDirectory = Paths.get("/Users/pk/temp/Huggingface_Java/raw-files/onnx/model.onnx");
        var tokenizerJson = Paths.get("/Users/pk/temp/Huggingface_Java/raw-files/tokenizer.json");
        var bertNer = BertNer.loadFrom(modelDirectory, tokenizerJson);
        var bertTagger = new ClassifyingTagger() {
            @Override
            public List<ClassifiedAnnotation> getAnnotations(String text) {
                return bertNer //
                        .getAnnotations(text) //
                        .stream() //
                        .filter(a -> a.getTag().equals("LOC")) //
                        .toList();
            }

            @Override
            public String toString() {
                return "bertTagger";
            }
        };
        evaluator.addExtractor(new PalladianLocationExtractor(database, bertTagger, new HeuristicDisambiguation()));

//        // (III) TODO - ML with default tagger
//        // (a) var mlModel = (QuickDtModel) FileHelper.deserialize("/Users/pk/temp/ld-all-training-10t.ser.gz");
//        var mlModel = (QuickDtModel) FileHelper.deserialize("/Users/pk/temp/ld-tud-loc-2013-10t.ser.gz");
//        var disambiguation = new FeatureBasedDisambiguation(mlModel);
//        evaluator.addExtractor(new PalladianLocationExtractor(database, disambiguation));

        // (IV)  TODO - ML with BERT NER (requires re-trained model)

        evaluator.runAll(true);
    }

}
