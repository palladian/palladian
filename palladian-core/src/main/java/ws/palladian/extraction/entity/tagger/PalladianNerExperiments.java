package ws.palladian.extraction.entity.tagger;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.tagger.PalladianNer.LanguageMode;
import ws.palladian.extraction.entity.tagger.PalladianNer.TrainingMode;
import ws.palladian.helper.io.FileHelper;

public class PalladianNerExperiments {

    public static void main(String[] args) {
        PalladianNer tagger = new PalladianNer();

        // String trainingPath = "data/ner/conll/training.txt";
        String trainingPath = "data/ner/conll/training_min.txt";
        String modelPath = "data/temp/conllModel";

        // set whether to tag dates
        tagger.setTagDates(false);

        // set whether to tag URLs
        tagger.setTagUrls(false);

        // set mode (English or language independent)
        tagger.setLanguageMode(LanguageMode.English);

        // set type of training set (complete supervised or sparse semi-supervised)
        tagger.setTrainingMode(TrainingMode.Complete);

        // create a dictionary from a dictionary txt file
        // tagger.makeDictionary("mergedDictComplete.csv");

        // train the tagger on the training file (with or without additional training annotations)

//         tagger.train(trainingPath, modelPath);

        // Annotations annotations = new Annotations();
        String trainingSeedFilePath = "data/ner/namesNerDictionary.txt";
        Annotations trainingAnnotations = FileFormatParser.getSeedAnnotations(trainingSeedFilePath, -1);
        tagger.train(trainingPath, trainingAnnotations, modelPath);

        EvaluationResult evaluationResult = tagger.evaluate("data/ner/conll/test_final.txt", TaggingFormat.COLUMN);
        System.out.println(evaluationResult.getMUCResultsReadable());
        System.out.println(evaluationResult.getExactMatchResultsReadable());
//        FileHelper.writeToFile("data/temp/conllEvaluation", evaluationResult.toString());
    }

}
