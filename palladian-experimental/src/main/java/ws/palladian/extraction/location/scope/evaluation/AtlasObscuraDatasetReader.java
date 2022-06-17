package ws.palladian.extraction.location.scope.evaluation;

import static ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.MORE_LIKE_THIS_QUERY_CREATOR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.lucene.store.FSDirectory;

import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.classification.text.BayesScorer;
import ws.palladian.classification.text.ExperimentalScorers;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguation;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.evaluation.ImmutableLocationDocument;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.persistence.lucene.LuceneLocationSource;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector;
import ws.palladian.extraction.location.scope.FirstScopeDetector;
import ws.palladian.extraction.location.scope.FrequencyScopeDetector;
import ws.palladian.extraction.location.scope.HighestPopulationScopeDetector;
import ws.palladian.extraction.location.scope.HighestTrustScopeDetector;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.NearestNeighborScopeModel;
import ws.palladian.extraction.location.scope.LeastDistanceScopeDetector;
import ws.palladian.extraction.location.scope.MidpointScopeDetector;
import ws.palladian.extraction.location.scope.MultiStepDictionaryScopeDetector;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeModel;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineIterator;
import ws.palladian.helper.io.ProgressReporterInputStream;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

public class AtlasObscuraDatasetReader implements Iterable<LocationDocument> {

	private final class AtlasObscuraJsonIterator extends AbstractIterator2<LocationDocument> implements CloseableIterator<LocationDocument> {
		private final LineIterator lineIterator;
		private final ProgressMonitor progressMonitor;

		private AtlasObscuraJsonIterator(File jsonPath) {
			this.lineIterator = new LineIterator(jsonPath);
			int numLines = FileHelper.getNumberOfLines(jsonPath);
			progressMonitor = new ProgressMonitor(numLines);
		}

		@Override
		protected LocationDocument getNext() {

			while (lineIterator.hasNext()) {

				try {
					String line = lineIterator.next();
					progressMonitor.increment();

					JsonObject json = new JsonObject(line);
					int id = json.getInt("id");
					String headline = json.getString("headline");
					String description = json.getString("description");
					String copy = json.getString("copy");
					String copyDirection = json.getString("copyDirection");
					double latitude = json.getDouble("latitude");
					double longitude = json.getDouble("longitude");

					String text = headline + "\n" + description + "\n" + copy + "\n" + copyDirection;
					GeoCoordinate geoCoordinate = new ImmutableGeoCoordinate(latitude, longitude);
					Location scopeLocation = new ImmutableLocation(-1, LocationDocument.UNDETERMINED,
							LocationType.UNDETERMINED, geoCoordinate, null);

					return new ImmutableLocationDocument(String.valueOf(id), text, Collections.emptyList(), scopeLocation);

				} catch (JsonException e) {
					// just continue for heaven's sake!
				}
			}

			return finished();
		}

		@Override
		public void close() throws IOException {
			lineIterator.close();
		}
	}

	private final File jsonPath;

	public AtlasObscuraDatasetReader(File jsonPath) {
		this.jsonPath = Objects.requireNonNull(jsonPath);
	}

	@Override
	public Iterator<LocationDocument> iterator() {
		return new AtlasObscuraJsonIterator(jsonPath);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException {
		AtlasObscuraDatasetReader reader = new AtlasObscuraDatasetReader(new File("/Users/pk/Desktop/atlas-obscura-crawler/places.json"));
		analyzeScopeDetection(reader, 1000);
		System.exit(0);

		ScopeDetectorEvaluator eval = new ScopeDetectorEvaluator();
		// for now, just take 25% for evaluation
		Iterable<LocationDocument> data = CollectionHelper.filter(reader, l -> Integer.valueOf(l.getFileName()) % 4 == 0);

		eval.addDataset(data);
		 
		// NearestNeighborScopeModel model = NearestNeighborScopeModel.fromIndex(new File("/Users/pk/Desktop/Location_Lab_Revisited/knn-scope-model-wikipedia-90-train"));
		// eval.addDetector(new KNearestNeighborScopeDetector(model, 1, MORE_LIKE_THIS_QUERY_CREATOR));
		// eval.addDetector(new KNearestNeighborScopeDetector(model, 3, MORE_LIKE_THIS_QUERY_CREATOR));
		// eval.addDetector(new KNearestNeighborScopeDetector(model, 5, MORE_LIKE_THIS_QUERY_CREATOR));
		// eval.addDetector(new KNearestNeighborScopeDetector(model, 10, MORE_LIKE_THIS_QUERY_CREATOR));
		 
		LocationSource source = new LuceneLocationSource(FSDirectory.open(Paths.get("/Users/pk/Desktop/Location_Lab_Revisited/Palladian_Location_Database_2022-04-19_13-45-08")));
		// eval.addDetector(new FirstScopeDetector(new PalladianLocationExtractor(source, new HeuristicDisambiguation())));
		// eval.addDetector(new FrequencyScopeDetector(new PalladianLocationExtractor(source, new HeuristicDisambiguation())));
		// eval.addDetector(new HighestPopulationScopeDetector(new PalladianLocationExtractor(source, new HeuristicDisambiguation())));
		// eval.addDetector(new LeastDistanceScopeDetector(new PalladianLocationExtractor(source, new HeuristicDisambiguation())));
		// eval.addDetector(new MidpointScopeDetector(new PalladianLocationExtractor(source, new HeuristicDisambiguation())));

		// QuickDtModel model1 = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/disambiguation-models/locationDisambiguationModel-tudLoc2013-100trees.ser.gz");
		// eval.addDetector(new LeastDistanceScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model1, 0))));
		// eval.addDetector(new FrequencyScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model1, 0))));
		// eval.addDetector(new FirstScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model1, 0))));
		// eval.addDetector(new HighestTrustScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model1, 0))));
		// QuickDtModel model2 = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/disambiguation-models/locationDisambiguationModel-lgl-100trees.ser.gz");
		// eval.addDetector(new LeastDistanceScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model2, 0))));
		// QuickDtModel model3 = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/disambiguation-models/locationDisambiguationModel-clust-100trees.ser.gz");
		// eval.addDetector(new LeastDistanceScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model3, 0))));
		// QuickDtModel model4 = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/disambiguation-models/locationDisambiguationModel-all-100trees.ser.gz");
		// eval.addDetector(new LeastDistanceScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model4, 0))));
		
//		DictionaryScopeModel dictionaryScopeModel = FileHelper.deserialize("/Users/pk/Repositories/palladian/palladian-experimental/enwiki-20220501-locationDictionary-1-word-0.3515625-min50.ser.gz");
		DictionaryScopeModel dictionaryScopeModel = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/enwiki-20220501-locationDictionary-1-word-0.3515625.ser.gz");
//		MultiStepDictionaryScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(dictionaryScopeModel, 22.5, 2.8125);
		// +++ MultiStepDictionaryScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(dictionaryScopeModel, new BayesScorer(BayesScorer.Options.LAPLACE, BayesScorer.Options.FREQUENCIES, BayesScorer.Options.COMPLEMENT), 22.5, 5.625, 1.40625);
		// --- MultiStepDictionaryScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(dictionaryScopeModel, new BayesScorer(BayesScorer.Options.FREQUENCIES, BayesScorer.Options.COMPLEMENT), 22.5, 5.625, 1.40625);
		// --- MultiStepDictionaryScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(dictionaryScopeModel, new BayesScorer(BayesScorer.Options.LAPLACE, BayesScorer.Options.FREQUENCIES), 22.5, 5.625, 1.40625);
		// MultiStepDictionaryScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(dictionaryScopeModel, new BayesScorer(BayesScorer.Options.FREQUENCIES, BayesScorer.Options.COMPLEMENT), 22.5, 5.625, 1.40625);
		MultiStepDictionaryScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(dictionaryScopeModel, new BayesScorer(BayesScorer.Options.FREQUENCIES, BayesScorer.Options.COMPLEMENT), 22.5, 5.625, 1.40625);
		eval.addDetector(scopeDetector);
//		
//		MultiStepDictionaryScopeDetector scopeDetector2 = new MultiStepDictionaryScopeDetector(dictionaryScopeModel, PalladianTextClassifier.DEFAULT_SCORER, 11.25, 5.625, 2.8125, 1.40625, 0.703125);
//		eval.addDetector(scopeDetector2);
		
//		MultiStepDictionaryScopeDetector scopeDetector3 = new MultiStepDictionaryScopeDetector(dictionaryScopeModel, new ExperimentalScorers.LogCountScorer(), 11.25, 5.625, 2.8125, 1.40625, 0.703125);
//		eval.addDetector(scopeDetector3);
		
		eval.runAll(true);
	}

	// compare different location extractors and scope detectors to see, how often we're able to extract a location at all 
	// (1) use location extractor to get ALL locations and see if one is within 100 km distance of the reference location (this is basically “as good as it gets”)
	// (2) if (1) fails, try text classification-based approaches “dictionary” and “KNN”
    private static void analyzeScopeDetection(AtlasObscuraDatasetReader reader, int limit) throws IOException {
        // int thresholdKm = 100;
        int thresholdKm = 161;
        
        Iterable<LocationDocument> data = CollectionHelper.limit(reader, limit);
        
        LocationSource source = new LuceneLocationSource(FSDirectory.open(Paths.get("/Users/pk/Desktop/Location_Lab_Revisited/Palladian_Location_Database_2022-04-19_13-45-08")));
        // HeuristicDisambiguation disambiguation = new HeuristicDisambiguation(); // 76 %
        FeatureBasedDisambiguation disambiguation = new FeatureBasedDisambiguation(FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/disambiguation-models/locationDisambiguationModel-tudLoc2013-100trees.ser.gz"), 0); // 86 %
        PalladianLocationExtractor locationExtractor = new PalladianLocationExtractor(source, disambiguation);

        NearestNeighborScopeModel nnModel = NearestNeighborScopeModel.fromIndex(new File("/Users/pk/Desktop/Location_Lab_Revisited/knn-scope-model-wikipedia-90-train"));
        KNearestNeighborScopeDetector nearestNeighborScopeDetector = new KNearestNeighborScopeDetector(nnModel, 1, MORE_LIKE_THIS_QUERY_CREATOR);
        
        DictionaryScopeModel dictionaryModel = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/enwiki-20220501-locationDictionary-1-word-0.3515625.ser.gz");
        MultiStepDictionaryScopeDetector dictionaryScopeDetector = new MultiStepDictionaryScopeDetector(dictionaryModel, new BayesScorer(BayesScorer.Options.FREQUENCIES, BayesScorer.Options.COMPLEMENT, BayesScorer.Options.LAPLACE), 22.5, 5.625, 1.40625);
        
        int matchedDocuments = 0;
        int matchedConditionalKnn = 0;
        int matchedConditionalDict = 0;
        int matchedConditional = 0;
        for (LocationDocument doc : data) {
            List<LocationAnnotation> annotations = locationExtractor.getAnnotations(doc.getText());
            long numMatches = annotations.stream() //
                    .map(LocationAnnotation::getLocation) //
                    .map(Location::getCoords) //
                    .filter(c -> c.isPresent()) //
                    .filter(c -> c.get().distance(doc.getMainLocation().getCoordinate()) < thresholdKm) //
                    .count(); //
            if (numMatches > 0) {
                matchedDocuments++;
            } else {
                System.out.println("=== location extractor was not able to get location, trying text classification ===");
                System.out.println(doc);
                System.out.println(doc.getText());
                System.out.println(annotations);
                GeoCoordinate knnDcope = nearestNeighborScopeDetector.getScope(doc.getText());
                GeoCoordinate dictScope = dictionaryScopeDetector.getScope(doc.getText());
                System.out.println("knn >>> " + knnDcope.distance(doc.getMainLocation().getCoordinate()));
                System.out.println("dict >>> " + dictScope.distance(doc.getMainLocation().getCoordinate()));
                boolean knnSuccess = knnDcope.distance(doc.getMainLocation().getCoordinate()) < thresholdKm;
                boolean dictSuccess = dictScope.distance(doc.getMainLocation().getCoordinate()) < thresholdKm;
                if (knnSuccess) {
                    matchedConditionalKnn++;
                }
                if (dictSuccess) {
                    matchedConditionalDict++;
                }
                if (knnSuccess || dictSuccess) {
                    matchedConditional++;
                }

                System.out.println("===");
            }
        }
        System.out.println("%/documents where PLE result is close to main location: "+ (double) matchedDocuments / limit);
        System.out.println("#/documents where PLE failed but KNN succeeded: " + matchedConditionalKnn);
        System.out.println("#/documents where PLE failed but Dictionary succeeded: " + matchedConditionalDict);
        System.out.println("#/documents where PLE failed but Dictionary/KNN succeeded: " + matchedConditional);
        System.out.println("%/documents where PLE failed but Dictionary/KNN succeeded: " + (double) matchedConditional / (limit - matchedDocuments));
        nearestNeighborScopeDetector.close();
    }

}
