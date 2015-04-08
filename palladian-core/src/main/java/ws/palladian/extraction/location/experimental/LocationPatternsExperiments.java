package ws.palladian.extraction.location.experimental;

import java.io.File;
import java.util.List;

import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.evaluation.TudLoc2013DatasetIterable;

public class LocationPatternsExperiments {

    public static void main(String[] args) {
        // File datasetDirectory = new File("/Users/pk/Documents/tud-loc-2013/1-training");
        File datasetDirectory = new File("/Users/pk/temp/LGL-converted/1-train");
        TudLoc2013DatasetIterable dataset = new TudLoc2013DatasetIterable(datasetDirectory);
        for (LocationDocument document : dataset) {
            List<LocationAnnotation> annotations = document.getAnnotations();
            LocationAnnotation previous = null;
            for (LocationAnnotation current : annotations) {
                if (previous != null) {
                    int currentStart = current.getStartPosition();
                    int previousEnd = previous.getEndPosition();
                    int distance = currentStart - previousEnd;
                    if (distance < 20) {
                        String span = document.getText().substring(previous.getStartPosition(),
                                current.getEndPosition());
                        if (span.contains("\n") || span.contains(". ") || span.contains("? ") || span.contains("! ")) {
                            continue;
                        }
                        System.out.println(span);
                    }
                }
                previous = current;
            }
        }
    }

}
