package ws.palladian.retrieval.feeds.parser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.feeds.Feed;

public abstract class BaseFeedParser implements FeedParser {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BaseFeedParser.class);
    
    private final DocumentRetriever documentRetriever;
    
    public BaseFeedParser() {
        documentRetriever = new DocumentRetriever();
    }
    
    
    /* (non-Javadoc)
     * @see ws.palladian.retrieval.feeds.parser.FeedParser#getFeed(java.io.File, boolean)
     */
    @Override
    public Feed getFeed(File file, boolean serializedGzip) throws FeedParserException {
        InputStream inputStream = null;
        try {
            
            if (serializedGzip) {
                
                HttpResult httpResult = documentRetriever.loadSerializedGzip(file);
                return getFeed(httpResult);
//                
//                
//                String unserializedFeed = FileHelper.ungzipFileToString(file.getPath());
//                
//                // get rid of header
//                int headerIndex = unserializedFeed.indexOf(DocumentRetriever.HTTP_RESULT_SEPARATOR);
//                headerIndex += DocumentRetriever.HTTP_RESULT_SEPARATOR.length();
//                unserializedFeed = unserializedFeed.substring(headerIndex);         
//                            
//                inputStream = new BufferedInputStream(new StringInputStream(unserializedFeed));
            } else {
                inputStream = new BufferedInputStream(new FileInputStream(file));
            }
            
            return getFeed(inputStream);
        } catch (FileNotFoundException e) {
            throw new FeedParserException(e);
        } finally {
            FileHelper.close(inputStream);
        }
    }

    /* (non-Javadoc)
     * @see ws.palladian.retrieval.feeds.parser.FeedParser#getFeed(java.io.File)
     */
    @Override
    public Feed getFeed(File file) throws FeedParserException {
        return getFeed(file,false);
    }

    /* (non-Javadoc)
     * @see ws.palladian.retrieval.feeds.parser.FeedParser#getFeed(java.lang.String)
     */
    @Override
    public Feed getFeed(String feedUrl) throws FeedParserException {
        try {
            StopWatch sw = new StopWatch();
            HttpResult httpResult = documentRetriever.httpGet(feedUrl);
            Feed feed = getFeed(httpResult);
            LOGGER.debug("downloaded feed from " + feedUrl + " in " + sw.getElapsedTimeString());
            return feed;
        } catch (HttpException e) {
            throw new FeedParserException(e);
        }
    }

    @Override
    public Feed getFeed(HttpResult httpResult) throws FeedParserException {
        return getFeed(new ByteArrayInputStream(httpResult.getContent()));
    }

//    @Override
//    public Feed getFeed(Document document) throws FeedParserException {
//        // TODO Auto-generated method stub
//        return null;
//    }

//    @Override
//    public Feed getFeed(InputStream inputStream) throws FeedParserException {
//        // TODO Auto-generated method stub
//        return null;
//    }

}
