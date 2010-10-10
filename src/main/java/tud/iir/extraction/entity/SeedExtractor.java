package tud.iir.extraction.entity;

import tud.iir.extraction.ExtractionType;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.web.Crawler;

/**
 * The SeedExtractor technique.
 * 
 * @author David Urbansky
 */
class SeedExtractor extends EntityExtractionTechnique {

    private EntityExtractor ee = null;
    private XPathWrapperInductor xwi = null;
    private AffixWrapperInductor awi = null;

    public SeedExtractor(EntityExtractor entityExtractor) {
        ee = entityExtractor;
        setExtractionTechnique(ExtractionType.ENTITY_SEED);
    }

    @Override
    public Integer[] getPatterns() {
        Integer[] patterns = { EntityQueryFactory.TYPE_SEED_2, EntityQueryFactory.TYPE_SEED_3, EntityQueryFactory.TYPE_SEED_4, EntityQueryFactory.TYPE_SEED_5 };
        return patterns;
    }

    @Override
    public EntityQuery getEntityQuery(Concept concept, int entityQueryType) {
        return EntityQueryFactory.getInstance().createSeedQuery(concept, entityQueryType, 20);
    }

    @Override
    public void extract(String url, EntityQuery eq, Concept concept) {

        // do not extract from the same url twice
        if (!urlProcessed.add(Crawler.getCleanURL(url))) {
            return;
        }

        // all seeds are taken, though some might not relate to the current url (TODO?)

        try {
            // TODO! focused crawl with pagination detection on seed retrieved page

            if (url.endsWith(".xml") || url.endsWith(".dat") || url.endsWith(".txt") || url.endsWith(".csv") || url.endsWith(".rss") || url.endsWith(".rss2")
                    || url.endsWith(".atom")) {
                // affix wrapper inductor
                awi = new AffixWrapperInductor(ee); // reinitialize (throw away old wrappers)
                awi.contructWrappers(eq.getSeeds(), url, true);
                awi.getWrappers().removeSubWrappers();
                awi.extract(url, eq, concept);
            } else {
                // xpath wrapper inductor
                xwi = new XPathWrapperInductor(ee); // reinitialize (throw away old wrappers)
                xwi.extractWithSeeds(url, eq, concept, eq.getSeeds());
            }
        } catch (Exception e) {
            ee.getLogger().error(url, e);
        }

    }

    @Override
    public String getName() {
        return "Seed Extraction";
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        KnowledgeManager km = new KnowledgeManager();
        Concept concept = new Concept("Mobile Phone", km);
        String url = "";
        // url = "http://www.gsmarena.com/motorola-phones-4.php"; String[] seeds = {"Aura","Z6c"};
        url = "http://www.imdb.com/List?heading=7%3BAll+titles%3B2008&&tv=on&&nav=/Sections/Years/2008/include-titles&&year=2008&&skip=21600";
        // concept.setName("Movie");
        // String[] seeds = {"Watch Out","We Have a Dream"};
        // url = "http://www.radioreloaded.com/actor/?tag=A"; concept.setName("Actor"); String[] seeds = {"Ajay Wadhavkar","Azad Khatri"}; // for awi very seed
        // dependent
        // url = "http://www.actorscelebs.com/browse/e/"; concept.setName("Actor"); String[] seeds = {"Edward Norton","Elijah Wood"};
        // url = "http://aston-martin.autoextra.com/model"; concept.setName("Car"); String[] seeds =
        // {"2006 Aston Martin Vantage ","2008 Aston Martin Vantage "};

        url = "http://de.wikipedia.org/wiki/Liste_der_Gro%C3%9Fst%C3%A4dte_in_Deutschland";
        concept.setName("City");
        String[] seeds = { "Dresden", "Erfurt" };

        EntityExtractor.getInstance().setKnowledgeManager(km);
        SeedExtractor se = new SeedExtractor(EntityExtractor.getInstance());
        EntityQuery eq = new EntityQuery();
        eq.setSeeds(seeds);
        se.extract(url, eq, concept);

        se.ee.normalizeAllEntities();
        se.ee.printExtractions();
    }
}