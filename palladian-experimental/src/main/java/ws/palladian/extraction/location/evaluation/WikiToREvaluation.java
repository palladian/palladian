package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.function.Consumer;

import org.apache.lucene.store.FSDirectory;

import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.BaselineDisambiguation;
import ws.palladian.extraction.location.disambiguation.DarwinDisambiguation;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguation;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.disambiguation.HeuristicScopeDisambiguation;
import ws.palladian.extraction.location.disambiguation.ScopeDisambiguation;
import ws.palladian.extraction.location.persistence.lucene.LuceneLocationSource;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.NearestNeighborScopeModel;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.ConsumerIteratorAdapter;
import ws.palladian.helper.io.FileHelper;

public class WikiToREvaluation {
	
	public static void main(String[] args) throws Exception {
		File xml = new File("/Users/pk/Desktop/Location_Lab_Revisited/WikToR(SciPaper).xml");
		LocationSource source = new LuceneLocationSource(FSDirectory.open(Paths.get("/Users/pk/Desktop/Location_Lab_Revisited/Palladian_Location_Database_2022-04-19_13-45-08")));
		QuickDtModel model = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/locationDisambiguationModel-tudLoc2013-10trees.ser.gz");
		// NearestNeighborScopeModel nnModel = NearestNeighborScopeModel.fromIndex(new File("/Users/pk/Desktop/Location_Lab_Revisited/knn-scope-model-wikipedia-90-train"));

		new ConsumerIteratorAdapter<LocationDocument>() {

			@Override
			protected void produce(Consumer<LocationDocument> action) throws Exception {
				WikiToRDatasetReader.parse(xml, action);
			}

			@Override
			protected void consume(Iterable<LocationDocument> iterable) {
				
				Iterable<LocationDocument> testData = CollectionHelper.limit(iterable, 500);
				
				LocationExtractionEvaluator evaluator = new LocationExtractionEvaluator();
				evaluator.addDataset(testData);
				evaluator.addExtractor(new PalladianLocationExtractor(source, new BaselineDisambiguation()));
				evaluator.addExtractor(new PalladianLocationExtractor(source, new HeuristicDisambiguation()));
				evaluator.addExtractor(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model, 0)));
				evaluator.addExtractor(new PalladianLocationExtractor(source, new DarwinDisambiguation(new FeatureBasedDisambiguation(model, 0))));
				
				// these are unfair, b/c it's Wikipedia data
				// evaluator.addExtractor(new PalladianLocationExtractor(source, new HeuristicScopeDisambiguation(new KNearestNeighborScopeDetector(nnModel, 10))));
				// evaluator.addExtractor(new PalladianLocationExtractor(source, new ScopeDisambiguation(new KNearestNeighborScopeDetector(nnModel, 10))));
				
				evaluator.runAll(true);
			}
		};
	}

}
