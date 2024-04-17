package ws.palladian.retrieval.analysis;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class SitemapRetrieverTest {

    @Test
    public void testFindSitemap() {
        SitemapRetriever sitemapRetriever = new SitemapRetriever();
        assertEquals(Arrays.asList("http://www.atlasobscura.com/sitemaps/sitemap_index.xml.gz"),
                sitemapRetriever.findSitemaps("https://www.atlasobscura.com"));

        assertEquals(Arrays.asList( //
                "https://www.apple.com/shop/sitemap.xml", //
                "https://www.apple.com/autopush/sitemap/sitemap-index.xml", //
                "https://www.apple.com/newsroom/sitemap.xml", //
                "https://www.apple.com/retail/sitemap/sitemap.xml", //
                "https://www.apple.com/today/sitemap.xml" //
        ), sitemapRetriever.findSitemaps("https://apple.com"));

        assertEquals(Collections.emptyList(), sitemapRetriever.findSitemaps("http://wikipedia.de"));
    }

}
