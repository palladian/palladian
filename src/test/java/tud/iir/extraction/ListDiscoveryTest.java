package tud.iir.extraction;

import tud.iir.control.AllTests;
import tud.iir.extraction.entity.ListDiscoverer;

import junit.framework.TestCase;

/**
 * Test cases for the list discoverer.
 * 
 * @author David Urbansky
 */
public class ListDiscoveryTest extends TestCase {

    public ListDiscoveryTest(String name) {
        super(name);
    }

    /*
     * public void testEntriesUniform() { ListDiscoverer ld = new ListDiscoverer(); ArrayList<String> list = new ArrayList<String>(); list.add("Abc ABC");
     * list.add("12. A"); list.add("Very Long Entry With More Than 12 Words A B C D!!!"); list.add("Short One"); list.add("Yes");
     * assertEquals(true,ld.entriesUniform(list,true)); list = new ArrayList<String>(); list.add("1."); list.add("2.");
     * list.add("Very Long Entry With More Than 12 Words A B C D!!!"); list.add("Short One"); list.add("Yes"); assertEquals(false,ld.entriesUniform(list,true));
     * list = new ArrayList<String>(); list.add("UPPERCASE"); list.add("LoWeRcAsE"); list.add("UPPERCASE"); list.add("Short One"); list.add("Yes");
     * assertEquals(false,ld.entriesUniform(list,true)); list = new ArrayList<String>(); list.add("Very Long Entry With More Than 12 Words A B C D!!!");
     * list.add("Very Long Entry With More Than 12 Words A B C D!!!"); list.add("Very Long Entry With More Than 12 Words A B C D!!!");
     * list.add("Very Long Entry With More Than 12 Words A B C D!!! Very Long Entry With More Than 12 Words A B C D!!!");
     * list.add("Short but still not too short"); assertEquals(false,ld.entriesUniform(list,true)); list = new ArrayList<String>(); list.add("UPPER CASE");
     * list.add("LoWeR cAsE"); list.add("d"); list.add("SHORT"); list.add("ONE"); assertEquals(false,ld.entriesUniform(list,true)); } public void
     * testFindEntityColumn() { ListDiscoverer ld = new ListDiscoverer(); Document document = null; Crawler crawler = new Crawler(); document =
     * crawler.getDocument("data/benchmarkSelection/entities/google8/website3.html"); //System.out.println(ld.findEntityColumn(document,
     * "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD")); assertEquals(2, ld.findEntityColumn(document, "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD")); document =
     * crawler.getDocument("data/benchmarkSelection/entities/google8/website5.html"); assertEquals(1, ld.findEntityColumn(document,
     * "/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD/P")); document =
     * crawler.getDocument("data/benchmarkSelection/entities/google8/website9.html"); assertEquals(0, ld.findEntityColumn(document,
     * "/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A/B")); document =
     * crawler.getDocument("data/benchmarkSelection/entities/google8/website11.html"); assertEquals(1, ld.findEntityColumn(document,
     * "/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD")); document = crawler.getDocument("data/benchmarkSelection/entities/google8/website17.html");
     * assertEquals(0, ld.findEntityColumn(document, "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[4]/TR/TD/UL/LI/A")); document =
     * crawler.getDocument("data/benchmarkSelection/entities/google8/website27.html"); assertEquals(3, ld.findEntityColumn(document,
     * "/HTML/BODY/FORM/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD" ));
     * document = crawler.getDocument("data/benchmarkSelection/entities/google8/website29.html"); assertEquals(-1, ld.findEntityColumn(document,
     * "/HTML/BODY/CENTER/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD")); document =
     * crawler.getDocument("data/benchmarkSelection/entities/google8/website33.html"); assertEquals(2, ld.findEntityColumn(document,
     * "/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD/A")); document =
     * crawler.getDocument("data/benchmarkSelection/entities/google8/website67.html"); assertEquals(2, ld.findEntityColumn(document,
     * "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD/I/A")); document = crawler.getDocument("data/benchmarkSelection/entities/google8/website69.html");
     * assertEquals(3, ld.findEntityColumn(document, "/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/P/TABLE/TR/TD/FONT/A")); document =
     * crawler.getDocument("data/benchmarkSelection/entities/google8/website87.html"); assertEquals(0, ld.findEntityColumn(document,
     * "/HTML/BODY/TABLE[3]/TR/TD/UL/LI")); }
     */

    // TODO see problems in comments
    /**
     * Problems: 1. lists with other elements in between two entries should be ranked lower 2. if a list is not uniform, the next highest ranked xpath should be
     * considered 3. sibling page is the same but in different language, look only for sibling pages when list has less than 50 entries 4. sibling page is
     * exactly the same or wrong or only different sorting etc. 5. page incorrectly encoded, can not be repaired easily 6. get more tables, especially when same
     * path but different table index have almost the same count 7. move stages up when current list not uniform | try other sibling stages to root stage 8.
     * more than one list on the page 9. remove common words around entries (for example ABC review, DEF review => review should be removed) 10. list on page
     * with horizontal entries (display:inline) 11. list too short or detail list 12. think about indices (keep last index only?) TODO
     * 
     * current precision': 34/44 ~ 0.7727 (correct assignment or empty path although pass expected) current accuracy: 34/60 ~ 0.5667 total tests: 60
     */
    public void testDiscoverEntityXPathOnline() {
        ListDiscoverer ld = new ListDiscoverer();

        if (AllTests.ALL_TESTS) {
            // // lists should be found
            // mixed
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A", ld.discoverEntityXPath("http://simple.wikipedia.org/wiki/List_of_diseases"));
            // TODO Problem 7 (currently empty list)
            // assertEquals("/HTML/BODY/TABLE/TR/TD[2]",ld.discoverEntityXPath("http://www.novamedia.de/devices/supportedPhones.php?s=flk"));
            assertEquals("/HTML/BODY/DIV/TABLE[3]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/FONT/A", ld
                    .discoverEntityXPath("http://www.fantasycars.com/derek/list.html"));
            assertEquals(
                    "/HTML/BODY/DIV/TABLE[1]/TR/TD/DIV/UL/LI/A",
                    ld
                            .discoverEntityXPath("http://www.racv.com.au/wps/wcm/connect/Internet/Primary/my+car/advice+%26+information/compare+cars/list+of+all+cars/"));
            // TODO Problem 6 (currently partly correct => wrong)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/NYT_TEXT/DIV/TABLE[3]/TR/TD[1]/A",ld.discoverEntityXPath("http://www.nytimes.com/ref/movies/1000best.html"));
            // TODO Problem 4,5 (check xpath), (currently empty list)
            // assertEquals("/HTML/BODY/TABLE[1]/TR/TD/P/TABLE/TR/TD/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/FONT/A",ld.discoverEntityXPath("http://www.state.gov/misc/list/index.htm"));
            // TODO Problem 3 (currently gets wrong list)
            // assertEquals("/HTML/BODY/DIV/TABLE[1]/TR/TD/TABLE[2]/TR/TD[2]/A",ld.discoverEntityXPath("http://www.who.int/countries/en/"));
            assertEquals("/HTML/BODY/UL/UL/UL/UL/UL/UL/UL/UL/P/UL/LI/A", ld.discoverEntityXPath("http://www.theodora.com/wfb/abc_world_fact_book.html"));
            assertEquals("/HTML/BODY/TABLE[1]/TR/TD/P/TABLE[1]/TR/TD/UL/LI/B/A", ld.discoverEntityXPath("http://www.austlii.edu.au/catalog/215.html"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/P/A", ld.discoverEntityXPath("http://www.imf.org/external/country/index.htm"));
            // TODO Problem 2 (currently finds wrong list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE/TR/TD[2]/DIV/A",ld.discoverEntityXPath("http://research.cars.com/go/crp/buyingGuides/Story.jsp?year=New&section=Sports&subject=Sports&story=highperformance"));
            // TODO Problem 1 (currently finds wrong list)
            // assertEquals("/HTML/BODY/TABLE[1]/TR/TD/TABLE/TR/TD/A", ld.discoverEntityXPath("http://www.hollywoodteenmovies.com/A.html"));
            assertEquals("/HTML/BODY/TABLE[1]/TR/TD/P/SPAN", ld
                    .discoverEntityXPath("http://naturalhealthtechniques.com/ExamForms-MedicalIntuitive/list_of_sports.htm"));
            // TODO Problem 6 (currently partly correct => wrong)
            // assertEquals("/HTML/BODY/DIV/DIV/TABLE/TR/TD/A",ld.discoverEntityXPath("http://www.mic.ki.se/Diseases/alphalist.html"));
            assertEquals("/HTML/BODY/TABLE[1]/TR/TD/FONT/B/A", ld.discoverEntityXPath("http://www.stanford.edu/~petelat1/birdlist.html"));
            assertEquals("/HTML/BODY/DIV/DIV/P/SPAN/A", ld.discoverEntityXPath("http://www.georgia.gov/00/topic_index_channel/0,2092,4802_5081,00.html"));

            // concept: country
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A", ld.discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_countries"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]/A", ld
                    .discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_countries_by_population"));
            assertEquals("/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD[1]", ld.discoverEntityXPath("http://www.internetworldstats.com/list2.htm"));
            assertEquals("/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A", ld.discoverEntityXPath("http://dir.yahoo.com/regional/countries/"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD[1]", ld
                    .discoverEntityXPath("http://www.auswaertiges-amt.de/diplo/en/WillkommeninD/EinreiseUndAufenthalt/StaatenlisteVisumpflicht.html"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE[1]/TR/TD[1]", ld.discoverEntityXPath("http://www.cellular-news.com/car_bans/"));
            // TODO Problem 8 (currently finds wrong list) also http://en.wikipedia.org/wiki/Lists_of_films
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE[1]/TR/TD[1]",ld.discoverEntityXPath("http://www.worldatlas.com/geoquiz/thelist.htm"));

            // concept: car
            assertEquals("/HTML/BODY/TABLE[5]/TR/TD/TABLE[4]/TR/TD/A", ld.discoverEntityXPath("http://wallpaper.diq.ru/17_map.htm"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A", ld.discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_Asian_cars"));
            assertEquals("/HTML/BODY/DIV/DIV/UL/LI/A", ld.discoverEntityXPath("http://www.rsportscars.com/cars/"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A", ld
                    .discoverEntityXPath("http://www.time.com/time/specials/2007/completelist/0,,1658545,00.html"));
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
            assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD[2]/A", ld.discoverEntityXPath("http://www.esato.com/phones/"));
            // TODO Problem 2,7 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_best-selling_mobile_phones"));
            // TODO Problem 4 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE/TBODY/TR/TD[2]/A",ld.discoverEntityXPath("http://www.expansys.com/n.aspx?c=169"));
            // TODO Problem 1,2,6,7,12 (currently empty list) (check xpath)
            // assertEquals("/HTML/BODY/DIV/CENTER/TABLE/TBODY/TR/TD/TABLE[3]/TBODY/TR/TD/TABLE[1]/TBODY/TR/TD/TABLE[2]/TBODY/TR/TD/DIV/TABLE[6]/CAPTION/A",ld.discoverEntityXPath("http://www.phone-critic.co.uk/phone.php?action=Browse&OrderBy=&Sort=&QuickSearch=&SearchResults=&MinPrice=&MaxPrice=&PhoneType=1&PhoneBrand=&Features1=&Features2=&Features3=&Features4=&Sort=&OrderBy=&Offset=112"));

            // TODO find xpath
            assertEquals("", ld.discoverEntityXPath("http://123simlock.nl/alletoestellen.php"));

            // concept: notebook
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI", ld
                    .discoverEntityXPath("http://danstechnstuff.com/2008/08/08/dell-adds-to-list-of-notebooks-with-nvidia-gpu-problems/"));
            // TODO Problem 2,11 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/TABLE[1]/TR/TD/TABLE[1]/TBODY/TR/TD/B/A",ld.discoverEntityXPath("http://asia.cnet.com/reviews/notebooks/0,39050495,39315110,00.htm"));

            // concept: movie
            // TODO worked before but now incorrect page
            // retrieved:assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD[2]/I/A",ld.discoverEntityXPath("http://en.wikipedia.org/wiki/100_Years...100_Movies"));
            assertEquals("/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/P/TABLE/TR/TD[3]/FONT/A", ld
                    .discoverEntityXPath("http://www.imdb.com/top_250_films"));
            assertEquals("/HTML/BODY/DIV/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD[2]/A", ld
                    .discoverEntityXPath("http://www.imdb.com/TitlesByYear?year=2008&start=A&nav=/Sections/Years/2008/include-titles"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/I/A", ld.discoverEntityXPath("http://en.wikiquote.org/wiki/List_of_films"));
            // TODO Problem 11 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H4",ld.discoverEntityXPath("http://www.firstshowing.net/2007/12/18/why-2008-will-be-an-awesome-year-for-movies/"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/OL/LI", ld
                    .discoverEntityXPath("http://www.berbecuta.com/2008/03/14/1001-movie-you-must-see-before-you-die/"));
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
            assertEquals("/HTML/BODY/DIV/DIV/TABLE[1]/TR/TD[1]", ld.discoverEntityXPath("http://www.actorscelebs.com/actor/Edward_Norton/"));
            // TODO Problem 2,7 (currently empty list) (check xpath)
            // assertEquals("/HTML/BODY/CENTER/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[2]/TR/TD[2]/A",ld.discoverEntityXPath("http://www.filmcrave.com/list_top_movie.php"));

            // concept: actor
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A", ld.discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_French_actors"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A", ld.discoverEntityXPath("http://www.atpictures.com/index.php%3Fview%3Dall"));
            assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A", ld.discoverEntityXPath("http://www.djmick.co.uk/actors_pictures.htm"));
            // TODO Problem 2,8 (currently empty list)
            // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath("http://koreanfilm.org/actors.html"));
            assertEquals("/HTML/BODY/DIV/DIV/A", ld.discoverEntityXPath("http://www.actorscelebs.com/browse/d/"));

            // // no list should be found
            // concept: car
            assertEquals("", ld.discoverEntityXPath("http://www.sealcoveautomuseum.org/list.html"));

            // concept: mobile phone
            assertEquals("", ld.discoverEntityXPath("http://wordpress.com/tag/price-list-of-mobile-phones-in-pakistan/"));
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
            assertEquals("", ld.discoverEntityXPath("http://131.230.176.4/cgi-bin/dol/dol_terminal.pl?taxon_name=Albizia_coriaria"));

            // concept: song TODO
            assertEquals("", ld.discoverEntityXPath("http://catamountjazz.com/Pictures2/Ultimate.html"));

            // concept: video game TODO
            assertEquals("", ld.discoverEntityXPath("http://www.arcadeathome.com/shots.phtml"));
            assertEquals("", ld.discoverEntityXPath("caesar.logiqx.com/php/emulator_games.php?id=mame&letter=S"));

            // concept: insect TODO
            assertEquals("", ld.discoverEntityXPath("http://www.pestcontrolcanada.com/INSECTS/insects%202.htm"));

            // concept: tv show TODO
            assertEquals("", ld.discoverEntityXPath("http://www.fancast.com/full_episodes;jsessionid=A3DE50C0E7A4F9D0D9BA53A3AE4FD312"));

        }
    }

    // TODO see problems in comments
    /*
     * public void testDiscoverEntityXPathOffline() { ListDiscoverer ld = new ListDiscoverer(); int correct = 0;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/B".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website1.html"))) correct++; //
     * even better if /HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]/A if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]".equalsIgnoreCase(ld
     * .discoverEntityXPath("data/benchmarkSelection/entities/google8/website3.html"))) correct++; // even better if
     * /HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD/P/A if("/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD[1]".equalsIgnoreCase(ld.
     * discoverEntityXPath("data/benchmarkSelection/entities/google8/website5.html"))) correct++;
     * if("/HTML/BODY/TABLE[1]/TBODY/TR/TD/P/TABLE/TBODY/TR/TD/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website7.html"))) correct++;
     * if("/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website9.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD[1]"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website11.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE[1]/TR/TD[1]"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website13.html"))) correct++; // for next line, distribution
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website15.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[4]/TR/TD/UL/LI/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website17.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website19.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website21.html")))
     * correct++;if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website23.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H3/A".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website25.html")))
     * correct++;if(
     * "/HTML/BODY/FORM/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD[3]"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website27.html"))) correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website29.html"))) correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website31.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD[2]/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website33.html"))) correct++; // next one would benefit if link would
     * need to be in table or list if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website35.html"))) correct++; // next
     * one would benefit if navigation lists were detected
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website37.html"))) correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website39.html"))) correct++; // next one would benefit it box
     * elements are analyzed and only those with few child structures are considered
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website41.html"))) correct++; // next one would benefit if DIV
     * got index if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website43.html"))) correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website49.html"))) correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website51.html"))) correct++; // next one is rendered with
     * javascript, DOM must be created after page is full loaded
     * if("/HTML/BODY/DIV/DIV/TABLE/TR/TD/TABLE/TBODY/TR/TD[1]".equalsIgnoreCase(ld.discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website55.html"))) correct++;
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website57.html"))) correct++; // next one would benefit if DIV
     * got index if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website59.html"))) correct++; // next one would benefit
     * from structure checking, and variability in word length
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website61.html"))) correct++; // next one would benefit if
     * navigation lists were detected if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website63.html"))) correct++; //
     * next one would also benefit from distribution if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website65.html")))
     * correct++; if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD[2]/I/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website67.html"))) correct++; // alternatively for the following one:
     * /HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD[3]
     * if("/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD/FONT/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website69.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/I/A".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website71.html"))) correct++;
     * // next one would benefit from box structure analysis and ranking h higher
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H4".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website73.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/OL/LI".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website77.html" )))
     * correct++; // next one would benefit if navigation lists were detected
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website79.html"))) correct++; // next one should be filtered out
     * //if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website81.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website83.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website85.html" )))
     * correct++; // next one should be filtered out
     * //if("/HTML/BODY/TABLE[3]/TR/TD/UL/LI".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website87.html"))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website89.html"))) correct++;
     * // next one would benefit if navigation lists were detected
     * if("".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website91.html"))) correct++; // next one would benefit if p got
     * index if("/HTML/BODY/DIV/P/TABLE/TR/TD/P/I".equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website93.html")))
     * correct++; // next one should be filtered out //if("/HTML/BODY/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/UL/LI/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website95.html"))) correct++;
     * System.out.println("correct: "+correct+" ~ "+(double)correct/41.0); //if (correct > -1) return; try { Thread.sleep(2000); } catch (InterruptedException
     * e) { e.printStackTrace(); }
     * //assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath("http://simple.wikipedia.org/wiki/List_of_diseases"));
     * //assertEquals("/HTML/BODY/TABLE/TR/TD[2]",ld.discoverEntityXPath("http://www.novamedia.de/devices/supportedPhones.php?s=flk"));
     * //assertEquals("/HTML/BODY/DIV/TABLE/TR/TD/TABLE/TR/TD/DIV/TABLE/TR/TD/TABLE/TR/TD/DIV/TABLE/TR/TD/FONT/A",ld.discoverEntityXPath(
     * "http://www.fantasycars.com/derek/list.html"));//assertEquals("/HTML/BODY/DIV/TABLE/TR/TD/DIV/UL/LI/A",ld.discoverEntityXPath(
     * "http://www.racv.com.au/wps/wcm/connect/Internet/Primary/my+car/advice+%26+information/compare+cars/list+of+all+cars/"));
     * //assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD/UL/LI/A",ld.discoverEntityXPath("http://en.wikipedia.org/wiki/List_of_cars")); // more tables
     * //assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/NYT_TEXT/DIV/TABLE/TR/TD[1]/A",ld.discoverEntityXPath(
     * "http://www.nytimes.com/ref/movies/1000best.html")); // sibling page is exactly the same (check xpath) //
     * assertEquals("/HTML/BODY/TABLE[1]/TR/TD/P/TABLE/TR/TD/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/FONT/A"
     * ,ld.discoverEntityXPath("http://www.state.gov/misc/list/index.htm")); // sibling page is the same but in different language (check xpath) //
     * assertEquals("/HTML/BODY/DIV/TABLE/TR/TD/TABLE/TR/TD[2]/A",ld.discoverEntityXPath("http://www.who.int/countries/en/")); //
     * assertEquals("/HTML/BODY/UL/UL/UL/UL/UL/UL/UL/UL/P/UL/LI/A",ld.discoverEntityXPath("http://www.theodora.com/wfb/abc_world_fact_book.html")); //
     * assertEquals("/HTML/BODY/TABLE[1]/TR/TD/P/TABLE[1]/TR/TD/UL/LI/B/A",ld.discoverEntityXPath("http://www.austlii.edu.au/catalog/215.html")); //
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/P/A",ld.discoverEntityXPath("http://www.imf.org/external/country/index.htm")); // got the wrong list
     * //assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE/TR/TD[2]/DIV/A",ld.discoverEntityXPath(
     * "http://research.cars.com/go/crp/buyingGuides/Story.jsp?year=New&section=Sports&subject=Sports&story=highperformance));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/B",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website1.html")); // even better if
     * /HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]/A
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website3.html")); // even
     * better if /HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD/P/A assertEquals("/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD[1]"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website5.html"));
     * assertEquals("/HTML/BODY/TABLE[1]/TBODY/TR/TD/P/TABLE/TBODY/TR/TD/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/A"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website7.html"));
     * assertEquals("/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website9.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD[1]"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website11.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE[1]/TR/TD[1]"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website13.html")); // detect horizontal lists //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website15.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[4]/TR/TD/UL/LI/A" ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website17.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website19.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/UL/LI/A",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website21.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website23.html")); // detect page similarities (works with sibling page analysis) //
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H3/A",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website25.html")); assertEquals
     * ("/HTML/BODY/FORM/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD[3]"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website27.html"));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website29.html"));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website31.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD[2]/A"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website33.html")); // detect horizontal lists (2) || next one would benefit if link
     * would need to be in table or list assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website35.html")); // detect page
     * similarities (2) (works with sibling page analysis) || do not take because not in list or table //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website37.html")); // detect horizontal lists (3) alternative path:
     * /HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website39.html")); // analyze
     * position on page // assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website41.html")); // detect page similarities?
     * filtering out? // assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website43.html"));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website49.html"));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website51.html")); // next one is rendered with javascript, DOM must be
     * created after page is full loaded //
     * assertEquals("/HTML/BODY/DIV/DIV/TABLE/TR/TD/TABLE/TBODY/TR/TD[1]",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website55.html" ));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website57.html")); // detect page similarities (3) (works with sibling
     * page analysis) // assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website59.html")); // check variability of content
     * between list entries // assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website61.html")); // detect page similarities
     * (4) (works with sibling page analysis) // assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website63.html")); // next one
     * would also benefit from distribution assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website65.html")); assertEquals(
     * "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD[2]/I/A",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website67.html")); // alternatively
     * for the following one: /HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD[3]
     * assertEquals("/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/P/TABLE/TR/TD[3]/FONT/A"
     * ,ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website69.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/I/A",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website71.html")); // next one would
     * benefit from box structure analysis and ranking h higher //
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H4",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website73.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/OL/LI",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website77.html")); // detect
     * page similarities (5) (works with sibling page analysis) //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website79.html")); // next one should be filtered out
     * //assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website81.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website83.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website85.html")); // next one
     * should be filtered out
     * //assertEquals("/HTML/BODY/TABLE[3]/TR/TD/UL/LI",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website87.html"));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website89.html")); // detect page
     * similarities (6) (works with sibling page analysis) //
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website91.html"));
     * assertEquals("",ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website93.html")); // next one should be filtered out
     * //if("/HTML/BODY/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/UL/LI/A"
     * .equalsIgnoreCase(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website95.html"))) correct++; } public void
     * testDiscoverEntityXPathNoCount() { ListDiscoverer ld = new ListDiscoverer(); int correct =
     * 0;if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/B".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website1.html")))) correct++; // even better if /HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]/A
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website3.html")))) correct++; // even better
     * if /HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD/P/A if("/HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD".equalsIgnoreCase
     * (PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website5.html")))) correct++;
     * if("/HTML/BODY/TABLE/TBODY/TR/TD/P/TABLE/TBODY/TR/TD/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website7.html")))) correct++;
     * if("/HTML/BODY/TABLE/TR/TD/TABLE/TR/TD/TABLE/TR/TD/DIV/UL/LI/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website9.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website11.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE/TR/TD"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website13.html")))) correct++; // for next
     * line, distribution if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website15.html"))))
     * correct++;if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD/UL/LI/A".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website17.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website19.html")))) correct++; if("/HTML/BODY/DIV/DIV/UL/LI/A".equalsIgnoreCase
     * (PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website21.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website23.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H3/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website25.html")))) correct++;
     * if("/HTML/BODY/FORM/TABLE/TR/TD/DIV/TABLE/TR/TD/TABLE/TR/TD/TABLE/TR/TD/DIV/TABLE/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website27.html")))) correct++;
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website29.html")))) correct++;
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website31.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/TABLE/TR/TD/P/TABLE/TR/TD/TABLE/TR/TD/A".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website33.html")))) correct++; // next one would benefit if link would need to be in table or list
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website35.html")))) correct++; // next
     * one would benefit if navigation lists were detected
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website37.html")))) correct++; // next
     * one would benefit it box elements are analyzed and only those with few child structures are considered if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website39.html")))) correct++; // next one
     * would benefit it box elements are analyzed and only those with few child structures are considered
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website41.html")))) correct++; // next
     * one would benefit if DIV got index
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website43.html")))) correct++;
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website49.html")))) correct++;
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website51.html")))) correct++; // next
     * one is rendered with javascript, DOM must be created after page is full loaded if("/HTML/BODY/DIV/DIV/TABLE/TR/TD/TABLE/TBODY/TR/TD"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website55.html")))) correct++;
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website57.html")))) correct++; // next
     * one would benefit if DIV got index
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website59.html")))) correct++; // next
     * one would benefit from structure checking, and variability in word length
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website61.html")))) correct++; // next
     * one would benefit if navigation lists were detected
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website63.html")))) correct++; // next
     * one would also benefit from distribution
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website65.html"))))
     * correct++;if("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD/I/A".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website67.html")))) correct++; // alternatively for the following one:
     * /HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD[3]
     * if("/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD/FONT/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website69.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/I/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website71.html")))) correct++; // next one
     * would benefit from box structure analysis and ranking h higher if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H4".equalsIgnoreCase(PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website73.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/OL/LI".equalsIgnoreCase
     * (PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website77.html")))) correct++; // next one would benefit if
     * navigation lists were detected
     * if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website79.html")))) correct++; // next
     * one should be filtered out //if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website81.html")))) correct++; if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(PageAnalyzer.
     * removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website83.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website85.html")))) correct++; // next one
     * should be filtered out//if("/HTML/BODY/TABLE[3]/TR/TD/UL/LI".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website87.html")))) correct++;
     * if("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A".equalsIgnoreCase(PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website89.html")))) correct++; // next one would benefit if navigation lists were
     * detected if("".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website91.html")))) correct++;
     * // next one would benefit if p got index if("/HTML/BODY/DIV/P/TABLE/TR/TD/P/I".equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website93.html")))) correct++; // next one should be filtered out
     * //if("/HTML/BODY/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/UL/LI/A"
     * .equalsIgnoreCase(PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website95.html"))) correct++;
     * System.out.println("correct: "+correct+" ~ "+(double)correct/41.0); //if (correct > -1) return;
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/B",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website1.html"))); // even better if /HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD[2]/A
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website3.html"))); // even better if
     * /HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD/P/A assertEquals("/HTML/BODY/CENTER/TABLE/TR/TD/BLOCKQUOTE/TABLE/TR/TD",PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website5.html")));
     * assertEquals("/HTML/BODY/TABLE/TBODY/TR/TD/P/TABLE/TBODY/TR/TD/DIV/DIV/DIV/DIV/BLOCKQUOTE/P/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website7.html")));
     * assertEquals("/HTML/BODY/TABLE/TR/TD/TABLE/TR/TD/TABLE/TR/TD/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website9.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website11.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/CENTER/TABLE/TR/TD"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website13.html"))); // for next line, distribution //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website15.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website17.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website19.html"))); assertEquals("/HTML/BODY/DIV/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website21.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website23.html"))); //
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H3/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website25.html")));
     * assertEquals("/HTML/BODY/FORM/TABLE/TR/TD/DIV/TABLE/TR/TD/TABLE/TR/TD/TABLE/TR/TD/DIV/TABLE/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website27.html")));
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website29.html")));
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website31.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/TABLE/TR/TD/P/TABLE/TR/TD/TABLE/TR/TD/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website33.html"))); // next one would benefit if link would
     * need to be in table or list //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website35.html"))); // next one would benefit
     * if navigation lists were detected | do not take because not in list or table //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website37.html"))); // next one would benefit
     * it box elements are analyzed and only those with few child structures are considered // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website39.html"))); // next one would benefit it box elements
     * are analyzed and only those with few child structures are considered | do not take because not in list or table //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website41.html"))); // next one would benefit
     * if DIV got index // assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website43.html")));
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website49.html")));
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website51.html"))); // next one is rendered
     * with javascript, DOM must be created after page is full loaded // assertEquals("/HTML/BODY/DIV/DIV/TABLE/TR/TD/TABLE/TBODY/TR/TD[1]",PageAnalyzer
     * .removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website55.html")));
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website57.html"))); // next one would benefit
     * if DIV got index // assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website59.html"))); //
     * next one would benefit from structure checking, and variability in word length //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website61.html"))); // next one would benefit
     * if navigation lists were detected //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website63.html"))); // next one would also
     * benefit from distribution assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website65.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/TABLE/TR/TD/I/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website67.html"))); // alternatively for the following one:
     * /HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD[3]
     * assertEquals("/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE/TR/TD/DIV/TABLE/TR/TD/P/TABLE/TR/TD/FONT/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website69.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/I/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website71.html"))); // next one would benefit from box
     * structure analysis and ranking h higher // assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H4",PageAnalyzer.removeCounts(ld.discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website73.html"))); assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/OL/LI",PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website77.html"))); // next one would benefit if navigation lists were detected //
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website79.html"))); // next one should be
     * filtered out//assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",PageAnalyzer.removeCounts(ld.discoverEntityXPath(
     * "data/benchmarkSelection/entities/google8/website81.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A",PageAnalyzer.removeCounts(ld.discoverEntityXPath
     * ("data/benchmarkSelection/entities/google8/website83.html"))); assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/UL/LI/A",PageAnalyzer.removeCounts
     * (ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website85.html"))); // next one should be filtered out
     * //assertEquals("/HTML/BODY/TABLE[3]/TR/TD/UL/LI"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website87.html")));
     * assertEquals("/HTML/BODY/DIV/DIV/DIV/DIV/UL/LI/A"
     * ,PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website89.html"))); // next one would benefit if navigation
     * lists were detected //assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website91.html"))); //
     * next one would benefit if p got index
     * assertEquals("",PageAnalyzer.removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website93.html"))); // next one should be
     * filtered out //if("/HTML/BODY/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/UL/LI/A".equalsIgnoreCase(PageAnalyzer
     * .removeCounts(ld.discoverEntityXPath("data/benchmarkSelection/entities/google8/website95.html"))) correct++; }
     */

    /**
     * Test the accuracy of recognizing pagination xPaths. Current accuracy: TODO update! 64/71 ~ 0.9014 total tests: 71
     */
    public void testGetPaginationXPath() {
        ListDiscoverer ld = new ListDiscoverer();

        ld.findPaginationURLs("data/test/webPages/website2.html");
        assertEquals("/html/body/div[1]/table[2]/tr/td/table[1]/tr/td/table[1]/tr/td/table[1]/tr/td/font[1]/b/a/@href", ld.getPaginationXPath().toLowerCase());

        ld.findPaginationURLs("data/test/webPages/website4.html");
        assertEquals("/html/body/a/@href", ld.getPaginationXPath().toLowerCase());

        if (AllTests.ALL_TESTS) {
            // TODO download urls and load from hard disk

            // // other concepts
            // pagination should be found
            ld.findPaginationURLs("http://countrymusic.about.com/library/blindex-d.htm#l");
            // System.out.println(ld.getPaginationXPath().toLowerCase());
            assertEquals("/html/body/div[1]/div[3]/div[1]/div[1]/center[2]/table[1]/tr/td/center[1]/a/@href", ld.getPaginationXPath().toLowerCase());

            // TODO xpath
            ld.findPaginationURLs("http://imfc.cfl.scf.rncan.gc.ca/insecte-insect/index-eng.asp?ind=B");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            // // concept country
            // pagination should be found

            // no pagination
            ld.findPaginationURLs("http://en.wikipedia.org/wiki/List_of_countries");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.infoplease.com/countries.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://web.worldbank.org/WBSITE/EXTERNAL/COUNTRIES/0,,pagePK:180619~theSitePK:136917,00.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.state.gov/misc/list/index.htm");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.cantonpl.org/kids/country.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.who.int/countries/en/");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.theodora.com/wfb/abc_world_fact_book.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.austlii.edu.au/catalog/215.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.imf.org/external/country/index.htm");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.atlapedia.com/online/country_index.htm");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://pe.usps.gov/text/imm/immctry.htm");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            // // concept car
            // pagination should be found
            ld.findPaginationURLs("http://www.automart.com/make-browse/a/acura/");
            assertEquals("/html/body/div[1]/div[1]/div[2]/div[2]/div[2]/div[11]/div[1]/div[3]/div[1]/form[1]/span/a/@href", ld.getPaginationXPath()
                    .toLowerCase());

            // additional since it is from the same domain as above
            // ld.findPaginationURLs("http://www.automart.com/newcarresearchcenter/all_convertible");
            // assertEquals("/html/body/div[4]/div[1]/div[2]/div[2]/div[2]/div/div[16]/form[1]/div/div[1]/span/a/@href",ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.buyyourcar.co.uk/used-car.aspx");
            assertEquals("/html/body/div[1]/div/div[2]/div[1]/div/div[1]/div[1]/div[1]/div/div/div/div/div[1]/div[1]/ul[1]/li/a/@href", ld.getPaginationXPath()
                    .toLowerCase());

            ld.findPaginationURLs("http://www.carmax.com/enUS/search-results/default.html?ANa=207&Ep=homepage+button+MPG&zip=98532&D=90");
            assertEquals("/html/body/div[1]/form[1]/div[2]/div[3]/div[1]/div[2]/div[1]/table[1]/tr/td/ul/li/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://aston-martin.autoextra.com/model");
            assertEquals("/html/body/div[1]/div[1]/div[1]/div[4]/div[5]/div[1]/div[3]/div[2]/table[1]/tr/td/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.classiccarsforsale.co.uk/view-classic-cars-for-sale.php");
            assertEquals("/html/body/div[1]/div[2]/div[1]/a/@href", ld.getPaginationXPath().toLowerCase());

            // TODO
            ld.findPaginationURLs("http://www.1888pressrelease.com/company-list-a_4.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            // TODO A-C D-...
            // ld.findPaginationURLs("http://www.rsportscars.com/cars/");
            // assertEquals("/html/body/div/div/div/div/a/@href",ld.getPaginationXPath().toLowerCase());

            // no pagination
            ld.findPaginationURLs("http://research.cars.com/go/crp/buyingGuides/Story.jsp?year=New&section=Sports&subject=Sports&story=highperformance");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.carseek.com/reviews/mazda/2009-tribute-hybrid/");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.netcars.co.uk/browse/aston_martin-used-cars-for-sale-by-model,4.aspx");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.autobytel.com/content/research/vir/index.cfm/vehicle_number_int/1024512/action/summary");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.autotrader.com/find/Chrysler-300-cars-for-sale.jsp");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.nascar.com/kyn/nbtn/cup/data/car/");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.car-list.net/");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            // // concept mobile phone
            // pagination should be found wrong xpath (DOM parser error?)
            // ld.findPaginationURLs("http://shopping.ninemsn.com.au/results/mobiles/bcatid1875/nokia/2-2581/forsale?text=category:mobiles+Brand:Nokia&page=1");
            // assertEquals("/html/body/div[2]/div[3]/div[7]/form/div[1]/div[1]/div[2]/a/@href",ld.getPaginationXPath().toLowerCase());

            // only one number
            // ld.findPaginationURLs("http://asia.cnet.com/reviews/mobilephones/0,39051206,20000066q-2,00.htm?checklist=&sort=&filter=gt");
            // assertEquals("/html/body/div[1]/div[3]/div[2]/form/div[2]/p/a/@href",ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.gsmarena.com/motorola-phones-4.php");
            // System.out.println("path:" + ld.getPaginationXPath().toLowerCase());
            assertEquals("/html/body/div[1]/div[3]/div[1]/div[1]/div[7]/div[1]/div[1]/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.amazon.com/s/?%255Fencoding=UTF8&index=wireless-phones&field-browse=301187");
            assertEquals("/html/body/table[3]/tr/td/table[2]/tr/td/table[1]/tr/td/span/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://findarticles.com/p/articles/mi_m0EIN/is_2007_Oct_23/ai_n27417795");
            assertEquals("/html/body/div[2]/div[2]/div[1]/div[1]/div[1]/div[2]/div[2]/div[2]/ul/li/a/@href", ld.getPaginationXPath().toLowerCase());

            // TODO find this kind of pagination (check xpath)
            // ld.findPaginationURLs("http://www.phone-critic.co.uk/phone.php?action=Browse&OrderBy=&Sort=&QuickSearch=&SearchResults=&MinPrice=&MaxPrice=&PhoneType=1&PhoneBrand=&Features1=&Features2=&Features3=&Features4=&Sort=&OrderBy=&Offset=112");
            // assertEquals("/html/body/div[5]/center/table/tbody/tr/td/table[3]/tbody/tr/td/table[1]/tbody/tr/td[3]/table[2]/tbody/tr/td[1]/div[2]/table[9]/tbody/tr/td[2]/table/tbody/tr/td[3]/a/@href",ld.getPaginationXPath().toLowerCase());

            // no pagination
            ld.findPaginationURLs("http://forums.whirlpool.net.au/forum-replies-archive.cfm/1037458.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.cellular.co.za/phones/java/javaphones.htm");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://pages.ebay.co.uk/buy/guides/mobile-phone-advice/");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.uk-business-directory.com/mobile-phone-listing/");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://newsgroups.derkeiler.com/Archive/Uk/uk.people.consumers.ebay/2005-11/msg02545.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            // // concept notebook
            // pagination should be found
            ld
                    .findPaginationURLs("http://www.bestbuy.com/site/olspage.jsp?id=pcat17080&type=page&qp=crootcategoryid%23%23-1%23%23-1~~q70726f63657373696e6774696d653a3e313930302d30312d3031~~cabcat0500000%23%230%23%2311a~~cabcat0502000%23%230%23%23o~~nf396||5370656369616c204f6666657273&list=y&nrp=15&sc=abComputerSP&sp=%2Bbrand+skuid&usc=abcat0500000");
            assertEquals("/html/body/div[2]/div[1]/div[5]/div[1]/div[2]/ul[1]/li/a/@href", ld.getPaginationXPath().toLowerCase());

            ld
                    .findPaginationURLs("http://www.notebookshop.com/index.php?main_page=advanced_search_result&keyword=keywords&search_in_description=1&categories_id=&inc_subcat=1&manufacturers_id=&pfrom=0&pto=999&dfrom=&dto=");
            assertEquals("/html/body/div[1]/div[4]/div[2]/div/div[1]/div[2]/a/@href", ld.getPaginationXPath().toLowerCase());

            ld
                    .findPaginationURLs("http://www.notebookshop.com.my/webshaper/store/advancedSearch.asp?relation=to&priceFrom=630&priceTo=945&FlagProcess=1&sort=priceLH");
            assertEquals("/html/body/div[1]/div[4]/div[1]/div[3]/form[1]/div[1]/div[2]/div[1]/ul[1]/li/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.yelp.com/biz/notebookshop-com-cerritos-2");
            assertEquals("/html/body/div[4]/div[2]/span/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://shop-ship-notebook.zlio.net/c/Notebook-Laptop/Asus/5104350");
            assertEquals("/html/body/div[1]/div[2]/div[1]/div[2]/div/div/div[2]/div[1]/a/@href", ld.getPaginationXPath().toLowerCase());

            // no pagination
            ld.findPaginationURLs("http://www.bolthole.com/solaris/x86-laptops.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://shopping.aol.com/aspire-one-aoa110-1295-notebook/83594512");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            // TODO link similarity check in crawler (currently wrong xpath assigned)
            // ld.findPaginationURLs("http://openwetware.org/wiki/OpenWetWare:Feature_list/Lab_notebook");
            // assertEquals("",ld.getPaginationXPath().toLowerCase());

            ld
                    .findPaginationURLs("http://www.shopping.hp.com/notebooks;HHOJSID=4w8vJp2H68wwJ4qcD2lynpJlLvfQmTHFbM132PYvGGcK2j2CcXGj!1457543426?jumpid=in_R329_prodexp/hhoslp/psg/notebooks");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.notebookshop.co.za/");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            // // concept movie
            // pagination should be found
            ld.findPaginationURLs("http://en.wikipedia.org/wiki/Lists_of_films");
            assertEquals("/html/body/div[1]/div[1]/div[1]/div[2]/div[3]/table[1]/tr/td/b/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://movies.yahoo.com/browse");
            assertEquals("/html/body/center/table[7]/tr/td/table[2]/tr/td/table[3]/tr/td/font/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.imdb.com/Sections/Years/2008/");
            assertEquals("/html/body/div/div[2]/table[1]/tr/td/p/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.hollywoodteenmovies.com/A%20To%20Z%20Movie%20List.html");
            assertEquals("/html/body/table[1]/tr/td/table[1]/tr/td/div[1]/p/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.blu-ray.com/movies/movies.php?genre=action&page=1");
            assertEquals("/html/body/center[3]/table[1]/tr/td/table[22]/tr/td/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.comingsoon.net/database.php");
            assertEquals("/html/body/div[2]/table[1]/tr/td/table[1]/tr/td/div[1]/p/b/font/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.actorscelebs.com/browse/e/");
            assertEquals("/html/body/div[3]/div[1]/p/a/@href", ld.getPaginationXPath().toLowerCase());

            // TODO non-capitalized letters (check xpath)
            // ld.findPaginationURLs("http://au.rottentomatoes.com/source-168/?letter=k");
            // assertEquals("/html/body/div[5]/div/div/div[3]/div[2]/div/div[2]/table/tbody/tr/td/table[1]/tbody/tr[2]/td[2]/table/tbody/tr/td[1]/a[16]",ld.getPaginationXPath().toLowerCase());

            // no pagination
            ld.findPaginationURLs("http://www.movie-list.com/");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.angelfire.com/mb/movielist/");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.john-bauer.com/movies.htm");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://bechdel.nullium.net/");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.whereincity.com/movies/bollywood/list");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.nytimes.com/ref/movies/1000best.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            // // concept actor
            // pagination should be found
            ld.findPaginationURLs("http://www.storycasting.com/browseactors.aspx");
            assertEquals("/html/body/form[1]/div[2]/div[4]/table[1]/tr/td/div[2]/span/a/@href", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.desiscreen.com/Actors-list");
            assertEquals("/html/body/form[1]/div[3]/div[1]/table[1]/tr/td/div[1]/div[1]/table[1]/tr/td/div[1]/a/@href", ld.getPaginationXPath().toLowerCase());

            // TODO problem non-capitalized letters
            // ld.findPaginationURLs("http://www.movieprofiler.com/index.php?option=com_staticxt&staticfile=browseactors.php&Itemid=52");
            // assertEquals("/html/body/table/tbody/tr/td/table[2]/tbody/tr/td[4]/table/tbody/tr/td/table/tbody/tr/td[1]/a[2]/b/@href",ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.chakpak.com/cpl/browse");
            assertEquals(
                    "/html/body/table/tr/td/table[1]/tr/td/table[1]/tr/td/table[1]/tr/td/table[1]/tr/td/table[1]/tbody[1]/tr/td/table[1]/tbody/tr/td/table[1]/tr/td/a/@href",
                    ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.radioreloaded.com/actor/?tag=A");
            assertEquals("/html/body/div[1]/div[5]/div[2]/ul[1]/li/a/@href", ld.getPaginationXPath().toLowerCase());

            // no pagination
            ld.findPaginationURLs("http://www.kaputz.com/actors-list.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://ondemandvideo.tlavideo.com/andrane/index.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.bestprices.com/cgi-bin/vlink/dvd_person?p_id=P%20%20%20%2055134&id=nsession");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://actor.tlavideo.com/jasojax/index.html");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            ld.findPaginationURLs("http://www.globaldust.com/moviestv/detail.php?id=1976");
            assertEquals("", ld.getPaginationXPath().toLowerCase());

            // // concept book author
            // TODO only partly found, non-uppercase letters
            // ld.findPaginationURLs("http://www.gocreate.com/books/authorindex.htm");
            // assertEquals("/html/body/table[2]/tr/td/center[5]/table[1]/tr/td/center/div[1]/table[1]/tr/td/b/a/@href",ld.getPaginationXPath().toLowerCase());

        }

    }

    public static void main(String[] a) {
        ListDiscoverer ld = new ListDiscoverer();

        ld.findPaginationURLs("http://www.softwaretipsandtricks.com/virus/");
        System.out.println(ld.getPaginationXPath().toLowerCase());
        assertEquals("/html/body/div[1]/table[2]/tr/td/table[1]/tr/td/table[1]/tr/td/table[1]/tr/td/font[1]/b/a/@href", ld.getPaginationXPath().toLowerCase());

    }
}