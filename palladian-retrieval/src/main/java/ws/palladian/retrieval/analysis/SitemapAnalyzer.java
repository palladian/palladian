package ws.palladian.retrieval.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.RetrieverCallback;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.services.GoogleIndexedPage;
import ws.palladian.retrieval.ranking.services.SemRush;

/**
 * <p>
 * Retrieve all pages listed in a sitemap and create a reporting file, that looks as follows:
 * </p>
 * 
 * <pre>
 * URL   |  accessible | internal-links-in | internal-links-out | external-links in | external-links out | #words | size-in-KB | indexed-by-Google
 * -----------------------------------------------------------------------------------------------------------------------------------------------
 * </pre>
 * 
 * @author David Urbansky
 * 
 */
public class SitemapAnalyzer {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(SitemapAnalyzer.class);

    /** The result table. */
    private final ConcurrentHashMap<String, Map<String, Object>> resultTable;

    /** We need to keep track of the internal links while crawling all pages from the sitemap. */
    private final CountMap<String> internalInboundLinkMap;

    /** The number of threads that will be used to crawl the documents from the sitemap. */
    private int numThreads = 10;

    public SitemapAnalyzer() {
        resultTable = new ConcurrentHashMap<String, Map<String, Object>>();
        internalInboundLinkMap = CountMap.create();
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public void analyzeSitemap(String sitemapUrl, String analysisResultFilePath) {

        final StopWatch stopWatch = new StopWatch();

        LOGGER.info("getting the page urls");
        List<String> urls = new SitemapRetriever().getUrls(sitemapUrl);
        final int totalCount = urls.size();

        final AtomicInteger count = new AtomicInteger(1);

        RetrieverCallback<Document> retrieverCallback = new RetrieverCallback<Document>() {

            @Override
            public void onFinishRetrieval(Document document) {
                Map<String, Object> map = CollectionHelper.newHashMap();

                Set<String> outInt = HtmlHelper.getLinks(document, true, false);
                Set<String> outExt = HtmlHelper.getLinks(document, false, true);

                synchronized (internalInboundLinkMap) {
                    for (String internalOutLink : outInt) {
                        if (!internalOutLink.equalsIgnoreCase(document.getDocumentURI())) {
                            internalInboundLinkMap.add(internalOutLink);
                        }
                    }
                }

                try {
                    HttpResult httpResult = (HttpResult)document.getUserData("httpResult");
                    map.put("accessible", httpResult.getStatusCode() < 400);
                } catch (Exception e) {
                }

                String htmlText = HtmlHelper.getInnerXml(document);
                String noHtml = HtmlHelper.stripHtmlTags(htmlText);
                int wordCount = StringHelper.countWords(noHtml);

                GoogleIndexedPage googleIndexedPage = new GoogleIndexedPage();
                Ranking ranking = googleIndexedPage.getRanking(document.getDocumentURI());
                SemRush semRush = new SemRush();
                Ranking ranking2 = semRush.getRanking(document.getDocumentURI());

                map.put("in-ext", ranking2.getValues().get(SemRush.BACKLINKS_PAGE));
                map.put("out-int", outInt.size());
                map.put("out-ext", outExt.size());
                map.put("#words", wordCount);
                map.put("size", SizeUnit.BYTES.toKilobytes(htmlText.length()));
                map.put("indexed", ranking.getValues().get(GoogleIndexedPage.GOOGLEINDEXED));

                resultTable.put(document.getDocumentURI(), map);

                ProgressHelper.showProgress(count.intValue(), totalCount, 1, stopWatch);
                count.incrementAndGet();
            }
        };

        LOGGER.info("starting to process each page (" + urls.size() + " in total), time elapsed: "
                + stopWatch.getElapsedTimeString());
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setNumThreads(getNumThreads());
        documentRetriever.getWebDocuments(urls, retrieverCallback);

        LOGGER.info("gathering all internal inbound link information, time elapsed: "
                + stopWatch.getElapsedTimeString());
        for (String url : urls) {
            Map<String, Object> map = resultTable.get(url);
            if (map != null) {
                Integer value = internalInboundLinkMap.getCount(url);
                map.put("in-int", value);
            }
        }

        LOGGER.info("saving the result table, time elapsed: " + stopWatch.getElapsedTimeString());
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(analysisResultFilePath, true),
                    "UTF-8"));
            writer.append("page;accessible;in-int,out-int,in-ext,out-ext,#words;size KB;indexed\n");
            for (Entry<String, Map<String, Object>> entry : resultTable.entrySet()) {
                writer.append(entry.getKey() + ";");
                writer.append(entry.getValue().get("accessible") + ";");
                writer.append(entry.getValue().get("in-int") + ";");
                writer.append(entry.getValue().get("out-int") + ";");
                writer.append(entry.getValue().get("in-ext") + ";");
                writer.append(entry.getValue().get("out-ext") + ";");
                writer.append(entry.getValue().get("#words") + ";");
                writer.append(entry.getValue().get("size") + ";");
                writer.append(entry.getValue().get("indexed") + "\n");
            }
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    public static void main(String[] args) {
        SitemapAnalyzer sitemapAnalyzer = new SitemapAnalyzer();
        sitemapAnalyzer.setNumThreads(10);
        sitemapAnalyzer.analyzeSitemap("http://eelee.com/sitemapIndex.xml", "sitemapAnalysis.csv");
    }
}
