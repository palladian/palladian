package ws.palladian.extraction.location.sources.importers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ws.palladian.helper.StopWatch;

/**
 * <p>
 * SAX handler for processing Wikipedia XML dumps. Mapping each page to a
 * {@link WikipediaPageCallback#callback(WikipediaPage)}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class WikipediaPageContentHandler extends DefaultHandler {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaPageContentHandler.class);

    private static final Pattern REDIRECT_MARKUP = Pattern.compile("#redirect\\s*\\[\\[(.*)\\]\\]",
            Pattern.CASE_INSENSITIVE);

    private int pageCounter;
    private final StopWatch stopWatch;

    private final WikipediaPageCallback callback;

    private StringBuilder buffer = new StringBuilder();
    private boolean bufferText = false;

    private boolean inRevision = false;

    private String title;
    private int pageId;
    private int namespaceId;
    private String text;
    private boolean redirect;
    private String redirectTitle;

    /**
     * <p>
     * Create a new {@link WikipediaPageContentHandler}.
     * </p>
     * 
     * @param callback The callback to trigger for parsed pages, not <code>null</code>.
     */
    public WikipediaPageContentHandler(WikipediaPageCallback callback) {
        Validate.notNull(callback, "callback must not be null");
        this.callback = callback;
        this.stopWatch = new StopWatch();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("text") || qName.equals("title") || qName.equals("ns") || (qName.equals("id") && !inRevision)) {
            bufferText = true;
        }
        if (qName.equals("revision")) {
            inRevision = true;
        }
        if (qName.equals("redirect")) {
            redirect = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("revision")) {
            inRevision = false;
        } else if (qName.equals("id") && !inRevision) {
            pageId = Integer.valueOf(getBuffer());
        } else if (qName.equals("text")) {
            text = getBuffer();
            if (redirect) {
                redirectTitle = parseRedirect(text);
            }
        } else if (qName.equals("title")) {
            title = getBuffer();
        } else if (qName.equals("ns")) {
            namespaceId = Integer.valueOf(getBuffer());
        } else if (qName.equals("page")) {
            processPage();
            redirect = false;
            redirectTitle = null;
        }
    }

    private String parseRedirect(String text) {
        Matcher matcher = REDIRECT_MARKUP.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        LOGGER.error("Error parsing {}", text);
        return null;
    }

    private void processPage() {
        if (++pageCounter % 1000 == 0) {
            float throughput = (float)pageCounter / TimeUnit.MILLISECONDS.toSeconds(stopWatch.getElapsedTime());
            LOGGER.info("Processed {} pages, throughput {} pages/second.", pageCounter, Math.round(throughput));
        }
        callback.callback(new WikipediaPage(pageId, namespaceId, title, text, redirectTitle));
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (bufferText) {
            buffer.append(ch, start, length);
        }
    }

    private String getBuffer() {
        try {
            return buffer.toString();
        } finally {
            buffer = new StringBuilder();
            bufferText = false;
        }
    }

    public static void main(String[] args) throws Exception {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser parser = saxParserFactory.newSAXParser();
        File redirects = new File("/Users/pk/Downloads/enwiki-latest-pages-articles.xml.bz2");
        InputStream inputStream = new MultiStreamBZip2InputStream(new BufferedInputStream(
                new FileInputStream(redirects)));
        parser.parse(inputStream, new WikipediaPageContentHandler(new WikipediaPageCallback() {

            @Override
            public void callback(WikipediaPage page) {
                if (page.isRedirect()) {
                    if (page.getRedirectTitle().contains("#")) {
                        System.out.println(page);
                    }
                }
//                if (page.getTitle().equalsIgnoreCase("Sherkin")) {
//                    System.out.println(page);
//                    System.exit(0);
//                }
            }
        }));
    }

}
