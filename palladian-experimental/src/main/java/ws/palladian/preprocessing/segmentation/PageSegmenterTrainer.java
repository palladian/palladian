package ws.palladian.preprocessing.segmentation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * The PageSegmenterTrainer is needed for the evaluation of the class PageSegmenter.
 * 
 * @author Silvio Rabe
 * 
 */
public class PageSegmenterTrainer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PageSegmenterTrainer.class);

    /**
     * Evaluation help function for the similarity check of documents with specific parameters.
     * Checks the similarity of an given document with a collection of other documents based
     * on specific parameters for q-gram-number and q-gram length.
     * 
     * It creates a detailed xls-files with similarity values per documents and saves it in the
     * folder of given documents.
     * 
     * @param orgURL The URL that needs to be compared.
     * @param place The local folder where the documents to compare can be found.
     * @param numberOfQgrams The number of q-grams to use.
     * @param lengthOfQgrams The length of q-grams to use.
     */
    public static void performDetailedParameterCheckForGivenValues(String orgURL, String place, int numberOfQgrams,
            int lengthOfQgrams) throws MalformedURLException, IOException {
        DocumentRetriever c = new DocumentRetriever();

        PageSegmenter seg = new PageSegmenter();

        File files[] = readURLsFromDisc(place);
        CountMap<String> page1 = seg.createFingerprint(c.getWebDocument(orgURL), numberOfQgrams,
                lengthOfQgrams);

        BufferedWriter doc1 = new BufferedWriter(new FileWriter(place + "results_" + numberOfQgrams + "_"
                + lengthOfQgrams + ".xls"));
        doc1.write("Original file: " + orgURL);
        doc1.newLine();
        doc1.newLine();
        doc1.write("Similarity\tJaccard\tAverage\tFilename");
        doc1.newLine();
        doc1.newLine();

        for (int i = 0; i < files.length; i++) {
            CountMap<String> page2 = seg.createFingerprint(c.getWebDocument(files[i].toString()),
                    numberOfQgrams, lengthOfQgrams);
            LOGGER.info(page2.toString());

            Double vari = Math.round((1 - SimilarityCalculator.calculateSimilarity(page1, page2)) * 100) / 100.0;
            Double jacc = Math.round(MathHelper.computeJaccardSimilarity(page1.uniqueItems(), page2.uniqueItems()) * 100) / 100.0;

            Double aver = Math.round((vari + jacc) / 2 * 100) / 100.0;

            LOGGER.info("vari: " + vari + "   jacc: " + jacc + "   aver: " + aver);

            doc1.write(vari + "\t" + jacc + "\t" + aver + "\t" + files[i].toString().replace(place, ""));
            doc1.newLine();

        }

        doc1.close();
    }

    /**
     * Evaluation help function for the similarity check of documents with specific parameters.
     * Checks the similarity of an given document with a collection of other documents based
     * on specific parameters for q-gram-number and q-gram length.
     * 
     * It calculates an average value of similarity as result.
     * 
     * @param orgURL The URL that needs to be compared.
     * @param place The local folder where the documents to compare can be found.
     * @param numberOfQgrams The number of q-grams to use.
     * @param lengthOfQgrams The length of q-grams to use.
     * @return An average value of similarity for the given parameters.
     */
    public static Double performAverageParameterCheckForGivenValues(String orgURL, String place, int numberOfQgrams,
            int lengthOfQgrams) throws MalformedURLException, IOException {
        DocumentRetriever c = new DocumentRetriever();

        PageSegmenter seg = new PageSegmenter();

        Double result = 0.00;
        ArrayList<Double> average = new ArrayList<Double>();

        File files[] = readURLsFromDisc(place);
        CountMap<String> page1 = seg.createFingerprint(c.getWebDocument(orgURL), numberOfQgrams,
                lengthOfQgrams);

        for (int i = 0; i < files.length; i++) {
            CountMap<String> page2 = seg.createFingerprint(c.getWebDocument(files[i].toString()),
                    numberOfQgrams, lengthOfQgrams);

            Double vari = Math.round((1 - SimilarityCalculator.calculateSimilarity(page1, page2)) * 100) / 100.0;
            Double jacc = Math.round(MathHelper.computeJaccardSimilarity(page1.uniqueItems(), page2.uniqueItems()) * 100) / 100.0;

            Double aver = Math.round((vari + jacc) / 2 * 100) / 100.0;

            average.add(aver);

        }

        Double helper = 0.00;
        for (int i = 0; i < average.size(); i++) {
            helper = helper + average.get(i);
        }
        result = helper / average.size();

        return result;
    }

    /**
     * Evaluation help function for the similarity check of documents with specific parameters.
     * Checks all combinations of the given parameters and either writes it detailed in several
     * xls-files or prints just the results on the console.
     * 
     * @param orgURL The URL that needs to be compared.
     * @param place The local folder where the documents to compare can be found.
     * @param numberOfQgrams An array of the amounts of q-grams to check.
     * @param lengthOfQgrams An array of the lengths of q-grams to check.
     * @param detailedCheck True if the result should be detailed printed in xls-files. False if
     *            it should just print the results on the console.
     */
    public static void performParameterCheck(String orgURL, String place, int[] numberOfQgrams, int[] lengthOfQgrams,
            Boolean detailedCheck) throws MalformedURLException, IOException {

        ArrayList<String> averageValues = new ArrayList<String>();

        for (int i = 0; i < numberOfQgrams.length; i++) {

            for (int j = 0; j < lengthOfQgrams.length; j++) {

                LOGGER.info("number: " + numberOfQgrams[i] + ", length: " + lengthOfQgrams[j]);

                if (detailedCheck) {
                    performDetailedParameterCheckForGivenValues(orgURL, place, numberOfQgrams[i], lengthOfQgrams[j]);
                } else {
                    Double result = performAverageParameterCheckForGivenValues(orgURL, place, numberOfQgrams[i],
                            lengthOfQgrams[j]);
                    averageValues.add("[" + numberOfQgrams[i] + "][" + lengthOfQgrams[j] + "] " + result);
                }

            }

        }

        for (int i = 0; i < averageValues.size(); i++) {
            LOGGER.info(averageValues.get(i));
        }

    }

    /**
     * Saves the content of an URL to local disc
     * 
     * @param URL The URL of the site to save
     * @param place The filename of the site to save
     */
    public static void saveURLToDisc(String URL, String place) throws TransformerFactoryConfigurationError,
            TransformerException, IOException {

        URL url = new URL(URL);
        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
        String line = "";
        BufferedWriter doc1 = new BufferedWriter(new FileWriter(place));

        LOGGER.info("geht los-----");
        while ((line = bufferedreader.readLine()) != null) {
            doc1.write(line);
            doc1.newLine();

        }
        bufferedreader.close();
        doc1.close();

    }

    /**
     * Reads the files of a specific folder and returns it as a list of files.
     * 
     * @param place The folder of the files to read.
     * @return A list of files.
     */
    public static File[] readURLsFromDisc(String place) {

        File maindir = new File(place);
        File files[] = maindir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".html");
            }
        });

        LOGGER.info("files(" + files.length + "):----------\n" + files[0]);

        return files;
    }

    /**
     * Saves all the linked URLs of the domain of the given URL to local disc.
     * It distinguishes between probably similar and probably not similar documents based on the URL.
     * 
     * @param URL The given URL
     * @param limit The limit of URLs to save
     */
    public static void saveAllURLsToDisc(String URL, int limit) throws TransformerFactoryConfigurationError,
            TransformerException, IOException {
        DocumentRetriever c = new DocumentRetriever();
        String domain = UrlHelper.getDomain(URL);
        Document d = c.getWebDocument(domain);

        Set<String> te = new HashSet<String>();
        te = HtmlHelper.getLinks(d, true, false, "");
        LOGGER.info(te.size() + " intern verlinkte URLs gefunden!");
        LOGGER.info(te.toString());

        Iterator<String> it = te.iterator();
        String currentElement = "";
        String place = "";
        String label = "";
        int count = 0;

        String title = "";
        String labelOfURL = PageSegmenterHelper.getLabelOfURL(URL);

        while (it.hasNext() && count < limit) {
            currentElement = it.next();
            LOGGER.info(currentElement);

            label = PageSegmenterHelper.getLabelOfURL(currentElement);
            title = UrlHelper.getCleanUrl(currentElement);

            title = title.replace("/", "_");
            title = title.replaceAll("[[^\\w\\däüöÄÜÖ\\+\\- ]]", "_");

            LOGGER.info(title + "\n" + label);

            if (labelOfURL.equals(label)
            /* && URL.length()>=currentElement.length()-1 && URL.length()<=currentElement.length()+1 */) {
                place = "test\\aehnlich\\" + title;
                LOGGER.info("-->ähnlich");
            } else {
                place = "test\\unaehnlich\\" + title;
                LOGGER.info("-->nicht ähnlich");
            }
            saveURLToDisc(currentElement, place);

            count++;
        }

    }

    /**
     * Saves defined URLs to local disc.
     */
    public static void saveChosenURLsToDisc() throws TransformerFactoryConfigurationError, TransformerException,
            IOException {

        String[] collectionOfURL = {
                "http://www.wer-weiss-was.de",
                "http://www.wikipedia.de",
                "http://www.google.de",
                "http://www.youtube.com",
                "http://www.wetter.com",
                "http://www.wissen.de",
                "http://dict.leo.org",
                "http://www.juraforum.de",
                "http://www.tomshardware.de",
                "http://www.treiber.de",
                "http://www.pixelquelle.de",
                "http://www.ebay.de",
                "http://www.expedia.de",
                "http://www.expedia.de/last-minute/default.aspx",
                "http://cgi.ebay.de/ws/eBayISAPI.dll?ViewItem&item=220680636640",
                "http://www.treiber.de/treiber-download/Anchor-Datacomm-updates",
                "http://www.amazon.com",
                "http://www.amazon.com/s/ref=nb_sb_noss?url=search-alias%3Daps&field-keywords=mouse",
                "http://wissen.de/wde/generator/wissen/ressorts/geschichte/was_geschah_am/index.html?day=13&month=10&year=1900&suchen=Suchen",
                "http://maps.google.de" };

        for (int i = 0; i < collectionOfURL.length; i++) {
            String title = collectionOfURL[i].substring(7);
            title = title.replace("/", "_");
            title = title.replaceAll("[[^\\w\\däüöÄÜÖ\\+\\- ]]", "_");

            saveURLToDisc(collectionOfURL[i], "test_2\\unaehnlich2\\" + title);
            LOGGER.info(title + " erfolgreich!");
        }

    }

    /**
     * Brings up random links of a website.
     * 
     * @param siteWithLinks A site with a lot of links.
     * @param count It takes every *count* link. E.g. every 5th.
     * @param limit The total number of links needed.
     */
    public static void downladRandomSitesForEvaluation2(String siteWithLinks, int count, int limit) {
        Set<String> evaLinks = new HashSet<String>();
        int limitHelp = 0;

        LOGGER.info("Ausgangs-URL: " + siteWithLinks);

        DocumentRetriever c = new DocumentRetriever();

        Document d = c.getWebDocument(siteWithLinks);

        Set<String> te = new HashSet<String>();
        te = HtmlHelper.getLinks(d, true, false, "");

        int i = 0;
        Iterator<String> it = te.iterator();
        while (it.hasNext()) {
            String actURL = it.next();

            if (i == count) {
                evaLinks.add(actURL);
                LOGGER.info("1actURL: " + actURL);

                limitHelp++;
                i = 0;
            }

            i++;
            if (limitHelp == limit) {
                break;
            }
        }

        LOGGER.info("---------------\nEVA-LINKS:");
        it = evaLinks.iterator();
        while (it.hasNext()) {
            LOGGER.info(it.next());
        }

    }

    /**
     * Converts an xPath to a form with all capitals and all square brackets.
     * E.g. .../DIV[1]/DIV[4]/DIV[1]/UL[1]/LI[2]
     * 
     * @param orgXPath The xPath to covert.
     * @return The converted xPath.
     */
    private static String convertXPath(String orgXPath) {
        String conXPath = "";

        orgXPath = orgXPath.toUpperCase();

        String[] split = orgXPath.split("/");
        for (int i = 1; i < split.length; i++) {
            String actSplit = split[i];

            if (i >= 3) {
                if (!actSplit.endsWith("]")) {
                    actSplit = actSplit + "[1]";
                }
            }

            conXPath = conXPath + "/" + actSplit;

        }

        return conXPath;
    }

    /**
     * Saves permanently files of evaluation.
     * 
     * @param URL The original file
     * @param similarFiles A list of similar files.
     * @param place The place to save.
     * @param name The name to save.
     * @return A list of paths of the saved files.
     */
    private static List<String> saveEvaluationFiles(String URL, List<Document> similarFiles, String place,
            String name) throws TransformerFactoryConfigurationError, TransformerException, IOException {
        List<String> result = new ArrayList<String>();
        String fullName = place + name + ".html";
        saveURLToDisc(URL, fullName);
        result.add(fullName);
        for (int i = 0; i < similarFiles.size(); i++) {
            String actSim = similarFiles.get(i).getDocumentURI();
            fullName = place + name + "a" + (i + 1) + ".html";
            saveURLToDisc(actSim, fullName);
            result.add(fullName);
        }
        return result;
    }

    /**
     * Reads the saved evaluation files from disc.
     * 
     * @param place The place where the files can be found.
     * @param name The name of the files.
     * @return A list of paths of the saved files.
     */
    private static List<String> readEvaluationFiles(String place, String name) {
        List<String> result = new ArrayList<String>();
        result.add(place + name + ".html");
        for (int i = 0; i < 5; i++) {
            if (FileHelper.readFileToString(place + name + "a" + (i + 1) + ".html").length() > 0) {
                result.add(place + name + "a" + (i + 1) + ".html");
            }
        }

        return result;
    }

    /**
     * Evaluated one files.
     * 
     * @param place The place to save the evaluation and the results.
     * @param label The unique label.
     * @param name The unique name.
     * @param buildNew True if it is the first start. False if there are allready files saved.
     */
    public static void cvsTest(String place, String label, String name, boolean buildNew)
            throws ParserConfigurationException, IOException, TransformerFactoryConfigurationError,
            TransformerException {
        String fullPlace = place + label + "\\" + name + "\\";
        String mainFile = place + "\\" + "evaluation.csv";

        List<String> liste = (ArrayList<String>) FileHelper.readFileToArray(fullPlace + name + ".csv");
        String sep = ";";
        String[] vals = liste.get(1).split(sep);
        String URL = vals[0];

        LOGGER.info("Länge: " + liste.size());

        List<String> savedFiles = new ArrayList<String>();
        if (buildNew) {
            PageSegmenter seg = new PageSegmenter();
            seg.setDocument(URL);
            seg.startPageSegmentation();
            savedFiles = saveEvaluationFiles(URL, seg.getSimilarFiles(), fullPlace, name);
        } else {
            savedFiles = readEvaluationFiles(fullPlace, name);
        }

        // read
        PageSegmenter seg2 = new PageSegmenter();
        seg2.setDocument(savedFiles.get(0));

        DocumentRetriever c = new DocumentRetriever();

        List<Document> simMap = new ArrayList<Document>();
        for (int j = 1; j < savedFiles.size(); j++) {
            simMap.add(c.getWebDocument(savedFiles.get(j)));
        }
        seg2.setSimilarFiles(simMap);

        seg2.startPageSegmentation();
        List<?> allSegments = seg2.getAllSegments();

        int allFound = 0;
        int allCorrect = 0;
        int allNotAssignabel = 0;

        for (int i = 1; i < liste.size(); i++) {
            String guessedXPath = liste.get(i).split(sep)[1];
            guessedXPath = convertXPath(guessedXPath);
            String guessedColor = liste.get(i).split(sep)[3];
            LOGGER.info("guessPath: " + guessedXPath);

            for (int i2 = 0; i2 < allSegments.size(); i2++) {
                Segment segment = (Segment) allSegments.get(i2);
                String foundXPath = convertXPath(segment.getXPath());
                String foundColor = "";
                if (segment.getVariability() < 0.42) {
                    foundColor = "u";
                } else if (segment.getVariability() >= 0.58) {
                    foundColor = "v";
                } else {
                    foundColor = "n";
                }

                if (guessedXPath.equals(foundXPath)/* || (guessedXPath+"[1]").equals(foundXPath) */) {
                    LOGGER.info("gefunden!");
                    allFound++;
                    liste.set(i, liste.get(i) + sep + "1" + sep + foundColor);

                    if (guessedColor.equals(foundColor)) {
                        LOGGER.info("farbe stimmt");
                        allCorrect++;
                    }

                    if (foundColor == "n") {
                        allNotAssignabel++;
                    }

                }

            }
            if (liste.get(i).split(sep).length <= 5) {
                liste.set(i, liste.get(i) + sep + "0" + sep + "0");
            }

            if (liste.get(i).split(sep)[3].equals(liste.get(i).split(sep)[5])) {
                liste.set(i, liste.get(i) + sep + "1");
            } else {
                liste.set(i, liste.get(i) + sep + "0");
            }
        }
        // seg2.setStoreLocation("C:\\Users\\Silvio\\Documents\\doc2.html");
        // seg2.colorSegments();

        String numberOfGuessedXPaths = "" + (liste.size() - 1);
        String numberOfFoundXPaths = "" + allFound;
        String numberOfAllXPaths = "" + allSegments.size();
        String numberOfCorrectLabels = "" + allCorrect;
        String numberOfIncorrectLabels = "" + (liste.size() - 1 - allCorrect);
        String numberOfNotAssignabels = "" + allNotAssignabel;

        String allNumbers = numberOfGuessedXPaths + sep + numberOfFoundXPaths + sep + numberOfAllXPaths + sep
                + numberOfCorrectLabels + sep + numberOfIncorrectLabels + sep + numberOfNotAssignabels;

        liste.add("");
        liste.add("guessed XP" + sep + "found XP" + sep + "all XP" + sep + "corr. Label" + sep + "incorr. Label" + sep
                + "not assignabel");
        liste.add(allNumbers);

        FileHelper.writeToFile(fullPlace + name + "_ausgewertet.csv", liste);

        List<String> mainListe = (ArrayList<String>) FileHelper.readFileToArray(mainFile);
        boolean foundEntry = false;
        for (int i = 1; i < mainListe.size(); i++) {
            if (mainListe.get(i).split(sep)[0].equals(liste.get(1).split(sep)[0])) {
                mainListe.set(i, mainListe.get(i).split(sep)[0] + sep + allNumbers);
                foundEntry = true;
            }
        }
        if (!foundEntry) {
            mainListe.add(liste.get(1).split(sep)[0] + sep + allNumbers);
        }
        FileHelper.writeToFile(mainFile, mainListe);

    }

}
