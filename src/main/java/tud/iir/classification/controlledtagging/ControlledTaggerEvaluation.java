package tud.iir.classification.controlledtagging;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.apache.commons.collections15.Bag;

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
    private int testLimit = 500;

    public ControlledTaggerEvaluation() {

        // tagger = ControlledTagger.load("data/tagger_data_2010-08-08.ser");

        // tagger.addToVocabularyFromFile("data/delicious_tags_t140.txt");
        // tagger.addToVocabularyFromFile("data/delicious_tags_t140_stemmed.txt");
        // tagger.addToVocabularyFromFile("data/delicious_tags_v30_raw.txt");
        // tagger.setUsePriors(true);
        // tagger.setWordCorrelationMatrix((WordCorrelationMatrix) FileHelper.deserialize("data/wcm.ser"));
        // XXX tagger.setWordCorrelationMatrix((WordCorrelationMatrix) FileHelper.deserialize("data/wcm_2.ser"));
        // tagger.setWordCorrelationMatrix((WordCorrelationMatrix)
        // FileHelper.deserialize("data/wcm_stem_1281199979403.ser"));
        // tagger.calculateStems();

        // ///// using no correlation
        // tagger.setCorrelationType(ControlledTagger.TaggingCorrelationType.NO_CORRELATIONS);
        // tagger.setTfidfThreshold(0.001f); --> average pr:0,16 rc:0,44 f1:0,24
        // tagger.setTfidfThreshold(0.002f); --> average pr:0,24 rc:0,37 f1:0,29
        // tagger.setTfidfThreshold(0.004f); --> average pr:0,34 rc:0,26 f1:0,29
        // tagger.setTfidfThreshold(0.005f); --> average pr:0,39 rc:0,23 f1:0,29
        // tagger.setTfidfThreshold(0.007f); --> average pr:0,49 rc:0,18 f1:0,27
        // tagger.setTfidfThreshold(0.008f); --> average pr:0,54 rc:0,16 f1:0,25
        // tagger.setTfidfThreshold(0.01f); --> average pr:0,53 rc:0,13 f1:0,21

        // //// using shallow correlation
        // tagger.setTfidfThreshold(0.007f);
        // tagger.setCorrelationType(ControlledTagger.TaggingCorrelationType.SHALLOW_CORRELATIONS);
        // tagger.setCorrelationWeight(2); --> average pr:0,42 rc:0,26 f1:0,32
        // tagger.setCorrelationWeight(3); --> average pr:0,38 rc:0,28 f1:0,32
        // tagger.setCorrelationWeight(5); --> average pr:0,35 rc:0,33 f1:0,34
        // tagger.setCorrelationWeight(8); --> average pr:0,32 rc:0,37 f1:0,34
        // tagger.setCorrelationWeight(10); --> average pr:0,30 rc:0,38 f1:0,34
        // tagger.setCorrelationWeight(20); --> average pr:0,26 rc:0,41 f1:0,32
        // tagger.setCorrelationWeight(40); --> average pr:0,24 rc:0,44 f1:0,31

        // //// using deep correlation
        // tagger.setTfidfThreshold(0.007f);
        // tagger.setCorrelationType(ControlledTagger.TaggingCorrelationType.DEEP_CORRELATIONS);
        // tagger.setCorrelationWeight(80); --> average pr:0,22 rc:0,45 f1:0,30
        // tagger.setCorrelationWeight(40); --> average pr:0,35 rc:0,38 f1:0,36
        // tagger.setCorrelationWeight(30); --> average pr:0,39 rc:0,33 f1:0,36
        // tagger.setCorrelationWeight(20); --> average pr:0,43 rc:0,27 f1:0,33

        // /// using deep correlation with different thresholds
        // tagger.setTfidfThreshold(0.004f); --> average pr:0,21 rc:0,46 f1:0,29
        // tagger.setTfidfThreshold(0.005f); --> average pr:0,26 rc:0,44 f1:0,32
        // tagger.setTfidfThreshold(0.007f); --> average pr:0,35 rc:0,38 f1:0,36
        // tagger.setTfidfThreshold(0.008f); --> average pr:0,39 rc:0,35 f1:0,37
        // tagger.setTfidfThreshold(0.009f); --> average pr:0,43 rc:0,31 f1:0,36
        // tagger.setCorrelationType(ControlledTagger.TaggingCorrelationType.DEEP_CORRELATIONS);
        // tagger.setCorrelationWeight(40);

        /**
         * old values:
         * 
         * 
         * 0.01 average pr:0,45 rc:0,19 f1:0,27
         * 0.008 average pr:0,42 rc:0,22 f1:0,29
         * 0.007 average pr:0,40 rc:0,24 f1:0,30
         * 0.006 average pr:0,36 rc:0,26 f1:0,30
         * 0.005 average pr:0,33 rc:0,28 f1:0,30
         * 0.001 average pr:0,12 rc:0,44 f1:0,19
         * 
         * 
         * Using David's tag normalization:
         * average pr:0,33 rc:0,29 f1:0,31
         * 
         * 
         * average pr:0,45 rc:0,20 f1:0,28
         * average pr:0,42 rc:0,21 f1:0,28
         * 
         * 
         */

        // tagger.setTaggingType(TaggingType.THRESHOLD);
        // tagger.setTfidfThreshold(0.008f);
        tagger.setTaggingType(TaggingType.FIXED_COUNT);
        tagger.setTagCount(10);
        tagger.setPriorWeight(1.0f);
        tagger.setCorrelationType(TaggingCorrelationType.DEEP_CORRELATIONS);
        tagger.setCorrelationWeight(25);
        tagger.setFastMode(true);

        // without gerund normalization -- average pr:0,38 rc:0,33 f1:0,35
        // with gerund normalization -- average pr:0,38 rc:0,32 f1:0,35
        // average pr:0,30 rc:0,40 f1:0,34

        DatasetFilter filter = new DatasetFilter();
        filter.addAllowedFiletype("html");
        filter.setMinUsers(50);
        reader.setFilter(filter);
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
                // tagger.addToIndex(content);
                tagger.train(content, entry.getTags());
                if (++counter[0] % 100 == 0) {
                    System.out.println(counter[0]);
                }
            }
        };
        reader.read(callback, trainLimit, trainOffset);
        System.out.println("trained with " + counter[0] + " documents.");

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
                // Bag<String> realTagsNormalized = tagger.singularPluralNormalization(realTags);
                Bag<String> realTagsNormalized = tagger.normalize(realTags);

                // List<Tag> assignedTags = tagger.assignTags(content);
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
        evaluation.train();
        evaluation.evaluate();

    }

}
