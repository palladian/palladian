package ws.palladian.extraction;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * Test cases for the list discoverer.
 * </p>
 * 
 * @author David Urbansky
 */
public class ListDiscovererTest {
    
    private final DocumentParser htmlParser = ParserFactory.createHtmlParser();

    @Test
    public void testEntriesUniform() {
        List<String> list = new ArrayList<String>();
        list.add("Abc ABC");
        list.add("12. A");
        list.add("Very Long Entry With More Than 12 Words A B C D!!!");
        list.add("Short One");
        list.add("Yes");
        assertEquals(true, ListDiscoverer.entriesUniform(list, true));
        list = new ArrayList<String>();
        list.add("1.");
        list.add("2.");
        list.add("Very Long Entry With More Than 12 Words A B C D!!!");
        list.add("Short One");
        list.add("Yes");
        assertEquals(false, ListDiscoverer.entriesUniform(list, true));
        list = new ArrayList<String>();
        list.add("UPPERCASE");
        list.add("LoWeRcAsE");
        list.add("UPPERCASE");
        list.add("Short One");
        list.add("Yes");
        assertEquals(false, ListDiscoverer.entriesUniform(list, true));
        list = new ArrayList<String>();
        list.add("Very Long Entry With More Than 12 Words A B C D!!!");
        list.add("Very Long Entry With More Than 12 Words A B C D!!!");
        list.add("Very Long Entry With More Than 12 Words A B C D!!!");
        list.add("Very Long Entry With More Than 12 Words A B C D!!! Very Long Entry With More Than 12 Words A B C D!!!");
        list.add("Short but still not too short");
        assertEquals(false, ListDiscoverer.entriesUniform(list, true));
        list = new ArrayList<String>();
        list.add("UPPER CASE");
        list.add("LoWeR cAsE");
        list.add("d");
        list.add("SHORT");
        list.add("ONE");
        assertEquals(false, ListDiscoverer.entriesUniform(list, true));
    }

    // TODO
    @Test
    @Ignore
    public void testFindEntityColumn() throws FileNotFoundException, ParserException {
        ListDiscoverer ld = new ListDiscoverer();
        Document document = null;
        document = htmlParser.parse(ResourceHelper.getResourceFile("data/benchmarkSelection/entities/google8/website3.html"));
        // System.out.println(ld.findEntityColumn(document, "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD"));
        // assertEquals(2, ld.findEntityColumn(document, "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD"));
        document = htmlParser.parse(ResourceHelper.getResourceFile("data/benchmarkSelection/entities/google8/website5.html"));
        assertEquals(1, ld.findEntityColumn(document, "/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD/P"));
        document = htmlParser.parse(ResourceHelper.getResourceFile("data/benchmarkSelection/entities/google8/website9.html"));
        assertEquals(0,
                ld.findEntityColumn(document, "/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A/B"));
        document = htmlParser.parse(ResourceHelper.getResourceFile("data/benchmarkSelection/entities/google8/website11.html"));
        assertEquals(1, ld.findEntityColumn(document, "/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD"));
        document = htmlParser.parse(ResourceHelper.getResourceFile("data/benchmarkSelection/entities/google8/website17.html"));
        assertEquals(0, ld.findEntityColumn(document, "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[4]/TR/TD/UL/LI/A"));
        document = htmlParser.parse(ResourceHelper.getResourceFile("data/benchmarkSelection/entities/google8/website27.html"));
        assertEquals(
                3,
                ld.findEntityColumn(
                        document,
                        "/HTML/BODY/FORM/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD"));
        document = htmlParser.parse(ResourceHelper.getResourceFile("data/benchmarkSelection/entities/google8/website29.html"));
        assertEquals(-1,
                ld.findEntityColumn(document, "/HTML/BODY/CENTER/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD"));
        document = htmlParser.parse(ResourceHelper.getResourceFile("data/benchmarkSelection/entities/google8/website33.html"));
        assertEquals(2,
                ld.findEntityColumn(document, "/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD/A"));
        document = htmlParser.parse(ResourceHelper.getResourceFile("data/benchmarkSelection/entities/google8/website67.html"));
        assertEquals(2, ld.findEntityColumn(document, "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD/I/A"));
        document = htmlParser.parse(ResourceHelper.getResourceFile("data/benchmarkSelection/entities/google8/website69.html"));
        assertEquals(3, ld.findEntityColumn(document,
                "/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/P/TABLE/TR/TD/FONT/A"));
        document = htmlParser.parse(ResourceHelper.getResourceFile("data/benchmarkSelection/entities/google8/website87.html"));
        assertEquals(0, ld.findEntityColumn(document, "/HTML/BODY/TABLE[3]/TR/TD/UL/LI"));
    }

    @Test
    // TODO see problems in comments
    /**
     * <p>Problems:
     * <ol>
     * <li>lists with other elements in between two entries should be ranked lower</li>
     * <li>if a list is not uniform, the next highest ranked xpath should be considered</li>
     * <li>sibling page is the same but in different language, look only for sibling pages when list has less than 50 entries</li>
     * <li>sibling page is exactly the same or wrong or only different sorting etc.</li>
     * <li>page incorrectly encoded, can not be repaired easily</li>
     * <li>get more tables, especially when same path but different table index have almost the same count</li>
     * <li>move stages up when current list not uniform | try other sibling stages to root stage</li>
     * <li>more than one list on the page</li>
     * <li>remove common words around entries (for example ABC review, DEF review => review should be removed)</li>
     * <li>list on page with horizontal entries (display:inline)</li>
     * <li>list too short or detail list</li>
     * <li>think about indices (keep last index only?) TODO</li>
     * </ol>
     * 
     * </p>
     * <p>current precision': 34/44 ~ 0.7727 (correct assignment or empty path although pass expected)<br>
     *  current accuracy: 34/60 ~ 0.5667 total tests: 60</p>
     */
    public void testDiscoverEntityXPath() throws FileNotFoundException, ParserException {

        ListDiscoverer ld = new ListDiscoverer();

        String url = "http://www.example.com/";
        ld.setUrl(url);

        Document document = null;
        String discoverdEntityXPath = "";
        List<Node> nodes = null;

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/webPageEntityList22.html"));
        discoverdEntityXPath = ld.discoverEntityXPath(document);
        nodes = XPathHelper.getXhtmlNodes(document, discoverdEntityXPath);
        // System.out.println(discoverdEntityXPath);
        // System.out.println(nodes.size());
        // for (Node node : nodes) {
        // System.out.println(node.getTextContent());
        // }
        assertEquals("//div/div/div/div/div/div/div/div/div/div/div/div/div/div/div/div/div/div/div/div/ul/li/a",
                discoverdEntityXPath);
        assertEquals(644, nodes.size());
        assertEquals("A Beautiful Life (2008)", nodes.get(0).getTextContent());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/webPageEntityList21.html"));
        discoverdEntityXPath = ld.discoverEntityXPath(document);
        nodes = XPathHelper.getXhtmlNodes(document, discoverdEntityXPath);
        // System.out.println(discoverdEntityXPath);
        // System.out.println(nodes.size());
        // for (Node node : nodes) {
        // System.out.println(node.getTextContent());
        // }
        assertEquals("//table[1]/tbody/tr/td/table/tbody/tr/td/font/a", discoverdEntityXPath);
        assertEquals(81, nodes.size());
        assertEquals("\nDiszaray", nodes.get(77).getTextContent());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination13.html"));
        discoverdEntityXPath = ld.discoverEntityXPath(document);
        nodes = XPathHelper.getXhtmlNodes(document, discoverdEntityXPath);
        assertEquals("//center/table[7]/tbody/tr/td/table[2]/tbody/tr/td/table[6]/tbody/tr/td/table[1]/tbody/tr/td/table/tbody/tr/td[2]/font/a",
                discoverdEntityXPath);
        assertEquals(30, nodes.size());
        assertEquals("Avatar", nodes.get(0).getTextContent());

        if (false) {
            // // lists should be found
            // mixed
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",
                    ld.discoverEntityXPath("http://simple.wikipedia.org/wiki/List_of_diseases"));
            // TODO Problem 7 (currently empty list)
            // assertEquals("/HTML/BODY/TABLE/TR/TD[2]",ld.discoverEntityXPath("http://www.novamedia.de/devices/supportedPhones.php?s=flk"));
            assertEquals(
                    "/HTML/BODY/DIV/TABLE[3]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/FONT/A",
                    ld.discoverEntityXPath("http://www.fantasycars.com/derek/list.html"));
            assertEquals(
                    "/HTML/BODY/DIV/TABLE[1]/TR/TD/DIV/UL/LI/A",
                    ld.discoverEntityXPath("http://www.racv.com.au/wps/wcm/connect/Internet/Primary/my+car/advice+%26+information/compare+cars/list+of+all+cars/"));
            // TODO Problem 6 (currently partly correct => wrong)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/NYT_TEXT/DIV/TABLE[3]/TR/TD[1]/A",ld.discoverEntityXPath("http://www.nytimes.com/ref/movies/1000best.html"));
            // TODO Problem 4,5 (check xpath), (currently empty list)
            // assertEquals("/HTML/BODY/TABLE[1]/TR/TD/P/TABLE/TR/TD/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/FONT/A",ld.discoverEntityXPath("http://www.state.gov/misc/list/index.htm"));
            // TODO Problem 3 (currently gets wrong list)
            // assertEquals("/HTML/BODY/DIV/TABLE[1]/TR/TD/TABLE[2]/TR/TD[2]/A",ld.discoverEntityXPath("http://www.who.int/countries/en/"));
            assertEquals("/HTML/BODY/UL/UL/UL/UL/UL/UL/UL/UL/P/UL/LI/A",
                    ld.discoverEntityXPath("http://www.theodora.com/wfb/abc_world_fact_book.html"));
            assertEquals("/HTML/BODY/TABLE[1]/TR/TD/P/TABLE[1]/TR/TD/UL/LI/B/A",
                    ld.discoverEntityXPath("http://www.austlii.edu.au/catalog/215.html"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/P/A",
                    ld.discoverEntityXPath("http://www.imf.org/external/country/index.htm"));
            // TODO Problem 2 (currently finds wrong list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE/TR/TD[2]/DIV/A",ld.discoverEntityXPath("http://research.cars.com/go/crp/buyingGuides/Story.jsp?year=New&section=Sports&subject=Sports&story=highperformance"));
            // TODO Problem 1 (currently finds wrong list)
            // assertEquals("/HTML/BODY/TABLE[1]/TR/TD/TABLE/TR/TD/A",
            // ld.discoverEntityXPath("http://www.hollywoodteenmovies.com/A.html"));
            assertEquals(
                    "/HTML/BODY/TABLE[1]/TR/TD/P/SPAN",
                    ld.discoverEntityXPath("http://naturalhealthtechniques.com/ExamForms-MedicalIntuitive/list_of_sports.htm"));
            // TODO Problem 6 (currently partly correct => wrong)
            // assertEquals("/HTML/BODY/DIV/DIV/TABLE/TR/TD/A",ld.discoverEntityXPath("http://www.mic.ki.se/Diseases/alphalist.html"));
            assertEquals("/HTML/BODY/TABLE[1]/TR/TD/FONT/B/A",
                    ld.discoverEntityXPath("http://www.stanford.edu/~petelat1/birdlist.html"));
            assertEquals("/HTML/BODY/DIV/DIV/P/SPAN/A",
                    ld.discoverEntityXPath("http://www.georgia.gov/00/topic_index_channel/0,2092,4802_5081,00.html"));

            // concept: country
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",
                    ld.discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_countries"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]/A",
                    ld.discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_countries_by_population"));
            assertEquals("/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD[1]",
                    ld.discoverEntityXPath("http://www.internetworldstats.com/list2.htm"));
            assertEquals("/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A",
                    ld.discoverEntityXPath("http://dir.yahoo.com/regional/countries/"));
            assertEquals(
                    "/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD[1]",
                    ld.discoverEntityXPath("http://www.auswaertiges-amt.de/diplo/en/WillkommeninD/EinreiseUndAufenthalt/StaatenlisteVisumpflicht.html"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE[1]/TR/TD[1]",
                    ld.discoverEntityXPath("http://www.cellular-news.com/car_bans/"));
            // TODO Problem 8 (currently finds wrong list) also http://en.wikipedia.org/wiki/Lists_of_films
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE[1]/TR/TD[1]",ld.discoverEntityXPath("http://www.worldatlas.com/geoquiz/thelist.htm"));

            // concept: car
            assertEquals("/HTML/BODY/TABLE[5]/TR/TD/TABLE[4]/TR/TD/A",
                    ld.discoverEntityXPath("http://wallpaper.diq.ru/17_map.htm"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",
                    ld.discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_Asian_cars"));
            assertEquals("/HTML/BODY/DIV/DIV/UL/LI/A", ld.discoverEntityXPath("http://www.rsportscars.com/cars/"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A",
                    ld.discoverEntityXPath("http://www.time.com/time/specials/2007/completelist/0,,1658545,00.html"));
            // TODO Problem 2,7,12 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/FORM/DIV/DIV[1]/DIV[2]/SPAN[1]/A",ld.discoverEntityXPath("http://www.automart.com/make-browse/a/acura/"));
            // TODO Problem 1 (currently wrong list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/P/A",ld.discoverEntityXPath("http://www.buyyourcar.co.uk/used-car.aspx"));
            // TODO Problem 2,4,7,12 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/FORM/DIV/DIV/DIV/DIV[2]/DIV[1]/A",ld.discoverEntityXPath("http://aston-martin.autoextra.com/model"));
            // TODO Problem is that there are duplicates which should not be a problem (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/FORM/DIV/DIV/DIV/DIV[2]/DIV[1]/A",ld.discoverEntityXPath("http://www.carmax.com/enUS/search-results/default.html?ANa=207&Ep=homepage+button+MPG&zip=98532&D=90"));

            // concept: mobile phone
            // TODO getTextByXPath should be updated
            assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD[2]/A",
                    ld.discoverEntityXPath("http://www.esato.com/phones/"));
            // TODO Problem 2,7 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_best-selling_mobile_phones"));
            // TODO Problem 4 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE/TBODY/TR/TD[2]/A",ld.discoverEntityXPath("http://www.expansys.com/n.aspx?c=169"));
            // TODO Problem 1,2,6,7,12 (currently empty list) (check xpath)
            // assertEquals("/HTML/BODY/DIV/CENTER/TABLE/TBODY/TR/TD/TABLE[3]/TBODY/TR/TD/TABLE[1]/TBODY/TR/TD/TABLE[2]/TBODY/TR/TD/DIV/TABLE[6]/CAPTION/A",ld.discoverEntityXPath("http://www.phone-critic.co.uk/phone.php?action=Browse&OrderBy=&Sort=&QuickSearch=&SearchResults=&MinPrice=&MaxPrice=&PhoneType=1&PhoneBrand=&Features1=&Features2=&Features3=&Features4=&Sort=&OrderBy=&Offset=112"));

            // TODO find xpath
            assertEquals("", ld.discoverEntityXPath("http://123simlock.nl/alletoestellen.php"));

            // concept: notebook
            assertEquals(
                    "/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI",
                    ld.discoverEntityXPath("http://danstechnstuff.com/2008/08/08/dell-adds-to-list-of-notebooks-with-nvidia-gpu-problems/"));
            // TODO Problem 2,11 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/TABLE[1]/TR/TD/TABLE[1]/TBODY/TR/TD/B/A",ld.discoverEntityXPath("http://asia.cnet.com/reviews/notebooks/0,39050495,39315110,00.htm"));

            // concept: movie
            // TODO worked before but now incorrect page
            // retrieved:assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD[2]/I/A",ld.discoverEntityXPath("http://en.wikipedia.org/wiki/100_Years...100_Movies"));
            assertEquals("/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/P/TABLE/TR/TD[3]/FONT/A",
                    ld.discoverEntityXPath("http://www.imdb.com/top_250_films"));
            assertEquals(
                    "/HTML/BODY/DIV/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD[2]/A",
                    ld.discoverEntityXPath("http://www.imdb.com/TitlesByYear?year=2008&start=A&nav=/Sections/Years/2008/include-titles"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/I/A",
                    ld.discoverEntityXPath("http://en.wikiquote.org/wiki/List_of_films"));
            // TODO Problem 11 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H4",ld.discoverEntityXPath("http://www.firstshowing.net/2007/12/18/why-2008-will-be-an-awesome-year-for-movies/"));
            assertEquals(
                    "/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/OL/LI",
                    ld.discoverEntityXPath("http://www.berbecuta.com/2008/03/14/1001-movie-you-must-see-before-you-die/"));
            // TODO Problem 6 (currently empty list)
            // assertEquals("/HTML/BODY/TABLE[1]/TR/TD/SPAN/TABLE/TR/TD/P/SPAN/FONT/B/A",ld.discoverEntityXPath("http://www.john-bauer.com/movies.htm"));
            // TODO Problem 2,7 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/I/A",ld.discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_films:_A"));
            // TODO Problem 2,4,7 (currently empty list)
            // assertEquals("/HTML/BODY/FORM/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/SPAN/DIV/DIV/DIV/DIV/H1/A",ld.discoverEntityXPath("http://www.cineplex.com/Movies/AllMovies.aspx"));
            // TODO Problem 6 (currently wrong list)
            // assertEquals("/HTML/BODY/CENTER/TABLE[1]/TR/TD/TABLE/TR/TD[2]/A/H3",ld.discoverEntityXPath("http://www.blu-ray.com/movies/movies.php?genre=action"));
            // TODO Problem ? (currently empty list)
            // assertEquals("/HTML/BODY/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/P/FONT/A",ld.discoverEntityXPath("http://www.comingsoon.net/database.php"));
            assertEquals("/HTML/BODY/DIV/DIV/TABLE[1]/TR/TD[1]",
                    ld.discoverEntityXPath("http://www.actorscelebs.com/actor/Edward_Norton/"));
            // TODO Problem 2,7 (currently empty list) (check xpath)
            // assertEquals("/HTML/BODY/CENTER/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[2]/TR/TD[2]/A",ld.discoverEntityXPath("http://www.filmcrave.com/list_top_movie.php"));

            // concept: actor
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",
                    ld.discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_French_actors"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A",
                    ld.discoverEntityXPath("http://www.atpictures.com/index.php%3Fview%3Dall"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",
                    ld.discoverEntityXPath("http://www.djmick.co.uk/actors_pictures.htm"));
            // TODO Problem 2,8 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath("http://koreanfilm.org/actors.html"));
            assertEquals("/HTML/BODY/DIV/DIV/A", ld.discoverEntityXPath("http://www.actorscelebs.com/browse/d/"));

            // // no list should be found
            // concept: car
            assertEquals("", ld.discoverEntityXPath("http://www.sealcoveautomuseum.org/list.html"));

            // concept: mobile phone
            assertEquals("",
                    ld.discoverEntityXPath("http://wordpress.com/tag/price-list-of-mobile-phones-in-pakistan/"));
            // TODO Problem 4, crawler finds wrong sibling because of unproper linking on the page
            // assertEquals("",ld.discoverEntityXPath("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html"));
            // TODO Problem 10 (currently wrong list is found)
            // assertEquals("",ld.discoverEntityXPath("http://www.skype.com/intl/en/download/skype/mobile/"));

            // concept: notebook
            assertEquals("", ld.discoverEntityXPath("http://www.apple.com/macosx/features/isync/"));
            assertEquals("", ld.discoverEntityXPath("http://www.digitalpersona.com/products/notebooks.php"));
            // TODO Problem 4 (currently wrong list)
            // assertEquals("",ld.discoverEntityXPath("http://www.chotocheeta.com/2008/02/18/budget-yet-performance-notebook-for-a-student-or-personal-or-small-business/"));

            // concept: actor
            assertEquals("", ld.discoverEntityXPath("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html"));

            // concept: mineral TODO
            assertEquals("", ld.discoverEntityXPath("http://crocoite.com/images/index.htm"));
            assertEquals("", ld.discoverEntityXPath("http://rruff.geo.arizona.edu/AMS/all_minerals.php"));

            // concept: plant TODO
            assertEquals(
                    "",
                    ld.discoverEntityXPath("http://131.230.176.4/cgi-bin/dol/dol_terminal.pl?taxon_name=Albizia_coriaria"));

            // concept: song TODO
            assertEquals("", ld.discoverEntityXPath("http://catamountjazz.com/Pictures2/Ultimate.html"));

            // concept: video game TODO
            assertEquals("", ld.discoverEntityXPath("http://www.arcadeathome.com/shots.phtml"));
            assertEquals("", ld.discoverEntityXPath("caesar.logiqx.com/php/emulator_games.php?id=mame&letter=S"));

            // concept: insect TODO
            assertEquals("", ld.discoverEntityXPath("http://www.pestcontrolcanada.com/INSECTS/insects%202.htm"));

            // concept: tv show TODO
            assertEquals(
                    "",
                    ld.discoverEntityXPath("http://www.fancast.com/full_episodes;jsessionid=A3DE50C0E7A4F9D0D9BA53A3AE4FD312"));

        }
    }

    // TODO see problems in comments
    /*
     * public void testDiscoverEntityXPathOffline() { ListDiscoverer ld = new ListDiscoverer(); int correct = 0;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/B".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website1.html"))) correct++; //
     * even better if /HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]/A
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]".equalsIgnoreCase(ld
     * .discoverEntityXPath("data/benchmarkSelection/entities/google8/website3.html"))) correct++; // even better if
     * /HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD/P/A
     * if("/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD[1]".equalsIgnoreCase(ld.
     * discoverEntityXPath("data/benchmarkSelection/entities/google8/website5.html"))) correct++;
     * if("/HTML/BODY/TABLE[1]/TBODY/TR/TD/P/TABLE/TBODY/TR/TD/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website7.html"))) correct++;
     * if("/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website9.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD[1]"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website11.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE[1]/TR/TD[1]"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website13.html"))) correct++;
     * // for next line, distribution
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website15.html")))
     * correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[4]/TR/TD/UL/LI/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website17.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website19.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website21.html")))
     * correct++;if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website23.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H3/A".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website25.html")))
     * correct++;if(
     * "/HTML/BODY/FORM/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD[3]"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website27.html"))) correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website29.html")))
     * correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website31.html")))
     * correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD[2]/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website33.html"))) correct++;
     * // next one would benefit if link would
     * need to be in table or list
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website35.html")))
     * correct++; // next
     * one would benefit if navigation lists were detected
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website37.html")))
     * correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website39.html")))
     * correct++; // next one would benefit it box
     * elements are analyzed and only those with few child structures are considered
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website41.html")))
     * correct++; // next one would benefit if DIV
     * got index
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website43.html")))
     * correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website49.html")))
     * correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website51.html")))
     * correct++; // next one is rendered with
     * javascript, DOM must be created after page is full loaded
     * if("/HTML/BODY/DIV/DIV/TABLE/TR/TD/TABLE/TBODY/TR/TD[1]".equalsIgnoreCase(ld.discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website55.html"))) correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website57.html")))
     * correct++; // next one would benefit if DIV
     * got index
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website59.html")))
     * correct++; // next one would benefit
     * from structure checking, and variability in word length
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website61.html")))
     * correct++; // next one would benefit if
     * navigation lists were detected
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website63.html")))
     * correct++; //
     * next one would also benefit from distribution
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website65.html")))
     * correct++; if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD[2]/I/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website67.html"))) correct++;
     * // alternatively for the following one:
     * /HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD[3]
     * if("/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD/FONT/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website69.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/I/A".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website71.html"))) correct++;
     * // next one would benefit from box structure analysis and ranking h higher
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H4".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website73.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/OL/LI".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website77.html" )))
     * correct++; // next one would benefit if navigation lists were detected
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website79.html")))
     * correct++; // next one should be filtered out
     * //if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website81.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website83.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website85.html" )))
     * correct++; // next one should be filtered out
     * //if("/HTML/BODY/TABLE[3]/TR/TD/UL/LI".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website87.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website89.html"))) correct++;
     * // next one would benefit if navigation lists were detected
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website91.html")))
     * correct++; // next one would benefit if p got
     * index if("/HTML/BODY/DIV/P/TABLE/TR/TD/P/I".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website93.html")))
     * correct++; // next one should be filtered out
     * //if("/HTML/BODY/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/UL/LI/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website95.html"))) correct++;
     * System.out.println("correct: "+correct+" ~ "+(double)correct/41.0); //if (correct > -1) return; try {
     * Thread.sleep(2000); } catch (InterruptedException
     * e) { e.printStackTrace(); }
     * //assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath(
     * "http://simple.wikipedia.org/wiki/List_of_diseases"));
     * //assertEquals("/HTML/BODY/TABLE/TR/TD[2]",ld.discoverEntityXPath(
     * "http://www.novamedia.de/devices/supportedPhones.php?s=flk"));
     * //assertEquals("/HTML/BODY/DIV/TABLE/TR/TD/TABLE/TR/TD/DIV/TABLE/TR/TD/TABLE/TR/TD/DIV/TABLE/TR/TD/FONT/A",ld.
     * discoverEntityXPath(
     * "http://www.fantasycars.com/derek/list.html"));//assertEquals("/HTML/BODY/DIV/TABLE/TR/TD/DIV/UL/LI/A",ld.
     * discoverEntityXPath(
     * "http://www.racv.com.au/wps/wcm/connect/Internet/Primary/my+car/advice+%26+information/compare+cars/list+of+all+cars/"
     * ));
     * //assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD/UL/LI/A",ld.discoverEntityXPath(
     * "http://en.wikipedia.org/wiki/List_of_cars")); // more tables
     * //assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/NYT_TEXT/DIV/TABLE/TR/TD[1]/A",ld.discoverEntityXPath(
     * "http://www.nytimes.com/ref/movies/1000best.html")); // sibling page is exactly the same (check xpath) //
     * assertEquals(
     * "/HTML/BODY/TABLE[1]/TR/TD/P/TABLE/TR/TD/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/FONT/A"
     * ,ld.discoverEntityXPath("http://www.state.gov/misc/list/index.htm")); // sibling page is the same but in
     * different language (check xpath) //
     * assertEquals("/HTML/BODY/DIV/TABLE/TR/TD/TABLE/TR/TD[2]/A",ld.discoverEntityXPath("http://www.who.int/countries/en/"
     * )); //
     * assertEquals("/HTML/BODY/UL/UL/UL/UL/UL/UL/UL/UL/P/UL/LI/A",ld.discoverEntityXPath(
     * "http://www.theodora.com/wfb/abc_world_fact_book.html")); //
     * assertEquals("/HTML/BODY/TABLE[1]/TR/TD/P/TABLE[1]/TR/TD/UL/LI/B/A",ld.discoverEntityXPath(
     * "http://www.austlii.edu.au/catalog/215.html")); //
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/P/A",ld.discoverEntityXPath("http://www.imf.org/external/country/index.htm"
     * )); // got the wrong list
     * //assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE/TR/TD[2]/DIV/A",ld.discoverEntityXPath(
     * "http://research.cars.com/go/crp/buyingGuides/Story.jsp?year=New&section=Sports&subject=Sports&story=highperformance));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/B",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website1.html")); // even better if
     * /HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]/A
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website3.html")); // even
     * better if /HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD/P/A
     * assertEquals("/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD[1]"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website5.html"));
     * assertEquals("/HTML/BODY/TABLE[1]/TBODY/TR/TD/P/TABLE/TBODY/TR/TD/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/A"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website7.html"));
     * assertEquals("/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website9.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD[1]"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website11.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE[1]/TR/TD[1]"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website13.html")); // detect horizontal lists
     * //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website15.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[4]/TR/TD/UL/LI/A"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website17.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website19.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/UL/LI/A",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website21.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website23.html")); // detect page similarities
     * (works with sibling page analysis) //
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H3/A",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website25.html")); assertEquals
     * (
     * "/HTML/BODY/FORM/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD[3]"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website27.html"));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website29.html"));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website31.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD[2]/A"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website33.html")); // detect horizontal lists
     * (2) || next one would benefit if link
     * would need to be in table or list
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website35.html")); // detect
     * page
     * similarities (2) (works with sibling page analysis) || do not take because not in list or table //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website37.html")); // detect
     * horizontal lists (3) alternative path:
     * /HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website39.html")); // analyze
     * position on page //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website41.html")); // detect
     * page similarities?
     * filtering out? //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website43.html"));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website49.html"));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website51.html")); // next one
     * is rendered with javascript, DOM must be
     * created after page is full loaded //
     * assertEquals("/HTML/BODY/DIV/DIV/TABLE/TR/TD/TABLE/TBODY/TR/TD[1]",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website55.html" ));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website57.html")); // detect
     * page similarities (3) (works with sibling
     * page analysis) //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website59.html")); // check
     * variability of content
     * between list entries //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website61.html")); // detect
     * page similarities
     * (4) (works with sibling page analysis) //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website63.html")); // next one
     * would also benefit from distribution
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website65.html")); assertEquals(
     * "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD[2]/I/A",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website67.html")); // alternatively
     * for the following one: /HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD[3]
     * assertEquals("/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/P/TABLE/TR/TD[3]/FONT/A"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website69.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/I/A",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website71.html")); // next one would
     * benefit from box structure analysis and ranking h higher //
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H4",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website73.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/OL/LI",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website77.html")); // detect
     * page similarities (5) (works with sibling page analysis) //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website79.html")); // next one
     * should be filtered out
     * //assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website81.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website83.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website85.html")); // next one
     * should be filtered out
     * //assertEquals("/HTML/BODY/TABLE[3]/TR/TD/UL/LI",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website87.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website89.html")); // detect page
     * similarities (6) (works with sibling page analysis) //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website91.html"));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website93.html")); // next one
     * should be filtered out
     * //if("/HTML/BODY/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/UL/LI/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website95.html"))) correct++;
     * } public void
     * testDiscoverEntityXPathNoCount() { ListDiscoverer ld = new ListDiscoverer(); int correct =
     * 0;if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/B".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website1.html")))) correct++; // even better if
     * /HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]/A
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website3.html")))) correct++; // even better
     * if /HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD/P/A
     * if("/HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD".equalsIgnoreCase
     * (PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website5.html"))))
     * correct++;
     * if("/HTML/BODY/TABLE/TBODY/TR/TD/P/TABLE/TBODY/TR/TD/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website7.html")))) correct++;
     * if("/HTML/BODY/TABLE/TR/TD/TABLE/TR/TD/TABLE/TR/TD/DIV/UL/LI/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website9.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website11.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE/TR/TD"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website13.html")))) correct++; // for next
     * line, distribution if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website15.html"))))
     * correct++;if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD/UL/LI/A".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.
     * discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website17.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website19.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/UL/LI/A".equalsIgnoreCase
     * (PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website21.html"))))
     * correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website23.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H3/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website25.html")))) correct++;
     * if(
     * "/HTML/BODY/FORM/TABLE/TR/TD/DIV/TABLE/TR/TD/TABLE/TR/TD/TABLE/TR/TD/DIV/TABLE/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website27.html")))) correct++;
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website29.html")))) correct++;
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website31.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/TABLE/TR/TD/P/TABLE/TR/TD/TABLE/TR/TD/A".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.
     * discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website33.html")))) correct++; // next one would benefit if link would
     * need to be in table or list
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website35.html")))) correct++; // next
     * one would benefit if navigation lists were detected
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website37.html")))) correct++; // next
     * one would benefit it box elements are analyzed and only those with few child structures are considered
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website39.html")))) correct++; // next one
     * would benefit it box elements are analyzed and only those with few child structures are considered
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website41.html")))) correct++; // next
     * one would benefit if DIV got index
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website43.html")))) correct++;
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website49.html")))) correct++;
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website51.html")))) correct++; // next
     * one is rendered with javascript, DOM must be created after page is full loaded
     * if("/HTML/BODY/DIV/DIV/TABLE/TR/TD/TABLE/TBODY/TR/TD"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website55.html")))) correct++;
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website57.html")))) correct++; // next
     * one would benefit if DIV got index
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website59.html")))) correct++; // next
     * one would benefit from structure checking, and variability in word length
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website61.html")))) correct++; // next
     * one would benefit if navigation lists were detected
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website63.html")))) correct++; // next
     * one would also benefit from distribution
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website65.html"))))
     * correct++;if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD/I/A".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.
     * discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website67.html")))) correct++; // alternatively for the following one:
     * /HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD[3]
     * if("/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD/FONT/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website69.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/I/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website71.html")))) correct++; // next one
     * would benefit from box structure analysis and ranking h higher
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H4".equalsIgnoreCase(PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website73.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/OL/LI".equalsIgnoreCase
     * (PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website77.html"))))
     * correct++; // next one would benefit if
     * navigation lists were detected
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website79.html")))) correct++; // next
     * one should be filtered out
     * //if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website81.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(PageAnalyzer.
     * removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website83.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website85.html")))) correct++; // next one
     * should be filtered
     * out//if("/HTML/BODY/TABLE[3]/TR/TD/UL/LI".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website87.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website89.html")))) correct++; // next one
     * would benefit if navigation lists were
     * detected if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website91.html")))) correct++;
     * // next one would benefit if p got index
     * if("/HTML/BODY/DIV/P/TABLE/TR/TD/P/I".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website93.html")))) correct++; // next one should be filtered out
     * //if("/HTML/BODY/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/UL/LI/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website95.html"))) correct++;
     * System.out.println("correct: "+correct+" ~ "+(double)correct/41.0); //if (correct > -1) return;
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/B",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website1.html"))); // even better if
     * /HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]/A
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website3.html"))); //
     * even better if
     * /HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD/P/A
     * assertEquals("/HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD",PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website5.html")));
     * assertEquals("/HTML/BODY/TABLE/TBODY/TR/TD/P/TABLE/TBODY/TR/TD/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website7.html")));
     * assertEquals("/HTML/BODY/TABLE/TR/TD/TABLE/TR/TD/TABLE/TR/TD/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website9.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website11.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE/TR/TD"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website13.html")));
     * // for next line, distribution //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website15.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website17.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website19.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website21.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website23.html")));
     * //
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H3/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website25.html")));
     * assertEquals(
     * "/HTML/BODY/FORM/TABLE/TR/TD/DIV/TABLE/TR/TD/TABLE/TR/TD/TABLE/TR/TD/DIV/TABLE/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website27.html")));
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website29.html")));
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website31.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE/TR/TD/P/TABLE/TR/TD/TABLE/TR/TD/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website33.html")));
     * // next one would benefit if link would
     * need to be in table or list //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website35.html"))); // next one would benefit
     * if navigation lists were detected | do not take because not in list or table //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website37.html"))); // next one would benefit
     * it box elements are analyzed and only those with few child structures are considered //
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website39.html")));
     * // next one would benefit it box elements
     * are analyzed and only those with few child structures are considered | do not take because not in list or table
     * //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website41.html"))); // next one would benefit
     * if DIV got index // assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website43.html")));
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website49.html")));
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website51.html"))); // next one is rendered
     * with javascript, DOM must be created after page is full loaded //
     * assertEquals("/HTML/BODY/DIV/DIV/TABLE/TR/TD/TABLE/TBODY/TR/TD[1]",PageAnalyzer
     * .removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website55.html")));
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website57.html"))); // next one would benefit
     * if DIV got index // assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website59.html"))); //
     * next one would benefit from structure checking, and variability in word length //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website61.html"))); // next one would benefit
     * if navigation lists were detected //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website63.html"))); // next one would also
     * benefit from distribution assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website65.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD/I/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website67.html")));
     * // alternatively for the following one:
     * /HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD[3]
     * assertEquals("/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD/FONT/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website69.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/I/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website71.html")));
     * // next one would benefit from box
     * structure analysis and ranking h higher //
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H4",PageAnalyzer.removeCounts(ld.discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website73.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/OL/LI",PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website77.html"))); // next one would benefit
     * if navigation lists were detected //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website79.html"))); // next one should be
     * filtered out//assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website81.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",PageAnalyzer.removeCounts(ld.discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website83.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A",PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website85.html"))); // next one should be
     * filtered out
     * //assertEquals("/HTML/BODY/TABLE[3]/TR/TD/UL/LI"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website87.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website89.html")));
     * // next one would benefit if navigation
     * lists were detected //assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website91.html"))); //
     * next one would benefit if p got index
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website93.html"))); // next one should be
     * filtered out
     * //if("/HTML/BODY/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/UL/LI/A".
     * equalsIgnoreCase(PageAnalyzer
     * .removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website95.html"))) correct++; }
     */

    @Test
    /**
     * <p>Test the accuracy of recognizing pagination xPaths.</p>
     * <p>Current accuracy: 17/17 = 1.0, total tests: 17</p>
     */
    public void testGetPaginationXPath() throws FileNotFoundException, ParserException {

        ListDiscoverer ld = new ListDiscoverer();
        String url = "http://www.example.com/";
        ld.setUrl(url);

        Document document = null;

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination15.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        // System.out.println(ld.getPaginationXPath());
        // System.out.println(ld.getPaginationURLs().size());
        // CollectionHelper.print(ld.getPaginationURLs());
        assertEquals("//div[1]/div[3]/div[1]/div[1]/div[2]/div[1]/div[2]/div[1]/p/a/@href", ld.getPaginationXPath());
        assertEquals(26, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination14.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//table[1]/tbody/tr/td/table[1]/tbody/tr/td/div[1]/p/a/@href", ld.getPaginationXPath());
        assertEquals(25, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination13.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//center/table[7]/tbody/tr/td/table[2]/tbody/tr/td/table[3]/tbody/tr/td/font/a/@href", ld.getPaginationXPath());
        assertEquals(26, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination12.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//div[3]/div[3]/div[4]/table[1]/tbody/tr/td/a/@href", ld.getPaginationXPath());
        assertEquals(14, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination11.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//div[2]/div[6]/div[1]/ul[1]/li/a/@href", ld.getPaginationXPath());
        assertEquals(25, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination10.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        ;
        assertEquals("//div[1]/div[4]/div[2]/div/div[1]/div[2]/a/@href", ld.getPaginationXPath());
        assertEquals(5, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination8.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//div[2]/div[1]/div[2]/div[1]/div[1]/div[3]/ul[1]/li/a/@href", ld.getPaginationXPath());
        assertEquals(1, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination7.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//div[2]/div[3]/div[1]/div[1]/div[4]/div[5]/div[1]/div[1]/span/a/@href", ld.getPaginationXPath());
        assertEquals(2, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination6.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//div[1]/div[2]/div[2]/div[1]/div[7]/div[1]/div[1]/a/@href", ld.getPaginationXPath());
        assertEquals(6, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination5.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//table[4]/tbody/tr/td/table[1]/tbody/tr/td/table[4]/tbody/tr/td/a/@href", ld.getPaginationXPath());
        assertEquals(7, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination4.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//div[1]/div[3]/div[1]/a/@href", ld.getPaginationXPath());
        assertEquals(7, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination3.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//div[1]/form[1]/div[2]/div[3]/div[1]/div[3]/div[2]/table[1]/tbody/tr/td/ul/li/a/@href",
                ld.getPaginationXPath());
        assertEquals(4, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/webPageEntityList5.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("", ld.getPaginationXPath());
        assertEquals(0, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination2.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//div[2]/div[7]/p/span/strong/a/@href", ld.getPaginationXPath());
        assertEquals(26, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/pagination1.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//div[1]/div[3]/div[1]/div[1]/center[2]/table[1]/tbody/tr/td/center[1]/a/@href",
                ld.getPaginationXPath());
        assertEquals(24, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/website2.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//div[1]/table[2]/tbody/tr/td/table[1]/tbody/tr/td/table[1]/tbody/tr/td/table[1]/tbody/tr/td/font[1]/b/a/@href",
                ld.getPaginationXPath());
        assertEquals(49, ld.getPaginationURLs().size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/website4.html"));
        ld.setDocument(document);
        ld.setPaginationXPath("");
        ld.findPaginationURLs();
        assertEquals("//a/@href", ld.getPaginationXPath());
        assertEquals(27, ld.getPaginationURLs().size());

        // TODO download urls and load from hard disk

        // TODO A-C D-...
        // ld.findPaginationURLs("http://www.rsportscars.com/cars/");
        // assertEquals("/html/body/div/div/div/div/a/@href",ld.getPaginationXPath().toLowerCase());

        // TODO find this kind of pagination (check xpath): Drop Down Menu with "Page X"
        // ld.findPaginationURLs("http://www.phone-critic.co.uk/phone.php?action=Browse&OrderBy=&Sort=&QuickSearch=&SearchResults=&MinPrice=&MaxPrice=&PhoneType=1&PhoneBrand=&Features1=&Features2=&Features3=&Features4=&Sort=&OrderBy=&Offset=112");
        // assertEquals("/html/body/div[5]/center/table/tbody/tr/td/table[3]/tbody/tr/td/table[1]/tbody/tr/td[3]/table[2]/tbody/tr/td[1]/div[2]/table[9]/tbody/tr/td[2]/table/tbody/tr/td[3]/a/@href",ld.getPaginationXPath().toLowerCase());

        // pagination should be found TODO Javascript paging
        // ld.findPaginationURLs("http://www.bestbuy.com/site/olspage.jsp?id=pcat17080&type=page&qp=crootcategoryid%23%23-1%23%23-1~~q70726f63657373696e6774696d653a3e313930302d30312d3031~~cabcat0500000%23%230%23%2311a~~cabcat0502000%23%230%23%23o~~nf396||5370656369616c204f6666657273&list=y&nrp=15&sc=abComputerSP&sp=%2Bbrand+skuid&usc=abcat0500000");
        // assertEquals("/html/body/div[2]/div[1]/div[5]/div[1]/div[2]/ul[1]/li/a/@href",
        // ld.getPaginationXPath().toLowerCase());
    }

    public static void main(String[] a) {
        ListDiscoverer ld = new ListDiscoverer();

        ld.findPaginationURLs("http://www.softwaretipsandtricks.com/virus/");
        System.out.println(ld.getPaginationXPath().toLowerCase());
        assertEquals("/html/body/div[1]/table[2]/tr/td/table[1]/tr/td/table[1]/tr/td/table[1]/tr/td/font[1]/b/a/@href",
                ld.getPaginationXPath().toLowerCase());

    }
}