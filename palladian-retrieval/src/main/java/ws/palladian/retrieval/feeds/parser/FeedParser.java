package ws.palladian.retrieval.feeds.parser;

import java.io.File;
import java.io.InputStream;

import org.w3c.dom.Document;

import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.feeds.Feed;

/**
 * <p>
 * A {@link FeedParser} takes various input data, like {@link File}s, {@link HttpResult}s, or {@link InputStream}s and
 * parses them as RSS or Atom feeds, generating an instance of {@link Feed}.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface FeedParser {

    /**
     * <p>
     * Parse a feed from a gzipped {@link HttpResult}. These gzips are available for example in the <a
     * href="http://areca.co/8/Feed-Item-Dataset-TUDCS5">TUD-CS5</a> feed dataset.
     * </p>
     * 
     * @param file The file with the RSS or Atom feed.
     * @param serializedGzip If true, the feed will be read from a serialized and gzipped {@link HttpResult}.
     * @return The parsed feed.
     * @throws FeedParserException
     */
    Feed getFeed(File file, boolean serializedGzip) throws FeedParserException;

    /**
     * <p>
     * Parse a feed from local file system.
     * </p>
     * 
     * @param file The file with the RSS or Atom feed.
     * @return The parsed feed.
     * @throws FeedParserException
     */
    Feed getFeed(File file) throws FeedParserException;

    /**
     * <p>
     * Downloads a feed from the specified URL.
     * </p>
     * 
     * @param feedUrl the URL to the RSS or Atom feed.
     * @return
     * @throws FeedParserException
     */
    Feed getFeed(String feedUrl) throws FeedParserException;

    /**
     * <p>
     * Parse a feed from the specified {@link HttpResult}.
     * </p>
     * 
     * @param httpResult The httpResult from the request.
     * @return The parsed feed.
     * @throws FeedParserException
     */
    Feed getFeed(HttpResult httpResult) throws FeedParserException;

    /**
     * <p>
     * Parse a feed from the specified {@link Document}.
     * </p>
     * 
     * @param document the Document containing the RSS or Atom feed.
     * @return
     * @throws FeedParserException
     */
    Feed getFeed(Document document) throws FeedParserException;

    /**
     * <p>
     * Parse a feed from the specified {@link InputStream}.
     * </p>
     * 
     * @param inputStream
     * @return
     * @throws FeedParserException
     */
    Feed getFeed(InputStream inputStream) throws FeedParserException;

}