package tud.iir.extraction.entity;

import junit.framework.TestCase;
import tud.iir.helper.CollectionHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.KnowledgeManager;

/**
 * Test the XPathWrapperInductor.
 * 
 * @author David Urbansky
 * 
 */
public class XPathWrapperInductorTest extends TestCase {

    public XPathWrapperInductorTest(String name) {
        super(name);
    }

    /**
     * Test the Xpath Wrapper Inductor seed extraction technique. Some tests don't cover all or too many entities but they are listed here to get a notice if
     * any code that is used by the XPathWrapperInductor changes.
     */
    public void testExtractWithSeeds() {

        XPathWrapperInductor wi = new XPathWrapperInductor(EntityExtractor.getInstance());

        KnowledgeManager km = new KnowledgeManager();
        Concept testConcept = new Concept("Test");
        km.addConcept(testConcept);
        EntityExtractor.getInstance().setKnowledgeManager(km);
        EntityExtractor.getInstance().setAutoSave(false);

        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList1.html", new EntityQuery(), testConcept, new String[] { "Iron-deficiency anemia", "Kuru" });
        assertEquals(195, wi.getExtractions().size());

        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList2.html", new EntityQuery(), testConcept, new String[] { "Kapono, Jason", "Noah, Joakim" });
        assertEquals(460, wi.getExtractions().size());

        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList3.html", new EntityQuery(), testConcept, new String[] { "Nothing Happens on the Moon",
                "Tis the Season" });
        assertEquals(49, wi.getExtractions().size());

        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList4.html", new EntityQuery(), testConcept, new String[] { "Lycidas", "1984" });
        assertEquals(625, wi.getExtractions().size());

        // TODO make this work
        // testConcept.clearEntities();
        // wi.extractWithSeeds("data/test/webPages/webPageEntityList5.html", new EntityQuery(), testConcept, new String[]{"The Riders","The Magic Pudding"});
        // assertEquals(xxx, wi.getExtractions().size());

        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList6.html", new EntityQuery(), testConcept, new String[] { "Al Minliar al Asad", "ALTAIR" });
        assertEquals(293, wi.getExtractions().size());

        // TODO make this work
        // testConcept.clearEntities();
        // wi.extractWithSeeds("data/test/webPages/webPageEntityList7.html", new EntityQuery(), testConcept, new String[]{"Abraham Lincoln","Richard Nixon"});
        // assertEquals(xxx, wi.getExtractions().size());

        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList8.html", new EntityQuery(), testConcept, new String[] { "Abraham Lincoln",
                "Richard Milhous Nixon" });
        assertEquals(44, wi.getExtractions().size());

        // TODO remove numbers and dates in extractiosn
        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList9.html", new EntityQuery(), testConcept, new String[] { "Abraham Lincoln",
                "Herbert Clark Hoover" });
        assertEquals(43, wi.getExtractions().size());

        // TODO make this work
        // testConcept.clearEntities();
        // wi.extractWithSeeds("data/test/webPages/webPageEntityList10.html", new EntityQuery(), testConcept, new String[]{"RIC 7","Alexander III, AR_Zeus"});
        // assertEquals(xxx, wi.getExtractions().size());

        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList11.html", new EntityQuery(), testConcept, new String[] { "Sankuru", "Khatanga" });
        assertEquals(191, wi.getExtractions().size());

        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList12.html", new EntityQuery(), testConcept, new String[] { "Menlo Park Inn", "Quality Inn" });
        assertEquals(30, wi.getExtractions().size());

        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList13.html", new EntityQuery(), testConcept,
                new String[] { "Lake Gairdner", "Lake Winnipesaukee" });
        assertEquals(225, wi.getExtractions().size());

        // TODO 110 would be better
        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList14.html", new EntityQuery(), testConcept, new String[] { "Lake Lessing", "Lake Champlain" });
        assertEquals(115, wi.getExtractions().size());

        testConcept.clearEntities();
        wi
                .extractWithSeeds("data/test/webPages/webPageEntityList15.html", new EntityQuery(), testConcept, new String[] { "Aquatic Warbler",
                        "Arctic Warbler" });
        assertEquals(28, wi.getExtractions().size());

        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList16.html", new EntityQuery(), testConcept, new String[] { "Bushtit", "Palila" });
        assertEquals(130, wi.getExtractions().size());

        // TODO make this work
        // testConcept.clearEntities();
        // wi.extractWithSeeds("data/test/webPages/webPageEntityList17.html", new EntityQuery(), testConcept, new
        // String[]{"'97 Bentley Continental T","'97 Eagle Talon TSi AWD"});
        // assertEquals(xxx, wi.getExtractions().size());

        // TODO better would be 50
        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList18.html", new EntityQuery(), testConcept, new String[] { "1911 Overland OctoAuto",
                "1957 Waterman Aerobile" });
        assertEquals(51, wi.getExtractions().size());

        // TODO should be more (countries starting with A are missing)
        testConcept.clearEntities();
        wi.extractWithSeeds("data/test/webPages/webPageEntityList19.html", new EntityQuery(), testConcept, new String[] { "Liberia", "Italy" });
        assertEquals(278, wi.getExtractions().size());

        // TODO make this work
        // testConcept.clearEntities();
        // wi.extractWithSeeds("data/test/webPages/webPageEntityList20.html", new EntityQuery(), testConcept, new String[] { "Andorra", "Zambia" });
        // assertEquals(239, wi.getExtractions().size());

        CollectionHelper.print(wi.getExtractions());

    }
}
