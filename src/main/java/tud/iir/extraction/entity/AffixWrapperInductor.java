package tud.iir.extraction.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.knowledge.Source;
import tud.iir.web.Crawler;

/**
 * Algorithm close to the one explained in the paper "Language-Independent Set Expansion of Named Entities using the Web". See also <a
 * href="http://www.rcwang.com/seal/">http://www.rcwang.com/seal/</a>.
 * 
 * @author David Urbansky
 */
class AffixWrapperInductor extends WrapperInductor implements WrapperInductorInterface {

    /** every seed entity has a number of pre- and suffixes */
    private HashMap<String, ArrayList<AffixWrapper>> seedWrappers;

    /** wrappers that all seed wrappers have in common */
    private WrapperSet<AffixWrapper> wrappers;

    /** limit maximum length of pre- and suffix */
    private int maxWrapperLength = 200;

    /** a set of current extractions, do not extract one entity twice or more from one site */
    private HashSet<String> currentExtractions;

    /** the domain of the site that is used for extraction */
    private String currentDomain = "";

    public AffixWrapperInductor(EntityExtractor entityExtractor) {
        ee = entityExtractor;
        currentExtractions = new HashSet<String>();
        seedWrappers = new HashMap<String, ArrayList<AffixWrapper>>();
        wrappers = new WrapperSet<AffixWrapper>();
    }

    /**
     * Find all seeds and construct their pre- and suffixes.
     * 
     * @param seeds An array of seeds.
     * @param page The content of a page.
     * @param isURL If true, the page is a URL and will be downloaded first.
     */
    public void contructWrappers(String[] seeds, String page, boolean isURL) {

        if (isURL) {
            Crawler crawler = new Crawler();
            page = crawler.download(page);
        }

        // 1. find seeds and create pre and suffix
        for (int i = 0; i < seeds.length; i++) {
            String seed = seeds[i];

            if (StringHelper.trim(seed).length() < 3)
                continue;

            ArrayList<AffixWrapper> currentSeedWrappers = new ArrayList<AffixWrapper>();

            // find all occurences of the seed in the page
            Pattern pat = Pattern.compile(StringHelper.escapeForRegularExpression(seed));
            Matcher m = pat.matcher(page);

            // System.out.println(pat.matcher(a).find());
            boolean seedFound = false;
            int maxWrappers = 100;
            int w = 0;
            while (m.find()) {
                System.out.println(m.start() + " " + m.group() + " " + m.end());

                int startIndex = Math.max(0, m.start() - getMaxWrapperLength());
                int endIndex = Math.min(page.length(), m.end() + getMaxWrapperLength());

                // pre- and suffix
                AffixWrapper wrapper = new AffixWrapper(page.substring(startIndex, m.start()), page.substring(m.end(), endIndex));
                currentSeedWrappers.add(wrapper);
                seedFound = true;
                w++;
                if (w >= maxWrappers) {
                    break;
                }
            }

            if (seedFound) {
                seedWrappers.put(seed, currentSeedWrappers);
            }
        }

        // 2. find longest pre and suffix by comparing all candidates from one seed to all from all other seeds
        Iterator<Map.Entry<String, ArrayList<AffixWrapper>>> wrappersIterator = seedWrappers.entrySet().iterator();
        while (wrappersIterator.hasNext()) {
            Map.Entry<String, ArrayList<AffixWrapper>> entry = wrappersIterator.next();

            // check all wrapper candidates for current seed with other seeds
            Iterator<Map.Entry<String, ArrayList<AffixWrapper>>> wrappersIterator2 = seedWrappers.entrySet().iterator();
            while (wrappersIterator2.hasNext()) {
                Map.Entry<String, ArrayList<AffixWrapper>> entry2 = wrappersIterator2.next();

                // do not compare with current entry itself
                if (entry2.getKey().equals(entry.getKey())) {
                    continue;
                }

                // iterate through all candidates found
                List<AffixWrapper> wrapperCandidates1 = entry.getValue(); // candidates for current entry
                List<AffixWrapper> wrapperCandidates2 = entry2.getValue(); // candidates for another entry to compare

                for (int i = 0; i < wrapperCandidates1.size(); i++) {
                    AffixWrapper wrapperCandidate1 = wrapperCandidates1.get(i);

                    int maximumPrefixLength = wrapperCandidate1.getPrefixLength();
                    int maximumSuffixLength = wrapperCandidate1.getSuffixLength();

                    int wrapperCandidate2MaxPrefixLength = 0;
                    int wrapperCandidate2MaxSuffixLength = 0;

                    for (int j = 0; j < wrapperCandidates2.size(); j++) {
                        AffixWrapper wrapperCandidate2 = wrapperCandidates2.get(j);

                        int prefixLength = 0;
                        int suffixLength = 0;

                        // align prefix and find maximal number of characters that are the same
                        String prefix1 = wrapperCandidate1.getPrefix();
                        String prefix2 = wrapperCandidate2.getPrefix();
                        for (int k = 1; k <= Math.min(prefix1.length(), prefix2.length()); k++) {
                            if (prefix1.charAt(prefix1.length() - k) == prefix2.charAt(prefix2.length() - k)) {
                                prefixLength++;
                            } else {
                                break;
                            }
                        }
                        if (prefixLength > wrapperCandidate2MaxPrefixLength)
                            wrapperCandidate2MaxPrefixLength = prefixLength;

                        // align suffix and find maximal number of characters that are the same
                        String suffix1 = wrapperCandidate1.getSuffix();
                        String suffix2 = wrapperCandidate2.getSuffix();
                        for (int k = 0; k < Math.min(suffix1.length(), suffix2.length()); k++) {
                            if (suffix1.charAt(k) == suffix2.charAt(k)) {
                                suffixLength++;
                            } else {
                                break;
                            }
                        }
                        if (suffixLength > wrapperCandidate2MaxSuffixLength) {
                            wrapperCandidate2MaxSuffixLength = suffixLength;
                        }
                    }

                    if (wrapperCandidate2MaxPrefixLength < maximumPrefixLength) {
                        wrapperCandidate1.setPrefixLength(wrapperCandidate2MaxPrefixLength);
                    }
                    if (wrapperCandidate2MaxSuffixLength < maximumSuffixLength) {
                        wrapperCandidate1.setSuffixLength(wrapperCandidate2MaxSuffixLength);
                    }
                }
            }
        }

        if (seedWrappers.entrySet().isEmpty()) {
            return;
        }

        // 3. find wrappers that all seeds have in common
        Map.Entry<String, ArrayList<AffixWrapper>> entry = seedWrappers.entrySet().iterator().next(); // first seed

        ArrayList<AffixWrapper> wrapperCandidates1 = entry.getValue(); // candidates for current entry

        for (int i = 0; i < wrapperCandidates1.size(); i++) {
            AffixWrapper wrapperCandidate1 = wrapperCandidates1.get(i);
            if (!wrapperCandidate1.isEmpty()) {
                wrappers.add(wrapperCandidate1);
            }
            // check all wrapper candidates for current seed with other seeds
            /*
             * Iterator<Map.Entry<String,ArrayList<Wrapper>>> wrappersIterator2 = seedWrappers.entrySet().iterator(); while (wrappersIterator2.hasNext()) {
             * Map.Entry<String,ArrayList<Wrapper>> entry2 = wrappersIterator2.next(); // do not compare with current entry itself if
             * (entry2.getKey().equals(entry.getKey())) continue; ArrayList<Wrapper> wrapperCandidates2 = entry2.getValue(); // candidates for another entry to
             * compare // the wrapper is taken only if prefix and suffix are the same for entry and entry2 for (int j = 0; j < wrapperCandidates2.size(); j++) {
             * Wrapper wrapperCandidate2 = wrapperCandidates2.get(j); if (wrapperCandidate1.getPrefix().equals(wrapperCandidate2.getPrefix()) &&
             * wrapperCandidate1.getSuffix().equals(wrapperCandidate2.getSuffix())) { //wrappers.add(wrapperCandidate1); } } }
             */
        }

    }

    @Override
    public void extract(String url, EntityQuery eq, Concept currentConcept) {

        Crawler crawler = new Crawler();
        String page = crawler.download(url);
        // page = StringHelper.unescapeHTMLEntities(page);
        // String page = url;

        String domain = Crawler.getDomain(url, true);
        if (!domain.equalsIgnoreCase(currentDomain)) {
            currentExtractions = new HashSet<String>();
            currentDomain = domain;
        }

        // wrappers
        Logger.getLogger(EntityExtractor.class).info("awi uses the following wrappers for the website " + url + ":");

        Iterator<AffixWrapper> wrapperIterator2 = getWrappers().iterator();
        while (wrapperIterator2.hasNext()) {
            AffixWrapper entry = wrapperIterator2.next();
            ee.getLogger().info("prefix: " + entry.getPrefix());
            ee.getLogger().info("suffix: " + entry.getSuffix() + "\n");
        }

        // apply each wrapper and extract entities
        Iterator<AffixWrapper> wrapperIterator = getWrappers().iterator();
        while (wrapperIterator.hasNext()) {
            AffixWrapper wrapper = wrapperIterator.next();

            // Pattern pattern =
            // Pattern.compile(StringHelper.escapeForRegularExpression(wrapper.getPrefix())+".+?"+StringHelper.escapeForRegularExpression(wrapper.getSuffix()),Pattern.CASE_INSENSITIVE);
            Pattern pattern = Pattern.compile(StringHelper.escapeForRegularExpression(wrapper.getPrefix()) + ".+?"
                    + StringHelper.escapeForRegularExpression(wrapper.getSuffix()));
            Matcher matcher = pattern.matcher(page);
            while (matcher.find()) {
                // System.out.println("found something"+matcher.group());
                int prefixIndex = wrapper.getPrefixLength();
                // check whether there is another occurrence of the prefix
                int additionalPrefixOccurrence = matcher.group().substring(0, matcher.group().length() - wrapper.getSuffixLength()).lastIndexOf(
                        StringHelper.escapeForRegularExpression(wrapper.getPrefix()));
                if (additionalPrefixOccurrence > wrapper.getPrefixLength()) {
                    prefixIndex += additionalPrefixOccurrence;
                }
                String entityName = matcher.group().substring(prefixIndex, matcher.group().length() - wrapper.getSuffixLength());
                if (currentExtractions.add(entityName)) {
                    Entity newEntity = new Entity(entityName, currentConcept);
                    newEntity.setExtractedAt(new Date(System.currentTimeMillis()));
                    Source s = new Source(Crawler.getCleanURL(url));
                    s.setExtractionType(eq.getQueryType());
                    newEntity.addSource(s);
                    ee.addExtraction(newEntity);
                    // ee.getKnowledgeManager().getConcept(currentConcept.getName()).addEntity(newEntity);
                    // ee.getExtractions().addExtraction(newEntity, eq.getRetrievalExtractionType(), eq.getQueryType());
                }
            }
        }
    }

    public HashMap<String, ArrayList<AffixWrapper>> getSeedWrappers() {
        return seedWrappers;
    }

    public void setSeedWrappers(HashMap<String, ArrayList<AffixWrapper>> wrappers) {
        this.seedWrappers = wrappers;
    }

    public WrapperSet<AffixWrapper> getWrappers() {
        return wrappers;
    }

    public void setWrappers(WrapperSet<AffixWrapper> wrappers) {
        this.wrappers = wrappers;
    }

    public int getMaxWrapperLength() {
        return maxWrapperLength;
    }

    public void setMaxWrapperLength(int maxWrapperLength) {
        this.maxWrapperLength = maxWrapperLength;
    }

    /**
     * TODO move tests to test class
     * 
     * @param args
     */
    public static void main(String[] args) {

        KnowledgeManager km = new KnowledgeManager();
        km.addConcept(new Concept("Actor"));
        EntityExtractor.getInstance().setKnowledgeManager(km);

        AffixWrapperInductor w1 = new AffixWrapperInductor(EntityExtractor.getInstance());

        String seedURL = "...<li class=\"ford\"><a href=\"http://www.curryford.com/\"><img src=\"/common/logos/ford/logo-horiz-rgb-lg-dkbg.gif\" alt=\"3\"></a><ul><li class=\"l�st�\"><a href=\"http://www.curryford.com/\"><span class=\"dName\">Curry Ford (2008)</span>...</li></ul></li><li class=\"honda\"><a href=\"http://www.curryhonda.com\"><img src=\"/common/logos/honda/logo-horiz-rgb-lg-dkbg.gif\" alt=\"4\"></a><ul><li><a href=\"http://www.curryhonda-ga.com/\"><span class=\"dName\">Curry Honda Atlanta (2008)</span>...</li><li><a href=\"http://www.curryhondamass.com/\"><span class=\"dName\">Curry Honda (2008)</span>...</li><li class=\"l�st�\"><a href=\"http://www.curryhondany.com/\"><span class=\"dName\">Curry Honda Yorktown (2008)</span>...</li></ul></li><li class=\"acura\"><a href=\"http://www.curryacura.com/\"><img src=\"/curryautogroup/images/logo-horiz-rgb-lg-dkbg.gif\" alt=\"5\"></a><ul><li class=\"l�st�\"><a href=\"http://www.curryacura.com/\"><span class=\"dName\">Curry Acura (2008)</span>...</li></ul></li><li class=\"nissan\"><a href=\"http://www.geisauto.com/nissan/\"><img src=\"/common/logos/nissan/logo-horiz-rgb-lg-dkbg.gif\" alt=\"6\"></a><ul><li class=\"l�st�\"><a href=\"http://www.geisauto.com/nissan/\"><span class=\"dName\">Curry Nissan (2008)</span>...</li></ul></li><li class=\"toyota\"><a href=\"http://www.geisauto.com/toyota/\"><img src=\"/common/logos/toyota/logo-horiz-rgb-lg-dkbg.gif\" alt=\"7\"></a><ul><li class=\"l�st�\"><a href=\"http://www.geisauto.com/toyota/\"><span class=\"dName\">Curry Toyota (2008)</span>...</li></ul></li>...";
        // String[] seeds = {"ford","nissan","toyota"};

        // String page = crawler.download("http://en.wikipedia.org/wiki/List_of_cars");
        // String[] seeds = {"Devaux","Pretty","Solana","Forster"};

        // page = "http://www.time.com/time/specials/2007/completelist/0,,1658545,00.html";
        // String[] seeds = {"1961 Corvair","1978 AMC Pacer","2004 Chevy SSR"};

        // page = "http://www.state.gov/misc/list/";
        // String[] seeds = {"Germany","Australia","Ireland"};
        // page = "http://www.imdb.com/List?heading=7%3BAll+titles%3B2008&&tv=on&&nav=/Sections/Years/2008/include-titles&&year=2008&&skip=21600";
        // String[] seeds = {"Unusual","Upstate","ABCDEFG123"};

        seedURL = "http://knuth.luther.edu/~bmiller/CS151/Spring05/actors.dat";
        // String[] seeds = {"Roy Ward","Hugh Laurie"};

        seedURL = "http://www.rsportscars.com/sitemap.xml";
        // String[] seeds = {"Ford"};

        seedURL = "http://www.atnf.csiro.au/people/mcalabre/WCS/ccs.pdf";
        // String[] seeds = {"Earth"};

        seedURL = "http://simple.wikipedia.org/wiki/List_of_diseases";
        // String[] seeds = {"Iron-deficiency anemia","Kuru"};

        seedURL = "http://www.mic.ki.se/Diseases/alphalist.html";
        // String[] seeds = {"Hypothyroidism","Pupillary Functions, Abnormal"};

        seedURL = "http://www.nba.com/players/";
        // String[] seeds = {"Kapono, Jason","Kleiza, Linas"};

        seedURL = "http://str8hoops.com/nba-player-nicknames/";
        // String[] seeds = {"Julian Wright","Tony Parker"};

        seedURL = "http://www.iblist.com/list.php?type=book";
        // String[] seeds = {"Nothing Happens on the Moon","Tis the Season"};

        seedURL = "http://books.mirror.org/gb.titles.html";
        // String[] seeds = {"Lycidas","1984"};

        seedURL = "http://www.abc.net.au/myfavouritebook/top10/100.htm";
        // String[] seeds = {"Harry Potter And The Chamber Of Secrets (Book 2)","Midnight's Children"};

        seedURL = "http://www.astro.wisc.edu/~dolan/constellations/starname_list.html";
        // String[] seeds = {"Al Minliar al Asad","ALTAIR"};

        seedURL = "http://www.atlasoftheuniverse.com/nearstar.html";
        // String[] seeds = {"LP 656-38","Lalande 27173"};

        seedURL = "http://en.wikipedia.org/wiki/List_of_Presidents_of_the_United_States";
        // String[] seeds = {"Abraham Lincoln", "Richard Nixon"};

        seedURL = "http://www.ipl.org/div/potus/";
        // String[] seeds = {"Abraham Lincoln", "Richard Milhous Nixon"};

        seedURL = "http://www.essortment.com/all/listpresidents_rhgd.htm";
        // String[] seeds = {"Benjamin Harrison","Zachary Taylor"};

        seedURL = "http://www.forumancientcoins.com/antonines/Liste.htm";
        // String[] seeds = {"RIC 7","Alexander III, AR_Zeus"};

        seedURL = "http://en.wikipedia.org/wiki/List_of_currency_units";
        // String[] seeds = {"Centavo","Kwacha"};

        seedURL = "http://www.answers.com/topic/list-of-rivers-by-length";
        // String[] seeds = {"Sankuru","Khatanga"};

        seedURL = "http://en.citizendium.org/wiki/List_of_rivers_by_length";
        // String[] seeds = {"Niger","Zambezi"};

        seedURL = "http://www.iitk.ac.in/counsel/hotels.htm";
        // String[] seeds = {"Geet","Madhubandan"};

        seedURL = "http://www.stanford.edu/dept/rde/chs/general/hotel.html";
        // String[] seeds = {"Menlo Park Inn","Quality Inn"};

        seedURL = "http://www.knowledgerush.com/kr/encyclopedia/List_of_lakes/";
        // String[] seeds = {"Lake Gairdner","Lake Winnipesaukee"};

        seedURL = "http://en.wikipedia.org/wiki/List_of_lakes";
        // String[] seeds = {"Lake Lessing","Lake Champlain"};

        seedURL = "http://www.birddiary.co.uk/birdalphabet.asp?ltr=a";
        // String[] seeds = {"Aquatic Warbler","Arctic Warbler"};

        seedURL = "http://www.stanford.edu/~petelat1/birdlist.html";
        // String[] seeds = {"Bushtit","Palila"};

        seedURL = "http://www.fantasycars.com/derek/list.html";
        // String[] seeds = {"'97 Bentley Continental T","'97 Eagle Talon TSi AWD"};

        seedURL = "http://www.racv.com.au/wps/wcm/connect/Internet/Primary/my+car/advice+%26+information/compare+cars/list+of+all+cars/";
        // String[] seeds = {"Honda Concerto","Lexus GS"};

        seedURL = "http://www.time.com/time/specials/2007/completelist/0,,1658545,00.html";
        // String[] seeds = {"1911 Overland OctoAuto","1957 Waterman Aerobile"};

        seedURL = "http://www.theodora.com/wfb/abc_world_fact_book.html";
        // String[] seeds = {"Liberia","Italy"};

        seedURL = "http://www.austlii.edu.au/catalog/215.html";
        // String[] seeds = {"Liberia","Italy"};

        // seedURL = "http://en.wikipedia.org/wiki/List_of_countries";
        // String[] seeds = {"Liberia","Italy"};

        // seedURL = "http://www.cellular-news.com/car_bans/";
        // String[] seeds = {"Malaysia","Italy"};

        seedURL = "http://www.imdb.com/top_250_films";
        // String[] seeds = {"Psycho","Kill Bill: Vol. 2"};

        seedURL = "http://en.wikiquote.org/wiki/List_of_films";
        // String[] seeds = {"Nine Lives of Fritz the Cat, The","In Her Shoes"};

        // seedURL = "http://www.berbecuta.com/2008/03/14/1001-movie-you-must-see-before-you-die/";
        // String[] seeds = {"The Mad Masters","Casablanca"};

        // seedURL = "http://www.john-bauer.com/movies.htm";
        // String[] seeds = {"Ferris Bueller's Day Off","The Godfather"};

        seedURL = "http://www.esato.com/phones/";
        // String[] seeds = {"W302","K810"};

        seedURL = "http://en.wikipedia.org/wiki/List_of_best-selling_mobile_phones";
        // String[] seeds = {"N-Gage","Palm Centro"};

        // seedURL = "http://www.expansys.com/n.aspx?c=169";
        // String[] seeds = {"Toshiba Portege G710 Value Pack","Samsung i780 Deluxe Pack"};

        // seedURL =
        // "http://www.phone-critic.co.uk/phone.php?action=Browse&OrderBy=&Sort=&QuickSearch=&SearchResults=&MinPrice=&MaxPrice=&PhoneType=1&PhoneBrand=&Features1=&Features2=&Features3=&Features4=&Sort=&OrderBy=&Offset=112";
        // String[] seeds = {"LG B2100","Sony Ericsson W350a"};

        seedURL = "http://danstechnstuff.com/2008/08/08/dell-adds-to-list-of-notebooks-with-nvidia-gpu-problems/";
        // String[] seeds = {"Latitude D630","Latitude D630c"};

        seedURL = "http://asia.cnet.com/reviews/notebooks/0,39050495,39315110,00.htm";
        // String[] seeds = {"Toshiba Satellite L20-C430","Dell Inspiron 630m"};

        // seedURL = "http://www.superwarehouse.com/Notebooks/c2b/3081";
        // String[] seeds = {"HP 2133 Notebook - KR922UT","Acer Aspire ONE A110-1295"};

        // seedURL = "http://www.amazon.co.uk/Laptops-Notebooks-PCs/b?ie=UTF8&node=14014431";
        // String[] seeds = {"Acer Aspire 5315 15.4-inch Laptop","Apple MacBook White"};

        seedURL = "http://en.wikipedia.org/wiki/List_of_French_actors";
        // String[] seeds = {"Melanie Doutey","Jacques Villeret"};

        seedURL = "http://www.atpictures.com/index.php%3Fview%3Dall";
        // String[] seeds = {"Alfred Molina","Rob Schneider"};

        // seedURL = "http://www.djmick.co.uk/actors_pictures.htm";
        // String[] seeds = {"John Travolta","Matt LeBlanc"};

        // seedURL = "http://koreanfilm.org/actors.html";
        // String[] seeds = {"Kang Su-yeon","Jang Dong-gun"};

        seedURL = "http://www.olympic.org/uk/sports/index_uk.asp";
        // String[] seeds = {"Fencing","Weightlifting"};

        seedURL = "http://en.wikipedia.org/wiki/List_of_sports";
        // String[] seeds = {"Wolf hunting","Race walking"};

        // seedURL = "http://www.ugsport.co.uk/sport-list-of-sports.asp";
        // String[] seeds = {"Archery","Golf"};

        // seedURL = "http://dir.yahoo.com/recreation/Sports/";
        // String[] seeds = {"Mountainboarding","Pickleball"};

        seedURL = "http://library.music.indiana.edu/music_resources/singers.html";
        // String[] seeds = {"Cecilia Bartoli","Barbara Bonney"};

        seedURL = "http://www.nationmaster.com/encyclopedia/List-of-folk-musicians";
        // String[] seeds = {"Kudsi Erguner","The Roches"};

        // seedURL = "http://www.taosmusic.com/content/category/4/56/44/";
        // String[] seeds = {"Taos Chamber Music Group","Adrienne Braswell"};

        // seedURL = "http://www.smashbros.com/en_us/music/music01.html";
        // String[] seeds = {"Yasunori Mitsuda","Motoi Sakuraba"};

        seedURL = "http://www.hitsdailydouble.com/news/songs.html";
        // String[] seeds = {"Johnny Angel","I'm On Fire"};

        // seedURL = "http://www.npr.org/programs/specials/vote/300list.html";
        // String[] seeds = {"Ebony Concerto","The Yankee Doodle Boy"};

        seedURL = "http://www.song-list.com/90/90A.html";
        // String[] seeds = {"All The Young Dudes","Are You Gonna Go My Way"};

        // seedURL = "http://en.wikipedia.org/wiki/List_of_songs_in_Guitar_Hero_III";
        // String[] seeds = {"Minus Celsius","She Builds Quick Machines"};

        // seedURL = "http://www.rockhall.com/exhibithighlights/500-songs/";
        // String[] seeds = {"Plynth","Time"};

        seedURL = "http://www.gbet.com/AtoZ_cities/";
        // String[] seeds = {"Dundee","Hereford"};

        seedURL = "http://www.mayorsforpeace.org/english/topic/list/16.htm";
        // String[] seeds = {"Paterno","Madrid"};

        // seedURL = "http://www.earthhour.org/cities";
        // String[] seeds = {"Bangkok","Suva and Lautoka"};

        // seedURL = "http://www.city-data.com/top2/c544.html";
        // String[] seeds = {"San Jose","Stockton"};

        // seedURL = "http://www.smashbros.com/en_us/music/music01.html";
        // String[] seeds = {"Yasunori Mitsuda","Motoi Sakuraba"};

        seedURL = "http://books.mirror.org/gb.titles.html";
        String[] seeds = { "Lycidas", "1984" };

        w1.contructWrappers(seeds, seedURL, true);

        // show constructed wrappers by seed
        Iterator<Map.Entry<String, ArrayList<AffixWrapper>>> wrappersIterator = w1.getSeedWrappers().entrySet().iterator();
        while (wrappersIterator.hasNext()) {
            Map.Entry<String, ArrayList<AffixWrapper>> entry = wrappersIterator.next();

            System.out.println("Wrappers for seed " + entry.getKey() + ":");
            Iterator<AffixWrapper> seedWrapperIterator = entry.getValue().iterator();
            while (seedWrapperIterator.hasNext()) {
                AffixWrapper seedWrapper = seedWrapperIterator.next();
                System.out.println("prefix: " + seedWrapper.getPrefix(false));
                System.out.println("suffix: " + seedWrapper.getSuffix(false) + "\n");
            }

        }

        // wrappers
        System.out.println("Wrappers:");

        // w1.getWrappers().removeShortWrappers();
        w1.getWrappers().removeSubWrappers();
        Iterator<AffixWrapper> wrapperIterator2 = w1.getWrappers().iterator();
        while (wrapperIterator2.hasNext()) {
            AffixWrapper entry = wrapperIterator2.next();

            System.out.println("prefix: " + entry.getPrefix());
            System.out.println("suffix: " + entry.getSuffix() + "\n");

        }

        // apply wrappers
        w1.extract(seedURL, new EntityQuery(), km.getConcept("Actor"));

        // show extractions
        System.out.println("Extractions:");
        CollectionHelper.print(w1.getExtractions());

        // Wrapper wr1 = new Wrapper("/","/");
        // Wrapper wr2 = new Wrapper("/","/");
        //		
        // System.out.println("equal: "+wr1.equals(wr2));
    }
}