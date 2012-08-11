package ws.palladian.extraction.content;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.NekoHtmlParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * The abstract class for Web page content extraction. The "content" is the main node with the contents of the page but
 * not the navigation, foot etc.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public abstract class WebPageContentExtractor {

    private static final Logger LOGGER = Logger.getLogger(WebPageContentExtractor.class);

    /**
     * <p>
     * Set Document to be processed. Method returns <code>this</code> instance of PageContentExtractor, to allow
     * convenient concatenations of method invocations, like:
     * <code>new PageContentExtractor().setDocument(...).getResultDocument();</code>
     * </p>
     * 
     * @param document
     * @return
     * @throws PageContentExtractorException
     */
    public abstract WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException;

    /**
     * <p>
     * Set URL of document to be processed. Method returns <code>this</code> instance of PageContentExtractor, to allow
     * convenient concatenations of method invocations, like:
     * <code>new PageContentExtractor().setDocument(new URL(...)).getResultDocument();</code>
     * </p>
     * 
     * @param url
     * @return
     * @throws PageContentExtractorException
     */
    // public abstract WebPageContentExtractor setDocument(InputSource source) throws PageContentExtractorException;

    /**
     * <p>
     * Set URL of document to be processed. Method returns <code>this</code> instance of PageContentExtractor, to allow
     * convenient concatenations of method invocations, like:
     * <code>new PageContentExtractor().setDocument(new URL(...)).getResultDocument();</code>
     * </p>
     * 
     * @param url
     * @return
     * @throws PageContentExtractorException
     */
    public final WebPageContentExtractor setDocument(URL url) throws PageContentExtractorException {
        boolean localFile = UrlHelper.isLocalFile(url);
        if (localFile) {
            return setDocument(new File(url.getFile()));
        } else {
            HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
            try {
                HttpResult httpResult = retriever.httpGet(url.toExternalForm());
                return setDocument(httpResult);
            } catch (HttpException e) {
                throw new PageContentExtractorException("error retrieving URL " + url.toExternalForm(), e);
            }
        }
    }

    public WebPageContentExtractor setDocument(HttpResult httpResult) throws PageContentExtractorException {
        try {
            DocumentParser parser = ParserFactory.createHtmlParser();
            Document document = parser.parse(httpResult);
            return setDocument(document);
        } catch (ParserException e) {
            throw new PageContentExtractorException("error parsing the file from " + httpResult.getUrl(), e);
        }
    }

    /**
     * <p>
     * Set File to be processed. Method returns <code>this</code> instance of PageContentExtractor, to allow convenient
     * concatenations of method invocations, like:
     * <code>new PageContentExtractor().setDocument(new File(...)).getResultDocument();</code>
     * </p>
     * 
     * @param file
     * @return
     * @throws PageContentExtractorException
     */
    public WebPageContentExtractor setDocument(File file) throws PageContentExtractorException {
        try {
            NekoHtmlParser parser = new NekoHtmlParser();
            Document document = parser.parse(file);
            return setDocument(document);
        } catch (ParserException e) {
            throw new PageContentExtractorException("error parsing the file " + file, e);
        }
    }

    /**
     * <p>
     * Set the location of document to be processed. Method returns <code>this</code> instance of PageContentExtractor,
     * to allow convenient concatenations of method invocations, like:
     * <code>new PageContentExtractor().setDocument("http://website.com").getResultDocument();</code>
     * </p>
     * 
     * @param documentLocation The location of the document. This can be either a local file or a URL.
     * @return The instance of the PageContentExtractor.
     * @throws PageContentExtractorException
     */
    public final WebPageContentExtractor setDocument(String documentLocation) throws PageContentExtractorException {
        try {
            URL url = createUrl(documentLocation);
            return setDocument(url);
        } catch (MalformedURLException e) {
            throw new PageContentExtractorException("could not resolve " + documentLocation, e);
        }
    }

    // TODO move to UrlHelper
    private static URL createUrl(String string) throws MalformedURLException {
        if (string.startsWith("http://") || string.startsWith("https://")) {
            return new URL(string);
        }
        if (string.startsWith("file:")) {
            return new URL(string);
        }
        string = "file:" + string;
        return new URL(string);
    }

    /**
     * <p>
     * Returns the filtered result document, as minimal XHTML fragment. Result just contains the filtered content, the
     * result is not meant to be a complete web page or even to validate.
     * </p>
     * 
     * @return
     */
    public abstract Node getResultNode();

    /**
     * <p>
     * Returns the filtered result as human readable plain text representation.
     * </p>
     * 
     * @return The extracted text from the document.
     */
    public abstract String getResultText();

    /**
     * <p>
     * Returns a list of (absolute) image URLs that are contained in the main content block.
     * </p>
     * 
     * @return A list of image URLs.
     */
    // public abstract List<String> getImages();

    /**
     * <p>
     * Shortcut method for <code>new PageContentExtractor().setDocument("http://website.com").getResultText();</code>.
     * </p>
     * 
     * @param documentLocation The location of the document. This can be either a local file or a URL.
     * @return The extracted text from the document.
     */
    public String getResultText(String documentLocation) {
        try {
            setDocument(documentLocation);
        } catch (PageContentExtractorException e) {
            LOGGER.error("location: " + documentLocation + " could not be loaded successfully, " + e.getMessage());
        }
        return getResultText();
    }

    /**
     * <p>
     * Returns the document's title. This will not just return the text from the document's <code>title</code> element,
     * but try to remove generic, irrelevant substrings. For example, for a document with title
     * <i>"Messi reveals close ties with Maradona - CNN.com"</i> this method will return
     * <i>"Messi reveals close ties with Maradona"</i>.
     * </p>
     * 
     * @return
     */
    public abstract String getResultTitle();

    public abstract String getExtractorName();

}