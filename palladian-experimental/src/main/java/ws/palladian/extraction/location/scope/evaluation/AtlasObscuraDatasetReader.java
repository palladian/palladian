package ws.palladian.extraction.location.scope.evaluation;

import static ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.MORE_LIKE_THIS_QUERY_CREATOR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

import org.apache.lucene.store.FSDirectory;

import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguation;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.evaluation.ImmutableLocationDocument;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.persistence.lucene.LuceneLocationSource;
import ws.palladian.extraction.location.scope.FirstScopeDetector;
import ws.palladian.extraction.location.scope.FrequencyScopeDetector;
import ws.palladian.extraction.location.scope.HighestPopulationScopeDetector;
import ws.palladian.extraction.location.scope.HighestTrustScopeDetector;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.NearestNeighborScopeModel;
import ws.palladian.extraction.location.scope.LeastDistanceScopeDetector;
import ws.palladian.extraction.location.scope.MidpointScopeDetector;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineIterator;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

public class AtlasObscuraDatasetReader implements Iterable<LocationDocument> {

	private final class AtlasObscuraJsonIterator extends AbstractIterator2<LocationDocument> implements CloseableIterator<LocationDocument> {
		private final LineIterator lineIterator;

		private AtlasObscuraJsonIterator(File jsonPath) {
			this.lineIterator = new LineIterator(jsonPath);
		}

		@Override
		protected LocationDocument getNext() {

			while (lineIterator.hasNext()) {

				try {
					String line = lineIterator.next();
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

	public static void main(String[] args) throws IOException {
		AtlasObscuraDatasetReader reader = new AtlasObscuraDatasetReader(new File("/Users/pk/Desktop/atlas-obscura-crawler/places.json"));

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

		QuickDtModel model1 = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/disambiguation-models/locationDisambiguationModel-tudLoc2013-100trees.ser.gz");
		// eval.addDetector(new LeastDistanceScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model1, 0))));
		// eval.addDetector(new FrequencyScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model1, 0))));
		// eval.addDetector(new FirstScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model1, 0))));
		eval.addDetector(new HighestTrustScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model1, 0))));
		// QuickDtModel model2 = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/disambiguation-models/locationDisambiguationModel-lgl-100trees.ser.gz");
		// eval.addDetector(new LeastDistanceScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model2, 0))));
		// QuickDtModel model3 = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/disambiguation-models/locationDisambiguationModel-clust-100trees.ser.gz");
		// eval.addDetector(new LeastDistanceScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model3, 0))));
		// QuickDtModel model4 = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/disambiguation-models/locationDisambiguationModel-all-100trees.ser.gz");
		// eval.addDetector(new LeastDistanceScopeDetector(new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model4, 0))));

		eval.runAll(true);
	}

}
