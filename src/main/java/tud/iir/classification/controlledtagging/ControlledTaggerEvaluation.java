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
    
    private ControlledTagger tagger = new ControlledTagger();
    private ControlledTaggerEvaluationResult evaluationResult;

    private NumberFormat format = new DecimalFormat("0.00");

    public ControlledTaggerEvaluation() {

    }
    
    public ControlledTaggerEvaluationResult evaluate(int trainLimit, int testLimit, ControlledTaggerSettings settings) {
        
        System.out.println("evaluating with " + settings);
        
        evaluationResult = new ControlledTaggerEvaluationResult();
        
        tagger.setSettings(settings);        
        setTrainLimit(trainLimit);
        setTestLimit(testLimit);
        
        read();
        
        return evaluationResult;
        
    }
    
    public ControlledTaggerEvaluationResult evaluate(ControlledTaggerEvaluationSettings settings) {
        
        System.out.println("evaluating with " + settings);
        
        evaluationResult = new ControlledTaggerEvaluationResult();
        
        tagger.setSettings(settings);
        setTrainLimit(settings.getTrainLimit());
        setTestLimit(settings.getTestLimit());
        
        read();
        
        return evaluationResult;
        
    }
    
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
        
        // ////// set up the tagger ////////
        // ControlledTagger tagger = new ControlledTagger();
        // tagger.load("data/controlledTagger20000_T140.ser");
        // tagger.load("/Users/pk/Studium/Diplomarbeit/workspace/newsseecr/data/controlledTagger40000_David_semantic.ser");
        // --> use 40.000 correlation
        // tagger.load("/Users/pk/Studium/Diplomarbeit/workspace/newsseecr/data/controlledTagger20000_David_semantic.ser");
        
        ControlledTaggerEvaluationSettings s1 = new ControlledTaggerEvaluationSettings(1000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 30000, 1.0f);
        ControlledTaggerEvaluationSettings s2 = new ControlledTaggerEvaluationSettings(2000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 30000, 1.0f);
        ControlledTaggerEvaluationSettings s3 = new ControlledTaggerEvaluationSettings(3000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 30000, 1.0f);
        ControlledTaggerEvaluationSettings s4 = new ControlledTaggerEvaluationSettings(5000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 30000, 1.0f);
        ControlledTaggerEvaluationSettings s5 = new ControlledTaggerEvaluationSettings(6000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 30000, 1.0f);
        
        List<ControlledTaggerEvaluationSettings> settings = Arrays.asList(s1, s2, s3, s4, s5);
        
        evaluation.evaluate(settings, "data/ControlledTaggerEvaluationResult.txt");



        // 1000/1000 average pr: 0,46 rc: 0,22 f1: 0,30
        // 2000/1000 average pr: 0,47 rc: 0,23 f1: 0,31
        // 3000/1000 average pr: 0,48 rc: 0,23 f1: 0,31
        // 5000/1000 average pr: 0,48 rc: 0,23 f1: 0,31
        // 10000/1000 average pr: 0,48 rc: 0,23 f1: 0,31
        // 20000/1000 average pr: 0,49 rc: 0,24 f1: 0,32
        // 20000/20000 average pr: 0,50 rc: 0,25 f1: 0,33
        // 40000/1000 average pr: 0,49 rc: 0,25 f1: 0,33
        // 60000/1000 average pr: 0,49 rc: 0,24 f1: 0,32

        
//        // ////////////// tagging with fixed tag count /////////////////
//        ControlledTaggerEvaluationSettings settings = new ControlledTaggerEvaluationSettings();
//        settings.setTaggingType(TaggingType.FIXED_COUNT);
//        settings.setTagCount(10);
//        // tagger.setTagCount(100);
//
//        // ///////////// general settings ////////////////////
//        settings.setPriorWeight(1.0f);
//        settings.setCorrelationType(TaggingCorrelationType.DEEP_CORRELATIONS);
//        settings.setCorrelationWeight(30000);
//        settings.setStopwords(new Stopwords(Stopwords.STOP_WORDS_EN));
//        
//        // ////////////// evaluation specific ///////////////////
//        settings.setTrainLimit(1000);
//        settings.setTestLimit(1000);
//        
//        ControlledTaggerEvaluationResult result = evaluation.evaluate(s1);
//        System.out.println(result);

        // ////////////////////////////////////////////////////
        // evaluation.read();
    }

}
