package tud.iir.classification.controlledtagging;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.apache.commons.collections15.Bag;

import tud.iir.classification.Stopwords;
import tud.iir.classification.controlledtagging.ControlledTagger.TaggingCorrelationType;
import tud.iir.classification.controlledtagging.ControlledTagger.TaggingType;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetCallback;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetEntry;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetFilter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StopWatch;
import tud.iir.news.Helper;

/**
 * Evaluator for the {@link ControlledTagger} using the delicious data set T140.
 * 
 * Important: VM args -Xmx1024M
 * 
 * @author Philipp Katz
 */
public class ControlledTaggerEvaluation {

    private ControlledTagger tagger = new ControlledTagger();
    private DeliciousDatasetReader reader = new DeliciousDatasetReader();

    private int trainOffset = 0;
    private int trainLimit = 20000;

    private int testOffset = 50000;
    private int testLimit = 5000;

    public ControlledTaggerEvaluation() {

        // ControlledTagger tagger = new ControlledTagger();
        // tagger.load("data/controlledTagger20000_T140.ser");
        // tagger.load("/Users/pk/Studium/Diplomarbeit/workspace/newsseecr/data/controlledTagger40000_David_semantic.ser"); --> use 40.000 correlation
        tagger.load("/Users/pk/Studium/Diplomarbeit/workspace/newsseecr/data/controlledTagger20000_David_semantic.ser");

        // ////////////// tagging with threshold
        // tagger.setTaggingType(TaggingType.THRESHOLD);
        // tagger.setTfidfThreshold(0.008f);

        // ////////////// tagging with fixed tag count
        tagger.setTaggingType(TaggingType.FIXED_COUNT);
        tagger.setTagCount(10);
        // tagger.setTagCount(100);

        // ///////////// general settings
        tagger.setPriorWeight(1.0f);
        tagger.setCorrelationType(TaggingCorrelationType.DEEP_CORRELATIONS);
        tagger.setCorrelationWeight(30000);
        tagger.setStopwords(new Stopwords(Stopwords.STOP_WORDS_EN));

        DatasetFilter filter = new DatasetFilter();
        filter.addAllowedFiletype("html");
        filter.setMinUsers(50);
        filter.setMaxFileSize(600000);
        reader.setFilter(filter);

        tagger.writeDataToReport();

    }

    private void train() {

        final int[] counter = new int[] { 0 };

        DatasetCallback callback = new DatasetCallback() {

            @Override
            public void callback(DatasetEntry entry) {

                String content = FileHelper.readFileToString(entry.getPath());
                content = HTMLHelper.htmlToString(content, true);

                tagger.train(content, entry.getTags());
                if (++counter[0] % 100 == 0) {
                    System.out.println(counter[0]);
                }
            }
        };
        reader.read(callback, trainLimit, trainOffset);
        System.out.println("trained with " + counter[0] + " documents.");

        // save the trained tagger

        tagger.save("data/controlledTagger20000_T140.ser");

    }

    private void evaluate() {

        final double[] stats = new double[4];
        final NumberFormat format = new DecimalFormat("0.00");
        StopWatch sw = new StopWatch();

        DatasetCallback callback = new DatasetCallback() {

            @Override
            public void callback(DatasetEntry entry) {

                String content = FileHelper.readFileToString(entry.getPath());
                content = HTMLHelper.htmlToString(content, true);
                
                Bag<String> realTags = entry.getTags();
                Bag<String> realTagsNormalized = tagger.normalize(realTags);

                List<Tag> assignedTags = tagger.tag(content);

                int correctlyAssigned = 0;
                for (Tag assignedTag : assignedTags) {
                    for (String realTag : realTagsNormalized.uniqueSet()) {
                        if (assignedTag.getName().equals(realTag)) {
                            correctlyAssigned++;
                        }
                    }
                }

                int totalAssigned = assignedTags.size();
                int realCount = realTagsNormalized.uniqueSet().size();

                double precision = (double) correctlyAssigned / totalAssigned;
                if (Double.isNaN(precision)) {
                    precision = 0;
                }
                double recall = (double) correctlyAssigned / realCount;

                stats[0]++;
                stats[1] += precision;
                stats[2] += recall;
                stats[3] += totalAssigned;

                System.out.println("doc: " + Helper.getFirstWords(content, 10));
                System.out.println("real tags: " + realTagsNormalized);
                System.out.println("assigned tags: " + assignedTags);
                System.out.println("pr:" + format.format(precision) + " rc:" + format.format(recall));

                assert precision <= 1.0;
                assert recall <= 1.0;

            }
        };
        reader.read(callback, testLimit, testOffset);

        double averagePrecision = stats[1] / stats[0];
        double averageRecall = stats[2] / stats[0];
        double averageFOne = 2 * averagePrecision * averageRecall / (averagePrecision + averageRecall);
        double averageTagCount = stats[3] / stats[0];

        System.out.println("---------------------------------------------------------------------");
        System.out.println("average pr: " + format.format(averagePrecision) + " rc: " + format.format(averageRecall)
                + " f1: " + format.format(averageFOne));
        System.out.println("average # assigned tags: " + format.format(averageTagCount));
        System.out.println("tagged entries: " + (int) stats[0]);
        System.out.println("elapsed time for tagging: " + sw.getElapsedTimeString());

        // System.out.println(counter[0]);

    }

    public static void main(String[] args) {

        ControlledTaggerEvaluation evaluation = new ControlledTaggerEvaluation();
        // evaluation.train();
        evaluation.evaluate();

    }

}
