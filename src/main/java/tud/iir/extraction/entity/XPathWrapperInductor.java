package tud.iir.extraction.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tud.iir.classification.Term;
import tud.iir.extraction.PageAnalyzer;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.StringHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.knowledge.Source;
import tud.iir.web.Crawler;

/**
 * The XPathWrapperInductor can find more instances on a web page using a few seeds. It compares the XPaths to the seed instances and generalizes the Xaths. The
 * generalized XPaths are then used to find and extract more instances.
 * 
 * @author David Urbansky
 */
class XPathWrapperInductor extends WrapperInductor implements WrapperInductorInterface {

    /** the XPath that points to (all) entity mentions on the given page */
    private String entityXPath = "";

    /** a set of current extractions, do not extract one entity twice or more from one site */
    private HashSet<String> currentExtractions;

    /** the domain of the site that is used for extraction */
    private String currentDomain = "";

    public XPathWrapperInductor(EntityExtractor entityExtractor) {
        ee = entityExtractor;
        currentExtractions = new HashSet<String>();
    }

    public void extractWithSeeds(String url, EntityQuery eq, Concept currentConcept, String[] seeds) {
        Crawler crawler = new Crawler();
        Document document = crawler.getWebDocument(url);

        if (document == null) {
            return;
        }

        PageAnalyzer pa = new PageAnalyzer();

        WrapperSet<XPathAffixWrapper> wrapperSet = new WrapperSet<XPathAffixWrapper>();

        HashSet<String> xPathSet = new HashSet<String>();
        for (int i = 0; i < seeds.length; i++) {
            HashSet<String> seedXPathSet = pa.constructAllXPaths(document, seeds[i], false, true);
            xPathSet.addAll(seedXPathSet);

            Iterator<String> seedXPathIterator = seedXPathSet.iterator();
            while (seedXPathIterator.hasNext()) {
                String seedXPath = seedXPathIterator.next();
                List<Node> nodeList = XPathHelper.getNodes(document, seedXPath);

                if (nodeList == null) {
                    return;
                }

                for (int j = 0; j < nodeList.size(); j++) {
                    Node node = nodeList.get(j);
                    String nodeText = node.getTextContent();
                    nodeText = StringHelper.removeBrackets(nodeText);
                    nodeText = nodeText.replaceAll("  ", " ");
                    String seed = StringHelper.trim(StringHelper.removeBrackets(seeds[i]));
                    int seedStartIndex = nodeText.indexOf(seed);
                    if (seedStartIndex < 0) {
                        seedStartIndex = 0;
                    }
                    int seedEndIndex = seedStartIndex + seed.length();
                    String prefix = nodeText.substring(Math.max(0, seedStartIndex - 2), seedStartIndex);
                    String suffix = "";
                    if (seedEndIndex <= Math.min(nodeText.length(), seedEndIndex + 2)) {
                        suffix = nodeText.substring(seedEndIndex, Math.min(nodeText.length(), seedEndIndex + 2));
                    }
                    XPathAffixWrapper xpaw = new XPathAffixWrapper(prefix, suffix, seedXPath);
                    wrapperSet.add(xpaw);
                }
            }
        }

        String commonPrefix = "";
        String commonSuffix = "";
        int i = 0;
        Iterator<AffixWrapper> affixIterator = wrapperSet.iterator();
        while (affixIterator.hasNext()) {
            XPathAffixWrapper xpaw = (XPathAffixWrapper) affixIterator.next();
            if (i == 0) {
                commonPrefix = xpaw.getPrefix();
                commonSuffix = xpaw.getSuffix();
            } else {
                int j = 0;
                for (j = 0; j < Math.min(commonPrefix.length(), xpaw.getPrefixLength()); j++) {
                    if (commonPrefix.charAt(j) != xpaw.getPrefix().charAt(j)) {
                        break;
                    }
                }
                commonPrefix = commonPrefix.substring(0, j);
                for (j = 0; j < Math.min(commonSuffix.length(), xpaw.getSuffixLength()); j++) {
                    if (commonSuffix.charAt(j) != xpaw.getSuffix().charAt(j)) {
                        break;
                    }
                }
                commonSuffix = commonSuffix.substring(0, j);
            }
            i++;
        }

        String domain = Crawler.getDomain(url, true);
        if (!domain.equalsIgnoreCase(currentDomain)) {
            currentExtractions = new HashSet<String>();
            currentDomain = domain;
        }

        // only remove indices when there are differences for the entities given
        String mutualXPath = pa.makeMutualXPath(xPathSet);
        if (mutualXPath.length() == 0) {
            Logger.getLogger(EntityExtractor.class).info("no mutual xpath was found at " + url + " for the seeds " + StringHelper.getArrayAsString(seeds));
            return;
        } else if (mutualXPath.split("/").length > 40) {
            Logger.getLogger(EntityExtractor.class).info("mutual xpath failed sanity test at" + url + " for the seeds " + StringHelper.getArrayAsString(seeds));
            Logger.getLogger(EntityExtractor.class).info("mutual xpath: " + mutualXPath);
            return;
        }

        Logger.getLogger(EntityExtractor.class).info(
                "xwi with seeds (" + StringHelper.getArrayAsString(seeds) + ") and affixes (" + commonPrefix + "," + commonSuffix + ") uses entity xpath: "
                        + mutualXPath + " for website: " + document.getDocumentURI());

        ArrayList<String> entityCandidates = new ArrayList<String>();

        List<Node> nodeList = XPathHelper.getNodes(document, mutualXPath);

        // if nodeList is null, no mutual xPath was found, that is, there is probably no uniform list on the page
        // and no extraction should be performed
        if (nodeList == null) {
            return;
        }

        for (i = 0; i < nodeList.size(); i++) {
            Node node = nodeList.get(i);
            String entityName = node.getTextContent();

            if (commonPrefix.length() > 0 || commonSuffix.length() > 0) {
                if (commonPrefix.length() == 0)
                    commonPrefix = "^";
                if (commonSuffix.length() == 1 && commonSuffix.charAt(0) == ' ') {
                    commonSuffix = "$";
                }
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?<=" + StringHelper.escapeForRegularExpression(commonPrefix) + ").+?(?="
                        + StringHelper.escapeForRegularExpression(commonSuffix) + ")");
                Matcher m = pattern.matcher(entityName);
                if (m.find()) {
                    entityName = m.group();
                }
            }

            entityCandidates.add(entityName);
        }

        // check if list is uniform
        if (ListDiscoverer.entriesUniform(entityCandidates, false)) {
            Iterator<String> entityIterator = entityCandidates.iterator();
            while (entityIterator.hasNext()) {
                String entityName = entityIterator.next();
                if (currentExtractions.add(entityName)) {
                    Entity newEntity = new Entity(entityName, currentConcept);
                    newEntity.setExtractedAt(new Date(System.currentTimeMillis()));
                    Source s = new Source(Crawler.getCleanURL(document.getDocumentURI()));
                    s.setExtractionType(eq.getQueryType());
                    newEntity.addSource(s);
                    ee.addExtraction(newEntity);
                }
            }
        }

        // extractEntities(document, eq, currentConcept, mutualXPath);

        // Iterator<String> xPathIterator = xPathSet.iterator();
        // while (xPathIterator.hasNext()) {
        // String xpath = xPathIterator.next();
        // // TODO find similarity of all xpath and delimit removing of indices
        // extractEntities(document, eq, currentConcept, xpath);
        // }
    }

    @Override
    public void extract(String url, EntityQuery eq, Concept currentConcept) {
        Crawler crawler = new Crawler();
        String domain = Crawler.getDomain(url, true);
        if (!domain.equalsIgnoreCase(currentDomain)) {
            currentExtractions = new HashSet<String>();
            currentDomain = domain;
        }
        Document document = crawler.getWebDocument(url);
        if (document == null) {
            return;
        }

        extractEntities(document, eq, currentConcept, getEntityXPath());
    }

    public void extract(Document document, EntityQuery eq, Concept currentConcept) {
        String domain = Crawler.getDomain(document.getDocumentURI(), true);
        if (!domain.equalsIgnoreCase(currentDomain)) {
            currentExtractions = new HashSet<String>();
            currentDomain = domain;
        }
        extractEntities(document, eq, currentConcept, getEntityXPath());
    }

    private void extractEntities(Document document, EntityQuery eq, Concept currentConcept, String entityXPath) {

        if (document == null) {
            return;
        }

        Logger.getLogger(EntityExtractor.class).info("xwi uses entity xpath: " + entityXPath + " for website: " + document.getDocumentURI());

        ArrayList<String> entityCandidates = new ArrayList<String>();

        List<Node> nodeList = XPathHelper.getNodes(document, entityXPath);

        // if nodeList is null, no mutual xPath was found, that is, there is probably no uniform list on the page
        // and no extraction should be performed
        if (nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.size(); i++) {
            Node node = nodeList.get(i);
            String entityName = node.getTextContent();
            entityCandidates.add(entityName);
        }

        // check if list is uniform
        if (ListDiscoverer.entriesUniform(entityCandidates, false)) {

            Iterator<String> entityIterator = entityCandidates.iterator();
            while (entityIterator.hasNext()) {
                String entityName = entityIterator.next();
                if (currentExtractions.add(entityName)) {
                    Entity newEntity = new Entity(entityName, currentConcept);
                    newEntity.setExtractedAt(new Date(System.currentTimeMillis()));
                    Source s = new Source(Crawler.getCleanURL(document.getDocumentURI()));
                    s.setExtractionType(eq.getQueryType());
                    newEntity.addSource(s);
                    ee.addExtraction(newEntity);
                }
            }
        }
    }

    public boolean hasExtractedFromCurrentURL() {
        if (currentExtractions.isEmpty()) {
            return false;
        }
        return true;
    }

    public String getEntityXPath() {
        return entityXPath;
    }

    public void setEntityXPath(String entityXPath) {
        this.entityXPath = entityXPath;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        Term t = new Term("abc");
        Map<Term, Integer> termMap = new HashMap<Term, Integer>();
        termMap.put(t, 1);
        System.out.println(termMap.containsKey(new Term("abc")));
        System.exit(0);

        KnowledgeManager km = new KnowledgeManager();
        km.addConcept(new Concept("Actor"));
        EntityExtractor.getInstance().setKnowledgeManager(km);

        // ListDiscoverer ld = new ListDiscoverer();
        // String url = "http://www.hollywood.com/a-z_index/celebrities_C";
        // String entityXPath = ld.discoverEntityXPath(url);

        XPathWrapperInductor wi = new XPathWrapperInductor(EntityExtractor.getInstance());
        // wi.setEntityXPath(entityXPath);
        // wi.extract(url, new EntityQuery(), km.getConcept("Actor"));

        // the following commented URLs have been moved to the XPathWrapperInductorTest
        // String seedURL = "http://knuth.luther.edu/~bmiller/CS151/Spring05/actors.dat";
        // // String[] seeds = {"Roy Ward","Hugh Laurie"};
        //
        // seedURL = "http://www.rsportscars.com/sitemap.xml";
        // // String[] seeds = {"Ford"};
        //
        // seedURL = "http://www.atnf.csiro.au/people/mcalabre/WCS/ccs.pdf";
        // // String[] seeds = {"Earth"};
        //
        // seedURL = "http://simple.wikipedia.org/wiki/List_of_diseases";
        // // String[] seeds = {"Iron-deficiency anemia","Kuru"};
        //
        // seedURL = "http://www.mic.ki.se/Diseases/alphalist.html";
        // // String[] seeds = {"Adenoma, Follicular","Anoxia, Brain"};
        //
        // seedURL = "http://www.nba.com/players/";
        // // String[] seeds = {"Kapono, Jason","Kleiza, Linas"};
        //
        // seedURL = "http://str8hoops.com/nba-player-nicknames/";
        // // String[] seeds = {"Julian Wright","Tony Parker"};
        //
        // seedURL = "http://www.iblist.com/list.php?type=book";
        // // String[] seeds = {"Nothing Happens on the Moon","Tis the Season"};
        //
        // seedURL = "http://books.mirror.org/gb.titles.html";
        // // String[] seeds = {"Lycidas","1984"};
        //
        // seedURL = "http://www.abc.net.au/myfavouritebook/top10/100.htm";
        // // String[] seeds = {"Harry Potter And The Chamber Of Secrets (Book 2)","Midnight's Children"};
        //
        // seedURL = "http://www.astro.wisc.edu/~dolan/constellations/starname_list.html";
        // // String[] seeds = {"Al Minliar al Asad","ALTAIR"};
        //
        // seedURL = "http://www.atlasoftheuniverse.com/nearstar.html";
        // // String[] seeds = {"LP 656-38","Lalande 27173"};
        //
        // seedURL = "http://en.wikipedia.org/wiki/List_of_Presidents_of_the_United_States";
        // // String[] seeds = {"Abraham Lincoln", "Richard Nixon"};
        //
        // seedURL = "http://www.ipl.org/div/potus/";
        // // String[] seeds = {"Abraham Lincoln", "Richard Milhous Nixon"};
        //
        // seedURL = "http://www.essortment.com/all/listpresidents_rhgd.htm";
        // // String[] seeds = {"Benjamin Harrison","Zachary Taylor"};
        //
        // seedURL = "http://www.forumancientcoins.com/antonines/Liste.htm";
        // // String[] seeds = {"RIC 7","Alexander III, AR_Zeus"};
        //
        // seedURL = "http://en.wikipedia.org/wiki/List_of_currency_units";
        // // String[] seeds = {"Centavo","Kwacha"};
        //
        // seedURL = "http://www.answers.com/topic/list-of-rivers-by-length";
        // // String[] seeds = {"Sankuru","Khatanga"};
        //
        // seedURL = "http://en.citizendium.org/wiki/List_of_rivers_by_length";
        // // String[] seeds = {"Niger","Zambezi"};
        //
        // seedURL = "http://www.iitk.ac.in/counsel/hotels.htm";
        // // String[] seeds = {"Geet","Madhubandan"};
        //
        // seedURL = "http://www.stanford.edu/dept/rde/chs/general/hotel.html";
        // // String[] seeds = {"Menlo Park Inn","Quality Inn"};
        //
        // seedURL = "http://www.knowledgerush.com/kr/encyclopedia/List_of_lakes/";
        // // String[] seeds = {"Lake Gairdner","Lake Winnipesaukee"};
        //
        // seedURL = "http://en.wikipedia.org/wiki/List_of_lakes";
        // // String[] seeds = {"Lake Lessing","Lake Champlain"};
        //
        // seedURL = "http://www.birddiary.co.uk/birdalphabet.asp?ltr=a";
        // // String[] seeds = {"Aquatic Warbler","Arctic Warbler"};
        //
        // seedURL = "http://www.stanford.edu/~petelat1/birdlist.html";
        // // String[] seeds = {"Bushtit","Palila"};
        //
        // seedURL = "http://www.fantasycars.com/derek/list.html";
        // // String[] seeds = {"'97 Bentley Continental T","'97 Eagle Talon TSi AWD"};
        //
        // seedURL = "http://www.racv.com.au/wps/wcm/connect/Internet/Primary/my+car/advice+%26+information/compare+cars/list+of+all+cars/";
        // // String[] seeds = {"Honda Concerto","Lexus GS"};
        //
        // seedURL = "http://www.time.com/time/specials/2007/completelist/0,,1658545,00.html";
        // // String[] seeds = {"1911 Overland OctoAuto","1957 Waterman Aerobile"};
        //
        // seedURL = "http://www.theodora.com/wfb/abc_world_fact_book.html";
        // // String[] seeds = {"Liberia","Italy"};
        //
        // seedURL = "http://www.austlii.edu.au/catalog/215.html";
        // String[] seeds = {"Liberia","Italy"};

        String seedURL = "http://en.wikipedia.org/wiki/List_of_countries";
        // String[] seeds = {"Liberia","Italy"};

        seedURL = "http://www.cellular-news.com/car_bans/";
        // String[] seeds = {"Malaysia","Italy"};

        seedURL = "http://www.imdb.com/top_250_films";
        // String[] seeds = {"Psycho","Kill Bill: Vol. 2"};

        seedURL = "http://en.wikiquote.org/wiki/List_of_films";
        // String[] seeds = {"Nine Lives of Fritz the Cat, The","In Her Shoes"};

        seedURL = "http://www.berbecuta.com/2008/03/14/1001-movie-you-must-see-before-you-die/";
        // String[] seeds = {"The Mad Masters","Casablanca"};

        seedURL = "http://www.john-bauer.com/movies.htm";
        // String[] seeds = {"Ferris Bueller's Day Off","The Godfather"};

        seedURL = "http://www.esato.com/phones/";
        // String[] seeds = {"W302","K810"};

        seedURL = "http://en.wikipedia.org/wiki/List_of_best-selling_mobile_phones";
        // String[] seeds = {"N-Gage","Palm Centro"};

        seedURL = "http://www.expansys.com/n.aspx?c=169";
        // String[] seeds = {"Toshiba Portege G710 Value Pack","Samsung i780 Deluxe Pack"};

        seedURL = "http://www.phone-critic.co.uk/phone.php?action=Browse&OrderBy=&Sort=&QuickSearch=&SearchResults=&MinPrice=&MaxPrice=&PhoneType=1&PhoneBrand=&Features1=&Features2=&Features3=&Features4=&Sort=&OrderBy=&Offset=112";
        // String[] seeds = {"LG B2100","Sony Ericsson W350a"};

        seedURL = "http://danstechnstuff.com/2008/08/08/dell-adds-to-list-of-notebooks-with-nvidia-gpu-problems/";
        // String[] seeds = {"Latitude D630","Latitude D630c"};

        seedURL = "http://asia.cnet.com/reviews/notebooks/0,39050495,39315110,00.htm";
        // String[] seeds = {"Toshiba Satellite L20-C430","Dell Inspiron 630m"};

        seedURL = "http://www.superwarehouse.com/Notebooks/c2b/3081";
        // String[] seeds = {"HP 2133 Notebook - KR922UT","Acer Aspire ONE A110-1295"};

        seedURL = "http://www.amazon.co.uk/Laptops-Notebooks-PCs/b?ie=UTF8&node=14014431";
        // String[] seeds = {"Acer Aspire 5315 15.4-inch Laptop","Apple MacBook White"};

        seedURL = "http://en.wikipedia.org/wiki/List_of_French_actors";
        // String[] seeds = {"Melanie Doutey","Jacques Villeret"};

        seedURL = "http://www.atpictures.com/index.php%3Fview%3Dall";
        // String[] seeds = {"Alfred Molina","Rob Schneider"};

        seedURL = "http://www.djmick.co.uk/actors_pictures.htm";
        // String[] seeds = {"John Travolta","Matt LeBlanc"};

        seedURL = "http://koreanfilm.org/actors.html";
        // String[] seeds = {"Kang Su-yeon","Jang Dong-gun"};

        seedURL = "http://www.olympic.org/uk/sports/index_uk.asp";
        // String[] seeds = {"Fencing","Weightlifting"};

        seedURL = "http://en.wikipedia.org/wiki/List_of_sports";
        // String[] seeds = {"Wolf hunting","Race walking"};

        seedURL = "http://www.ugsport.co.uk/sport-list-of-sports.asp";
        // String[] seeds = {"Archery","Golf"};

        seedURL = "http://dir.yahoo.com/recreation/Sports/";
        // String[] seeds = {"Mountainboarding","Pickleball"};

        seedURL = "http://library.music.indiana.edu/music_resources/singers.html";
        // String[] seeds = {"Cecilia Bartoli","Barbara Bonney"};

        seedURL = "http://www.nationmaster.com/encyclopedia/List-of-folk-musicians";
        // String[] seeds = {"Kudsi Erguner","The Roches"};

        seedURL = "http://www.taosmusic.com/content/category/4/56/44/";
        // String[] seeds = {"Taos Chamber Music Group","Adrienne Braswell"};

        seedURL = "http://www.smashbros.com/en_us/music/music01.html";
        // String[] seeds = {"Yasunori Mitsuda","Motoi Sakuraba"};

        seedURL = "http://www.hitsdailydouble.com/news/songs.html";
        // String[] seeds = {"Johnny Angel","I'm On Fire"};

        seedURL = "http://www.npr.org/programs/specials/vote/300list.html";
        // String[] seeds = {"Ebony Concerto","The Yankee Doodle Boy"};

        seedURL = "http://www.song-list.com/90/90A.html";
        // String[] seeds = {"All The Young Dudes","Are You Gonna Go My Way"};

        seedURL = "http://en.wikipedia.org/wiki/List_of_songs_in_Guitar_Hero_III";
        // String[] seeds = {"Minus Celsius","She Builds Quick Machines"};

        seedURL = "http://www.rockhall.com/exhibithighlights/500-songs/";
        // String[] seeds = {"Plynth","Time"};

        seedURL = "http://www.gbet.com/AtoZ_cities/";
        // String[] seeds = {"Dundee","Hereford"};

        seedURL = "http://www.mayorsforpeace.org/english/topic/list/16.htm";
        String[] seeds = { "Paterno", "Madrid" };

        // seedURL = "http://www.mic.ki.se/Diseases/alphalist.html";
        // String[] seeds = { "Adenoma, Follicular", "Anoxia, Brain" };

        wi.extractWithSeeds(seedURL, new EntityQuery(), km.getConcept("Actor"), seeds);
        CollectionHelper.print(wi.getExtractions());
        System.exit(0);

        seedURL = "http://www.earthhour.org/cities";
        // String[] seeds = {"Bangkok","Suva and Lautoka"};

        seedURL = "http://www.city-data.com/top2/c544.html";
        // String[] seeds = {"San Jose","Stockton"};

        seedURL = "http://www.smashbros.com/en_us/music/music01.html";
        // String[] seeds = {"Yasunori Mitsuda","Motoi Sakuraba"};

        wi.extractWithSeeds(seedURL, new EntityQuery(), km.getConcept("Actor"), seeds);

        CollectionHelper.print(wi.getExtractions());
    }
}