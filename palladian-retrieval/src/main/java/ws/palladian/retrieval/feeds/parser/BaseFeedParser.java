package ws.palladian.retrieval.feeds.parser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.feeds.Feed;

/**
 * <p>
 * Base implementation for feed parsers with common functionality.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class BaseFeedParser implements FeedParser {

    /*
     * (non-Javadoc)
     * @see ws.palladian.retrieval.feeds.parser.FeedParser#getFeed(java.io.File, boolean)
     */
    @Override
    public Feed getFeed(File file, boolean serializedGzip) throws FeedParserException {
        if (serializedGzip) {
            HttpResult httpResult = HttpRetriever.loadSerializedHttpResult(file);
            if (httpResult == null) {
                throw new FeedParserException("Error loading serialized file from \"" + file + "\"");
            }
            return getFeed(httpResult);
        } else {
            InputStream inputStream = null;
            try {
                inputStream = new BufferedInputStream(new FileInputStream(file));
                return getFeed(inputStream);
            } catch (FileNotFoundException e) {
                throw new FeedParserException("File \"" + file + "\" not found");
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.retrieval.feeds.parser.FeedParser#getFeed(java.io.File)
     */
    @Override
    public Feed getFeed(File file) throws FeedParserException {
        return getFeed(file, false);
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.retrieval.feeds.parser.FeedParser#getFeed(java.lang.String)
     */
    @Override
    public Feed getFeed(String feedUrl) throws FeedParserException {
        try {
            HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
            HttpResult httpResult = httpRetriever.httpGet(feedUrl);
            return getFeed(httpResult);
        } catch (HttpException e) {
            throw new FeedParserException("Error downloading feed from \"" + feedUrl + "\"", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see ws.palladian.retrieval.feeds.parser.FeedParser#getFeed(ws.palladian.retrieval.HttpResult)
     */
    @Override
    public Feed getFeed(HttpResult httpResult) throws FeedParserException {
        Feed feed = getFeed(new ByteArrayInputStream(httpResult.getContent()));
        feed.setFeedUrl(httpResult.getUrl());
        return feed;
    }

}
