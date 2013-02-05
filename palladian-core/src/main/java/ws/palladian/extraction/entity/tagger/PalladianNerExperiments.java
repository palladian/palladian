package ws.palladian.extraction.entity.tagger;

import java.io.File;
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
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.OpenCalaisLocationExtractor;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;

public class PalladianNerExperiments {

    // FIXME remove hard coded api keys
    public static final String WX_API_KEY = "ubve84tz3498zncq84z59238bzv5389";
    public static final String GEONAMES_USERNAME = "qqilihq";

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

    public void tag(String text, String fileName, NamedEntityRecognizer tagger) throws PageContentExtractorException {

        String taggerName = tagger.getName();

        // PalladianContentExtractor pce = new PalladianContentExtractor();

        // pce.setDocument(url);
        // String text = pce.getResultText();
        text = HtmlHelper.stripHtmlTags(text);
        String taggedText = tagger.tag(text);
        // taggedText = taggedText.replaceAll("\\</(PER|LOC|MISC|ORG|City|Country)\\>", "</span>");
        List<String> temp = CollectionHelper.newArrayList();
        for (LocationType t : LocationType.values()) {
            temp.add(t.name());
        }
        String typeString = StringUtils.join(temp, "|");
        taggedText = taggedText.replaceAll("\\<(" + typeString + ")\\>", "<span class=\"$1\">");
        taggedText = taggedText.replaceAll("\\</(" + typeString + ")\\>", "</span>");

        String html = FileHelper.readFileToString("data/temp/raw.html");
        html = html.replace("XXX", taggedText);

        FileHelper.writeToFile("data/temp/tagged_" + fileName + "_" + taggerName + ".html", html);
    }

    public void tagText(String text, PalladianNer tagger) {

    }

    public static void main(String[] args) throws PageContentExtractorException {

        // FileHelper.removeDuplicateLines("data/temp/nerDictionary.csv");
        // System.exit(0);
        LocationExtractor palladianTagger = new PalladianLocationExtractor(WX_API_KEY, GEONAMES_USERNAME);
        LocationExtractor calaisTagger = new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5");
        PalladianNerExperiments exp = new PalladianNerExperiments();

        File[] files = FileHelper.getFiles("/Users/pk/Desktop/LocationLab/LocationExtractionDataset", ".txt");
        for (File file : files) {
            System.out.println(file);
            String text = FileHelper.readFileToString(file);
            try {
                exp.tag(text, file.getName(), palladianTagger);
                exp.tag(text, file.getName(), calaisTagger);

                // LocationExtractionEvaluator evaluator = new LocationExtractionEvaluator();
                // evaluator.evaluate(file.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();

            }
        }

        // PalladianNer tagger = PalladianNer.load("data/temp/conllModel.model.gz");
        // tagger.setEntityDictionary("data/temp/nerDictionary.csv");
        // exp.tag("http://www.bbc.co.uk/news/world-europe-20265166", tagger);
        // exp.tag("http://www.bbc.co.uk/news/uk-20277732", tagger);
        // exp.tag("http://www.bbc.com/travel/feature/20120925-following-the-buddha-around-bombay", tagger);
        // exp.tag("http://www.bbc.com/travel/feature/20121108-irelands-outlying-islands", tagger);

    }

}
