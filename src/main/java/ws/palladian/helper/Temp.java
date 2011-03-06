package ws.palladian.helper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ws.palladian.classification.page.ClassifierManager;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextClassifier;
import ws.palladian.classification.page.TextInstance;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.classification.page.evaluation.FeatureSetting;
import ws.palladian.extraction.PageAnalyzer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.preprocessing.nlp.LingPipePOSTagger;
import ws.palladian.preprocessing.nlp.TagAnnotation;
import ws.palladian.preprocessing.nlp.TagAnnotations;
import ws.palladian.tagging.StringTagger;
import ws.palladian.web.Crawler;
import ws.palladian.web.feeds.Feed;
import ws.palladian.web.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.web.feeds.persistence.FeedDatabase;

/**
 * Dump class to test various algorithms.
 * 
 * @author David Urbansky
 * 
 */
public class Temp {

    public static void classify() {

        // create a classifier mananger object
        ClassifierManager classifierManager = new ClassifierManager();

        // specify the dataset that should be used as training data
        Dataset dataset = new Dataset();

        // set the path to the dataset
        dataset.setPath("G:\\My Dropbox\\movieRatingsTitleTraining.csv");

        dataset.setSeparationString("###");

        // tell the preprocessor that the first field in the file is a link to the actual document
        dataset.setFirstFieldLink(false);

        // create a text classifier by giving a name and a path where it should be saved to
        TextClassifier classifier = new DictionaryClassifier();

        // specify the settings for the classification
        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();

        // we use only a single category per document
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.REGRESSION);

        // we want the classifier to be serialized in the end
        classificationTypeSetting.setSerializeClassifier(false);

        // specify feature settings that should be used by the classifier
        FeatureSetting featureSetting = new FeatureSetting();

        // we want to create character-level n-grams
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);

        // the minimum length of our n-grams should be 1
        featureSetting.setMinNGramLength(3);

        // the maximum length of our n-grams should be 1
        featureSetting.setMaxNGramLength(7);

        // terms can be one char
        featureSetting.setMinimumTermLength(3);

        // we assign the settings to our classifier
        classifier.setClassificationTypeSetting(classificationTypeSetting);
        classifier.setFeatureSetting(featureSetting);

        // now we can train the classifier using the given dataset
        classifierManager.trainClassifier(dataset, classifier);

        // set the path to the dataset
        List<String> movies = FileHelper.readFileToArray("G:\\My Dropbox\\movieRatingsTitleTesting.csv");

        StringBuilder sb = new StringBuilder();
        int totalRealRating = 0;
        for (String movie : movies) {
            String[] parts = movie.split("###");

            TextInstance cd = classifier.classify(parts[0]);
            sb.append(cd.getMainCategoryEntry().getCategory().getName()).append(";").append(parts[1]).append("\n");
            totalRealRating += Integer.valueOf(parts[1]);
        }

        FileHelper.writeToFile("data/temp/rmseOutput.txt", sb);
        System.out.println(MathHelper.calculateRMSE("data/temp/rmseOutput.txt", ";"));
        System.out.println("Average Rating: " + totalRealRating / movies.size());

        sb = new StringBuilder();
        for (String movie : movies) {
            String[] parts = movie.split("###");
            sb.append(totalRealRating / movies.size()).append(";").append(parts[1]).append("\n");
        }

        FileHelper.writeToFile("data/temp/rmseOutputGuess.txt", sb);
        System.out.println(MathHelper.calculateRMSE("data/temp/rmseOutputGuess.txt", ";"));

    }

    public static void createTrainingData() {

        FeedDatabase fd = new FeedDatabase();
        List<Feed> feeds = fd.getFeeds();

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(new File("data/temp/learningData.txt"));

            int mod = 100 / 10;

            int feedCounter = 0;
            for (Feed feed : feeds) {

                feedCounter++;

                // skip some feeds if we want to take a sample only
                if ((feedCounter + 1) % mod != 0) {
                    continue;
                }

                String safeFeedName = feed.getId()
                        + "_"
                        + StringHelper.makeSafeName(feed.getFeedUrl().replaceFirst("http://www.", "").replaceFirst(
                                "www.", ""), 30);

                String fileName = FeedReaderEvaluator.findHistoryFile(safeFeedName);

                List<String> items = FileHelper.readFileToArray(fileName);

                if (items.size() < 6) {
                    continue;
                }

                List<Long> intervals = new ArrayList<Long>();

                // make list of intervals
                for (int i = items.size() - 2; i >= 0; i--) {

                    String[] item1 = items.get(i).split(";");
                    String[] item2 = items.get(i + 1).split(";");

                    long interval = (Long.valueOf(item1[0]) - Long.valueOf(item2[0])) / DateHelper.MINUTE_MS;

                    intervals.add(interval);
                }

                // move a 6 item long window through the file, 5 variables + 1 outcome
                for (int i = 0; i < intervals.size() - 6; i++) {

                    fileWriter.write(String.valueOf(intervals.get(i)));
                    fileWriter.write(";");
                    fileWriter.write(String.valueOf(intervals.get(i + 1)));
                    fileWriter.write(";");
                    fileWriter.write(String.valueOf(intervals.get(i + 2)));
                    fileWriter.write(";");
                    fileWriter.write(String.valueOf(intervals.get(i + 3)));
                    fileWriter.write(";");
                    fileWriter.write(String.valueOf(intervals.get(i + 4)));
                    fileWriter.write(";");
                    fileWriter.write(String.valueOf(intervals.get(i + 5)));
                    fileWriter.write(";\n");
                    fileWriter.flush();
                }

            }

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }

    }

    public static void imprintExtractor(String imprintURL) {

        String companyName = ""; // 8/10 (regex)
        String ceo = "";
        String street = ""; // 7/10 (str, regex)
        String zipAndCity = ""; // 10/10 (regex)
        String telephone = ""; // 10/10 (regex)
        String fax = ""; // 10/10 (regex)
        String email = ""; //

        Crawler crawler = new Crawler();
        PageAnalyzer pa = new PageAnalyzer();

        Document webPage = crawler.getWebDocument(imprintURL);
        String rawMarkup = HTMLHelper.documentToHTMLString(webPage);
        // String plainContent = Crawler.extractBodyContent(rawMarkup, true);
        String plainContent = Crawler.extractBodyContent(webPage);
        // plainContent = PageAnalyzer.getReadableTextDump(webPage);

        // search for company name
        String regex = "^.*\\s(gmbh|e.v.|ltd.|limited|co. kg|gbr)";
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(plainContent);

        if (m.find()) {
            companyName = m.group().replaceAll("Impressum", "").replaceAll("\\s{2,}", " ").trim();
            // System.out.println(m.group());
        }

        // search for zip and City
        regex = "[0-9]{5}\\s.*?$";
        m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(plainContent);

        if (m.find()) {
            zipAndCity = m.group().replaceAll("\\s{2,}", " ").trim();
            // System.out.println(m.group());
        }

        // search for street
        regex = "^.{2,20}(straße|str.|strasse)\\s[0-9]{1,4}";
        m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(plainContent);

        if (m.find()) {
            street = m.group().replaceAll("\\s{2,}", " ").trim();
            // System.out.println(m.group());
        }

        // search for telephone
        // regex = "(?<=(tel.:|telefon:|phone:|fon)\\s?)\\+?[0-9-/() ]{6,20}";
        regex = "(?<=(tel.:|phone:|fon:|telefon:)\\s?).{0,2}[0-9-/()\\xa0 ]{7,20}";
        m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(plainContent);

        if (m.find()) {
            telephone = m.group().replaceAll("\\s{2,}", " ").trim();
            // System.out.println(m.group());
        }

        // search for fax
        regex = "(?<=(fax:|telefax:)\\s?)\\+?[0-9-/()\\xa0 ]{7,20}";
        m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(plainContent);

        if (m.find()) {
            fax = m.group().replaceAll("\\s{2,}", " ").trim();
            // System.out.println(m.group());
        }

        // search for e-mail
        regex = "(?<=(email:|e-mail:)\\s?)[A-Z0-9._%-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}";
        m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(plainContent);

        if (m.find()) {
            email = m.group().replaceAll("\\s{2,}", " ").trim();
            // System.out.println(m.group());
        }

        System.out.println("Company : " + companyName);
        System.out.println("CEO     : " + ceo);
        System.out.println("Street  : " + street);
        System.out.println("Zip/City: " + zipAndCity);
        System.out.println("Phone   : " + telephone);
        System.out.println("Fax     : " + fax);
        System.out.println("E-Mail  : " + email);

    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // pos tagging

        // Object[] o = TUDNER.removeDateFragment("January James Hatfield Feb");
        // CollectionHelper.print(o);
        // System.exit(0);

        CountMap posCounts = new CountMap();
        LingPipePOSTagger lpt = new LingPipePOSTagger();
        lpt.loadModel();

        List<String> gs = FileHelper.readFileToArray("data/datasets/ner/conll/goldStandard.txt");
        for (String line : gs) {
            String[] parts = line.split(";");
            TagAnnotations tagAnnotations = lpt.tag(parts[3]).getTagAnnotations();
            for (TagAnnotation ta : tagAnnotations) {
                posCounts.increment(ta.getTag());
            }
        }
        CollectionHelper.print(posCounts);
        System.exit(0);

        TagAnnotations tagAnnotations = lpt.loadModel().tag("Jim Carrey is an actor living in Los Angeles.")
                .getTagAnnotations();
        CollectionHelper.print(tagAnnotations);
        tagAnnotations = lpt.loadModel().tag("Each").getTagAnnotations();
        CollectionHelper.print(tagAnnotations);
        tagAnnotations = lpt.loadModel().tag("An").getTagAnnotations();
        CollectionHelper.print(tagAnnotations);
        tagAnnotations = lpt.loadModel().tag("The").getTagAnnotations();
        CollectionHelper.print(tagAnnotations);
        tagAnnotations = lpt.loadModel().tag("Our").getTagAnnotations();
        CollectionHelper.print(tagAnnotations);
        tagAnnotations = lpt.loadModel().tag("Peter").getTagAnnotations();
        CollectionHelper.print(tagAnnotations);
        tagAnnotations = lpt.loadModel().tag("U.S.").getTagAnnotations();
        CollectionHelper.print(tagAnnotations);

        String taggedString = lpt
                .loadModel()
                .tag(
                        "Jim Carrey is an actor living in Los Angeles. He is not the only actor there. However, Jim is an awesome comedian. Two men in December on a Thursday. LONDON IS A CITY IN ENGLAND. In his book The Groove, Mr. Harrison explains a lot.")
                .getTaggedString();

        System.out.println(taggedString);

        taggedString = StringTagger.tagPosString(taggedString);

        System.out.println(taggedString);

        System.exit(0);

        Temp.classify();
        System.exit(0);

        FileHelper
                .removeDuplicateLines(
                        "C:\\My Dropbox\\semanticads\\Advertiser und Agenturen\\Spacedealer\\BCM\\keywordliste_mix.txt",
                        "C:\\My Dropbox\\semanticads\\Advertiser und Agenturen\\Spacedealer\\BCM\\keywordliste_mix_deduplicated.txt");
        System.exit(0);

        Temp.imprintExtractor("http://www.autozubehoer.de/Impressum:_:4.html?XTCsid=6i9fm908jrt6j62pefmqi4p200");
        Temp.imprintExtractor("http://www.granzow.de/web/index.php/impressum.html");
        Temp.imprintExtractor("http://domcam.de/impressum.html");
        Temp.imprintExtractor("http://www.sos-kinderdorf.de/sos_kinderdorf/de/impressum,np=76590.html");
        Temp
                .imprintExtractor("http://www.clickandprint.de/ServiceImpressum.php?UID=1290466142037850A85D51A5F2E12F836FDDB6688E4CEAF35EA4606");
        Temp.imprintExtractor("http://www.ti-sa.com/shop/shop_content.php?coID=4&XTCsid=ae0fehu5vlrsit0c04rq38onq1");
        Temp.imprintExtractor("http://www.schloesser-gbr.de/impressum.php");
        Temp
                .imprintExtractor("http://www.goldhahnundsampson.de/shop/About-us-Imprint-Customer-Service:_:4.html?XTCsid=41d7e6492b00e13e54f5a205ba0070dc");
        Temp.imprintExtractor("http://www.reifen-server.de/cgi-bin/kfzshop/reifenversand.pl?t=impressum&userid=78376");
        Temp.imprintExtractor("http://www.ledick.de/oxid/Impressum/");

        // Temp.createTrainingData();

        // deserialize model
        // Classifier cls = (Classifier) weka.core.SerializationHelper.read("data/temp/weka.model");
        //
        // Instance i = new Instance(5);
        // i.setValue(0, 2712);
        // i.setValue(1, 1757);
        // i.setValue(2, 4099);
        // i.setValue(3, 2913);
        // i.setValue(4, 19941);
        // double d = cls.classifyInstance(i);
        // System.out.println(d);
    }

}
