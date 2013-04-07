package ws.palladian.extraction.entity.tagger;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.extraction.content.PageContentExtractorException;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.tagger.PalladianNer.LanguageMode;
import ws.palladian.extraction.entity.tagger.PalladianNer.TrainingMode;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

public class PalladianNerExperiments {

    public void trainTest() {
        PalladianNer tagger = new PalladianNer(LanguageMode.English, TrainingMode.Complete);

        // String trainingPath = "data/ner/conll/training.txt";
        String trainingPath = "data/datasets/ner/conll/training.txt";
        String modelPath = "data/temp/conllModel";

        // set whether to tag dates
        tagger.setTagDates(false);

        // set whether to tag URLs
        tagger.setTagUrls(false);

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

    public void tag(String text, String fileName, NamedEntityRecognizer tagger) throws PageContentExtractorException {

        String taggerName = tagger.getName();

        text = HtmlHelper.stripHtmlTags(text);
        String taggedText = tagger.tag(text);
        List<String> temp = CollectionHelper.newArrayList();
        for (LocationType t : LocationType.values()) {
            temp.add(t.name());
        }
        String typeString = StringUtils.join(temp, "|");
        taggedText = taggedText.replaceAll("\\<(" + typeString + ")\\>", "<span class=\"$1\">");
        taggedText = taggedText.replaceAll("\\</(" + typeString + ")\\>", "</span>");
        taggedText = taggedText.replace("\n", "<br>");

        String html = FileHelper.readFileToString("data/temp/raw.html");
        html = html.replace("XXX", taggedText);

        FileHelper.writeToFile("data/temp/tagged_" + fileName + "_" + taggerName + ".html", html);
    }

    public void tagText(String text, PalladianNer tagger) {

    }

    public static void main(String[] args) throws PageContentExtractorException {

        // FileHelper.removeDuplicateLines("data/temp/nerDictionary.csv");
        // System.exit(0);
        // LocationExtractor palladianTagger = new PalladianLocationExtractor(WX_API_KEY, GEONAMES_USERNAME);
        // LocationExtractor calaisTagger = new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5");
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        // /String DATASET_LOCATION = "/Users/pk/Desktop/LocationLab/LocationExtractionDataset";
        // String DATASET_LOCATION = "/Users/pk/Desktop/Test";
        // String DATASET_LOCATION = "C:\\Users\\Sky\\Desktop\\LocationExtractionDatasetSmall";
        String DATASET_LOCATION = "Q:\\Users\\David\\Desktop\\LocationExtractionDatasetSmall";
        // Map<String, Double> results = evaluator.evaluateAll(
        // new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5"), DATASET_LOCATION);
        // Map<String, Double> results = evaluator.evaluateAll(new AlchemyLocationExtractor(
        // "b0ec6f30acfb22472f458eec1d1acf7f8e8da4f5"), DATASET_LOCATION);
        // Map<String, Double> results = evaluator.evaluateAll(new YahooLocationExtractor(), DATASET_LOCATION);
        PalladianLocationExtractor ex = new PalladianLocationExtractor(database);
        PalladianNerExperiments exp = new PalladianNerExperiments();
        exp.tag(HtmlHelper.stripHtmlTags(FileHelper.readFileToString(DATASET_LOCATION + "\\text14.txt")), "XXX", ex);
        //
        // File[] files = FileHelper.getFiles("C:\\Users\\Sky\\Desktop\\LocationExtractionDataset", "text");
        // for (File file : files) {
        // System.out.println(file);
        // String text = FileHelper.readFileToString(file);
        // try {
        // exp.tag(text, file.getName(), palladianTagger);
        // exp.tag(text, file.getName(), calaisTagger);
        //
        // // LocationExtractionEvaluator evaluator = new LocationExtractionEvaluator();
        // // evaluator.evaluate(file.getAbsolutePath());
        //
        // } catch (Exception e) {
        // e.printStackTrace();
        //
        // }
        // }

        // PalladianNer tagger = PalladianNer.load("data/temp/conllModel.model.gz");
        // tagger.setEntityDictionary("data/temp/nerDictionary.csv");
        // exp.tag("http://www.bbc.co.uk/news/world-europe-20265166", tagger);
        // exp.tag("http://www.bbc.co.uk/news/uk-20277732", tagger);
        // exp.tag("http://www.bbc.com/travel/feature/20120925-following-the-buddha-around-bombay", tagger);
        // exp.tag("http://www.bbc.com/travel/feature/20121108-irelands-outlying-islands", tagger);

    }

}
