package tud.iir.classification.controlledtagging;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
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
 * Evaluator for the {@link ControlledTagger} using delicious data set.
 * 
 * @author Philipp Katz
 */
public class ControlledTaggerEvaluation {

    private ControlledTagger tagger = new ControlledTagger();
    private DeliciousDatasetReader reader = new DeliciousDatasetReader();

    private int trainOffset = 0;
    private int trainLimit = 20000;

    private int testOffset = 60000;
    private int testLimit = 1000;

    public ControlledTaggerEvaluation() {
        
        tagger = ControlledTagger.load("data/controlledTagger20000_David_idf.ser");

        // ////////////// tagging with threshold
        // tagger.setTaggingType(TaggingType.THRESHOLD);
        // tagger.setTfidfThreshold(0.008f);

        // ////////////// tagging with fixed tag count
        tagger.setTaggingType(TaggingType.FIXED_COUNT);
        tagger.setTagCount(10);

        // ///////////// general settings
        tagger.setPriorWeight(1.0f);
        tagger.setCorrelationType(TaggingCorrelationType.DEEP_CORRELATIONS);
        tagger.setCorrelationWeight(25);
        tagger.setFastMode(true);

        
        // tagger.stopwords = new Stopwords(Stopwords.STOP_WORDS_EN);
        tagger.setStopwords(Collections.<String>emptySet());
        
        DatasetFilter filter = new DatasetFilter();
        filter.addAllowedFiletype("html");
        filter.setMinUsers(50);
        reader.setFilter(filter);
        
        
        
        
        
        
        
        // tagger.writeDataToReport();
        // correlationWeight(25) average pr:0,31 rc:0,16 f1:0,21
        // correlationWeight(40) average pr:0,33 rc:0,17 f1:0,22
        // correlationWeight(100) average pr:0,40 rc:0,20 f1:0,27
        // correlationWeight(200) average pr:0,44 rc:0,22 f1:0,29
        // correlationWeight(500) average pr:0,45 rc:0,23 f1:0,30


        // tagger.setCorrelationWeight(4000);
    }

    private void train() {

        final int[] counter = new int[] { 0 };

        DatasetCallback callback = new DatasetCallback() {

            @Override
            public void callback(DatasetEntry entry) {

                String content = FileHelper.readFileToString(entry.getPath());

                // There are some huuuuuge HTML files in the dataset which will cause the HTML parser to stall.
                // We will simply skip them here.
                if (content.length() > 600000) {
                    return;
                }

                content = HTMLHelper.htmlFragmentsToString(content, true);
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

        final int[] counter = new int[] { 0 };
        final double[] stats = new double[] { 0, 0 };
        final NumberFormat format = new DecimalFormat("0.00");
        StopWatch sw = new StopWatch();

        DatasetCallback callback = new DatasetCallback() {

            @Override
            public void callback(DatasetEntry entry) {

                String content = FileHelper.readFileToString(entry.getPath());
                if (content.length() > 600000) {
                    return;
                }

                content = HTMLHelper.htmlFragmentsToString(content, true);
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

                stats[0] += precision;
                stats[1] += recall;

                System.out.println("doc: " + Helper.getFirstWords(content, 10));
                System.out.println("real tags: " + realTagsNormalized);
                System.out.println("assigned tags: " + assignedTags);
                System.out.println("pr:" + format.format(precision) + " rc:" + format.format(recall));

                assert precision <= 1.0;
                assert recall <= 1.0;

                counter[0]++;

            }
        };
        reader.read(callback, testLimit, testOffset);

        double averagePrecision = stats[0] / counter[0];
        double averageRecall = stats[1] / counter[0];
        double averageFOne = 2 * averagePrecision * averageRecall / (averagePrecision + averageRecall);

        System.out.println("---------------------------------------------------------------------");
        System.out.println("average pr:" + format.format(averagePrecision) + " rc:" + format.format(averageRecall)
                + " f1:" + format.format(averageFOne));
        System.out.println(sw.getElapsedTimeString());

        // System.out.println(counter[0]);

    }

    public static void main(String[] args) {

        ControlledTaggerEvaluation evaluation = new ControlledTaggerEvaluation();
        // evaluation.train();
        evaluation.evaluate();

    }

}
