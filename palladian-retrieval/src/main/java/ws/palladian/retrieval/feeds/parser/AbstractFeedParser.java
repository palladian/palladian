package ws.palladian.retrieval.feeds.parser;

import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.helper.HttpHelper;

import java.io.*;

/**
 * <p>
 * Base implementation for feed parsers with common functionality.
 * </p>
 *
 * @author Philipp Katz
 */
public abstract class AbstractFeedParser implements FeedParser {

    @Override
    public Feed getFeed(File file, boolean serializedGzip) throws FeedParserException {
        if (serializedGzip) {
            HttpResult httpResult = HttpHelper.loadSerializedHttpResult(file);
            if (httpResult == null) {
                throw new FeedParserException("Error loading serialized file from \"" + file + "\"");
            }
            return getFeed(httpResult);
        } else {
            try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                return getFeed(inputStream);
            } catch (FileNotFoundException e) {
                throw new FeedParserException("File \"" + file + "\" not found");
            } catch (IOException e) {
                throw new FeedParserException("Encountered IOException", e);
            }
        }
    }

    @Override
    public Feed getFeed(File file) throws FeedParserException {
        return getFeed(file, false);
    }

    @Override
    public Feed getFeed(String feedUrl) throws FeedParserException {
        try {
            HttpRetriever httpRetriever = new HttpRetrieverFactory(true).create();
            HttpResult httpResult = httpRetriever.httpGet(feedUrl);
            return getFeed(httpResult);
        } catch (HttpException e) {
            throw new FeedParserException("Error downloading feed from \"" + feedUrl + "\"", e);
        }
    }

    @Override
    public Feed getFeed(HttpResult httpResult) throws FeedParserException {
        Feed feed = getFeed(new ByteArrayInputStream(httpResult.getContent()));
        feed.setFeedUrl(httpResult.getUrl());
        return feed;
    }

}
