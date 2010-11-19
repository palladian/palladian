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
import tud.iir.extraction.entity.ner.evaluation.EvaluationResult;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StringHelper;

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

        System.out.println("doc: " + StringHelper.getFirstWords(content, 10));
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
    
    // just do an evaluation with "optimal" settings.
    // temporary method, remove after testing?
    public ControlledTaggerEvaluationResult justTest(int testLimit) {
        
        tagger = new ControlledTagger();
        //tagger.load("/home/pk/workspace/newsseecr/data/newsTagger.ser");
        tagger.load("data/models/controlledTaggerModel.ser");

        
        ControlledTaggerSettings settings = new ControlledTaggerSettings();
        settings.setTaggingType(TaggingType.FIXED_COUNT);
        settings.setTagCount(10);
        settings.setPriorWeight(1.0f);
        settings.setCorrelationType(TaggingCorrelationType.DEEP_CORRELATIONS);
        settings.setCorrelationWeight(20000);
        settings.setStopwords(new Stopwords(Stopwords.Predefined.EN));
        
        tagger.setSettings(settings);
        
        evaluationResult = new ControlledTaggerEvaluationResult();
        
        setTrainLimit(-1);
        setTestLimit(testLimit);
        
        read();
        
        System.out.println(evaluationResult);
        
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
        
        /*evaluation.justTest(10000);
        
        System.exit(0);*/
        
        // parameters : trainSize, testSize, tagType, correlationType, tfidfThreshold, tagCount, correlationWeight, priorWeight
        
        //ControlledTaggerEvaluationSettings s1 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 15000, 1.0f);
        //ControlledTaggerEvaluationSettings s2 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 25000, 1.0f);
        
        // ControlledTaggerEvaluationSettings s3 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 0.5f);
        // ControlledTaggerEvaluationSettings s4 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 0.75f);
        // ControlledTaggerEvaluationSettings s5 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.25f);
        // ControlledTaggerEvaluationSettings s6 = new ControlledTaggerEvaluationSettings(30000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.5f);
        
        // ControlledTaggerEvaluationSettings s = new ControlledTaggerEvaluationSettings(1000, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.NO_CORRELATIONS, 0, 10, 0, -1f);
        
        
        // ControlledTaggerEvaluationSettings:trainLimit:30000,testLimit:1000,ControlledTaggerSettings:taggingType=FIXED_COUNT,correlationType=DEEP_CORRELATIONS,tagCount=10,correlationWeight=20000.0,priorWeight=1.0,ControlledTaggerEvaluationResult:taggedEntries:1000,timeForTraining:40m:20s:180ms,timeForTesting:2m:13s:988ms,averageTagCount:9.811,averagePr:0.4935059523809526,averageRc:0.248121375737789,averageF1:0.33021807907257816

        //ControlledTaggerEvaluationSettings s1 = new ControlledTaggerEvaluationSettings(20000, 5000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.0f, 1);
        //ControlledTaggerEvaluationSettings s2 = new ControlledTaggerEvaluationSettings(20000, 5000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.0f, 2);
        //ControlledTaggerEvaluationSettings s3 = new ControlledTaggerEvaluationSettings(20000, 5000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.0f, 3);
        ControlledTaggerEvaluationSettings s4 = new ControlledTaggerEvaluationSettings(20000, 5000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.0f, 4);

        // with memorysavingfix ------> 20.000 -> average pr: 0,49 rc: 0,24 f1: 0,33
        // without -------------------> average pr: 0,50 rc: 0,25 f1: 0,33


        
        
        //        ControlledTaggerEvaluationSettings s1 = new ControlledTaggerEvaluationSettings(5, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.0f);
//        ControlledTaggerEvaluationSettings s1 = new ControlledTaggerEvaluationSettings(10, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.0f);
//        ControlledTaggerEvaluationSettings s2 = new ControlledTaggerEvaluationSettings(20, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.0f);
//        ControlledTaggerEvaluationSettings s3 = new ControlledTaggerEvaluationSettings(50, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.0f);
//        ControlledTaggerEvaluationSettings s4 = new ControlledTaggerEvaluationSettings(100, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.0f);
//        ControlledTaggerEvaluationSettings s5 = new ControlledTaggerEvaluationSettings(250, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.0f);
//        ControlledTaggerEvaluationSettings s6 = new ControlledTaggerEvaluationSettings(500, 1000, TaggingType.FIXED_COUNT, TaggingCorrelationType.DEEP_CORRELATIONS, 0, 10, 20000, 1.0f);
        
        
        // s1.setStopwords(new Stopwords(Stopwords.STOP_WORDS_EN));

        
        List<ControlledTaggerEvaluationSettings> settings = Arrays.asList(s4); //,s2,s3);//,s2,s3,s4,s5,s6);

        evaluation.evaluate(settings, "data/ControlledTaggerEvaluationResult.txt");

    }

}
