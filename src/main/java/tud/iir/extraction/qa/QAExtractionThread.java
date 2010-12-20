package tud.iir.extraction.qa;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import tud.iir.extraction.PageAnalyzer;
import tud.iir.helper.StopWatch;
import tud.iir.helper.XPathHelper;
import tud.iir.knowledge.QA;
import tud.iir.knowledge.Source;
import tud.iir.web.Crawler;

public class QAExtractionThread extends Thread {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(QAExtractionThread.class);

    /** Use the crawler. */
    private Crawler crawler = null;
    private PageAnalyzer pa = null;

    /** A question-answer website. */
    private QASite qaSite = null;

    public QAExtractionThread(ThreadGroup threadGroup, String name, QASite qaSite) {
        super(threadGroup, name);
        setPa(new PageAnalyzer());
        setCrawler(new Crawler());
        this.qaSite = qaSite;
    }

    @Override
    public void run() {
        QAExtractor.getInstance().increaseThreadCount();

        StopWatch sw = new StopWatch();

        // get web page from stack
        QAUrl qaURL = qaSite.getURLFromStack();

        LOGGER.info("try to extract Q/A (type " + qaURL.getType() + ") from " + qaURL.getUrl());

        // set up the crawler
        crawler.setDocument(qaURL.getUrl());
        pa.setDocument(crawler.getDocument());
        HashSet<String> inDomainLinks = null;

        inDomainLinks = crawler.getLinks(true, false, qaSite.getEntryURL());
        extractFromQASite(qaURL);

        // add all internal links to stack
        for (String url : inDomainLinks) {

            // replace ?xyz if not "=" follows
            url = url.replaceAll("\\?[^=]+(?!(.*?=))", "");

            // replace &abc=
            url = url.replaceAll("&.+?=.*", "");

            QAUrl newQAUrl = new QAUrl(url, qaURL.getUrl());
            qaSite.addURLToStack(newQAUrl);
        }

        LOGGER.info("finished extracting Q/A (type " + qaURL.getType() + ") from " + qaURL.getUrl() + " in "
                + sw.getElapsedTimeString());
        QAExtractor.getInstance().decreaseThreadCount();
    }

    /**
     * Try to extract a QA tuple from a QA site.
     * 
     * @param qaSite The QA site the tuple should be extracted from.
     */
    private void extractFromQASite(QAUrl qaURL) {

        // use patterns to extract question and answer(s)
        String question = pa.getTextByXPath(qaSite.getQuestionXPath());
        String bestAnswer = pa.getTextByXPath(qaSite.getBestAnswerXPath());

        // create Q/A
        QA qa = new QA(qaSite);

        if (question.trim().length() > 0) {
            qa.setExtractedAt(new Date());
            qa.addSource(new Source(qaURL.getUrl()));
            qa.setQuestion(question, qaURL.getUrl(), qaSite.getQuestionXPath());
            if (bestAnswer.length() > 0) {
                qa.addAnswer(bestAnswer, qaURL.getUrl(), qaSite.getBestAnswerXPath());
            }

            List<Node> answerNodes = XPathHelper.getNodes(crawler.getDocument(), qaSite.getAllAnswersXPath());
            for (Iterator<Node> iterator = answerNodes.iterator(); iterator.hasNext();) {
                Node node = iterator.next();
                String answer = node.getTextContent();
                if (answer.length() > 0) {
                    qa.addAnswer(answer, qaURL.getUrl(), qaSite.getAllAnswersXPath());
                }
            }
        }

        boolean questionNew = qaSite.addQuestionHash(question.hashCode());
        if (question.trim().length() > 0 && qa.getAnswers().size() > 0 && questionNew) {
            LOGGER.info("+++ add Q/A " + question + " | (" + qa.getAnswers().size() + ") " + qa.getAnswers().get(0));
            qaSite.updatePositivePrefixes(qaURL);
            QAExtractor.getInstance().addQA(qa);
        } else if (question.trim().length() == 0) {
            qaSite.updateNegativePrefix(qaURL);
        } else if (!questionNew) {
            LOGGER.info("--- question has been seen before: " + question);
        }
    }

    public PageAnalyzer getPa() {
        return pa;
    }

    public void setPa(PageAnalyzer pa) {
        this.pa = pa;
    }

    public Crawler getCrawler() {
        return crawler;
    }

    public void setCrawler(Crawler crawler) {
        this.crawler = crawler;
    }
}