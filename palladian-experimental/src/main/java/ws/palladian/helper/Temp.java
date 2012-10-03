package ws.palladian.helper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.classification.DatasetManager;
import ws.palladian.classification.page.ClassifierManager;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextClassifier;
import ws.palladian.classification.page.TextInstance;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.classification.page.evaluation.FeatureSetting;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.persistence.ResultSetCallback;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.PageAnalyzer;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * Dump class to test various algorithms.
 * 
 * @author David Urbansky
 * 
 */
public class Temp {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Temp.class);


//    public static void performanceCheck() {
//
//        Set<String> urls = new HashSet<String>();
//
//        urls.add("http://www.literatura-obcojezyczna.1up.pl/mapa/1107045/informatyka/");
//        urls.add("http://www.territorioscuola.com/wikipedia/en.wikipedia.php?title=Wikipedia:WikiProject_Deletion_sorting/Bands_and_musicians/archive");
//        urls.add("http://www.designquote.net/directory/ny");
//        urls.add("http://wikyou.info/index3.php?key=clwyd");
//        urls.add("http://lashperfect.com/eyelash-salon-finder");
//        urls.add("http://www.ics.heacademy.ac.uk/publications/book_reviews/books.php?status=r&ascendby=author");
//        urls.add("http://www.letrs.indiana.edu/cgi/t/text/text-idx?c=wright2;cc=wright2;view=text;rgn=main;idno=wright2-0671");
//        urls.add("http://justintadlock.com/archives/2007/12/09/structure-wordpress-theme");
//        urls.add("http://www.editionbeauce.com/archives/photos/");
//        urls.add("http://nouvellevintage.wordmess.net/20100309/hello-from-the-absense/");
//        urls.add("http://xn--0tru33arqi4jn7xzda.jp/index3.php?key=machinerie");
//        urls.add("http://katalog.svkul.cz/a50s.htm");
//        urls.add("http://gosiqumup.fortunecity.com/2009_04_01_archive.html");
//        urls.add("http://freepages.genealogy.rootsweb.ancestry.com/~sauve/indexh.htm");
//        urls.add("http://www.blog-doubleclix.com/index.php?q=erix");
//        urls.add("http://meltingpot.fortunecity.com/virginia/670/FichierLoiselle.htm");
//        urls.add("http://canada-info.ca/directory/category/consulting/index.php");
//        urls.add("http://www.infopig.com/news/07-19-2008.html");
//
//        DocumentRetriever crawler = new DocumentRetriever();
//        crawler.getDownloadFilter().setMaxFileSize(-1);
//
//        double[] x = new double[urls.size()];
//        double[] y = new double[urls.size()];
//        int c = 0;
//        long sumBytes = 0;
//        long sumTime = 0;
//        for (String url : urls) {
//            StopWatch sw = new StopWatch();
//            crawler.getWebDocument(url);
//
//            LOGGER.info(sw.getElapsedTimeString() + " for " + crawler.getLastDownloadSize() + " Bytes of url " + url);
//
//            sumBytes += crawler.getLastDownloadSize();
//            sumTime += sw.getElapsedTime();
//            x[c] = crawler.getLastDownloadSize();
//            y[c] = sw.getElapsedTime();
//            c++;
//        }
//
//        double[] parameters = MathHelper.performLinearRegression(x, y);
//
//        LOGGER.info("the linear regression formula for download and parsing time [ms] in respect to the size is: "
//                + Math.round(parameters[0]) + " * downloadSize [KB] + " + parameters[1]);
//        LOGGER.info("total time [ms] and total traffic [Bytes]: " + sumTime + " / " + sumBytes);
//        if (sumTime > 0) {
//            LOGGER.info("on average: " + MathHelper.round(sumBytes / 1024 / (sumTime / 1000), 2) + "[KB/s]");
//        }
//    }


    // public static void threadPoolTest() {
    //
    // ExecutorService threadPool = Executors.newFixedThreadPool(10);
    // FeedDatabase feedDatabase = new FeedDatabase();
    // List<Feed> feeds = feedDatabase.getFeeds();
    // LOGGER.info("# feeds " + feeds.size());
    // final AtomicInteger counter = new AtomicInteger();
    //
    // for (final Feed feed : feeds) {
    // threadPool.submit(new Runnable() {
    // @Override
    // public void run() {
    // LOGGER.info("run " + feed.getFeedUrl());
    // DocumentRetriever documentRetriever = new DocumentRetriever();
    // documentRetriever.setOverallTimeout(10000);
    // documentRetriever.getWebDocument(feed.getFeedUrl());
    // counter.incrementAndGet();
    // }
    // });
    // }
    //
    // while (true) {
    // try {
    // Thread.sleep(1000);
    // // System.gc();
    // } catch (InterruptedException e) {
    // LOGGER.error(e);
    // }
    // }
    // }

//    public static void downloadFeedTest() {
//
//        FeedDatabase feedDatabase = DatabaseManagerFactory.create(FeedDatabase.class);
//        List<Feed> feeds = feedDatabase.getFeeds();
//
//        DocumentRetriever documentRetriever = new DocumentRetriever();
//
//        Set<String> urls = new HashSet<String>();
//        for (Feed feed : feeds) {
//            urls.add(feed.getFeedUrl());
//        }
//
//        documentRetriever.setNumThreads(200);
//
//        RetrieverCallback retrieverCallback = new RetrieverCallback() {
//
//            @Override
//            public void onFinishRetrieval(Document document) {
//                if (document != null) {
//                    System.out.println("downloaded " + document.getDocumentURI());
//                }
//
//            }
//        };
//
//        documentRetriever.getWebDocuments(urls, retrieverCallback);
//    }

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
        System.out.println(MathHelper.computeRootMeanSquareError("data/temp/rmseOutput.txt", ";"));
        System.out.println("Average Rating: " + totalRealRating / movies.size());

        sb = new StringBuilder();
        for (String movie : movies) {
            String[] parts = movie.split("###");
            sb.append(totalRealRating / movies.size()).append(";").append(parts[1]).append("\n");
        }

        FileHelper.writeToFile("data/temp/rmseOutputGuess.txt", sb);
        System.out.println(MathHelper.computeRootMeanSquareError("data/temp/rmseOutputGuess.txt", ";"));

    }

    public static void createTrainingData() {

        FeedDatabase fd = DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig());
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

        DocumentRetriever crawler = new DocumentRetriever();

        Document webPage = crawler.getWebDocument(imprintURL);
        // String rawMarkup = HTMLHelper.documentToHTMLString(webPage);
        // String plainContent = Crawler.extractBodyContent(rawMarkup, true);
        String plainContent = PageAnalyzer.extractBodyContent(webPage);
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

    public static void train(Dataset dataset, String classifierName, String classifierPath) {
        train(dataset, classifierName, classifierPath, null, null);
    }

    public static void train(Dataset dataset, String classifierName, String classifierPath,
            ClassificationTypeSetting cts, FeatureSetting fs) {

        // take the time for the learning
        StopWatch stopWatch = new StopWatch();

        // create a classifier mananger object
        ClassifierManager classifierManager = new ClassifierManager();

        // tell the preprocessor that the first field in the file is a link to the actual document
        dataset.setFirstFieldLink(true);

        // create a text classifier by giving a name and a path where it should be saved to
        TextClassifier classifier = new DictionaryClassifier(classifierName, classifierPath + classifierName + "/");
        // TextClassifier classifier = new DictionaryClassifier(classifierName,classifierPath);

        // specify the settings for the classification
        ClassificationTypeSetting classificationTypeSetting = cts;
        if (classificationTypeSetting == null) {
            classificationTypeSetting = new ClassificationTypeSetting();

            // we use only a single category per document
            classificationTypeSetting.setClassificationType(ClassificationTypeSetting.SINGLE);

            // we want the classifier to be serialized in the end
            classificationTypeSetting.setSerializeClassifier(true);
        }

        // specify feature settings that should be used by the classifier
        FeatureSetting featureSetting = fs;

        if (featureSetting == null) {
            featureSetting = new FeatureSetting();

            // we want to create character-level n-grams
            featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);

            // the minimum length of our n-grams should be 4
            featureSetting.setMinNGramLength(4);

            // the maximum length of our n-grams should be 7
            featureSetting.setMaxNGramLength(7);
        }

        // we assign the settings to our classifier
        classifier.setClassificationTypeSetting(classificationTypeSetting);
        classifier.setFeatureSetting(featureSetting);

        // now we can train the classifier using the given dataset
        // classifier.train(dataset);
        // classifier.save(classifierPath);
        classifierManager.trainClassifier(dataset, classifier);


        LOGGER.info("finished training classifier in " + stopWatch.getElapsedTimeString());
    }

    public void topicClassifier() throws IOException {

        String datasetRootFolder = "data/datasets/topic/";

        // /////////////////////// train //////////////////////
        // create an index over the dataset
        DatasetManager dsManager = new DatasetManager();
        String path = "index.txt";

        // create an excerpt with 1000 instances per class
        String indexExcerpt = dsManager.createIndexExcerpt(datasetRootFolder + path, " ", 100);

        String[] indexSplits = dsManager.splitIndex(indexExcerpt, 50);

        // specify the dataset that should be used as training data
        Dataset dataset = new Dataset();

        // set the path to the dataset, the first field is a link, and columns are separated with a space
        dataset.setPath(indexSplits[0]);

        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");

        // train(dataset, "TopicClassifier", "data/models/");
        // ////////////////////////////////////////////////////////

        DictionaryClassifier classifier = DictionaryClassifier.load("data/models/TopicClassifier/TopicClassifier.gz");

        // /////////////////////// evaluate //////////////////////
        Dataset testDataset = new Dataset();
        //
        // set the path to the dataset, the first field is a link, and columns are separated with a space
        testDataset.setPath(indexSplits[1]);

        testDataset.setFirstFieldLink(true);
        testDataset.setSeparationString(" ");

        System.out.println(classifier.evaluate(testDataset).toReadableString());
        // ////////////////////////////////////////////////////////

        // /////////////////////// use the classifier //////////////////////
        // use the classifier
        TextInstance classifiedInstance = classifier.classify("Microsofts market share increased by ten percent.");
        System.out.println(classifiedInstance);
        // ////////////////////////////////////////////////////////

    }

    public void createSubset(String rootPath,String targetPath, int num) {
        
        FileHelper.addTrailingSlash(rootPath);
        FileHelper.addTrailingSlash(targetPath);

        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance().getConfig());

        final Set<Integer> useFeedIds = new HashSet<Integer>();
        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                useFeedIds.add(resultSet.getInt("id"));
            }

        };

        dbm.runQuery(callback, "SELECT id FROM feeds ORDER BY RAND() LIMIT 0," + num);

        //useFeedIds.add(123);

        System.out.println("selected " + useFeedIds + " random ids");
          
        int copiedFeeds = 0;
        for (Integer integer : useFeedIds) {

            if (copiedFeeds == num) {
                break;
            }

            int folderK = integer / 1000;

            String pathToFeedFolder = rootPath + folderK + "/" + integer;

            System.out.println("copy gzips from " + pathToFeedFolder);

            File[] zipFiles = FileHelper.getFiles(pathToFeedFolder, ".gz");

            System.out.println("found " + zipFiles.length + " zip files");

            String newTargetPath = targetPath + integer + "/";

            for (File file : zipFiles) {
                FileHelper.copyFile(file.getPath(), newTargetPath + file.getName());
            }

            if (zipFiles.length > 0) {
                copiedFeeds++;
            }
        }

        System.out.println("copied " + copiedFeeds);

        // File[] filesAndDirectories = FileHelper.getFilesRecursive(rootPath);
        // for (File file : filesAndDirectories) {
        // if (!file.isDirectory() && file.getName().endsWith("gz")) {
        // String path = file.getParent();
        // // System.out.println("path: " + path);
        // try {
        // Integer id = Integer.valueOf(path.substring(path.lastIndexOf(File.separator)+1));
        // if (useFeedIds.contains(id)) {
        // String tPath = targetPath+id+"/"+file.getName();
        // // System.out.println("tpath: " + tPath);
        // FileHelper.copyFile(file.getPath(), tPath);
        // }
        // } catch (Exception e) {
        // //e.printStackTrace();
        // }
        //
        //
        // }
        // // System.out.println(file.getPath());
        // }
    }
    
    public List<String> getPaginationFromGoogle(String googlePage) {
        //googlePage = "http://www.google.de/search?sourceid=chrome&ie=UTF-8&q=test";
        
        DocumentRetriever dret = new DocumentRetriever();
        Document webDocument = dret.getWebDocument(googlePage);
        List<Node> paginationCandidates = XPathHelper.getXhtmlNodes(webDocument, "//div[4]/div/div/div[9]/span/div/table/tr/td/a");
        List<String> paginationUrls = new ArrayList<String>();
        for (Node n : paginationCandidates) {
            paginationUrls.add(n.getAttributes().getNamedItem("href").getTextContent());
        }
        CollectionHelper.print(paginationUrls);
        return paginationUrls;
        
//        ListDiscoverer ld = new ListDiscoverer();
//        Set<String> urls = ld.findPaginationURLs(url);
    }
    
    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        new Temp().getPaginationFromGoogle("http://www.google.de/search?sourceid=chrome&ie=UTF-8&q=test");
        System.exit(0);
        //args = new String[]{"data/temp/feedgz","data/temp/feedgz/copy/","100"};
        new Temp().createSubset(args[0],args[1], Integer.valueOf(args[2]));
        //new Temp().topicClassifier();
        System.exit(0);
        System.out.println(System.currentTimeMillis());

        FileHelper.removeDuplicateLines("data/datasets/ner/tud/manuallyPickedSeeds/seedList.xml",
        "data/datasets/ner/tud/manuallyPickedSeeds/seedList2.xml");
        List<String> readFileToArray = FileHelper
        .readFileToArray("data/datasets/ner/tud/manuallyPickedSeeds/seedList2.xml");

        Collections.shuffle(readFileToArray);
        FileHelper.writeToFile("data/datasets/ner/tud/manuallyPickedSeeds/seedList2.xml", readFileToArray);
        CountMap<String> countMap = CountMap.create();
        for (String string : readFileToArray) {
            countMap.add(string.substring(1, string.indexOf(">")));
        }
        CollectionHelper.print(countMap);

        // StringBuilder seedFile = new StringBuilder();
        //
        // String seedFolderPath = "G:\\Projects\\Programming\\Java\\WebKnox\\data\\knowledgeBase\\seedEntities\\";
        // File[] files = FileHelper.getFiles("H:\\PalladianData\\Datasets\\wwwner\\ner\\www_cleansed");
        // for (File file : files) {
        //
        // if (file.isDirectory()) {
        //
        // List<String> seeds = FileHelper.readFileToArray(file.getPath() + "\\seeds\\seeds.txt");
        // List<String> cleansedSeeds = new ArrayList<String>();
        // for (String string : seeds) {
        // cleansedSeeds.add(string.replaceAll("###.*", ""));
        // }
        // CollectionHelper.print(cleansedSeeds);
        //
        // String conceptName = file.getPath().substring(file.getPath().lastIndexOf("\\") + 1).toLowerCase();
        //
        // List<String> seeds2 = FileHelper.readFileToArray(seedFolderPath+conceptName+".txt");
        //
        // for (String string : seeds2) {
        // if (string.indexOf("#") > -1) {
        // continue;
        // }
        // if (!cleansedSeeds.contains(string)) {
        // seedFile.append("<" + conceptName.toUpperCase() + ">" + string + "</"
        // + conceptName.toUpperCase() + ">\n");
        // }
        // }
        //
        //
        // }
        //
        // }
        //
        // FileHelper.writeToFile("data/datasets/ner/tud/manuallyPickedSeeds/seedList.xml", seedFile);

        // /////////////////////////////// check xml file for closed tags ///////////////////////////////
        // List<String> fileArray = FileHelper.readFileToArray("G:\\My Dropbox\\tud2011Complete.xml");
        // List<String> fileArray = FileHelper.readFileToArray("data/datasets/ner/tud/tud2011Complete.xml");
        //
        // boolean lastClosing = false;
        //
        // for (String string : fileArray) {
        //
        // Pattern pattern = Pattern.compile("<(.*?)>(?=(.{0,10}))");
        // Matcher matcher = pattern.matcher(string);
        //
        // String lastTag = "";
        //
        // while (matcher.find()) {
        //
        // String currentTag = matcher.group(1);
        //
        // boolean closingTag = false;
        // if (currentTag.indexOf("/") > -1) {
        // closingTag = true;
        // }
        //
        // currentTag = currentTag.replace("/", "").replace(">", "").replace("<", "");
        //
        // if (closingTag && !lastTag.equals(currentTag)) {
        // System.out.println("here1 !!! " + matcher.group(2));
        // }
        //
        // if (lastClosing && closingTag) {
        // System.out.println("here2 !!! " + matcher.group(2));
        // }
        //
        // if (!lastClosing && !closingTag && lastTag.length() > 0) {
        // System.out.println("here3 !!! " + matcher.group(2) + ", " + matcher.start());
        // System.out.println("last tag: " + lastTag + ", this tag: " + currentTag);
        // }
        //
        // lastClosing = closingTag;
        // lastTag = currentTag;
        // // System.out.println(lastTag);
        //
        // }
        //
        // }
        // /////////////////////////////////////////////////////////////////////////////////////////////////

        // downloadFeedTest();
        // threadPoolTest();
        System.exit(0);

        // pos tagging

        // Object[] o = TUDNER.removeDateFragment("January James Hatfield Feb");
        // CollectionHelper.print(o);
        // System.exit(0);

        // CountMap posCounts = new CountMap();
        // LingPipePOSTagger lpt = new LingPipePOSTagger();
        // lpt.loadModel();
        //
        // List<String> gs = FileHelper.readFileToArray("data/datasets/ner/conll/goldStandard.txt");
        // for (String line : gs) {
        // String[] parts = line.split(";");
        // TagAnnotations tagAnnotations = lpt.tag(parts[3]).getTagAnnotations();
        // for (TagAnnotation ta : tagAnnotations) {
        // posCounts.increment(ta.getTag());
        // }
        // }
        // CollectionHelper.print(posCounts);
        // System.exit(0);
        //
        // TagAnnotations tagAnnotations = lpt.loadModel().tag("Jim Carrey is an actor living in Los Angeles.")
        // .getTagAnnotations();
        // CollectionHelper.print(tagAnnotations);
        // tagAnnotations = lpt.loadModel().tag("Each").getTagAnnotations();
        // CollectionHelper.print(tagAnnotations);
        // tagAnnotations = lpt.loadModel().tag("An").getTagAnnotations();
        // CollectionHelper.print(tagAnnotations);
        // tagAnnotations = lpt.loadModel().tag("The").getTagAnnotations();
        // CollectionHelper.print(tagAnnotations);
        // tagAnnotations = lpt.loadModel().tag("Our").getTagAnnotations();
        // CollectionHelper.print(tagAnnotations);
        // tagAnnotations = lpt.loadModel().tag("Peter").getTagAnnotations();
        // CollectionHelper.print(tagAnnotations);
        // tagAnnotations = lpt.loadModel().tag("U.S.").getTagAnnotations();
        // CollectionHelper.print(tagAnnotations);
        //
        // String taggedString = lpt
        // .loadModel()
        // .tag(
        // "Jim Carrey is an actor living in Los Angeles. He is not the only actor there. However, Jim is an awesome comedian. Two men in December on a Thursday. LONDON IS A CITY IN ENGLAND. In his book The Groove, Mr. Harrison explains a lot.")
        // .getTaggedString();
        //
        // System.out.println(taggedString);
        //
        // // taggedString = StringTagger.tagPosString(taggedString);
        //
        // System.out.println(taggedString);
        //
        // System.exit(0);
        //
        // Temp.classify();
        // System.exit(0);
        //
        // FileHelper
        // .removeDuplicateLines(
        // "C:\\My Dropbox\\semanticads\\Advertiser und Agenturen\\Spacedealer\\BCM\\keywordliste_mix.txt",
        // "C:\\My Dropbox\\semanticads\\Advertiser und Agenturen\\Spacedealer\\BCM\\keywordliste_mix_deduplicated.txt");
        // System.exit(0);
        //
        // Temp.imprintExtractor("http://www.autozubehoer.de/Impressum:_:4.html?XTCsid=6i9fm908jrt6j62pefmqi4p200");
        // Temp.imprintExtractor("http://www.granzow.de/web/index.php/impressum.html");
        // Temp.imprintExtractor("http://domcam.de/impressum.html");
        // Temp.imprintExtractor("http://www.sos-kinderdorf.de/sos_kinderdorf/de/impressum,np=76590.html");
        // Temp
        // .imprintExtractor("http://www.clickandprint.de/ServiceImpressum.php?UID=1290466142037850A85D51A5F2E12F836FDDB6688E4CEAF35EA4606");
        // Temp.imprintExtractor("http://www.ti-sa.com/shop/shop_content.php?coID=4&XTCsid=ae0fehu5vlrsit0c04rq38onq1");
        // Temp.imprintExtractor("http://www.schloesser-gbr.de/impressum.php");
        // Temp
        // .imprintExtractor("http://www.goldhahnundsampson.de/shop/About-us-Imprint-Customer-Service:_:4.html?XTCsid=41d7e6492b00e13e54f5a205ba0070dc");
        // Temp.imprintExtractor("http://www.reifen-server.de/cgi-bin/kfzshop/reifenversand.pl?t=impressum&userid=78376");
        // Temp.imprintExtractor("http://www.ledick.de/oxid/Impressum/");

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
