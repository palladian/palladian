package tud.iir.extraction.qa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.ho.yaml.Yaml;

import tud.iir.classification.qa.AnswerClassifier;
import tud.iir.classification.qa.AnswerFeatures;
import tud.iir.control.Controller;
import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.extraction.Extractor;
import tud.iir.extraction.PageAnalyzer;
import tud.iir.gui.GUIManager;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.knowledge.QA;
import tud.iir.persistence.DatabaseManager;
import tud.iir.web.Crawler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.BlockDistance;
import uk.ac.shef.wit.simmetrics.similaritymetrics.CosineSimilarity;
import uk.ac.shef.wit.simmetrics.similaritymetrics.DiceSimilarity;
import uk.ac.shef.wit.simmetrics.similaritymetrics.EuclideanDistance;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.OverlapCoefficient;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

/**
 * The main class for the Q/A extraction. (QUAX) QUAX knows a set of Q/A pages with information about question xPaths and answer xPaths. QUAX performs a focused
 * crawl over the Q/A pages and remembers URLs of visited pages also over extraction session. New Q/As are extracted and written in the database.
 * 
 * @author David Urbansky
 */
public class QAExtractor extends Extractor {

    /** the instance of this class */
    private static QAExtractor INSTANCE = new QAExtractor();

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(QAExtractor.class);

    // private static final Logger flashOutputLogger = Logger.getLogger("FlashOutputLogger");

    /** if true, more statistics are gathered for benchmarking purposes */
    // private boolean benchmark = false;

    /** list of QA tuples */
    private List<QA> qas;

    /** count the number of extracted questions */
    private int qaExtractionCount = 0;

    /** a classifier for answers */
    private AnswerClassifier answerClassifier;

    /** the list of QA sites that are used for crawling and extraction */
    private QASites qaSites;

    /** an FAQ site which holds all question hashes and visited URLs for any FAQ site during crawling */
    private QASite faqSite;

    /** a page analyzer */
    private PageAnalyzer pa;

    private QAExtractor() {
        setPa(new PageAnalyzer());
        initialize();
    }

    /**
     * Get the instance of the QAExtractor, which itself is singleton.
     * 
     * @return The QAExtractor instance.
     */
    public static QAExtractor getInstance() {
        return INSTANCE;
    }

    private void initialize() {

        // keep track of extracted q/a
        qas = new ArrayList<QA>();

        // create an empty qaSite for all FAQs that could occur while crawling
        HashMap<String, Object> siteInformation = new HashMap<String, Object>();
        siteInformation.put("name", "");
        siteInformation.put("type", "FAQ");
        siteInformation.put("maximumURLs", 1);
        siteInformation.put("entryURL", "");
        siteInformation.put("questionXPath", "");

        faqSite = new QASite(siteInformation);
        qaSites = new QASites();
        qaSites.add(faqSite);

        // update the GUI if it is initialized
        if (GUIManager.isInstanciated()) {
            // logger.addObserver(GUIManager.getInstance());
        }
    }

    public void setAnswerClassifier(int type) {
        answerClassifier = new AnswerClassifier(type);
        answerClassifier.useTrainedClassifier();
    }

    /**
     * Load descriptions of sites to extract question and answers from.
     * 
     * @param continueExtraction If false, the extraction will start from the beginning, if true, it is tried to deserialize QASites from previous extraction
     *            run.
     */
    @SuppressWarnings("unchecked")
    private void loadSiteDescriptions() {
        qaSites = new QASites();

        // restart qa extraction
        if (!ExtractionProcessManager.isContinueQAExtraction()) {

            try {
                Object object = Yaml.load(new File(Controller.getConfig().getString("ontology.qaSites")));
                HashMap<String, ArrayList<HashMap<String, Object>>> siteMap = (HashMap<String, ArrayList<HashMap<String, Object>>>) object;

                ArrayList<HashMap<String, Object>> sites = siteMap.get("sites");

                for (int i = 0; i < sites.size(); i++) {
                    HashMap<String, Object> entry = sites.get(i);
                    QASite qaSite = new QASite(entry);
                    qaSites.add(qaSite);
                }
                // QASite qaSite = Yaml.loadType(new File("data/knowledgeBase/qaSiteConfigurations.yml"),QASite.class);
                LOGGER.info("Q/A sites loaded, start extraction...");
            } catch (FileNotFoundException e) {
                LOGGER.error(e.getMessage());
            }

            // continue qa extraction
        } else {

            // copy file first in case there is a problem
            FileHelper.copyFile("data/status/qaSites.ser", "data/status/qaSites" + System.currentTimeMillis() + ".ser");

            FileInputStream fis = null;
            ObjectInputStream in = null;

            try {
                fis = new FileInputStream("data/status/qaSites.ser");
                in = new ObjectInputStream(fis);

                Object obj = in.readObject();

                if (obj instanceof QASites) {
                    qaSites = (QASites) obj;
                }

                LOGGER.info("successfully loaded Q/A states, continue extraction now...");

            } catch (FileNotFoundException e) {
                LOGGER.error("could not continue extraction of QAs, restarting now", e);
                ExtractionProcessManager.setContinueQAExtraction(false);
                loadSiteDescriptions();
            } catch (IOException e) {
                LOGGER.error("could not continue extraction of QAs, restarting now", e);
                ExtractionProcessManager.setContinueQAExtraction(false);
                loadSiteDescriptions();
            } catch (ClassNotFoundException e) {
                LOGGER.error("could not continue extraction of QAs, restarting now", e);
                ExtractionProcessManager.setContinueQAExtraction(false);
                loadSiteDescriptions();
            }
        }
    }

    /**
     * Log a message for Flash.
     * 
     * @param message The message to log.
     */
    // private void logForFlash(String message) {
    // flashOutputLogger.info(message);
    // }

    /**
     * The Q/A extraction is a bootstrapped process with two steps alternately performed in a loop: 1: use a seed query to retrieve urls with question and
     * answers 2: perform a focused crawling on each retrieved url to increase the Q/A set
     */
    public void startExtraction() {
        startExtraction(true);
    }

    public void startExtraction(boolean continueExtraction) {

        setStopped(false);
        LOGGER.info("start qa extraction");

        ExtractionProcessManager.setContinueQAExtraction(continueExtraction);

        loadSiteDescriptions();

        // create a thread group
        extractionThreadGroup = new ThreadGroup("qaExtractionThreadGroup");

        // alternate over all qa sites
        int i = 0;
        int numberOfSites = qaSites.size();

        // if all sites do not have any URLs on stack anymore, they vote for stop
        int stopVotes = 0;
        for (QASite qaSite : qaSites) {
            if (qaSite.hasVoted()) {
                stopVotes++;
            }
        }

        int iterations = 0;
        while (stopVotes < numberOfSites /* && iterations < 10000 */&& !isStopped()) {
            // i = 2;
            QASite qaSite = qaSites.get(i);

            // wait if maximum number of threads are running
            int maxThreads = 1;
            if (iterations > numberOfSites || ExtractionProcessManager.isContinueQAExtraction()) {
                maxThreads = 3 * numberOfSites;
            }
            while (getThreadCount() >= maxThreads) {
                LOGGER.info("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + " active threads)");
                ThreadHelper.sleep(WAIT_FOR_FREE_THREAD_SLOT);
                if (extractionThreadGroup.activeCount() + extractionThreadGroup.activeGroupCount() == 0) {
                    LOGGER.warn("apparently " + getThreadCount() + " threads have not finished correctly but thread group is empty, continuing...");
                    resetThreadCount();
                    break;
                }
            }

            if (qaSite.urlsAvailable()) {
                LOGGER.info("getting url from website " + i + ":" + qaSite.getName());

                Thread qaThread = new QAExtractionThread(extractionThreadGroup, "QA Extraction Thread for " + qaSite.getName(), qaSite);
                qaThread.start();

            } else if (!qaSite.hasVoted()) {
                LOGGER.info("STOP VOTE from page " + qaSite.getName() + " after iteration " + iterations + " having " + qaSite.getURLStackSize()
                        + " URLs on the stack");
                stopVotes++;
                qaSite.setVoted();
            }

            LOGGER.info("### iteration number " + iterations);
            LOGGER.info("### total url stack of all " + qaSites.size() + " pages: " + qaSites.getTotalURLStackSize() + ", " + stopVotes + " stop votes, "
                    + getThreadCount() + " threads active)");
            LOGGER.info("### total Q/As extracted so far " + qaExtractionCount + " (" + qas.size() + " unsaved)");
            // logForFlash("p = new Point("+iterations+","+qas.size()+"); dataPoints10.push(p);");
            // logForFlash("p = new Point("+iterations+","+qaSites.getTotalURLStackSize()+"); dataPoints11.push(p);");

            // use another site in the next iteration
            i = (i + 1) % numberOfSites;
            iterations++;
        }

        // stopExtraction(true);

        // save logs
        LOGGER.info("total Q/As extracted in this run: " + qaExtractionCount);
    }

    @Override
    protected void saveExtractions(boolean saveExtractions) {
        if (saveExtractions) {
            saveQAs();
            LOGGER.info("saved QAs to database...");
            saveExtractionStatus();
        }
    }

    private void saveExtractionStatus() {
        // serialize current state of qa sites
        LOGGER.info("serialize states of Q/A sites...");
        qaSites.serialize();
        LOGGER.info("...successfully serialized states of Q/A sites");
    }

    /**
     * Analyze page for FAQ and extract QA tuples if possible.
     * 
     * @param url The url to analyze.
     * @return A set of QA tuples if an FAQ was found.
     */
    public ArrayList<QA> extractFAQ(String url) {

        ArrayList<QA> faqQAs = new ArrayList<QA>();

        // set up the crawler
        Crawler crawler = new Crawler();
        crawler.setDocument(url);
        pa.setDocument(crawler.getDocument());

        // check if FAQ or a similar string appears on site
        String siteText = StringHelper.trim(pa.getDocumentTextDump());
        String siteTextLowerCase = siteText.toLowerCase();
        if (siteTextLowerCase.indexOf("faq") > -1 || siteTextLowerCase.indexOf("f.a.q.") > -1 || siteTextLowerCase.indexOf("frequently asked questions") > -1) {

            // detect all questions from the FAQ page
            ArrayList<String> questions = detectQuestions();

            if (questions.size() > 0) {

                // String pageContent = siteText;//pa.getDocumentAsString();
                // pageContent = StringHelper.unescapeHTMLEntities(pageContent);

                // String pageContentLowerCase = siteText;//pageContent.toLowerCase();

                // iterate through all question pairs
                for (int i = 0; i < questions.size(); i++) {

                    String question1 = questions.get(i);

                    // boolean questionNew = faqSite.addQuestionHash(question1.hashCode());
                    // if (!questionNew) continue;

                    String answer = "";
                    int index1 = -1;
                    int index2 = -1;

                    // if there is another question after the current one, try to find the answer in between
                    if (i < questions.size() - 1) {
                        String question2 = questions.get(i + 1);

                        // get the content between questions
                        index1 = siteTextLowerCase.lastIndexOf(question1.toLowerCase());
                        index2 = siteTextLowerCase.lastIndexOf(question2.toLowerCase());// ,index1 + question1.length());
                        // index2 = pageContent.lastIndexOf(question2,index1 + question1.length());

                        // otherwise take all until the next closing tag.
                    } else {
                        index1 = siteTextLowerCase.lastIndexOf(question1.toLowerCase());
                        index2 = siteTextLowerCase.lastIndexOf("</", index1 + question1.length() + 15);
                    }

                    if (index2 <= index1 || index1 == -1) {
                        continue;
                    }

                    answer = siteText.substring(index1 + question1.length(), index2);
                    // answer = StringHelper.removeHTMLTags(answer, true, true, true, true);
                    // do not remove links
                    // answer = answer.replaceAll("(<[^a].*?[^a]>)|(^.*?[^a]>)|(<[^>]*?$)","");

                    // remove numbers and "Q:" before question
                    question1 = StringHelper.removeNumbering(question1);
                    question1 = question1.replaceAll("^\\s*(q|Q):\\s*", "");

                    // create Q/A
                    QA qa = new QA(faqSite);
                    qa.setExtractedAt(new Date());
                    HashSet<String> questionXPaths = pa.constructAllXPaths(question1);
                    String questionXPath = "";
                    if (questionXPaths.size() > 0) {
                        questionXPath = questionXPaths.iterator().next();
                    }
                    qa.setQuestion(question1, url, questionXPath);
                    if (answer.length() > 0) {
                        String answerXPath = "";
                        HashSet<String> answerXPaths = pa.constructAllXPaths(answer);
                        if (answerXPaths.size() > 0) {
                            answerXPath = answerXPaths.iterator().next();
                        }
                        qa.addAnswer(answer, url, answerXPath);
                        // addQA(qa);
                        faqQAs.add(qa);
                    }
                }
            }
        }

        return faqQAs;
    }

    private ArrayList<String> detectQuestions() {
        HashSet<String> xPathSet = pa.constructAllXPaths("?");
        String questionXPath = pa.makeMutualXPath(xPathSet);

        return pa.getTextsByXPath(questionXPath);
    }

    /**
     * Add a QA tuple and save them if they are over a certain number. This method is called by QAExtractionThreads and thus must be synchronized.
     * 
     * @param qa The QA tuple to add.
     */
    public synchronized void addQA(QA qa) {
        qas.add(qa);
        qaExtractionCount++;
        if (qas.size() >= 100) {
            saveQAs();
        }
    }

    /**
     * Save QA tuples to the database.
     */
    private void saveQAs() {
        if (qas.size() > 0) {
            LOGGER.info("save extracted Q/As to database");
            DatabaseManager.getInstance().addQAs(qas);
            qas.clear();
        }
    }

    /**
     * Detect an answer without knowing its xPath. Build a candidate set, detect features and use a learned classifier to rank the candidates.
     * 
     * @param question
     * @param pa
     * @return A two entry long string array with the answer and its XPath.
     */
    public String[] detectAnswer(String question, LinkedHashSet<String> questionXPaths) {
        String[] answerInformation = new String[2];

        // build candidate set of answers
        LinkedHashSet<String> answerCandidates = pa.constructAllXPaths(" ");

        // filter answer candidates
        LinkedHashSet<String> filteredAnswerCandidates = filterAnswerCandidates(questionXPaths, answerCandidates);

        // get features for each candidate and classify/rank each candidate
        Map<String, Double> rankedCandidateAnswers = new HashMap<String, Double>();
        for (Iterator<String> iterator = filteredAnswerCandidates.iterator(); iterator.hasNext();) {

            String answerCandidateXPath = iterator.next();
            // System.out.println("xpath: " + answerCandidateXPath);
            // String answerContent = pa.getTextByXPath(answerCandidateXPath);
            String htmlAnswerContent = pa.getHTMLTextByXPath(answerCandidateXPath);

            // get features for each candidate
            AnswerFeatures af = getAnswerFeatures(question, htmlAnswerContent);

            // classify/rank each candidate
            double rank = answerClassifier.rankAnswer(af);
            rankedCandidateAnswers.put(answerCandidateXPath, rank);
        }

        // choose best ranked answer candidate
        String highestRankedXPath = "";
        double highestRank = -1.0;
        for (Iterator<Map.Entry<String, Double>> iterator = rankedCandidateAnswers.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Double> entry = iterator.next();
            if (entry.getValue() > highestRank) {
                highestRankedXPath = entry.getKey();
                highestRank = entry.getValue();
            }
            //String answerContent = pa.getTextByXPath(entry.getKey());
            // System.out.println("answer: " + answerContent.substring(0,Math.min(100,answerContent.length())));
            // System.out.println("probability: " + entry.getValue()+"\n");
        }

        answerInformation[0] = pa.getTextByXPath(highestRankedXPath);
        answerInformation[1] = highestRankedXPath;

        return answerInformation;
    }

    /**
     * Filter out candidates that point to the same or a parent xPath of the question.
     * 
     * @return A filtered set of candidate answers.
     */
    public LinkedHashSet<String> filterAnswerCandidates(LinkedHashSet<String> questionXPaths, LinkedHashSet<String> answerCandidates) {
        LinkedHashSet<String> filteredAnswerCandidates = new LinkedHashSet<String>();

        // answers are only expected in block elements
        String[] targetNodes = { "div", "p", "td", "h2", "h3", "h4", "h5", "h6", "font" };
        answerCandidates = PageAnalyzer.keepXPathPointingTo(answerCandidates, targetNodes);

        for (Iterator<String> iterator = answerCandidates.iterator(); iterator.hasNext();) {
            String xPath = iterator.next();

            // check if current xPath is prefix of question xPaths
            boolean isPrefix = false;
            for (Iterator<String> iterator2 = questionXPaths.iterator(); iterator2.hasNext();) {
                String string = iterator2.next();

                if (string.startsWith(xPath)) {
                    isPrefix = true;
                    break;
                }
            }

            if (isPrefix) {
                // System.out.println("skipping");
                continue;
            }

            filteredAnswerCandidates.add(xPath);
        }

        return filteredAnswerCandidates;
    }

    /**
     * Get features for the given answer.
     * 
     * @return
     */
    public AnswerFeatures getAnswerFeatures(String question, String htmlAnswer) {

        String answer = StringHelper.removeHTMLTags(htmlAnswer, true, true, true, true);

        AnswerFeatures af = new AnswerFeatures();

        OverlapCoefficient oc = new OverlapCoefficient();
        QGramsDistance qg = new QGramsDistance();
        CosineSimilarity cs = new CosineSimilarity();
        BlockDistance bd = new BlockDistance();
        ;
        DiceSimilarity ds = new DiceSimilarity();
        EuclideanDistance ed = new EuclideanDistance();
        JaccardSimilarity js = new JaccardSimilarity();
        JaroWinkler jw = new JaroWinkler();

        // word count
        af.setAnswerWordCount(answer.split("\\s").length);

        // string similarity to question using different measures
        // af.setSimilarity1(oc.getSimilarity(question,answer));
        // af.setSimilarity2(qg.getSimilarity(question,answer));
        // af.setSimilarity3(cs.getSimilarity(question,answer));
        // af.setSimilarity4(bd.getSimilarity(question,answer));
        // af.setSimilarity5(ds.getSimilarity(question,answer));
        // af.setSimilarity6(ed.getSimilarity(question,answer));
        // af.setSimilarity7(js.getSimilarity(question,answer));
        // af.setSimilarity8(jw.getSimilarity(question,answer));

        String answerStart = StringHelper.removeSpecialChars(answer.substring(0, Math.min(100, answer.length())));
        af.setSimilarity1(oc.getSimilarity(question, answerStart));
        af.setSimilarity2(qg.getSimilarity(question, answerStart));
        af.setSimilarity3(cs.getSimilarity(question, answerStart));
        af.setSimilarity4(bd.getSimilarity(question, answerStart));
        af.setSimilarity5(ds.getSimilarity(question, answerStart));
        af.setSimilarity6(ed.getSimilarity(question, answerStart));
        af.setSimilarity7(js.getSimilarity(question, answerStart));
        af.setSimilarity8(jw.getSimilarity(question, answerStart));

        // "A:" or "Answer:" before answer
        if (answerHintAvailable(answer)) {
            af.setAnswerHintBeforeAnswer(1);
        } else {
            af.setAnswerHintBeforeAnswer(0);
        }

        // tag and word distance to question
        Integer[] tagAndWordDistanceToQuestion = getTagAndWordDistances(question, answer);
        af.setTagDistance(tagAndWordDistanceToQuestion[0]);
        af.setWordDistance(tagAndWordDistanceToQuestion[1]);

        // number of tags in the html answer
        int tagCount = StringHelper.countTags(htmlAnswer, false);
        af.setTagCount(tagCount);

        // number of distinct tags in the html answer
        int distinctTagCount = StringHelper.countTags(htmlAnswer, true);
        af.setDistinctTagCount(distinctTagCount);

        return af;
    }

    /**
     * Check whether an "Answer" or "A:" can be found in front of the answer candidate.
     * 
     * @param answer The answer candidate.
     * @return True if a hint has been found.
     */
    private boolean answerHintAvailable(String answer) {
        String pageContent = pa.getDocumentAsString();
        pageContent = StringHelper.removeHTMLTags(pageContent, true, false, true, true);
        pageContent = StringHelper.unescapeHTMLEntities(pageContent);

        int indexOfAnswer = pageContent.indexOf(answer);
        int indexOfAnswerHint = -1;
        while (pageContent.indexOf("Answer", indexOfAnswerHint) > -1) {
            int newIndexOfAnswerHint = pageContent.indexOf("Answer", indexOfAnswerHint);
            if (newIndexOfAnswerHint > indexOfAnswer) {
                break;
            }
            indexOfAnswerHint = newIndexOfAnswerHint + 6;
        }

        int indexOfAHint = -1;
        while (pageContent.indexOf("A:", indexOfAHint) > -1) {
            int newIndexOfAHint = pageContent.indexOf("Answer", indexOfAHint);
            if (newIndexOfAHint > indexOfAnswer) {
                break;
            }
            indexOfAHint = newIndexOfAHint + 2;
        }

        if (indexOfAnswerHint > -1 && indexOfAnswerHint < indexOfAnswer || indexOfAHint > -1 && indexOfAHint < indexOfAnswer) {
            return true;
        }

        return false;
    }

    /**
     * Get the tag and word distance from the question to the answer candidate.
     * 
     * @param question The question.
     * @param answer The answer candidate.
     * @return 0: The tag distance, 1: the word distance.
     */
    private Integer[] getTagAndWordDistances(String question, String answer) {
        Integer[] distances = new Integer[2];
        distances[0] = -1;
        distances[1] = 200;

        String pageContent = pa.getDocumentAsString();
        pageContent = StringHelper.unescapeHTMLEntities(pageContent);
        String pageContentNoTags = StringHelper.removeHTMLTags(pageContent, true, true, true, true);

        int answerIndex = pageContentNoTags.indexOf(answer);

        int questionIndex = -1;
        while (pageContentNoTags.indexOf(question, questionIndex) > -1) {
            int newQuestionIndex = pageContentNoTags.indexOf(question, questionIndex);
            if (newQuestionIndex > answerIndex) {
                break;
            }
            questionIndex = newQuestionIndex + question.length();
        }

        if (questionIndex == -1 || answerIndex == -1 || questionIndex > answerIndex) {
            // Logger.getInstance().logError("getTagAndWordDistance failed because question or answer was not found or answer was found before question", new
            // Throwable(""));
            return distances;
        }

        String diffText = pageContentNoTags.substring(questionIndex, answerIndex);

        // TODO get tag distance
        // distances[0] = StringHelper.countTags(diffText);
        //			
        // if (distances[0] == -1) {
        // distances[0] = 100;
        // }

        // get word count without tags
        distances[1] = diffText.split("\\s").length;

        return distances;
    }

    public void runQAFromOfflineTestset() {

        // load the questions to the web pages in an array
        ArrayList<String> questions = FileHelper.readFileToArray("data/benchmarkSelection/qa/testset/questions.txt");

        // iterate through all web pages of the qa test set
        File folder = new File("data/benchmarkSelection/qa/testset");
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (!listOfFiles[i].isFile()) {
                continue;
            }
            if (listOfFiles[i].getName().endsWith("txt")) {
                continue;
            }

            int questionIndex = Integer.valueOf(listOfFiles[i].getName().replaceAll("webpage", "").replaceAll(".html", "")) - 1;

            String question = questions.get(questionIndex);
            if (question.endsWith("?")) {
                question = question.substring(0, question.length() - 1);
            // question = StringHelper.removeStopWords(question);
            }

            LOGGER.info("\nFile " + listOfFiles[i].getName() + ", question: " + question);

            // OverlapCoefficient oc = new OverlapCoefficient();
            QGramsDistance qg = new QGramsDistance();
            // CosineSimilarity cs = new CosineSimilarity();
            // BlockDistance bd = new BlockDistance();
            // ;
            // DiceSimilarity ds = new DiceSimilarity();
            // EuclideanDistance ed = new EuclideanDistance();
            // JaccardSimilarity js = new JaccardSimilarity();
            // JaroWinkler jw = new JaroWinkler();

            PageAnalyzer pa = new PageAnalyzer();
            pa.setDocument("data/benchmarkSelection/qa/testset/" + listOfFiles[i].getName());

            LinkedHashSet<String> qXpaths = pa.constructAllXPaths(question);
            LOGGER.info("the questions points to " + qXpaths.size() + " xpaths");

            double topSim = 0.0;
            String mostSimString = "";
            LinkedHashSet<String> xPaths = pa.constructAllXPaths(" ");
            for (Iterator<String> iterator = xPaths.iterator(); iterator.hasNext();) {
                String xPath = iterator.next();
                // System.out.println(xPath);

                // check if current xPath is prefix of question xpaths
                boolean isPrefix = false;
                for (Iterator<String> iterator2 = qXpaths.iterator(); iterator2.hasNext();) {
                    String string = iterator2.next();

                    if (string.startsWith(xPath)) {
                        isPrefix = true;
                        break;
                    }
                }

                if (isPrefix) {
                    // System.out.println("skipping");
                    continue;
                }

                String content = pa.getTextByXPath(xPath);
                double sim = qg.getSimilarity(question, content);
                if (sim > topSim) {
                    topSim = sim;
                    mostSimString = content;
                }
            }

            mostSimString = StringHelper.trim(mostSimString);
            LOGGER.info(topSim + " | " + mostSimString);
        }

    }

    public PageAnalyzer getPa() {
        return pa;
    }

    public void setPa(PageAnalyzer pa) {
        this.pa = pa;
    }

    public static void main(String[] arguments) {

        QAExtractor.getInstance().startExtraction(false);
        System.exit(0);

        ArrayList<QA> qas0 = QAExtractor.getInstance().extractFAQ("http://blog.pandora.com/faq/");
        // ArrayList<QA> qas0 = QAExtractor.getInstance().extractFAQ("http://secondlife.com/whatis/faq.php");
        // ArrayList<QA> qas0 = QAExtractor.getInstance().extractFAQ("http://www.freerice.com/faq.html");
        // ArrayList<QA> qas0 = QAExtractor.getInstance().extractFAQ("http://bandcamp.com/faq");
        // ArrayList<QA> qas0 = QAExtractor.getInstance().extractFAQ("http://www.copyright.gov/help/faq/faq-register.html");
        // ArrayList<QA> qas0 = QAExtractor.getInstance().extractFAQ("http://www.cookiecentral.com/faq/");
        // ArrayList<QA> qas0 = QAExtractor.getInstance().extractFAQ("http://wiki.creativecommons.org/Frequently_Asked_Questions");

        CollectionHelper.print(qas0);

        System.exit(0);

        String a = "div></b><br> <a href=\"/cawp/abbzh259/42e61c619d42325bc1256c4e003de6e3.aspx\" >Contact us</a><br> <br> <b><a name=\"Can**I**subscribe**to**get**your**news**automatically?\"></a><div class=\"subHeadline\"";
        // a = a.replaceAll("(<[^a].*?>)|(<?[^a]*?>)","");
        a = a.replaceAll("(<[^a].*?>)|(^.*?>)|(<[^>]*?$)", "");
        System.out.println(a);
        // System.exit(0);

        // QAExtractor.getInstance().runQAFromOfflineTestset();
        QAExtractor.getInstance().startExtraction();
        System.exit(0);

        HashSet<String> a2 = new HashSet<String>();
        for (int i = 0; i < 1000000; i++) {
            String s = "absdfahsdfjhasdlfhasdjfhasjkdfhaskldjfhasdhfasdhfuoaweruogzwuodgkas" + i;
            a2.add("-" + s.hashCode());
        }
        long t1 = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            String s = i + "sdo54uh23895zr " + i + " jsdhnfljsdflsdfn" + i;
            a2.contains("-" + s.hashCode());
        }

        long t2 = System.currentTimeMillis() - t1;
        System.out.println(t2);

        System.exit(0);

        ArrayList<QA> qas = QAExtractor.getInstance().extractFAQ("http://www.abb.com/cawp/abbzh259/14ea3e81ca94263bc1256fbe0030e163.aspx");
        // HashSet<QA> qas = QAExtractor.getInstance().extractFAQ("http://www.sony.com/faq.shtml");
        // HashSet<QA> qas = QAExtractor.getInstance().extractFAQ("http://csottointer.kissnofrog.com/info/faq");

        CollectionHelper.print(qas);

        System.exit(0);

        Crawler c = new Crawler();
        PageAnalyzer pa = new PageAnalyzer();
        // c.setDocument("http://www.mahalo.com/answers/answer/i-just-found-a-banana-spider-living-on-my-deck-what-should-i-do");
        // c.setDocument("http://www.mahalo.com/answers/graphic-design/i-can-only-use-an-elliptical-marquee-tool-on-my-photoshop-7-program-how-can-i-include-the-rectangular-and-other-tools");
        // c.setDocument("http://answers.yahoo.com/question/index;_ylt=A0WTcZawr0dKKfoAXg_e7BR.;_ylv=3?qid=20090628104449AAblSvO");
        // c.setDocument("http://www.mahalo.com/answers/celebrities/do-you-hate-miley-cyrus-hannah-montana-why");
        // c.setDocument("http://www.abb.com/cawp/abbzh259/14ea3e81ca94263bc1256fbe0030e163.aspx");
        c.setDocument("http://yedda.com/questions/unified_voice_calling_aid_Haiti_518111363398926/");
        pa.setDocument(c.getDocument());
        CollectionHelper.print(pa.constructAllXPaths("need to survive Kids out there are"));
        System.out.println("result: " + pa.getTextByXPath("/html/body/div/form/div/div/div[2]/div[2]/div[1]/div[1]/div[2]/div[2]/h1[1]/span[1]".toUpperCase()));
        System.out.println("result: " + pa.getTextByXPath("/html/body/div[2]/div[1]/div[1]/div[1]/div[7]/div[3]/div[1]/div[2]/div[2]".toUpperCase()));
        CollectionHelper.print(pa.getTextsByXPath("/HTML/BODY/DIV[1]/DIV[2]/DIV[1]/DIV[1]/DIV[2]/DIV[1]/DIV".toUpperCase()));
    }
}