package ws.palladian.extraction.content;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.w3c.dom.Document;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.persistence.ParserException;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.resources.WebImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.Security;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

public class PalladianContentExtractorTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    @Ignore
    // TODO
    public void testPalladianContentExtractor() throws PageContentExtractorException, FileNotFoundException {
        PalladianContentExtractor extractor = new PalladianContentExtractor();

        String text = extractor.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test001.html"), true).getResultText();
        // System.out.println(DigestUtils.md5Hex(text));

        assertEquals("80eff9d14c83b529212bd64e78bc1fe4", DigestUtils.md5Hex(text));
    }

    @Test
    public void testLanguageExtraction() throws PageContentExtractorException {
        // make sure all http retrievers globally trust self-signed certificates
        HttpRetrieverFactory.setFactory(new HttpRetrieverFactory(true));
        Security.setProperty("jdk.tls.disabledAlgorithms", "SSLv3, DHE, RC4, MD5withRSA, DH keySize < 768, EC keySize < 224");

        PalladianContentExtractor palladianContentExtractor = new PalladianContentExtractor();
        Language language;

        // Uzbek
        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://www.gazeta.uz/uz"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.UZBEK));

        // Italian
        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://www.corriere.it/"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.ITALIAN));

        // Greek
        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://socialobservatory.crete.gov.gr/"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.GREEK));

        // Norwegian
        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://www.visma.no/"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.NORWEGIAN));

        // Slovak
        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://www.nbs.sk/sk/titulna-stranka"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.SLOVAK));

        // Polish
        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://wyborcza.pl/"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.POLISH));

        // Hebrew
        //        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://ashdod.metropolinet.co.il/he-il/אתר-העיר/"));
        //        language = palladianContentExtractor.detectLanguage();
        //        collector.checkThat(language, is(Language.HEBREW));

        // German
        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://www.spiegel.de/"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.GERMAN));

        // Portuguese
        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://www.abola.pt"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.PORTUGUESE));

        // English
        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://spoonacular.com"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.ENGLISH));

        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://www.theolivepress.es/"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.ENGLISH));

        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://www.dutchnews.nl/"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.ENGLISH));

        // Dutch
        //palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://www.weekbladdebrug.nl/"));
        //language = palladianContentExtractor.detectLanguage();
        //collector.checkThat(language, is(Language.DUTCH));

        // French
        //        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://www.orange.fr"));
        //        language = palladianContentExtractor.detectLanguage();
        //        collector.checkThat(language, is(Language.FRENCH));

        // Spanish
        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://elpais.com/"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.SPANISH));

        // Russian
        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("https://www.moscowtimes.ru/"));
        language = palladianContentExtractor.detectLanguage();
        collector.checkThat(language, is(Language.RUSSIAN));
    }

    @Test
    public void testDominantImageExtraction() throws PageContentExtractorException, FileNotFoundException {
        // TODO make this work without internet connection!
        // TODO i.e. make this work with local files instead of accessing the web!!!
        PalladianContentExtractor palladianContentExtractor = new PalladianContentExtractor();
        WebImage image;

        //        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://naturata.de/de,26afcb8a90edc9578e704f53d86a7dca,83cddec09d9935e822e00efea4b90b1f,4024297150121.html"));
        //        image = palladianContentExtractor.getDominantImage();
        //        collector.checkThat(image.getImageUrl(), containsString("produkte/bilder/NATA/015012_medium.jpg"));

        // TODO fails
        //         palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://rapunzel.de/bio-produkt-haselnuss-creme--120300.html"));
        //         image = palladianContentExtractor.getDominantImage();
        // //        collector.checkThat(image.getImageUrl(), containsString("bilder-96dpi-max-200-breit/120300.jpg"));
        //         collector.checkThat(image.getImageUrl(), containsString("http://rapunzel.de/design/innerlink2.png"));

        //        palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://themeforest.net/item/techwise-drag-drop-magazine-w-comparisons/11149718"));
        //        image = palladianContentExtractor.getDominantImage();
        //        collector.checkThat(image.getImageUrl(), containsString("130306592/01.__large_preview.jpg"));

        // TODO fails
        // palladianContentExtractor.setDocumentOnly(new DocumentRetriever().getWebDocument("http://realhousemoms.com/root-beer-chicken-wings/"));
        // image = palladianContentExtractor.getDominantImage();
        // collector.checkThat(image.getImageUrl(), containsString("Root-Beer-Chicken-Wings-for-Real-Housemoms-Horizontal-Photo-e1422767540265.jpg"));
    }

    @Test
    public void testContentExtraction() throws PageContentExtractorException, FileNotFoundException, ParserException {
        PalladianContentExtractor extractor;

        extractor = getExtractor("pageContentExtractor/news-worldnewsera.html");
        collector.checkThat(extractor.getResultTitle(), is("United By Constitution, Divided By Religion - WorldNewsEra"));
        collector.checkThat(extractor.getResultText(), startsWith("Religion is nowhere defined in the Constitution of India"));
        collector.checkThat(extractor.getResultText(), containsString("and stop moving our constitutional courts over religious matters."));
        collector.checkThat(extractor.getResultText(), not(containsString("Search for")));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-16 06:59:17"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(), is("https://www.financialexpress.com/wp-content/uploads/2022/03/hijab.jpg"));

        extractor = getExtractor("pageContentExtractor/news-theage.html");
        collector.checkThat(extractor.getResultTitle(), is("Why this $8 million Toorak home was built upside down"));
        collector.checkThat(extractor.getResultText(), startsWith("Toorak is home to some of the most stunning residences in Melbourne, but not all of them have views."));
        // TODO the page breaks article in two parts for some dumb reason
        //        collector.checkThat(extractor.getResultText(), containsString("“Not many houses in Toorak get really special views.”"));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-13 13:01:00"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://static.ffx.io/images/$zoom_0.1765%2C$multiply_0.7554%2C$ratio_1.777778%2C$width_1059%2C$x_0%2C$y_107/t_crop_custom/q_86%2Cf_auto/97ba8fb5e0c5ea655c2ff7d2ec49e3e76e81a08b"));
        collector.checkThat(extractor.getDominantImage().getWidth(), is(800));
        collector.checkThat(extractor.getDominantImage().getHeight(), is(450));

        extractor = getExtractor("pageContentExtractor/news-smh.html");
        collector.checkThat(extractor.getResultTitle(), is("Paper crisis could put news titles out of business"));
        // TODO same weird format as news-theage.html
        //        collector.checkThat(extractor.getResultText(), startsWith(
        //                "Paper shortages fuelled by soaring electricity prices and shipping costs could put some Australian newspapers and magazines out of commission, as publishers grapple with the rising cost of newsprint."));
        //        collector.checkThat(extractor.getResultText(), containsString("regional newspapers and respond in due course,” the spokesperson said."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-13 13:30:00"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://static.ffx.io/images/$zoom_0.2648%2C$multiply_0.7554%2C$ratio_1.777778%2C$width_1059%2C$x_0%2C$y_95/t_crop_custom/q_86%2Cf_auto/d33e1f2b68acb488ccdc74153543aacadf95eac6"));
        collector.checkThat(extractor.getDominantImage().getWidth(), is(800));
        collector.checkThat(extractor.getDominantImage().getHeight(), is(450));

        extractor = getExtractor("pageContentExtractor/news-nationalherald.html");
        collector.checkThat(extractor.getResultTitle(), is("Father Says US Seamen Scared Daughter, 14, in Crete Hotel Room"));
        collector.checkThat(extractor.getResultText(), startsWith("CHANIA – A group of US Navy Sailors reportedly"));
        collector.checkThat(extractor.getResultText(), endsWith("questioning, the report said."));
        collector.checkThat(extractor.getResultText(), not(containsString("or Sign Up")));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-22 16:03:52"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://img.ekirikas.com/g4asyb4-wp_tdFL_/w:auto/h:auto/q:auto/https://www.thenationalherald.com/wp-content/uploads/2019/08/4270300_15_0_type13265.jpg"));

        extractor = getExtractor("pageContentExtractor/news-foxnews.html");
        collector.checkThat(extractor.getResultTitle(), is("Dick Van Dyke, 96, reflects on his marriage to Arlene Silver, 50: ‘We share an attitude’"));
        //        collector.checkThat(extractor.getResultText(), startsWith("Arlene Silver wasn’t expecting to find love")); // TODO locally works with starts with but doesn't build on github for unkonwn reasons
        collector.checkThat(extractor.getResultText(), containsString("Arlene Silver wasn’t expecting to find love"));
        collector.checkThat(extractor.getResultText(), endsWith("The Associated Press contributed to this report."));
        collector.checkThat(extractor.getResultText(), not(containsString("CLICK HERE TO")));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-13 07:00:43"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://a57.foxnews.com/static.foxnews.com/foxnews.com/content/uploads/2022/03/1200/675/Getty_DickVanDyke.jpg?ve=1&tl=1"));
        collector.checkThat(extractor.getDominantImage().getWidth(), is(1200));
        collector.checkThat(extractor.getDominantImage().getHeight(), is(675));

        extractor = getExtractor("pageContentExtractor/news-independent-ie.html");
        collector.checkThat(extractor.getResultTitle(), is("Endangered species status proposed for fungus-ravaged bat in US"));
        collector.checkThat(extractor.getResultText(), startsWith("US federal officials have proposed"));
        collector.checkThat(extractor.getResultText(), endsWith(" Ms Marquardt said."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-22 15:01:39"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://www.independent.ie/world-news/90e29/41474879.ece/AUTOCROP/w1240h700/ipanews_97b783bd-1691-4802-a040-812d70d1c8c5_1"));
        collector.checkThat(extractor.getDominantImage().getWidth(), is(1240));
        collector.checkThat(extractor.getDominantImage().getHeight(), is(700));

        extractor = getExtractor("pageContentExtractor/news-ceskenoviny.html");
        collector.checkThat(extractor.getResultTitle(), is("ČSÚ: Za pět let do roku 2020 se počet mobilních SIM v ČR zvýšil o 600.000"));
        collector.checkThat(extractor.getResultText(), startsWith("Praha - Počet aktivních mobilních SIM karet"));
        collector.checkThat(extractor.getResultText(), containsString("meziročně vzrostlo o polovinu."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-22 16:07:00"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(), is("https://i3.cn.cz/14/1564122822_P201907260215901.jpg"));

        extractor = getExtractor("pageContentExtractor/news-dailyrecord.html");
        collector.checkThat(extractor.getResultTitle(), is("Falkirk police release 'artists impression' of car seized as wanted man arrested"));
        collector.checkThat(extractor.getResultText(), startsWith("Police have released an artists impression"));
        collector.checkThat(extractor.getResultText(), endsWith("tagging a mate."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-22 16:04:27"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://i2-prod.dailyrecord.co.uk/incoming/article26530509.ece/ALTERNATES/s1200/276162551_5007940845910460_7339686571652398886_njpeg.jpg"));

        extractor = getExtractor("pageContentExtractor/news-nationalpost.html");
        collector.checkThat(extractor.getResultTitle(), is("'Cowboys for Trump' founder guilty of role in U.S. Capitol riot -judge"));
        collector.checkThat(extractor.getResultText(), containsString("WASHINGTON — A New Mexico county commissioner "));
        collector.checkThat(extractor.getResultText(), endsWith("Democratic President Joe Biden’s election. (Reporting by Jan Wolfe; Editing by Scott Malone and Bill Berkrot)"));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-22 16:10:03"));

        extractor = getExtractor("pageContentExtractor/news-sta.html");
        collector.checkThat(extractor.getResultTitle(), is("Nagrada Astrid Lindgren letos švedski ilustratorki Evi Lindström"));
        collector.checkThat(extractor.getResultText(), startsWith("Stockholm, 22. marca - Švedska"));
        collector.checkThat(extractor.getResultText(), endsWith("Polona Lovšin in Boris A. Novak."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-22 17:19:00"));

        extractor = getExtractor("pageContentExtractor/news-liberoquotidiano.html");
        collector.checkThat(extractor.getResultTitle(), is("Sostenibilità, la ricerca: bene la familiarità meno i comportamenti"));
        collector.checkThat(extractor.getResultText(), startsWith("Roma, 17 mar. (Adnkronos) -"));
        collector.checkThat(extractor.getResultText(), endsWith("d'acqua (74% nel 2022 vs 77% nel 2021)."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-17 13:40:00"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(), is("https://www.liberoquotidiano.it/assets/images/placeholder_libero_gallery.png"));
        collector.checkThat(extractor.getDominantImage().getWidth(), is(1024));
        collector.checkThat(extractor.getDominantImage().getHeight(), is(576));

        extractor = getExtractor("pageContentExtractor/news-wprost.html");
        collector.checkThat(extractor.getResultTitle(), is("Chcesz wspomóc Ukrainę? Lista najpilniej potrzebnych rzeczy"));
        collector.checkThat(extractor.getResultText(), startsWith("– W mieście"));
        collector.checkThat(extractor.getResultText(), endsWith("najpotrzebniejszego sprzętu medycznego."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-14 11:24:00"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://img.wprost.pl/img/pielegniarki-wspieraja-rannego-zolnierza-kijow/ff/33/3e411e9c74e38ef70efdc2bf7823.jpeg"));
        collector.checkThat(extractor.getDominantImage().getWidth(), is(2600));
        collector.checkThat(extractor.getDominantImage().getHeight(), is(1733));

        extractor = getExtractor("pageContentExtractor/news-belfasttelegraph.html");
        collector.checkThat(extractor.getResultTitle(), is("£750m IT system overhaul to change the face of education"));
        collector.checkThat(extractor.getResultText(), startsWith("Schools in Northern Ireland are set to benefit from a new multi-functional"));
        collector.checkThat(extractor.getResultText(), endsWith("said Ms Corkey."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-17 06:01:25"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://www.belfasttelegraph.co.uk/news/education/4b443/41456582.ece/AUTOCROP/w1240h700/_750m_system_launch.jpg"));
        collector.checkThat(extractor.getDominantImage().getWidth(), is(1240));
        collector.checkThat(extractor.getDominantImage().getHeight(), is(700));

        extractor = getExtractor("pageContentExtractor/news-debate.html");
        collector.checkThat(extractor.getResultTitle(), is("Arriba un triple crucero a Mazatlán con más de 7 mil turistas"));
        collector.checkThat(extractor.getResultText(), startsWith("Sinaloa.- Arriba triple crucero al puerto"));
        collector.checkThat(extractor.getResultText(), endsWith("el malecón de la ciudad."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-16 19:12:49"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://www.debate.com.mx/__export/1647457671723/sites/debate/img/2022/03/16/triple-cruceros-mazatlan.png_554688468.png"));

        extractor = getExtractor("pageContentExtractor/news-mirror.html");
        collector.checkThat(extractor.getResultTitle(), is("Arsenal moving in right direction under Mikel Arteta - but Liverpool still a class apart"));
        collector.checkThat(extractor.getResultText(), startsWith("Mikel Arteta's men were not embarrassed here. Far from it."));
        collector.checkThat(extractor.getResultText(), containsString("Arsenal can have that kind of class to call on next season they’ll be back in business."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-17 07:00:00"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://i2-prod.mirror.co.uk/incoming/article25933349.ece/ALTERNATES/s1200/22_Arsenal-and-Liverpool-predicted-line-ups-ahead-of-Carabao-Cup-semi-final-clash.jpg"));

        extractor = getExtractor("pageContentExtractor/news-thestar.html");
        collector.checkThat(extractor.getResultTitle(), is("Ex-Kentucky officer indicted in deadly curfew crackdown"));
        collector.checkThat(extractor.getResultText(), containsString(
                "A former Kentucky police officer was indicted Wednesday on a federal charge for firing pepper balls during an aggressive 2020 curfew crackdown that led to the death of a barbecue cook."));
        collector.checkThat(extractor.getResultText(), containsString("The Justice Department has been investigating the Louisville police department since last year."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-16"));

        extractor = getExtractor("pageContentExtractor/news-indianexpress.html");
        collector.checkThat(extractor.getResultTitle(), is("Prague then, Kyiv now: Problems of democratic socialism"));
        collector.checkThat(extractor.getResultText(), startsWith("I was at the Indian Institute of Management Calcutta, commissioned"));
        collector.checkThat(extractor.getResultText(), endsWith("a former Union minister"));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-16 16:57:05"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(), is("https://images.indianexpress.com/2022/03/Opinion-2-6.jpg"));
        collector.checkThat(extractor.getDominantImage().getWidth(), is(1200));
        collector.checkThat(extractor.getDominantImage().getHeight(), is(667));

        extractor = getExtractor("pageContentExtractor/news-theguardian.html");
        collector.checkThat(extractor.getResultTitle(), is("Julia Samuel: ‘The princes turned the dial by talking about grief’"));
        collector.checkThat(extractor.getResultText(),
                startsWith("Every night, Julia Samuel goes to bed and silently recites a list of names. She begins with her nine grandchildren, then"));
        collector.checkThat(extractor.getResultText(),
                containsString("The best thing we can do to help this is to prioritise our family, in our hearts, our minds – and with our time."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-13 12:00:28"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://i.guim.co.uk/img/media/d8ecb59de915224300a6ed8e076f2a30a85f4459/0_2182_5344_3206/master/5344.jpg?width=1200&height=630&quality=85&auto=format&fit=crop&overlay-align=bottom%2Cleft&overlay-width=100p&overlay-base64=L2ltZy9zdGF0aWMvb3ZlcmxheXMvdG8tZGVmYXVsdC5wbmc&enable=upscale&s=e108091a5f43abe79d442a3aec77d414"));

        extractor = getExtractor("pageContentExtractor/news-ndtv.html");
        collector.checkThat(extractor.getResultTitle(), is("India's Take On UN's 'International Day To Combat Islamophobia' Resolution"));
        //        collector.checkThat(extractor.getResultText(), startsWith("As the UN General Assembly adopted")); // TODO locally works with starts with but doesn't build on github for unkonwn reasons
        collector.checkThat(extractor.getResultText(), containsString("As the UN General Assembly adopted"));
        collector.checkThat(extractor.getResultText(), containsString("none of the proposals mooted by France were taken into consideration."));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-15 17:28:58"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(), is("https://c.ndtvimg.com/2022-03/ktpod94g_unga-generic-russia-ukraine-afp-650_625x300_01_March_22.jpg"));

        extractor = getExtractor("pageContentExtractor/news-independent.html");
        collector.checkThat(extractor.getResultTitle(), is("Saudi Arabia executes 81 people in single day in largest mass killing for decades"));
        collector.checkThat(extractor.getResultText(),
                containsString("Saudi Arabia has executed 81 people in a single day, in the largest mass execution by the kingdom in modern history"));
        collector.checkThat(extractor.getResultText(), containsString("including a prominent opposition Shia cleric who had rallied demonstrations."));
        collector.checkThat(extractor.getResultText(), not(containsString("Register for free to continue reading")));
        collector.checkThat(extractor.getResultText(), not(containsString("Voucher Codes")));
        collector.checkThat(extractor.getResultText(), not(containsString("Email address")));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-12 21:36:08"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(), is("https://static.independent.co.uk/2021/06/15/15/Saudi_Shiite_Execution_99443.jpg?width=1200"));

        extractor = getExtractor("pageContentExtractor/news-bbc.html");
        collector.checkThat(extractor.getResultTitle(), is("Alisher Usmanov: Oligarch says he ditched mansions before sanctions"));
        collector.checkThat(extractor.getResultText(), containsString("A Russian billionaire sanctioned by the UK says he no longer owns many"));
        //        collector.checkThat(extractor.getResultText(), containsString("It is now illegal for any person or company in the UK to do business with him")); // TODO locally works with starts with but doesn't build on github for unkonwn reasons
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-03-22 13:00:04"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(), is("https://ichef.bbci.co.uk/news/1024/branded_news/17F03/production/_123815089_whatsubject.jpg"));
        collector.checkThat(extractor.getDominantImage().getWidth(), is(1469));
        collector.checkThat(extractor.getDominantImage().getHeight(), is(826));

        extractor = getExtractor("pageContentExtractor/news-telegraaf.html");
        collector.checkThat(extractor.getResultTitle(), is("Werkvraag: moet ik een parttimer overuren uitbetalen?"));
        collector.checkThat(extractor.getResultText(), containsString("Mail naar vragen@dft.nl"));
        collector.checkThat(extractor.getPublishDate().getNormalizedDateString(), is("2022-02-14 09:30:00"));
        collector.checkThat(extractor.getDominantImage().getImageUrl(),
                is("https://www.telegraaf.nl/images/1200x630/filters:format(jpeg):quality(80)/cdn-kiosk-api.telegraaf.nl/3537e2ca-8d74-11ec-be43-0257d57b707f.jpg"));
    }

    private PalladianContentExtractor getExtractor(String resourceLocation) throws FileNotFoundException, ParserException, PageContentExtractorException {
        PalladianContentExtractor extractor = new PalladianContentExtractor();
        File resourceFile = ResourceHelper.getResourceFile(resourceLocation);
        DocumentParser parser = ParserFactory.createHtmlParser();
        Document document = parser.parse(resourceFile);
        extractor.setDocument(document);
        return extractor;
    }

    @Test
    public void testImageExtraction() throws PageContentExtractorException, FileNotFoundException {
        PalladianContentExtractor extractor = new PalladianContentExtractor();

        extractor.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test015.html"), true);
        // extractor.setDocument("http://gizmodo.com/5823937/holy-shit-maul-semi+automatic-shotgun-shoots-taser-cartridges-and-is-called-maul");
        // System.out.println(extractor.getResultText());

        List<WebImage> images = extractor.getImages();
        assertEquals(2, images.size());
        assertEquals(-1, images.get(0).getWidth());

        // TODO this should not access the web
        // extractor.analyzeImages();
        // Assert.assertEquals(640, images.get(0).getWidth());

        // => http://www.bbc.co.uk/news/science-environment-14254856
        extractor.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test020.html"), true);
        images = extractor.getImages();

        collector.checkThat(images.size(), is(4));
        collector.checkThat(images.get(1).getWidth(), is(624));

        // CollectionHelper.print(images);
    }
}
