package tud.iir.extraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import junit.framework.TestCase;
import tud.iir.extraction.fact.FactExtractionDecisionTree;
import tud.iir.extraction.fact.FactExtractor;
import tud.iir.extraction.fact.FactString;
import tud.iir.extraction.fact.LiveFactExtractor;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Fact;

/**
 * Test cases for fact extraction.
 * 
 * @author David Urbansky
 */
public class FactExtractionTest extends TestCase {

    public FactExtractionTest(String name) {
        super(name);
    }

    // TODO make faultless!
    public void testFactExtraction() {
        Concept concept = new Concept("test Concept");
        Entity e = new Entity("test entity", concept);

        ExtractionProcessManager.setBenchmarkSet(ExtractionProcessManager.YAHOO_8);
        FactExtractionDecisionTree dt = new FactExtractionDecisionTree(e, "data/test/webPages/website1753.html");

        Attribute currentAttribute = new Attribute("processor speed", Attribute.VALUE_NUMERIC, concept);
        dt.setAttribute(currentAttribute);
        HashMap<Attribute, ArrayList<FactString>> factStrings = dt.getFactStrings(currentAttribute);

        // // extract the fact values from each of the strings for the current attribute
        // Iterator<Map.Entry<Attribute,ArrayList<FactString>>> factIterator = factStrings.entrySet().iterator();
        // while (factIterator.hasNext()) {
        // Map.Entry<Attribute,ArrayList<FactString>> currentEntry = factIterator.next();
        // System.out.println("\n"+currentEntry.getKey().getName()+" ("+currentEntry.getKey().getValueTypeName()+")");
        // CollectionHelper.print(currentEntry.getValue());
        // }

        assertEquals(30, factStrings.size());

        currentAttribute = new Attribute("capital", Attribute.VALUE_STRING, concept);
        dt.setDocument("data/test/webPages/website328.html");
        dt.setAttribute(currentAttribute);
        factStrings = dt.getFactStrings(currentAttribute);

        assertEquals(11, factStrings.size());

        currentAttribute = new Attribute("redline", Attribute.VALUE_NUMERIC, concept);
        dt.setDocument("data/test/webPages/website1212.html");
        dt.setAttribute(currentAttribute);
        factStrings = dt.getFactStrings(currentAttribute);

        assertEquals(52, factStrings.size());

        currentAttribute = new Attribute("torque", Attribute.VALUE_NUMERIC, concept);
        dt.setDocument("data/test/webPages/website1213.html");
        dt.setAttribute(currentAttribute);
        factStrings = dt.getFactStrings(currentAttribute);

        assertEquals(17, factStrings.size());

        currentAttribute = new Attribute("weight", Attribute.VALUE_NUMERIC, concept);
        dt.setDocument("data/test/webPages/website1215.html");
        dt.setAttribute(currentAttribute);
        factStrings = dt.getFactStrings(currentAttribute);

        // TODO 4 would be better
        assertEquals(3, factStrings.size());

        currentAttribute = new Attribute("director", Attribute.VALUE_STRING, concept);
        dt.setDocument("data/test/webPages/website2082.html");
        dt.setAttribute(currentAttribute);
        factStrings = dt.getFactStrings(currentAttribute);

        // TODO assertEquals(5, factStrings.size());

        currentAttribute = new Attribute("genre", Attribute.VALUE_STRING, concept);
        dt.setDocument("data/test/webPages/website2086.html");
        dt.setAttribute(currentAttribute);
        factStrings = dt.getFactStrings(currentAttribute);

        // TODO assertEquals(5, factStrings.size());

        currentAttribute = new Attribute("exterior height", Attribute.VALUE_NUMERIC, concept);
        dt.setDocument("data/test/webPages/website1282.html");
        dt.setAttribute(currentAttribute);
        factStrings = dt.getFactStrings(currentAttribute);

        // TODO assertEquals(5, factStrings.size());
    }

    public void testDetectTableFacts() {
        String url = "";
        Concept c = new Concept("TestConcept");
        HashSet<Attribute> seedAttributes = null;
        ArrayList<Fact> detectedFacts = null;

        // nokia n95 mobile phone
        url = "data/test/webPages/detectTableFacts1.html";
        seedAttributes = new HashSet<Attribute>();
        seedAttributes.add(new Attribute("Camera", Attribute.VALUE_STRING, c));
        seedAttributes.add(new Attribute("Memory card", Attribute.VALUE_STRING, c));
        seedAttributes.add(new Attribute("Form factor", Attribute.VALUE_STRING, c));
        detectedFacts = FactExtractor.extractFacts(url, seedAttributes);

        assertEquals(44, detectedFacts.size());

        // 2010 baby bugatti car
        url = "data/test/webPages/detectTableFacts2.html";
        seedAttributes = new HashSet<Attribute>();
        seedAttributes.add(new Attribute("displacement", Attribute.VALUE_NUMERIC, c));
        seedAttributes.add(new Attribute("curb weight", Attribute.VALUE_NUMERIC, c));
        detectedFacts = FactExtractor.extractFacts(url, seedAttributes);

        assertEquals(6, detectedFacts.size());

        // jim carrey person
        url = "data/test/webPages/detectTableFacts3.html";
        seedAttributes = new HashSet<Attribute>();
        seedAttributes.add(new Attribute("height", Attribute.VALUE_NUMERIC, c));
        seedAttributes.add(new Attribute("birth name", Attribute.VALUE_STRING, c));
        detectedFacts = FactExtractor.extractFacts(url, seedAttributes);

        assertEquals(18, detectedFacts.size());
    }

    /**
     * Live fact extraction does not use any domain knowledge.
     */
    public void testLiveFactExtraction() {

        LiveFactExtractor lfe = new LiveFactExtractor("unknown");

        assertEquals(20, lfe.extractFacts("data/test/liveFactExtraction/website1.html").size());
        assertEquals(14, lfe.extractFacts("data/test/liveFactExtraction/website2.html").size());
        assertEquals(11, lfe.extractFacts("data/test/liveFactExtraction/website3.html").size());
        assertEquals(27, lfe.extractFacts("data/test/liveFactExtraction/website4.html").size());
        assertEquals(14, lfe.extractFacts("data/test/liveFactExtraction/website5.html").size());
        assertEquals(17, lfe.extractFacts("data/test/liveFactExtraction/website6.html").size());
        assertEquals(22, lfe.extractFacts("data/test/liveFactExtraction/website7.html").size());
        assertEquals(5, lfe.extractFacts("data/test/liveFactExtraction/website8.html").size());
        assertEquals(45, lfe.extractFacts("data/test/liveFactExtraction/website9.html").size());
        assertEquals(7, lfe.extractFacts("data/test/liveFactExtraction/website10.html").size());

    }
}