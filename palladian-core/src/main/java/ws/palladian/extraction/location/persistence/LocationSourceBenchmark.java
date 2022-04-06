package ws.palladian.extraction.location.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.evaluation.TudLoc2013DatasetIterable;
import ws.palladian.extraction.location.persistences.h2.H2LocationSource;
import ws.palladian.extraction.location.persistences.sqlite.SQLiteLocationSource;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.StopWatch;

public class LocationSourceBenchmark {

	private static final File DATASET = new File("/private/tmp/palladian_pk-tud-loc-2013-8528ef934a0a/0-all");

	private static final int REPEATS = 1;

	public static void main(String[] args) {

		List<LocationSource> sources = new ArrayList<LocationSource>();
		sources.add(H2LocationSource.open(new File("/private/tmp/locations_h2.mv.db")));
		sources.add(SQLiteLocationSource.open(new File("/Users/pk/Desktop/locations_final.sqlite")));

		for (LocationSource source : sources) {
			StopWatch stopWatch = new StopWatch();
			for (int i = 0; i < REPEATS; i++) {
				runWithTestData(source);
			}
			System.out.println(source.toString() + " : " + stopWatch);
		}
	}

	private static void runWithTestData(LocationSource source) {
		TudLoc2013DatasetIterable dataset = new TudLoc2013DatasetIterable(DATASET, () -> NoProgress.INSTANCE);
		PalladianLocationExtractor extractor = new PalladianLocationExtractor(source);
		for (LocationDocument doc : dataset) {
			extractor.getAnnotations(doc.getText());
		}
	}

}
