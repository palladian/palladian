package ws.palladian.retrieval;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

// TODO methods for parsing do not belong here and should be removed in the medium term
// TODO remove deprecated methods, after dependent code has been adapted
// TODO role of DownloadFilter is unclear, shouldn't the client itself take care about what to download?
// TODO completely remove all java.net.* stuff
// TODO remove properties configuration via file, dependend clients should set their preferences programmatically

/**
 * <p>
 * The DocumentRetriever allows to download pages from the Web or the hard disk.
 * </p>
 * <p>
 * You may configure it using the appropriate setter and getter methods or accept the default values.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class DocumentRetriever {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DocumentRetriever.class);
    
    private final HttpRetriever httpRetriever;


    // ///////////// Misc. ////////
    /** The number of threads for downloading in parallel. */
    public static final int DEFAULT_NUM_THREADS = 10;

    
    /** The maximum number of threads to use. */
    private int numThreads = DEFAULT_NUM_THREADS;
    
    /** The filter for the retriever. */
    private DownloadFilter downloadFilter = new DownloadFilter();



    /** The callbacks that are called after each parsed page. */
    private final List<RetrieverCallback> retrieverCallbacks = new ArrayList<RetrieverCallback>();
    
    public DocumentRetriever() {
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }
    
    public DocumentRetriever(HttpRetriever httpRetriever) {
        this.httpRetriever = httpRetriever;
    }


    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing (X)HTML documents
    // ////////////////////////////////////////////////////////////////

    /**
     * Get a web page ((X)HTML document).
     * 
     * @param url The URL or file path of the web page.
     * @return The W3C document.
     */
    public Document getWebDocument(String url) {
        return getDocument(url, false);
    }

    /**
     * Get multiple URLs in parallel, for each finished download the supplied callback is invoked.
     * 
     * @param urls the URLs to download.
     * @param callback the callback to be called for each finished download.
     */
    public void getWebDocuments(Collection<String> urls, RetrieverCallback callback) {

        BlockingQueue<String> urlQueue = new LinkedBlockingQueue<String>(urls);

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Runnable runnable = new DocumentRetrieverThread(urlQueue, callback, this);
            threads[i] = new Thread(runnable);
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                LOGGER.error(e);
            }
        }
    }

    /**
     * Get multiple URLs in parallel.
     * 
     * @param urls the URLs to download.
     * @return set with the downloaded documents.
     */
    public Set<Document> getWebDocuments(Collection<String> urls) {
        final Set<Document> result = new HashSet<Document>();
        getWebDocuments(urls, new RetrieverCallback() {
            @Override
            public void onFinishRetrieval(Document document) {
                synchronized (result) {
                    result.add(document);
                }
            }
        });
        return result;
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing XML documents
    // ////////////////////////////////////////////////////////////////

    /**
     * Get XML document from a URL. Pure XML documents can created with the native DocumentBuilderFactory, which works
     * better with the native XPath queries.
     * 
     * @param url The URL or file path pointing to the XML document.
     * @return The XML document.
     */
    public Document getXMLDocument(String url) {
        return getDocument(url, true);
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing JSON documents
    // ////////////////////////////////////////////////////////////////

    /**
     * Get a JSON object from a URL. The retrieved contents must return a valid JSON object.
     * TODO rename this to getJSONObject
     * 
     * @param url the URL pointing to the JSON string.
     * @return the JSON object.
     */
    public JSONObject getJSONDocument(String url) {
        String json = getTextDocument(url);

        // delicous feeds return the whole JSON object wrapped in [square brackets],
        // altough this seems to be valid, our parser doesn't like this, so we remove
        // those brackets before parsing -- Philipp, 2010-07-04
        if (json != null) {
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
            }

            JSONObject jsonOBJ = null;

            if (json.length() > 0) {
                try {
                    jsonOBJ = new JSONObject(json);
                } catch (JSONException e) {
                    LOGGER.error(url + ", " + e.getMessage(), e);
                }
            }

            return jsonOBJ;
        }
        return null;
    }

    /**
     * Get a JSON array from a URL. The retrieved contents must return a valid JSON array.
     * 
     * @param url the URL pointing to the JSON string.
     * @return the JSON array.
     */
    public JSONArray getJSONArray(String url) {
        String json = getTextDocument(url);

        // since we know this string should be an JSON array,
        // we will directly parse it
        if (json != null) {
            json = json.trim();

            JSONArray jsonAR = null;

            if (json.length() > 0) {
                try {
                    jsonAR = new JSONArray(json);
                } catch (JSONException e) {
                    LOGGER.error(url + ", " + e.getMessage(), e);
                }
            }

            return jsonAR;

        }
        return null;

    }

    // ////////////////////////////////////////////////////////////////
    // method for retrieving plain text
    // ////////////////////////////////////////////////////////////////

    /**
     * Download the contents that are retrieved from the given URL.
     * 
     * @param url The URL of the desired contents.
     * @return The contents as a string or <code>null</code> if contents could no be retrieved. See the error log for
     *         possible errors.
     */
    public String getTextDocument(String url) {

        String contentString = null;
        Reader reader = null;

        if (downloadFilter.isAcceptedFileType(url)) {
            try {
                if (isFile(url)) {
                    reader = new FileReader(url);
                    contentString = IOUtils.toString(reader);
                } else {
                    HttpResult httpResult = httpRetriever.httpGet(url);
                    contentString = new String(httpResult.getContent());
                }
            } catch (IOException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }

        return contentString;
    }

    // ////////////////////////////////////////////////////////////////
    // internal methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Multi-purpose method to get a {@link Document}, either by downloading it from the Web, or by reading it from
     * disk. The document may be parsed using an XML parser or a dedicated (X)HTML parser.
     * 
     * @param url the URL of the document to retriever or the file path.
     * @param xml indicate whether the document is well-formed XML or needs to be processed using an (X)HTML parser.
     * @return the parsed document, or <code>null</code> if any kind of error occurred or the document was filtered by
     *         {@link DownloadFilter}.
     */
    private Document getDocument(String url, boolean xml) {

        Document document = null;
        String cleanUrl = url.trim();
        InputStream inputStream = null;

        if (downloadFilter.isAcceptedFileType(cleanUrl)) {

            try {

                if (isFile(cleanUrl)) {
                    File file = new File(cleanUrl);
                    inputStream = new BufferedInputStream(new FileInputStream(new File(cleanUrl)));
                    document = parse(inputStream, xml);
                    document.setDocumentURI(file.toURI().toString());
                } else {
                    HttpResult httpResult = httpRetriever.httpGet(cleanUrl);
                    document = parse(new ByteArrayInputStream(httpResult.getContent()), xml);
                    document.setDocumentURI(cleanUrl);
                }

                callRetrieverCallback(document);

            } catch (FileNotFoundException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } catch (DOMException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } catch (ParserException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } catch (HttpException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        return document;
    }

    private static boolean isFile(String url) {
        boolean isFile = false;
        if (url.indexOf("http://") == -1 && url.indexOf("https://") == -1) {
            isFile = true;
        }
        return isFile;
    }

    /**
     * Parses a an {@link InputStream} to a {@link Document}.
     * 
     * @param inputStream the stream to parse.
     * @param xml <code>true</code> if this document is an XML document, <code>false</code> if HTML document.
     * @throws ParserException if parsing failed.
     */
    private Document parse(InputStream inputStream, boolean xml) throws ParserException {
        Document document = null;
        DocumentParser parser;

        if (xml) {
            parser = ParserFactory.createXmlParser();
        } else {
            parser = ParserFactory.createHtmlParser();
        }

        document = parser.parse(inputStream);
        return document;
    }




    /**
     * Set the maximum number of simultaneous threads for downloading.
     * 
     * @param numThreads the number of threads to use.
     */
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }


    public void setDownloadFilter(DownloadFilter downloadFilter) {
        this.downloadFilter = downloadFilter;
    }

    public DownloadFilter getDownloadFilter() {
        return downloadFilter;
    }



    // ////////////////////////////////////////////////////////////////
    // Callbacks
    // ////////////////////////////////////////////////////////////////

    private void callRetrieverCallback(Document document) {
        for (RetrieverCallback retrieverCallback : retrieverCallbacks) {
            retrieverCallback.onFinishRetrieval(document);
        }
    }

    public List<RetrieverCallback> getRetrieverCallbacks() {
        return retrieverCallbacks;
    }

    public void addRetrieverCallback(RetrieverCallback retrieverCallback) {
        retrieverCallbacks.add(retrieverCallback);
    }

    public void removeRetrieverCallback(RetrieverCallback retrieverCallback) {
        retrieverCallbacks.remove(retrieverCallback);
    }


    // ////////////////////////////////////////////////////////////////
    // main method
    // ////////////////////////////////////////////////////////////////

    /**
     * The main method for testing and usage purposes.
     * 
     * @param args The arguments.
     */
    public static void main(String[] args) throws Exception {

        // #261 example code
        DocumentRetriever retriever = new DocumentRetriever();

        // String filePath = "/home/pk/1312910093553_2011-08-09_19-14-53.gz";
        // HttpResult httpResult = retriever.loadSerializedGzip(new File(filePath));
        // XmlParser parser = new XmlParser();
        // Document document = parser.parse(httpResult);
        // System.out.println(HTMLHelper.getXmlDump(document));
        // System.exit(0);
        //
        //
        // // Wrap this with a GZIPInputStream, if necessary.
        // // Do not use InputStreamReader, as this works encoding specific.
        // InputStream inputStream = new FileInputStream(new File(filePath));
        // inputStream = new GZIPInputStream(inputStream);
        //
        // // Read the header information, until the HTTP_RESULT_SEPARATOR is reached.
        // // We assume here, that one byte resembles one character, which is not true
        // // in general, but should suffice in our case. Hopefully.
        // StringBuilder headerText = new StringBuilder();
        // int b;
        // while ((b = inputStream.read()) != -1) {
        // headerText.append((char) b);
        // if (headerText.toString().endsWith(HTTP_RESULT_SEPARATOR)) {
        // break;
        // }
        // }
        //
        // // Read the payload.
        // ByteArrayOutputStream payload = new ByteArrayOutputStream();
        // while ((b = inputStream.read()) != -1) {
        // payload.write(b);
        // }
        //
        // // Try to parse.
        // //Document document = parser.parse(new ByteArrayInputStream(payload.toByteArray()));
        // System.out.println(headerText.toString());
        // System.out.println("===================");

        System.exit(0);

        // HttpResult result = retriever.httpGet(url);
        // String eTag = result.getHeaderString("Last-Modified");
        //
        // Map<String, String> header = new HashMap<String, String>();
        // header.put("If-Modified-Since", eTag);
        //
        // retriever.httpGet(url, header);
        // System.exit(0);
        //
        // // download and save a web page including their headers in a gzipped file
        // retriever.downloadAndSave("http://cinefreaks.com", "data/temp/cf_no_headers.gz", new HashMap<String,
        // String>(),
        // true);

        // create a retriever that is triggered for every retrieved page
        RetrieverCallback crawlerCallback = new RetrieverCallback() {
            @Override
            public void onFinishRetrieval(Document document) {
                // do something with the page
                LOGGER.info(document.getDocumentURI());
            }
        };
        retriever.addRetrieverCallback(crawlerCallback);

        // give the retriever a list of URLs to download
        Set<String> urls = new HashSet<String>();
        urls.add("http://www.cinefreaks.com");
        urls.add("http://www.imdb.com");

        // set the maximum number of threads to 10
        retriever.setNumThreads(10);

        // download documents
        Set<Document> documents = retriever.getWebDocuments(urls);
        CollectionHelper.print(documents);

        // or just get one document
        Document webPage = retriever.getWebDocument("http://www.cinefreaks.com");
        LOGGER.info(webPage.getDocumentURI());

    }
}