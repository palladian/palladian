package ws.palladian.extraction.location.sources.importers;

import org.apache.commons.lang3.Validate;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <p>
 * SAX handler for processing Wikipedia XML dumps. Mapping each page to a
 * {@link WikipediaPageCallback#callback(WikipediaPage)}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class WikipediaPageContentHandler extends DefaultHandler {

    private final WikipediaPageCallback callback;

    private StringBuilder buffer = new StringBuilder();
    private boolean bufferText = false;

    private boolean inRevision = false;

    private String title;
    private int pageId;
    private int namespaceId;
    private String text;

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
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("text") || qName.equals("title") || qName.equals("ns") || (qName.equals("id") && !inRevision)) {
            bufferText = true;
        }
        if (qName.equals("revision")) {
            inRevision = true;
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
        } else if (qName.equals("title")) {
            title = getBuffer();
        } else if (qName.equals("ns")) {
            namespaceId = Integer.valueOf(getBuffer());
        } else if (qName.equals("page")) {
            callback.callback(new WikipediaPage(pageId, namespaceId, title, text));
        }
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

}
