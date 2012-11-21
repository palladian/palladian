package ws.palladian.extraction.entity.tagger;

import ws.palladian.extraction.content.PageContentExtractorException;
import ws.palladian.extraction.content.PalladianContentExtractor;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.tagger.PalladianNer.LanguageMode;
import ws.palladian.extraction.entity.tagger.PalladianNer.TrainingMode;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

public class PalladianNerExperiments {

    public void trainTest() {
        PalladianNer tagger = new PalladianNer();

        // String trainingPath = "data/ner/conll/training.txt";
        String trainingPath = "data/datasets/ner/conll/training.txt";
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
        String trainingSeedFilePath = "data/namesNerDictionary.txt";
        Annotations trainingAnnotations = FileFormatParser.getSeedAnnotations(trainingSeedFilePath, -1);
        tagger.train(trainingPath, trainingAnnotations, modelPath);

        EvaluationResult evaluationResult = tagger.evaluate("data/datasets/ner/conll/test_final.txt",
                TaggingFormat.COLUMN);
        System.out.println(evaluationResult.getMUCResultsReadable());
        System.out.println(evaluationResult.getExactMatchResultsReadable());
//        FileHelper.writeToFile("data/temp/conllEvaluation", evaluationResult.toString());
    }

    public void tag(String url, PalladianNer tagger) throws PageContentExtractorException {

        PalladianContentExtractor pce = new PalladianContentExtractor();

        pce.setDocument(url);
        String text = pce.getResultText();
        String taggedText = tagger.tag(text);
        taggedText = taggedText.replaceAll("\\</(PER|LOC|MISC|ORG)\\>", "</span>");
        taggedText = taggedText.replaceAll("\\</(PER|LOC|MISC|ORG|City|Country)\\>", "</span>");
        taggedText = taggedText.replaceAll("\\<(PER|LOC|MISC|ORG|City|Country)\\>", "<span class=\"$1\">");

        String html = FileHelper.readFileToString("data/temp/ner/raw.html");
        html = html.replace("XXX", taggedText);

        FileHelper.writeToFile("data/temp/tagged_" + StringHelper.makeSafeName(url) + ".html", html);
    }

    public void tagText(String text, PalladianNer tagger) {

    }

    public static void main(String[] args) throws PageContentExtractorException {

        // FileHelper.removeDuplicateLines("data/temp/nerDictionary.csv");
        // System.exit(0);

        PalladianNer tagger = PalladianNer.load("data/temp/conllModel.model.gz");
        tagger.setEntityDictionary("data/temp/nerDictionary.csv");
        PalladianNerExperiments exp = new PalladianNerExperiments();
        // exp.tag("http://www.bbc.co.uk/news/world-europe-20265166", tagger);
        // exp.tag("http://www.bbc.co.uk/news/uk-20277732", tagger);
        // exp.tag("http://www.bbc.com/travel/feature/20120925-following-the-buddha-around-bombay", tagger);
        // exp.tag("http://www.bbc.com/travel/feature/20121108-irelands-outlying-islands", tagger);
        System.out.println(tagger.tag("Ireland is north of France and Paris."));

    }

}
