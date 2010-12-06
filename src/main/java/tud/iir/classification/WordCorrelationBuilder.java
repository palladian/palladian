package tud.iir.classification;

import java.util.Set;

import tud.iir.classification.controlledtagging.DeliciousDatasetReader;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetCallback;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetEntry;
import tud.iir.helper.Counter;
import tud.iir.helper.StopWatch;

/**
 * Performance testing for WCM.
 * 
 * @author Philipp Katz
 * 
 */
public class WordCorrelationBuilder {

    public static void main(String[] args) {

        StopWatch sw = new StopWatch();
        final WordCorrelationMatrix wcm;
        final Counter counter = new Counter();

        // ////////// different implementations ////////////
        // wcm = new WordCorrelationMatrix();
        // wcm = new FastWordCorrelationMatrix();
        // wcm = new UJMPWordCorrelationMatrix();
        wcm = new MKMWordCorrelationMatrix();

        // create the correlations
        DeliciousDatasetReader reader = new DeliciousDatasetReader();
        DatasetCallback callback = new DatasetCallback() {

            @Override
            public void callback(DatasetEntry entry) {

                Set<String> tags = entry.getTags().uniqueSet();
                wcm.updateGroup(tags);

                counter.increment();
                if (counter.getCount() % 1000 == 0) {
                    System.out.println(counter);
                }

            }
        };
        reader.read(callback, 30000);
        System.out.println("created wcm in " + sw.getElapsedTimeString());

        // calculate relative scores
        sw.start();
        wcm.makeRelativeScores();
        System.out.println("made relative scores in " + sw.getElapsedTimeString());

        // retrieve some correlations ...
        sw.start();
        for (int i = 0; i < 10000; i++) {
            wcm.getCorrelation("python", "standards");
            wcm.getCorrelation("notes", "programming");
            wcm.getCorrelation("opensource", "style");
            wcm.getCorrelation("optimization", "python");
        }
        System.out.println("retrieved correlations 1 in " + sw.getElapsedTimeString());

        sw.start();
        for (int i = 0; i < 10000; i++) {
            wcm.getCorrelations("standards", 10);
            wcm.getCorrelations("gridcomputing", 10);
            wcm.getCorrelations("packer", 10);
            wcm.getCorrelations("shrink", 10);
            wcm.getCorrelations("fuzed", 10);
        }
        System.out.println("retrieved correlations 2 in " + sw.getElapsedTimeString());

        // wcm.showGui();

    }

}
