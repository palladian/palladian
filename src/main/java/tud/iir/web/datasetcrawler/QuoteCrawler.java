package tud.iir.web.datasetcrawler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;

/**
 * Crawl quotes.
 * 
 * @author David Urbansky
 */
public class QuoteCrawler {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(QuoteCrawler.class);

    /**
     * Extracts quotes
     */
    public void extractQuotes() {

        long t1 = System.currentTimeMillis();

        ArrayList<Quote> quotes = new ArrayList<Quote>();

        Crawler c = new Crawler();

        Document peoplePage = c.getWebDocument("http://refspace.com/people");

        List<Node> people = XPathHelper.getNodes(peoplePage,
                "/html/body/div/table/tr/td/div/table/tr/td/div/table/tr/td/a".toUpperCase());

        HashSet<String> peopleLinks = new HashSet<String>();
        for (Node peopleNode : people) {
            if (peopleNode.getAttributes().getNamedItem("href") != null) {
                peopleLinks.add(peopleNode.getAttributes().getNamedItem("href").getNodeValue());
            }
        }

        for (String url : peopleLinks) {

            String author = url.substring(27).replaceAll("_", " ");

            for (int i = 0; i < 200; i += 25) {

                String countURL = url;
                if (i > 0) {
                    countURL = url + "/s:" + i;
                }

                LOGGER.info("look for quotes from " + author + " on " + countURL);

                Document quotePage = c.getWebDocument(countURL);
                List<Node> quoteNodes = XPathHelper.getNodes(quotePage,
                        "/html/body/div/table/tr/td/div/table/tr/td/div/div/div".toUpperCase());

                // skip first two nodes
                int j = 0;
                for (Node quoteNode : quoteNodes) {
                    j++;
                    if (j < 3) {
                        continue;
                    }

                    String quoteText = quoteNode.getTextContent();

                    Quote quote = new Quote(author, quoteText);
                    LOGGER.info("add quote " + quote);
                    quotes.add(quote);
                }

                ThreadHelper.sleep(2000);
            }

        }

        StringBuilder quoteString = new StringBuilder();
        for (Quote quote : quotes) {
            quoteString.append(quote).append("\n");
        }

        FileHelper.writeToFile("data/extra/quoteCrawl.csv", quoteString);

        LOGGER.info("crawled and saved quotes in " + DateHelper.getRuntime(t1));
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        QuoteCrawler qc = new QuoteCrawler();
        qc.extractQuotes();
    }

}

/**
 * Quote.
 * 
 * @author David Urbansky
 */
class Quote {

    private String author = "";
    private String quote = "";

    public Quote(String author, String quote) {
        super();
        this.author = author;
        this.quote = quote;
    }

    @Override
    public String toString() {
        return System.currentTimeMillis() + "#\"" + quote + "\"#\"" + author + "\"";
    }

}