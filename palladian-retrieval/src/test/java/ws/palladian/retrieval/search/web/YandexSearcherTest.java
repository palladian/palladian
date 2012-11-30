package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.search.SearcherException;

public class YandexSearcherTest {

    private final DocumentParser xmlParser = ParserFactory.createXmlParser();
    private YandexSearcher yandexSearcher;
    
    @Before
    public void setUp() {
        yandexSearcher = new YandexSearcher();
    }

    @Test
    public void testParseResponse() throws FileNotFoundException, ParserException {
        Document document = xmlParser.parse(ResourceHelper.getResourceFile("/apiresponse/response1.xml"));
        try {
            List<WebResult> webResults = yandexSearcher.parse(document);
            assertEquals(10, webResults.size());
            assertEquals("http://www.mercedes-benz.ru/", webResults.get(0).getUrl());
            assertEquals("\"Mercedes-Benz in Russia\" - продажа автомобилей", webResults.get(0).getTitle());
            assertEquals("Информация о работе дилеров и авторизованных техцентров. Каталог автомобилей. Рекомендованные розничные цены и специальные предложения. FAQ.", webResults.get(0).getSummary());
        } catch (SearcherException e) {
            fail();
        }

        document = xmlParser.parse(ResourceHelper.getResourceFile("/apiresponse/error2.xml"));
        try {
            yandexSearcher.parse(document);
            fail();
        } catch (SearcherException e) {
        }
    }

    @Test
    public void testBuildRequestUrl() {
        String requestUrl = yandexSearcher.buildRequestUrl("http://xmlsearch.yandex.ru/xmlsearch", "moscow", 10, 1);
        assertEquals("http://xmlsearch.yandex.ru/xmlsearch&query=moscow&page=1&groupby=groups-on-page%3D10docs-in-group%3D1&filter=none", requestUrl);
    }

    @Test
    public void testCheckSearchUrlValidity() {
        yandexSearcher
                .checkSearchUrlValidity("http://xmlsearch.yandex.ru/xmlsearch?user=pkatz&key=03.156690494:67abdff20756319b24dc308f8d216e22");
        try {
            yandexSearcher.checkSearchUrlValidity("http://xmlsearch.yandex.ru/xmlsearch?user=&key=");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            yandexSearcher.checkSearchUrlValidity("");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            yandexSearcher.checkSearchUrlValidity(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

}
