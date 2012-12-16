package ws.palladian.extraction.content;

import java.io.File;
import java.net.URL;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.retrieval.HttpResult;

import com.gravity.goose.Article;
import com.gravity.goose.Configuration;
import com.gravity.goose.Goose;

/**
 * <p>
 * Content extractor using <a href="https://github.com/jiminoc/goose/wiki">Goose</a>. Goose only accepts URLs on the
 * web, already downloaded {@link HttpResult}s, or local {@link File}s are not supported.
 * </p>
 * 
 * @author Philipp Katz
 */
public class GooseContentExtractor extends WebPageContentExtractor {

    private Article article;

    @Override
    public WebPageContentExtractor setDocument(File file) throws PageContentExtractorException {
        throw new PageContentExtractorException("Local files are not supported");
    }

    @Override
    public WebPageContentExtractor setDocument(HttpResult httpResult) throws PageContentExtractorException {
        throw new PageContentExtractorException("HttpResults are not supported");
    }

    @Override
    public WebPageContentExtractor setDocument(String documentLocation) throws PageContentExtractorException {
        if (documentLocation.startsWith("http://") || documentLocation.startsWith("https://")) {
            Configuration config = new Configuration();
            config.setEnableImageFetching(false);
            Goose goose = new Goose(config);
            this.article = goose.extractContent(documentLocation);
        }
        return this;
    }

    @Override
    public WebPageContentExtractor setDocument(URL url) throws PageContentExtractorException {
        if (url.toString().startsWith("file://")) {
            throw new PageContentExtractorException("Local files are not supported");
        }
        return setDocument(url.toString());
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        String url = document.getDocumentURI();
        if (url == null || url.startsWith("file:")) {
            throw new IllegalArgumentException("Only extraction from web URLs is supported.");
        }
        return setDocument(url);
    }

    @Override
    public Node getResultNode() {
        throw new UnsupportedOperationException("Not supported by Goose");
    }

    @Override
    public String getResultText() {
        return article.cleanedArticleText();
    }

    @Override
    public String getResultTitle() {
        return article.title();
    }

    @Override
    public String getExtractorName() {
        return "Goose";
    }

    public static void main(String[] args) throws PageContentExtractorException {
        GooseContentExtractor gooseExtactor = new GooseContentExtractor();
        gooseExtactor.setDocument("http://techcrunch.com/2012/12/01/facebook-photo-sync-data/");
        System.out.println(gooseExtactor.getResultTitle());
        System.out.println(gooseExtactor.getResultText());
    }

}
