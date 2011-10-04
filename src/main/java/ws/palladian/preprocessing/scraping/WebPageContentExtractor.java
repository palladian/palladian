package ws.palladian.preprocessing.scraping;

import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * <p>The abstract class for Web page content extraction. The "content" is the main node with the contents of the page but not the navigation, foot etc.</p>
 * @author David Urbansky
 *
 */
public abstract class WebPageContentExtractor {

    private static final Logger LOGGER = Logger.getLogger(WebPageContentExtractor.class);

    /** We use the Crawler to take care of retrieving the input stream from remote locations. */
    private DocumentRetriever crawler;
    
    public WebPageContentExtractor() {
        crawler = new DocumentRetriever();
    }

    /**
     * <p>Set Document to be processed. Method returns <code>this</code> instance of PageContentExtractor, to allow
     * convenient concatenations of method invocations, like:
     * <code>new PageContentExtractor().setDocument(...).getResultDocument();</code></p>
     * 
     * @param document
     * @return
     * @throws PageContentExtractorException
     */
    public abstract WebPageContentExtractor setDocument(Document document)
    throws PageContentExtractorException;

    /**
     * Set URL of document to be processed. Method returns <code>this</code> instance of PageContentExtractor, to allow
     * convenient concatenations of method invocations, like:
     * <code>new PageContentExtractor().setDocument(new URL(...)).getResultDocument();</code>
     * 
     * @param url
     * @return
     * @throws PageContentExtractorException
     */
    // public abstract WebPageContentExtractor setDocument(InputSource source) throws PageContentExtractorException;

    /**
     * Set URL of document to be processed. Method returns <code>this</code> instance of PageContentExtractor, to allow
     * convenient concatenations of method
     * invocations, like: <code>new PageContentExtractor().setDocument(new URL(...)).getResultDocument();</code>
     * 
     * @param url
     * @return
     * @throws PageContentExtractorException
     */
    public WebPageContentExtractor setDocument(URL url) throws PageContentExtractorException {
        try {
            return setDocument(crawler.getWebDocument(url.toExternalForm()));
        } catch (Throwable t) {
            throw new PageContentExtractorException(t);
        }
    }

    /**
     * Set File to be processed. Method returns <code>this</code> instance of PageContentExtractor, to allow convenient
     * concatenations of method invocations, like:
     * <code>new PageContentExtractor().setDocument(new File(...)).getResultDocument();</code>
     * 
     * @param file
     * @return
     * @throws PageContentExtractorException
     */
    public WebPageContentExtractor setDocument(File file) throws PageContentExtractorException {
        try {
            return setDocument(crawler.getWebDocument(file.getPath()));
        } catch (Throwable t) {
            throw new PageContentExtractorException(t);
        }
    }

    /**
     * Set the location of document to be processed. Method returns <code>this</code> instance of PageContentExtractor,
     * to allow convenient concatenations of method invocations, like:
     * <code>new PageContentExtractor().setDocument("http://website.com").getResultDocument();</code>
     * 
     * @param documentLocation The location of the document. This can be either a local file or a URL.
     * @return The instance of the PageContentExtractor.
     * @throws PageContentExtractorException
     */
    public WebPageContentExtractor setDocument(String documentLocation) throws PageContentExtractorException {
        try {
            if (UrlHelper.isValidURL(documentLocation)) {
                return setDocument(new URL(documentLocation));
            } else {
                return setDocument(new File(documentLocation));
            }
        } catch (Throwable t) {
            throw new PageContentExtractorException(t);
        }
    }

    /**
     * Returns the filtered result document, as minimal XHTML fragment. Result just contains the filtered content, the
     * result is not meant to be a complete web page or even to validate.
     * 
     * @return
     */
    public abstract Node getResultNode();

    /**
     * Returns the filtered result as human readable plain text representation.
     * 
     * @return The extracted text from the document.
     */
    public abstract String getResultText();

    /**
     * Returns a list of (absolute) image URLs that are contained in the main content block.
     * 
     * @return A list of image URLs.
     */
    //public abstract List<String> getImages();

    /**
     * Shortcut method for <code>new PageContentExtractor().setDocument("http://website.com").getResultText();</code>.
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
        return HtmlHelper.documentToReadableText(getResultNode());
    }


    /**
     * Returns the document's title. This will not just return the text from the document's <code>title</code> element,
     * but try to remove generic, irrelevant
     * substrings. For example, for a document with title <i>"Messi reveals close ties with Maradona - CNN.com"</i> this
     * method will return
     * <i>"Messi reveals close ties with Maradona"</i>.
     * 
     * @return
     */
    public abstract String getResultTitle();
    
    public abstract String getExtractorName();

}