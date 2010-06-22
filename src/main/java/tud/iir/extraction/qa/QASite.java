package tud.iir.extraction.qa;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.StringHelper;

public class QASite implements Serializable {

    private static final long serialVersionUID = -3232813296290133466L;

    // QA types
    public static int FAQ = 1;
    public static int QA_SITE = 2;

    // the name of the site for identification
    private String name = "";

    // the type of the expected QAs
    private int type = 0;

    // the maximum number of URLs that are crawled, -1 is infinity
    private int maximumURLs = -1;

    // the first URL to start the crawling process
    private String entryURL = "";

    // the xPath that points to the question
    private String questionXPath = "";

    // the xPath that points to the "best answer"
    private String bestAnswerXPath = "";

    // the xPath that points to all other answers
    private String allAnswersXPath = "";

    // prefix that must appear before the answer
    private String answerPrefix = "";

    // suffix that must appear after the answer
    private String answerSuffix = "";

    // the stack on which the URLs are kept that need to be crawled
    private QAUrlStack urlStack = null;

    // the stack of visited urls
    // TODO performance: write into index because memory fills up otherwise
    private HashSet<String> urlsVisited = null;

    // the prefix for pages with Q/As on them, preferably analyze those pages first
    private String greenPrefix = "";

    // how many "/" appear in the url?
    private int greenUrlDepth = 0;

    // true if two different URLs to different questions have been processed to on common prefix
    private boolean greenPrefixCreated = false;

    // the prefix for pages that directly link to pages with Q/As on them, preferably analyze those pages right after the green ones
    private HashSet<String> yellowPrefixes = null;

    // the prefix for pages that have no Q/As on them and it is still unknown whether they link to those pages.
    private HashSet<String> redPrefixes = null;

    // if no URLs are available or the maximum number of URLs exceeded, the page votes for stopping the crawler
    private boolean voted = false;

    // count number of times no green, yellow or non-red prefix url has been found to clean url stack
    private int nothingFoundCount = 0;

    // count number of times no question and answer has been extracted, if too often in a row, choose url from stack randomly
    private int extractionFailCount = 0;

    // save hashes of questions, to not extract the same question multiple times
    // TODO performance: write into index because memory fills up otherwise
    private TreeSet<Integer> questionHashes = null;

    public QASite(HashMap<String, Object> siteInformation) {
        setName((String) siteInformation.get("name"));
        setType((String) siteInformation.get("type"));
        setMaximumURLs(siteInformation.get("maximumURLs"));
        setEntryURL((String) siteInformation.get("entryURL"));
        setQuestionXPath((String) siteInformation.get("questionXPath"));

        setBestAnswerXPath((String) siteInformation.get("bestAnswerXPath"));
        setAllAnswersXPath((String) siteInformation.get("allAnswersXPath"));

        if (getType() == QASite.QA_SITE) {
            setAnswerPrefix((String) siteInformation.get("answerPrefix"));
            setAnswerSuffix((String) siteInformation.get("answerSuffix"));
        }

        QAUrl entry = new QAUrl(entryURL, "");
        urlStack = new QAUrlStack();
        urlStack.add(entry);

        yellowPrefixes = new HashSet<String>();
        redPrefixes = new HashSet<String>();

        questionHashes = new TreeSet<Integer>();

        urlsVisited = new HashSet<String>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(String type) {
        if (type.equalsIgnoreCase("FAQ")) {
            this.type = QASite.FAQ;
        } else if (type.equalsIgnoreCase("QASite")) {
            this.type = QASite.QA_SITE;
        }
    }

    public int getMaximumURLs() {
        return maximumURLs;
    }

    public void setMaximumURLs(Object maximumURLs) {
        if (maximumURLs == null)
            return;
        try {
            this.maximumURLs = (Integer) maximumURLs;
        } catch (Exception e) {
            Logger.getLogger(QAExtractor.class).error(this.getName() + " maximum urls", e);
        }
    }

    public String getEntryURL() {
        return entryURL;
    }

    public void setEntryURL(String entryURL) {
        this.entryURL = entryURL;
    }

    public String getQuestionXPath() {
        return questionXPath.toUpperCase();
    }

    public void setQuestionXPath(String questionXPath) {
        this.questionXPath = questionXPath;
    }

    public String getBestAnswerXPath() {
        if (this.bestAnswerXPath == null)
            return "";
        return this.bestAnswerXPath.toUpperCase();
    }

    public void setBestAnswerXPath(String bestAnswerXPath) {
        this.bestAnswerXPath = bestAnswerXPath;
    }

    public String getAllAnswersXPath() {
        if (this.allAnswersXPath == null)
            return "";
        return this.allAnswersXPath.toUpperCase();
    }

    public void setAllAnswersXPath(String allAnswersXPath) {
        this.allAnswersXPath = allAnswersXPath;
    }

    public String getAnswerPrefix() {
        return answerPrefix;
    }

    public void setAnswerPrefix(String answerPrefix) {
        this.answerPrefix = answerPrefix;
    }

    public String getAnswerSuffix() {
        return answerSuffix;
    }

    public void setAnswerSuffix(String answerSuffix) {
        this.answerSuffix = answerSuffix;
    }

    public int getURLStackSize() {
        return urlStack.size();
    }

    /**
     * Try to get green prefix urls (pages with Q/As) first. If none of these is available try to get yellow prefix urls (urls that directly point to Q/A
     * pages). If none of these is available, take any url.
     * 
     * @return
     */
    public synchronized QAUrl getURLFromStack() {

        // try to find green, yellow and non-red prefix urls
        QAUrl greenPrefixUrl = null;
        QAUrl yellowPrefixUrl = null;
        QAUrl nonRedPrefixUrl = null;
        for (Iterator<QAUrl> iterator = urlStack.iterator(); iterator.hasNext();) {
            QAUrl qaUrl = iterator.next();

            int depthCount = getUrlDepth(qaUrl.getUrl());

            if (greenPrefixCreated() && qaUrl.getUrl().startsWith(getGreenPrefix()) && !hasRedPrefix(qaUrl.getUrl()) && getGreenPrefix().length() > 0) {
                if (depthCount == greenUrlDepth) {
                    Logger.getLogger(QAExtractor.class).info("get GREEN URL directly " + qaUrl.getUrl());
                    removeURLFromStack(qaUrl);
                    qaUrl.setType(QAUrl.GREEN);
                    return qaUrl;
                } else {
                    greenPrefixUrl = qaUrl;
                    greenPrefixUrl.setType(QAUrl.GREEN);
                }
            } else if (greenPrefixCreated() && yellowPrefixUrl == null && hasYellowPrefix(qaUrl.getUrl())) {
                yellowPrefixUrl = qaUrl;
                yellowPrefixUrl.setType(QAUrl.YELLOW);
            } else if (nonRedPrefixUrl == null && !hasRedPrefix(qaUrl.getUrl()) && depthCount > 1) {
                nonRedPrefixUrl = qaUrl;
                nonRedPrefixUrl.setType(QAUrl.NON_RED);
            }
        }

        // return green prefix url if available
        if (greenPrefixUrl != null) {
            removeURLFromStack(greenPrefixUrl);
            return greenPrefixUrl;
        }

        // return yellow prefix url if available
        if (yellowPrefixUrl != null) {
            cleanUpStack();
            removeURLFromStack(yellowPrefixUrl);
            return yellowPrefixUrl;
        }

        // return yellow prefix url if available
        if (nonRedPrefixUrl != null && extractionFailCount < 5) {
            removeURLFromStack(nonRedPrefixUrl);
            return nonRedPrefixUrl;
        }

        // if no green, yellow or non-red URL has been found for a few times in a row, clean up the stack
        nothingFoundCount++;
        if (nothingFoundCount >= 3) {
            cleanUpStack();
            nothingFoundCount = 0;
        }

        // return any url if nothing else is available
        if (urlStack.size() > 0) {
            QAUrl qaUrl = urlStack.iterator().next();
            removeURLFromStack(qaUrl);
            return qaUrl;
        }
        return new QAUrl("", "");
    }

    public synchronized void addURLToStack(QAUrl url) {
        if (!urlsVisited.contains(url.getUrl()) && !urlStack.contains(url)) {
            urlStack.add(url);
        }
    }

    public synchronized void removeURLFromStack(QAUrl url) {
        urlStack.remove(url);
        urlsVisited.add(url.getUrl());
    }

    /**
     * Remove all red prefix urls from stack before getting more urls from a yellow prefix site.
     */
    private synchronized void cleanUpStack() {
        QAUrlStack removeURLs = new QAUrlStack();
        for (QAUrl qaURL : urlStack) {
            String url = qaURL.getUrl();
            int depth = getUrlDepth(url);
            for (String redPrefix : redPrefixes) {
                String[] prefixInformation = redPrefix.split("##");
                if (url.startsWith(prefixInformation[0]) && depth == Integer.valueOf(prefixInformation[1]) && !hasYellowPrefix(url)
                        && !url.startsWith(getGreenPrefix())) {
                    removeURLs.add(qaURL);
                }
            }
        }
        urlStack.removeAll(removeURLs);
        Logger.getLogger(QAExtractor.class).info(
                "cleansed urlStack of " + this.getName() + " (" + removeURLs.size() + " removed, new size " + urlStack.size() + ")");
        Logger.getLogger(QAExtractor.class).info("red prefixes\n" + CollectionHelper.getPrint(redPrefixes));
        Logger.getLogger(QAExtractor.class).info("yellow prefixes\n" + CollectionHelper.getPrint(yellowPrefixes));
        Logger.getLogger(QAExtractor.class).info("green prefix\n" + getGreenPrefix());
    }

    private int getUrlDepth(String url) {
        int depthCount = url.replace(getEntryURL(), "").split("/").length;
        return depthCount;
    }

    public synchronized boolean urlsAvailable() {
        if (urlStack.size() == 0)
            return false;
        if (urlsVisited.size() >= getMaximumURLs() && getMaximumURLs() != -1)
            return false;
        return true;
    }

    /**
     * Update prefixes only if url is a page where at least a question was extracted.
     * 
     * @param url The url object.
     */
    public synchronized void updatePositivePrefixes(QAUrl url) {
        updateGreenPrefix(url.getUrl());
        updateYellowPrefix(url.getParentURL());
        extractionFailCount = 0;
    }

    public synchronized void updateNegativePrefix(QAUrl url) {
        updateRedPrefix(url.getUrl());
        extractionFailCount++;
    }

    /**
     * Compare longest common path for current green prefix and new url.
     * 
     * @param url The new Q/A url.
     */
    private void updateGreenPrefix(String url) {
        url = url.trim();

        // green prefix can not just be the entry URL
        if (url.length() == getEntryURL().length())
            return;

        int depthCount = getUrlDepth(url);

        if (getGreenPrefix().length() == 0) {
            setGreenPrefix(url);
            setGreenUrlDepth(depthCount);
            redPrefixes.remove(url + "##" + depthCount);
            return;
        }
        String newGreen = StringHelper.getLongestCommonString(getGreenPrefix(), url, false, false);
        setGreenPrefix(newGreen);
        redPrefixes.remove(newGreen + "##" + depthCount);
        setGreenPrefixCreated(true);
    }

    public String getGreenPrefix() {
        return greenPrefix;
    }

    public void setGreenPrefix(String greenPrefix) {
        this.greenPrefix = greenPrefix;
    }

    public boolean greenPrefixCreated() {
        return greenPrefixCreated;
    }

    public void setGreenPrefixCreated(boolean greenPrefixCreated) {
        this.greenPrefixCreated = greenPrefixCreated;
    }

    public int getGreenUrlDepth() {
        return greenUrlDepth;
    }

    public void setGreenUrlDepth(int greenUrlDepth) {
        this.greenUrlDepth = greenUrlDepth;
    }

    /**
     * Compare longest common path for current yellow prefix and new url.
     * 
     * @param url The new Q/A url.
     */
    private void updateYellowPrefix(String url) {
        url = url.trim();

        int depthCount = url.replace(getEntryURL(), "").split("/").length;

        for (String prefix : yellowPrefixes) {
            String shortenedPrefix = StringHelper.getLongestCommonString(prefix, url, false, false);
            if (shortenedPrefix.length() > getEntryURL().length()) {
                yellowPrefixes.remove(prefix);
                yellowPrefixes.add(shortenedPrefix);
                redPrefixes.remove(shortenedPrefix + "##" + depthCount);
                return;
            }
        }

        // if no similar url was found in the current prefix set, add the url
        if (url.length() > getEntryURL().length()) {
            yellowPrefixes.add(url);
            redPrefixes.remove(url + "##" + depthCount);
        }
    }

    /**
     * Check whether given url starts with one of the yellow prefixes.
     * 
     * @param url The url that needs to be checked for prefixes.
     * @return True if the url starts with a yellow prefix, false otherwise.
     */
    private boolean hasYellowPrefix(String url) {
        for (String prefix : yellowPrefixes) {
            if (url.startsWith(prefix))
                return true;
        }
        return false;
    }

    /**
     * Compare longest common path for current red prefix and new url.
     * 
     * @param url The new Q/A url.
     */
    private void updateRedPrefix(String url) {
        url = url.trim();

        if (url.equals(getGreenPrefix()))
            return;

        for (String prefix : redPrefixes) {
            String shortenedPrefix = StringHelper.getLongestCommonString(prefix, url, false, false);
            if (shortenedPrefix.length() > getEntryURL().length() && !shortenedPrefix.equals(getGreenPrefix())) {

                // if red prefix has different url depth, add the new shortened prefix with other url depth
                int depthCountPrefix = Integer.valueOf(prefix.split("##")[1]);
                int depthCount = getUrlDepth(url);

                redPrefixes.remove(prefix);

                if (depthCountPrefix != depthCount) {
                    redPrefixes.add(shortenedPrefix + "##" + depthCountPrefix);
                }

                redPrefixes.add(shortenedPrefix + "##" + depthCount);
                return;
            }
        }

        // if no similar url was found in the current prefix set, add the url
        if (url.length() > getEntryURL().length()) {
            int depthCount = getUrlDepth(url);
            redPrefixes.add(url + "##" + depthCount);
        }
    }

    /**
     * Check whether given url starts with one of the red prefixes.
     * 
     * @param url The url that needs to be checked for prefixes.
     * @return True if the url starts with a red prefix, false otherwise.
     */
    private boolean hasRedPrefix(String url) {
        int depthCount = getUrlDepth(url);
        for (String prefix : redPrefixes) {
            String[] redPrefix = prefix.split("##");
            if (url.startsWith(redPrefix[0]) && Integer.valueOf(redPrefix[1]) == depthCount)
                return true;
        }

        return false;
    }

    public boolean hasVoted() {
        return voted;
    }

    public void setVoted() {
        this.voted = true;
    }

    /**
     * Add the hash of a question. Return true if hash existed already, else false.
     * 
     * @param questionHash The hash of the question.
     * @return True if the question was extracted on the site already, false otherwise.
     */
    public boolean addQuestionHash(int questionHash) {
        return questionHashes.add(questionHash);
    }

    public TreeSet<Integer> getQuestionHashes() {
        return questionHashes;
    }

    public void setQuestionHashes(TreeSet<Integer> questionHashes) {
        this.questionHashes = questionHashes;
    }

    @Override
    public String toString() {
        return this.getName() + " (" + getEntryURL() + ")";
    }

}