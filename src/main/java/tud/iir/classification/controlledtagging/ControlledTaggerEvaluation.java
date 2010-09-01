package tud.iir.classification.controlledtagging;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections15.Bag;

import tud.iir.classification.Stopwords;
import tud.iir.classification.controlledtagging.ControlledTaggerSettings.TaggingCorrelationType;
import tud.iir.classification.controlledtagging.ControlledTaggerSettings.TaggingType;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.news.Helper;

/**
 * Evaluator for the {@link ControlledTagger} using the delicious data set T140.
 * 
 * Important: VM args -Xmx1024M
 * 
 * @author Philipp Katz
 */
public class ControlledTaggerEvaluation extends DeliciousDatasetSplitter {
    
    private ControlledTagger tagger;
    private ControlledTaggerEvaluationResult evaluationResult;

    private NumberFormat format = new DecimalFormat("0.00");

    public ControlledTaggerEvaluation() {

    }
    
    /**
     * Evaluate with the specified {@link ControlledTaggerEvaluationSettings}.
     */
    public ControlledTaggerEvaluationResult evaluate(ControlledTaggerEvaluationSettings settings) {
        
        System.out.println("evaluating with " + settings);
        
        evaluationResult = new ControlledTaggerEvaluationResult();
        
        tagger = new ControlledTagger();
        tagger.setSettings(settings);
        setTrainLimit(settings.getTrainLimit());
        setTestLimit(settings.getTestLimit());
        
        read();
        
        return evaluationResult;
        
    }
    
    /**
     * Do evaluation with a list of different settings, save result to textfile.
     * The result file contains one line for each evaluation step with settings and corresponding results.
     * 
     * @param settings
     * @param resultFilePath
     */
    public void evaluate(List<ControlledTaggerEvaluationSettings> settings, String resultFilePath) {
        
        System.out.println("start evaluation for " + settings.size() + " settings");
        
        for (ControlledTaggerEvaluationSettings setting : settings) {
            
            ControlledTaggerEvaluationResult result = evaluate(setting);
            StringBuilder fileLine = new StringBuilder();
            fileLine.append(setting.toString());
            fileLine.append(",");
            fileLine.append(result.toString());
            fileLine.append("\n");
            
            try {
                FileHelper.appendFile(resultFilePath, fileLine.toString());
            } catch (IOException e) {
                System.out.println("could not write file");
            }
        }
        
    }

    @Override
    public void train(DatasetEntry entry, int index) {

        String content = FileHelper.readFileToString(entry.getPath());
        content = HTMLHelper.htmlToString(content, true);

        tagger.train(content, entry.getTags());

    }

    @Override
    public void test(DatasetEntry entry, int index) {

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

        evaluationResult.addTestResult(precision, recall, totalAssigned);

        System.out.println("doc: " + Helper.getFirstWords(content, 10));
        System.out.println("real tags: " + realTagsNormalized);
        System.out.println("assigned tags: " + assignedTags);
        System.out.println("pr:" + format.format(precision) + " rc:" + format.format(recall));

        assert precision <= 1.0;
        assert recall <= 1.0;

    }

    @Override
    public void startTrain() {
        evaluationResult.startTraining();
    }

    @Override
    public void finishTrain() {
        evaluationResult.stopTraining();
    }

    @Override
    public void startTest() {
        evaluationResult.startTesting();
    }

    @Override
    public void finishTest() {
        evaluationResult.stopTesting();
        evaluationResult.printStatistics();
    }
    
    public ControlledTaggerEvaluationResult getEvaluationResult() {
        return evaluationResult;
    }

    public static void main(String[] args) {
        ControlledTaggerEvaluation evaluation = new ControlledTaggerEvaluation();
        
        // general filter for the data set
        DatasetFilter filter = new DatasetFilter();
        filter.addAllowedFiletype("html");
        filter.setMinUsers(50);
        filter.setMaxFileSize(600000);
        evaluation.setFilter(filter);        
        
        // parameters : trainSize, testSize, tagType, correlationType, tfidfThreshold, tagCount, correlationWeight, priorWeight
        
        //ControlledTaggerEvaluationSettings s1 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 15000, 1.0f);
        //ControlledTaggerEvaluationSettings s2 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 25000, 1.0f);
        
        ControlledTaggerEvaluationSettings s3 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 0.5f);
        ControlledTaggerEvaluationSettings s4 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 0.75f);
        ControlledTaggerEvaluationSettings s5 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.25f);
        ControlledTaggerEvaluationSettings s6 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.5f);
        
        // s1.setStopwords(new Stopwords(Stopwords.STOP_WORDS_EN));

        
        List<ControlledTaggerEvaluationSettings> settings = Arrays.asList(s3, s4, s5, s6);

        evaluation.evaluate(settings, "data/ControlledTaggerEvaluationResult.txt");

    }

}
