package ws.palladian.extraction.content;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ErrorCollector;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.resources.WebImage;

public class PalladianContentExtractorTest {

    /***/
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    @Ignore
    // FIXME
    public void testPalladianContentExtractor() throws PageContentExtractorException, FileNotFoundException {
        PalladianContentExtractor extractor = new PalladianContentExtractor();

        String text = extractor.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test001.html"))
                .getResultText();
        // System.out.println(DigestUtils.md5Hex(text));

        assertEquals("80eff9d14c83b529212bd64e78bc1fe4", DigestUtils.md5Hex(text));

    }

    @Test
    public void testLanguageExtraction() throws PageContentExtractorException, FileNotFoundException {

        PalladianContentExtractor palladianContentExtractor = new PalladianContentExtractor();
        Language language;

        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://www.cinefreaks.com"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.GERMAN));

        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://www.funny.pt"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.PORTUGUESE));

        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://www.spiegel.de/"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.GERMAN));

        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://spoonacular.com"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.ENGLISH));

    }

    @Test
    public void testDominantImageExtraction() throws PageContentExtractorException, FileNotFoundException {
    	
    	// TODO make this work without internet connection!

        PalladianContentExtractor palladianContentExtractor = new PalladianContentExtractor();
        WebImage image;

//        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://naturata.de/de,26afcb8a90edc9578e704f53d86a7dca,83cddec09d9935e822e00efea4b90b1f,4024297150121.html"));
//        image = palladianContentExtractor.getDominantImage();
//        collector.checkThat(image.getImageUrl(), containsString("produkte/bilder/NATA/015012_medium.jpg"));

        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://rapunzel.de/bio-produkt-haselnuss-creme--120300.html"));
        image = palladianContentExtractor.getDominantImage();
        collector.checkThat(image.getImageUrl(), containsString("bilder-96dpi-max-200-breit/120300.jpg"));

        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://themeforest.net/item/techwise-drag-drop-magazine-w-comparisons/11149718"));
        image = palladianContentExtractor.getDominantImage();
        collector.checkThat(image.getImageUrl(), containsString("130306592/01.__large_preview.jpg"));

        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://realhousemoms.com/root-beer-chicken-wings/"));
        image = palladianContentExtractor.getDominantImage();
        collector.checkThat(image.getImageUrl(), containsString("Root-Beer-Chicken-Wings-for-Real-Housemoms-Horizontal-Photo-e1422767540265.jpg"));

    }

    @Test
    public void testImageExtraction() throws PageContentExtractorException, FileNotFoundException {
        PalladianContentExtractor extractor = new PalladianContentExtractor();

        extractor.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test015.html"));
        // extractor.setDocument("http://gizmodo.com/5823937/holy-shit-maul-semi+automatic-shotgun-shoots-taser-cartridges-and-is-called-maul");
        // System.out.println(extractor.getResultText());

        List<WebImage> images = extractor.getImages();
        assertEquals(2, images.size());
        assertEquals(-1, images.get(0).getWidth());

        // TODO this should not access the web
        // extractor.analyzeImages();
        // Assert.assertEquals(640, images.get(0).getWidth());

        // => http://www.bbc.co.uk/news/science-environment-14254856
        extractor.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test020.html"));
        images = extractor.getImages();

        collector.checkThat(images.size(), is(4));
        collector.checkThat(images.get(1).getWidth(), is(624));

        // CollectionHelper.print(images);
    }

}
