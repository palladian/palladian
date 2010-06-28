package tud.iir.knowledge;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.extraction.ConceptDateComparator;
import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.extraction.ExtractionType;
import tud.iir.extraction.fact.NumericFactDistribution;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.MathHelper;
import tud.iir.persistence.DatabaseManager;
import tud.iir.persistence.PersistenceManager;

/**
 * TODO separate conceptual and instance knowledge (concept,attribute | entity,fact) The KnowledgeManager manages all
 * other knowledge units.
 * 
 * @author David Urbansky
 */
public class KnowledgeManager implements Serializable {

    /** the serial version id to serialize the KnowledgeManager */
    private static final long serialVersionUID = 2284737131182925479L;

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(KnowledgeManager.class);

    private static final double ATTRIBUTE_SYNONYM_TRUST_THRESHOLD = 0.05;
    private static final double ENTITY_TRUST_THRESHOLD = 0.9;

    /** list of concepts held by the KnowledgeManager */
    private ArrayList<Concept> concepts;

    /** keep track of which fact has been reviewed already, only run highest corroboration fact value */
    private Set<Fact> factsReviewed = new HashSet<Fact>();

    // test on facts that have not been reviewed yet

    public KnowledgeManager() {
        this.concepts = new ArrayList<Concept>();
    }

    public void serialize() {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = new FileOutputStream("data/test/domainManager.ser");
            out = new ObjectOutputStream(fos);
            out.writeObject(this);
            out.close();
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    /**
     * Add a concept to the KnowledgeManager, if a concept with the same name does not yet exist.
     * 
     * @param concept The concept to add.
     */
    public void addConcept(Concept concept) {
        if (getConcept(concept.getName()) == null) {
            concepts.add(concept);
        }
    }

    /**
     * Add a set of concepts if they do not yet exist.
     * 
     * @param concepts The set of concepts to add.
     */
    public void addConcepts(Set<Concept> concepts) {
        for (Concept concept : concepts) {
            addConcept(concept);
        }
    }

    /**
     * In the extraction loop, the status is saved. The concepts in the saved status are not necessarily the updated
     * ones from the database. We need to add all
     * entities and the lastSearched field from the extraction status concepts to the loaded ones.
     */
    public void mergeConcepts(HashSet<Concept> concepts2) {
        for (Concept concept2 : concepts2) {
            Concept concept = getConcept(concept2.getName());
            if (concept != null) {
                // TODO merge attributes concept.setAttributes(concept2.getA)
                concept.setLastSearched(concept2.getLastSearched());
                concept.setEntities(concept2.getEntities());
            }
        }
    }

    public ArrayList<Concept> getConcepts() {
        return getConcepts(false);
    }

    public ArrayList<Concept> getConcepts(boolean sortedByDate) {
        if (sortedByDate) {
            Collections.sort(concepts, new ConceptDateComparator());
        }
        return concepts;
    }

    /**
     * Get a certain concept by name.
     * 
     * @param conceptName The name of the concept.
     * @return The concept.
     */
    public Concept getConcept(String conceptName) {
        return getConcept(conceptName, true);
    }

    public Concept getConcept(String conceptName, boolean useSynonyms) {
        Iterator<Concept> cIt = this.concepts.iterator();
        while (cIt.hasNext()) {
            Concept c = cIt.next();
            if (c.getName().equalsIgnoreCase(conceptName)) {
                return c;
            }
            if (useSynonyms && c.hasSynonym(conceptName)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Get a certain concept by id.
     * 
     * @param conceptId The id of the concept.
     * @return The concept.
     */

    public Concept getConcept(int conceptId) {
        Iterator<Concept> cIt = this.concepts.iterator();
        while (cIt.hasNext()) {
            Concept c = cIt.next();
            if (c.getID() == conceptId) {
                return c;
            }
        }
        return null;
    }

    public void removeConcept(String conceptName) {
        Iterator<Concept> cIt = this.concepts.iterator();
        while (cIt.hasNext()) {
            Concept c = cIt.next();
            if (c.getName().equalsIgnoreCase(conceptName)) {
                removeConcept(c);
            }
        }
    }

    public void removeConcept(Concept concept) {
        this.concepts.remove(concept);
    }

    public void createSnippetBenchmarks() {

        Concept c;

        // products
        c = new Concept("Product", this);
        // c.addAttribute(new Attribute("Population",Attribute.VALUE_NUMERIC,c));
        c.addEntity(new Entity("Palm Pre", c, true));
        c.addEntity(new Entity("MacBook Pro", c, true));
        c.addEntity(new Entity("Bugatti Veyron 16.4", c, true));
        c.addEntity(new Entity("Volvo C30 BEV", c, true));
        c.addEntity(new Entity("The Lost Symbol", c, true));
        // this.addConcept(c);

        // places
        c = new Concept("Place", this);
        c.addEntity(new Entity("San Francisco", c, true));
        c.addEntity(new Entity("Frankfurt", c, true));
        c.addEntity(new Entity("Riesa", c, true));
        c.addEntity(new Entity("Gilroy", c, true));
        c.addEntity(new Entity("Luxora", c, true));
        // this.addConcept(c);

        // people
        c = new Concept("Person", this);
        c.addEntity(new Entity("Barack Obama", c, true));
        c.addEntity(new Entity("Amy MacDonald", c, true));
        c.addEntity(new Entity("Robin Williams", c, true));
        c.addEntity(new Entity("Bill Gates", c, true));
        c.addEntity(new Entity("Reiner Kraft", c, true));
        // this.addConcept(c);

        // organizations
        c = new Concept("Organization", this);
        c.addEntity(new Entity("Yahoo Inc", c, true));
        c.addEntity(new Entity("AT&T Inc", c, true));
        c.addEntity(new Entity("Rotary International", c, true));
        c.addEntity(new Entity("IKEA", c, true));
        c.addEntity(new Entity("Live Like a German", c, true));
        // this.addConcept(c);
    }

    public void createBenchmarkConcepts() {
        createBenchmarkConcepts(false);
    }

    public void createBenchmarkConcepts(boolean imageAttributes) {

        boolean fullSet = true;
        if (ExtractionProcessManager.getBenchmarkSetSize() == ExtractionProcessManager.BENCHMARK_HALF_SET) {
            fullSet = false;
        }

        Concept c;
        // Entity e; // sample entities

        // countries
        c = new Concept("Country", this);
        c.addAttribute(new Attribute("Population", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Capital", Attribute.VALUE_STRING, c));
        c.addAttribute(new Attribute("Largest City", Attribute.VALUE_STRING, c));
        c.addAttribute(new Attribute("Area", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("HDI", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Currency Code", Attribute.VALUE_STRING, c));
        c.addAttribute(new Attribute("Calling Code", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Population growth rate", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Birth rate", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Death rate", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Unemployment rate", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Geographic Coordinates", Attribute.VALUE_MIXED, c));
        c.addAttribute(new Attribute("Coastline", Attribute.VALUE_NUMERIC, c));
        if (imageAttributes) {
            c.addAttribute(new Attribute("flag", Attribute.VALUE_IMAGE, c));
        }
        // d.addAttribute(new Attribute("Independence",Attribute.VALUE_DATE));
        // d.addAttribute(new Attribute("Flag",Attribute.VALUE_IMAGE));
        // d.addAttribute(new Attribute("Map",Attribute.VALUE_IMAGE));
        c.addEntity(new Entity("USA", c, true));
        c.addEntity(new Entity("Australia", c, true));
        c.addEntity(new Entity("Germany", c, true));
        c.addEntity(new Entity("Brazil", c, true));
        c.addEntity(new Entity("Japan", c, true));
        if (fullSet) {
            c.addEntity(new Entity("Ireland", c, true));
            c.addEntity(new Entity("China", c, true));
            c.addEntity(new Entity("South Africa", c, true));
            c.addEntity(new Entity("Nigeria", c, true));
            c.addEntity(new Entity("Bhutan", c, true));
        }
        addConcept(c);

        // city
        // d = new Concept();
        // d.setName("City");
        // d.addAttribute(new Attribute("Population",Attribute.VALUE_NUMBER));
        // d.addAttribute(new Attribute("Area",Attribute.VALUE_NUMBER));
        // // d.addAttribute(new Attribute("Country",Attribute.VALUE_STRING));
        // d.addAttribute(new Attribute("Coordinates",Attribute.VALUE_MIXED));
        // d.addAttribute(new Attribute("Established",Attribute.VALUE_DATE));
        // d.addAttribute(new Attribute("Time zone",Attribute.VALUE_STRING));
        // // d.addAttribute(new Attribute("Images",Attribute.VALUE_IMAGE));
        // // d.addAttribute(new Attribute("Map",Attribute.VALUE_IMAGE));
        // d.addEntity(new Entity("London",d,true));
        // d.addEntity(new Entity("Tokyo",d,true));
        // // d.addEntity(new Entity("Tokyo",d,true));
        // d.addEntity(new Entity("Dresden",d,true));
        // d.addEntity(new Entity("Bruges",d,true));
        // // d.addEntity(new Entity("Tokyo",d,true));
        //		
        // this.addDomain(d);

        // car
        c = new Concept("Car", this);
        c.addAttribute(new Attribute("Curb Weight", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Acceleration", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Top Speed", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Horsepower", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Torque", Attribute.VALUE_NUMERIC, c));
        // d.addAttribute(new Attribute("Engine",Attribute.VALUE_STRING));
        if (imageAttributes) {
            c.addAttribute(new Attribute("_entity_image_", Attribute.VALUE_IMAGE, c));
        }

        c.addEntity(new Entity("2006 Bugatti Veyron 16.4", c, true));
        c.addEntity(new Entity("2008 Lamborghini Reventon", c, true));
        c.addEntity(new Entity("2009 Jaguar XF", c, true));
        c.addEntity(new Entity("2009 Tesla Roadster", c, true));
        c.addEntity(new Entity("2009 Maserati GranTurismo S", c, true));
        if (fullSet) {
            c.addEntity(new Entity("2009 Aston Martin V8 Vantage", c, true));
            c.addEntity(new Entity("2009 Ford Mustang GT", c, true));
            c.addEntity(new Entity("2008 Audi R8", c, true));
            c.addEntity(new Entity("2009 Bentley Continental Flying Spur", c, true));
            c.addEntity(new Entity("2010 Lotus Evora", c, true));
        }

        addConcept(c);

        // mobile phone
        c = new Concept("Mobile Phone", this);
        // d.addAttribute(new Attribute("Networks",Attribute.VALUE_STRING));
        c.addAttribute(new Attribute("Talk time", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Standby time", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Camera", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Dimensions", Attribute.VALUE_MIXED, c));
        c.addAttribute(new Attribute("Weight", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Internal Memory", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Display Size", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("3G", Attribute.VALUE_BOOLEAN, c));
        c.addAttribute(new Attribute("WLAN", Attribute.VALUE_BOOLEAN, c));
        c.addAttribute(new Attribute("HSCSD", Attribute.VALUE_BOOLEAN, c));
        c.addAttribute(new Attribute("Bluetooth", Attribute.VALUE_BOOLEAN, c));
        c.addAttribute(new Attribute("GPRS", Attribute.VALUE_BOOLEAN, c));
        c.addAttribute(new Attribute("Infrared", Attribute.VALUE_BOOLEAN, c));
        c.addAttribute(new Attribute("USB", Attribute.VALUE_BOOLEAN, c));
        c.addAttribute(new Attribute("Vibration", Attribute.VALUE_BOOLEAN, c));
        // d.addAttribute(new Attribute("Images",Attribute.VALUE_IMAGE));
        // d.addAttribute(new Attribute("Video",Attribute.VALUE_VIDEO));
        if (imageAttributes) {
            Attribute a1 = new Attribute("_entity_image_", Attribute.VALUE_IMAGE, c);
            a1.setValueCount(4);
            c.addAttribute(a1);
        }

        c.addEntity(new Entity("Nokia N95", c, true));
        c.addEntity(new Entity("Samsung i8510", c, true));
        c.addEntity(new Entity("Motorola W218", c, true));
        c.addEntity(new Entity("Sony Ericsson V600", c, true));
        c.addEntity(new Entity("LG CU915 Vu", c, true));
        if (fullSet) {
            c.addEntity(new Entity("O2 XDA Star", c, true));
            c.addEntity(new Entity("NEC N343i", c, true));
            c.addEntity(new Entity("HP iPAQ 610c", c, true));
            c.addEntity(new Entity("Eten glofiish X610", c, true));
            c.addEntity(new Entity("Vodafone 527", c, true));
        }
        // d.addEntity(new Entity("Sony Ericsson T700i",d,true));
        // d.addEntity(new Entity("Samsung A727",d,true));

        addConcept(c);

        // notebook
        c = new Concept("Notebook", this);
        c.addAttribute(new Attribute("RAM", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Processor Type", Attribute.VALUE_STRING, c));
        c.addAttribute(new Attribute("CPU Speed", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Hard Disk", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Front Side Bus", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Battery Life", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Display Size", Attribute.VALUE_NUMERIC, c));
        if (imageAttributes) {
            c.addAttribute(new Attribute("_entity_image_", Attribute.VALUE_IMAGE, c));
        }

        c.addEntity(new Entity("Apple MacBook Air MB003LL/A", c, true));
        c.addEntity(new Entity("ASUS Eee PC 1000H 80G", c, true));
        c.addEntity(new Entity("Sony VAIO VGN-FW139E/H", c, true));
        c.addEntity(new Entity("HP 2133-KX870AT", c, true));
        c.addEntity(new Entity("Dell Latitude E6400", c, true));
        if (fullSet) {
            c.addEntity(new Entity("Lenovo ThinkPad X300", c, true));
            c.addEntity(new Entity("Gateway P-7811FX", c, true));
            c.addEntity(new Entity("Averatec Voya 6494", c, true));
            c.addEntity(new Entity("Acer Aspire 8920-6671", c, true));
            c.addEntity(new Entity("Asus M70SA-X2", c, true));
        }

        addConcept(c);

        // movies
        c = new Concept("Movie", this);
        c.addAttribute(new Attribute("Director", Attribute.VALUE_STRING, c));
        c.addAttribute(new Attribute("Writer", Attribute.VALUE_STRING, c));
        c.addAttribute(new Attribute("Release Date", Attribute.VALUE_DATE, c));
        // d.addAttribute(new Attribute("Studio",Attribute.VALUE_STRING));
        // d.addAttribute(new Attribute("Rating",Attribute.VALUE_STRING));
        c.addAttribute(new Attribute("Genre", Attribute.VALUE_STRING, c));
        // d.addAttribute(new Attribute("Starring",Attribute.VALUE_STRING));
        c.addAttribute(new Attribute("Budget", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Runtime", Attribute.VALUE_NUMERIC, c));
        c.addAttribute(new Attribute("Aspect Ratio", Attribute.VALUE_MIXED, c));
        if (imageAttributes) {
            c.addAttribute(new Attribute("poster", Attribute.VALUE_IMAGE, c));
            Attribute a2 = new Attribute("scene", Attribute.VALUE_IMAGE, c);
            a2.setValueCount(4);
            c.addAttribute(a2);
        }

        c.addEntity(new Entity("Braveheart", c, true));
        c.addEntity(new Entity("The Dark Knight", c, true));
        c.addEntity(new Entity("Idiocracy", c, true));
        c.addEntity(new Entity("Code 46", c, true));
        c.addEntity(new Entity("Iron Man", c, true));
        if (fullSet) {
            c.addEntity(new Entity("The Descent", c, true));
            c.addEntity(new Entity("Hostel", c, true));
            c.addEntity(new Entity("Wedding Crashers", c, true));
            c.addEntity(new Entity("The Truman Show", c, true));
            c.addEntity(new Entity("Eternal Sunshine of a Spotless Mind", c, true));
        }

        addConcept(c);

        // actors
        c = new Concept("Actor", this);
        c.addAttribute(new Attribute("Birth Name", Attribute.VALUE_STRING, c));
        c.addAttribute(new Attribute("Date of Birth", Attribute.VALUE_DATE, c));
        c.addAttribute(new Attribute("Place of Birth", Attribute.VALUE_STRING, c));
        c.addAttribute(new Attribute("Height", Attribute.VALUE_NUMERIC, c));
        // d.addAttribute(new Attribute("Nationality",Attribute.VALUE_STRING));
        // d.addAttribute(new Attribute("Debut",Attribute.VALUE_STRING));
        if (imageAttributes) {
            c.addAttribute(new Attribute("_entity_image_", Attribute.VALUE_IMAGE, c));
        }

        c.addEntity(new Entity("Jim Carrey", c, true));
        c.addEntity(new Entity("Mel Gibson", c, true));
        c.addEntity(new Entity("Laura Dern", c, true));
        c.addEntity(new Entity("Monica Potter", c, true));
        c.addEntity(new Entity("Natalie Portman", c, true));
        if (fullSet) {
            c.addEntity(new Entity("Brendan Fraser", c, true));
            c.addEntity(new Entity("Nicholas Cage", c, true));
            c.addEntity(new Entity("Bruce Willis", c, true));
            c.addEntity(new Entity("Meryl Streep", c, true));
            c.addEntity(new Entity("Tom Cruise", c, true));
        }

        addConcept(c);

        // total values to extract: 255
        // total images to extract: 65

    }

    /**
     * Set the correct values for the benchmark concepts, entities and attributes.
     */
    public void setCorrectValues() {

        boolean fullSet = true;
        if (ExtractionProcessManager.getBenchmarkSetSize() == ExtractionProcessManager.BENCHMARK_HALF_SET) {
            fullSet = false;
        }

        // country
        Concept d = this.getConcept("Country");

        Entity e1 = d.getEntity("USA");
        Entity e2 = d.getEntity("Australia");
        Entity e3 = d.getEntity("Germany");
        Entity e4 = d.getEntity("Brazil");
        Entity e5 = d.getEntity("Japan");
        Entity e6 = null;
        Entity e7 = null;
        Entity e8 = null;
        Entity e9 = null;
        Entity e10 = null;
        if (fullSet) {
            e6 = d.getEntity("Ireland");
            e7 = d.getEntity("China");
            e8 = d.getEntity("South Africa");
            e9 = d.getEntity("Nigeria");
            e10 = d.getEntity("Bhutan");
        }
        Attribute a = d.getAttribute("Population");
        e1.addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("82369548", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("191908598 ", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("127288419", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("4156119", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a),
                    new FactValue("303824646", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Capital");
        e1
                .addFactForBenchmark(new Fact(a), new FactValue("Washington DC", new Source("GIVEN"),
                        ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("Canberra", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("Berlin", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("Brasilia", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("Tokyo", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("4156119", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a),
                    new FactValue("303824646", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Largest City");
        e1
                .addFactForBenchmark(new Fact(a), new FactValue("New York City", new Source("GIVEN"),
                        ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("Sydney", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("Berlin", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("Sao Paulo", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("Tokyo", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("4156119", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a),
                    new FactValue("303824646", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Area");
        e1
                .addFactForBenchmark(new Fact(a), new FactValue("9161923000000", new Source("GIVEN"),
                        ExtractionType.UNKNOWN));
        e2
                .addFactForBenchmark(new Fact(a), new FactValue("7617930000000", new Source("GIVEN"),
                        ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("357021000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4
                .addFactForBenchmark(new Fact(a), new FactValue("8511965000000", new Source("GIVEN"),
                        ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("377835000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("68890", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("47000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("HDI");
        e1.addFactForBenchmark(new Fact(a), new FactValue("0.951", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("0.962", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("0.935", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("0.8", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("0.953", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("0.959", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("0.579", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Currency Code");
        e1.addFactForBenchmark(new Fact(a), new FactValue("USD", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("AUD", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("EUR", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("BRL", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("JPY", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("EUR", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("BTN", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Calling Code");
        e1.addFactForBenchmark(new Fact(a), new FactValue("1", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("61", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("49", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("55", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("81", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("353", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("975", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Population Growth Rate");
        e1.addFactForBenchmark(new Fact(a), new FactValue("0.883", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("0.801", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("-0.044", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("0.98", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("-0.139", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("1.133", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("1.301", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Birth Rate");
        e1.addFactForBenchmark(new Fact(a), new FactValue("14.18", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("11.9", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("8.18", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("16.04", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("7.87", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("14.33", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("20.56", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Death Rate");
        e1.addFactForBenchmark(new Fact(a), new FactValue("8.27", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("7.62", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("10.8", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("6.22", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("9.26", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("7.77", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("7.54", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Unemployment Rate");
        e1.addFactForBenchmark(new Fact(a), new FactValue("4.6", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("4.4", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("8.4", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("9.3", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("3.9", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("4.6", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("2.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Geographic Coordinates");
        e1.addFactForBenchmark(new Fact(a), new FactValue("38 00 N, 97 00 W", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("28 00 S, 133 00 E", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("51 00 N, 9 00 E", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("10 00 S, 55 00 W", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("36 00 N, 138 00 E", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("53 00 N, 800 W", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("27 30 N, 90 30 E", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Coastline");
        e1.addFactForBenchmark(new Fact(a), new FactValue("1992400000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("2576000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("238900000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("749100000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("2975100000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("1448", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("682321", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8
                    .addFactForBenchmark(new Fact(a), new FactValue("303824646", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("20600856", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("0", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        // car
        d = this.getConcept("Car");
        e1 = d.getEntity("2006 Bugatti Veyron 16.4");
        e2 = d.getEntity("2008 Lamborghini Reventon");
        e3 = d.getEntity("2009 Jaguar XF");
        e4 = d.getEntity("2009 Tesla Roadster");
        e5 = d.getEntity("2009 Maserati GranTurismo S");
        if (fullSet) {
            e6 = d.getEntity("2009 Aston Martin V8 Vantage");
            e7 = d.getEntity("2009 Ford Mustang GT");
            e8 = d.getEntity("2008 Audi R8");
            e9 = d.getEntity("2009 Bentley Continental Flying Spur");
            e10 = d.getEntity("2010 Lotus Evora");
        }

        a = d.getAttribute("Curb Weight");
        e1.addFactForBenchmark(new Fact(a), new FactValue("1888000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("1665000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("1901000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("1220163.48", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("1882408.34", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("1617.75", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Acceleration");
        e1.addFactForBenchmark(new Fact(a), new FactValue("2.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("3.3", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("5.1", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("3.9", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("4.8", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("1617.75", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Top Speed");
        e1.addFactForBenchmark(new Fact(a), new FactValue("408.47", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("340", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("250", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("201.168", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("294.51", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("1617.75", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Horsepower");
        e1.addFactForBenchmark(new Fact(a), new FactValue("1001", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("650", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("420", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("248", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("433", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("1617.75", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Torque");
        e1.addFactForBenchmark(new Fact(a), new FactValue("1250", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("660", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("413", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("286.078", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("490", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("1617.75", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("1210.5", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        // a = d.getAttribute("Engine");
        // e1.addFact(new Fact(a), new FactValue("8.0 L quad-turbocharge W16",new
        // Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e2.addFact(new Fact(a), new FactValue("6.5 L V12",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e3.addFact(new Fact(a), new FactValue("1888000",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e4.addFact(new Fact(a), new FactValue("1665000",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e5.addFact(new Fact(a), new FactValue("1888000",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // if (fullSet) {
        // e3.addFact(new Fact(a), new FactValue("4.7 L V8",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e4.addFact(new Fact(a), new FactValue("Electro Motor",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // }

        // mobile phone
        d = this.getConcept("Mobile Phone");

        e1 = d.getEntity("Nokia N95");
        e2 = d.getEntity("Samsung i8510");
        e3 = d.getEntity("Motorola W218");
        e4 = d.getEntity("Sony Ericsson V600");
        e5 = d.getEntity("LG CU915 Vu");
        if (fullSet) {
            e6 = d.getEntity("O2 XDA Star");
            e7 = d.getEntity("NEC N343i");
            e8 = d.getEntity("HP iPAQ 610c");
            e9 = d.getEntity("Eten glofiish X610");
            e10 = d.getEntity("Vodafone 527");
        }

        a = d.getAttribute("Talk Time");
        e1.addFactForBenchmark(new Fact(a), new FactValue("23400", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("36000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("25200", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("28800", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("10800", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("7", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("8", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("7", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("8", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("7", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        // a = d.getAttribute("Networks");
        // e1.addFact(new Fact(a), new FactValue("GSM 850 / 900 / 1800 / 1900, HSDPA 2100 ",new
        // Source("GIVEN"),ExtractionType.UNKNOWN));
        // e2.addFact(new Fact(a), new FactValue("GSM 850 / 900 / 1800 / 1900, HSDPA 2100 / 900",new
        // Source("GIVEN"),ExtractionType.UNKNOWN));
        // if (fullSet) {
        // e3.addFact(new Fact(a), new FactValue("GSM 900 / 1800",new Source("GIVEN"),ExtractionType.UNKNOWN));
        // e4.addFact(new Fact(a), new FactValue("GSM 900 / 1800 / 1900, UMTS 2100",new
        // Source("GIVEN"),ExtractionType.UNKNOWN));
        // }

        a = d.getAttribute("Standby Time");
        e1.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("1080000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("1296000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("900000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Camera");
        e1.addFactForBenchmark(new Fact(a), new FactValue("5000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("8000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("1300000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("1300000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("2000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Dimensions");
        e1.addFactForBenchmark(new Fact(a), new FactValue("99 x 53 x 21 mm", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("106.5 x 53.9 x 17.2 mm", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("109 x 45 x 15 mm ", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("105 x 45.5 x 19.5 mm", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("108 x 54.9 x 13 mm", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Weight");
        e1.addFactForBenchmark(new Fact(a), new FactValue("120", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("140", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("78", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("105", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("89.6", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Internal Memory");
        e1.addFactForBenchmark(new Fact(a), new FactValue("167772160", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("17179869184", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("512000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("33554432", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("134217728", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Display Size");
        e1.addFactForBenchmark(new Fact(a), new FactValue("6.604", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("7.112", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("4.064", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("4.572", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("7.62", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("3G");
        e1.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("WLAN");
        e1.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("HSCSD");
        e1.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Bluetooth");
        e1.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("GPRS");
        e1.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Infrared");
        e1.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("USB");
        e1.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("no", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Vibration");
        e1.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("yes", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("300", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("360", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("1116000", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("792000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        // notebook
        d = this.getConcept("Notebook");
        e1 = d.getEntity("Apple MacBook Air MB003LL/A");
        e2 = d.getEntity("ASUS Eee PC 1000H 80G");
        e3 = d.getEntity("Sony VAIO VGN-FW139E/H");
        e4 = d.getEntity("HP 2133-KX870AT");
        e5 = d.getEntity("Dell Latitude E6400");
        if (fullSet) {
            e6 = d.getEntity("Lenovo ThinkPad X300");
            e7 = d.getEntity("Gateway P-7811FX");
            e8 = d.getEntity("Averatec Voya 6494");
            e9 = d.getEntity("Acer Aspire 8920-6671");
            e10 = d.getEntity("Asus M70SA-X2");
        }

        a = d.getAttribute("RAM");
        e1.addFactForBenchmark(new Fact(a), new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("3221225472", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("2147483648", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Processor Type");
        e1.addFactForBenchmark(new Fact(a), new FactValue("Intel Core 2 Duo", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("Intel Atom", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("Intel Core 2 Duo P8400", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("VIA C7-M ULV", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("Intel Core 2 Duo P8400", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("2147483648", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("CPU Speed");
        e1.addFactForBenchmark(new Fact(a), new FactValue("1600000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("1600000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("2260000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("1600000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("2260000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("2147483648", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Hard Disk");
        e1.addFactForBenchmark(new Fact(a), new FactValue("85899345920", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("85899345920", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("268435456000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("128849018880", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("171798691840", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("2147483648", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Front Side Bus");
        e1.addFactForBenchmark(new Fact(a), new FactValue("800000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("533000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("1066000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("800000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("800000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("2147483648", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Battery Life");
        e1.addFactForBenchmark(new Fact(a), new FactValue("18000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("25200", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("7200", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("16200", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("68400", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("2147483648", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Display Size");
        e1.addFactForBenchmark(new Fact(a), new FactValue("33.782", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("25.4", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("41.656", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("22.606", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("35.814", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("2147483648", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a),
                    new FactValue("1073741824", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("2147483648", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        // movie
        d = this.getConcept("Movie");

        e1 = d.getEntity("Braveheart");
        e2 = d.getEntity("The Dark Knight");
        e3 = d.getEntity("Idiocracy");
        e4 = d.getEntity("Code 46");
        e5 = d.getEntity("Iron Man");
        if (fullSet) {
            e6 = d.getEntity("The Descent");
            e7 = d.getEntity("Hostel");
            e8 = d.getEntity("Wedding Crashers");
            e9 = d.getEntity("The Truman Show");
            e10 = d.getEntity("Eternal Sunshine of a Spotless Mind");
        }

        a = d.getAttribute("Director");
        e1.addFactForBenchmark(new Fact(a), new FactValue("Mel Gibson", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("Christopher Nolan", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("Mike Judge", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("Michael Winterbottom", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("Jon Favreau", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("Mike Judge", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("Michael Winterbottom", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("Mel Gibson", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("Christopher Nolan", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("Mel Gibson", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Writer");
        e1.addFactForBenchmark(new Fact(a), new FactValue("Randall Wallace", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a),
                new FactValue("Jonathan Nolan", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("Mike Judge", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("Frank Cottrell Boyce", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("Mark Fergus", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("Mike Judge", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("Michael Winterbottom", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("Mel Gibson", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("Christopher Nolan", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("Mel Gibson", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Release Date");
        e1.addFactForBenchmark(new Fact(a), new FactValue("1995-05-24", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("2008-07-18", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("2006-09-01", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("2003-09-02", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("2008-05-02", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("Mike Judge", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("Michael Winterbottom", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("Mel Gibson", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("Christopher Nolan", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("Mel Gibson", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        // a = d.getAttribute("Studio");
        // e1.addFact(new Fact(a), new FactValue("Fox",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e2.addFact(new Fact(a), new FactValue("Warner",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e3.addFact(new Fact(a), new FactValue("Mel Gibson",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e4.addFact(new Fact(a), new FactValue("Christopher Nolan",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e5.addFact(new Fact(a), new FactValue("Mel Gibson",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // if (fullSet) {
        // e6.addFact(new Fact(a), new FactValue("Mike Judge",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e7.addFact(new Fact(a), new FactValue("Michael Winterbottom",new Source("GIVEN"),ExtractionType.UNKNOWN),
        // true);
        // e8.addFact(new Fact(a), new FactValue("Mel Gibson",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e9.addFact(new Fact(a), new FactValue("Christopher Nolan",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e10.addFact(new Fact(a), new FactValue("Mel Gibson",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // }

        a = d.getAttribute("Genre");
        e1.addFactForBenchmark(new Fact(a), new FactValue("Action", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("Action", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("Comedy", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("Drama", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("Action", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("Mike Judge", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("Michael Winterbottom", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("Mel Gibson", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("Christopher Nolan", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("Mel Gibson", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        // a = d.getAttribute("Starring");
        // e1.addFact(new Fact(a), new FactValue("Mel Gibson, Sophie Marceau, Catherine McCormack",new
        // Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e2.addFact(new Fact(a), new FactValue("Christian Bale, Heath Ledger, Aaron Eckhart, Michael Caine",new
        // Source("GIVEN"),ExtractionType.UNKNOWN),
        // true);
        // if (fullSet) {
        // e3.addFact(new Fact(a), new FactValue("Luke Wilson, Maya Rudolph, Dax Shepard, Terry Crews",new
        // Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e4.addFact(new Fact(a), new FactValue("Tim Robbins, Togo Igawa",new Source("GIVEN"),ExtractionType.UNKNOWN),
        // true);
        // }
        a = d.getAttribute("Budget");
        e1.addFactForBenchmark(new Fact(a), new FactValue("53000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("180000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("25000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("7500000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("185000000", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("Mike Judge", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("Michael Winterbottom", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("Mel Gibson", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("Christopher Nolan", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("Mel Gibson", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Runtime");
        e1.addFactForBenchmark(new Fact(a), new FactValue("10620", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("9120", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("5040", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("5520", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("7560", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("Mike Judge", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("Michael Winterbottom", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("Mel Gibson", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("Christopher Nolan", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("Mel Gibson", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Aspect Ratio");
        e1.addFactForBenchmark(new Fact(a), new FactValue("2.35:1", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("2.35:1", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("1.85:1", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("2.35:1", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("2.35:1", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a),
                    new FactValue("Mike Judge", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("Michael Winterbottom", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("Mel Gibson", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("Christopher Nolan", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("Mel Gibson", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        // actor
        d = this.getConcept("Actor");
        e1 = d.getEntity("Jim Carrey");
        e2 = d.getEntity("Mel Gibson");
        e3 = d.getEntity("Laura Dern");
        e4 = d.getEntity("Monica Potter");
        e5 = d.getEntity("Natalie Portman");
        if (fullSet) {
            e6 = d.getEntity("Brendan Fraser");
            e7 = d.getEntity("Nicholas Cage");
            e8 = d.getEntity("Bruce Willis");
            e9 = d.getEntity("Meryl Streep");
            e10 = d.getEntity("Tom Cruise");
        }

        a = d.getAttribute("Birth Name");
        e1.addFactForBenchmark(new Fact(a), new FactValue("James Eugene Carrey", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("Mel Columcille Gerard Gibson", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("Laura Elizabeth Dern", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("Monica Louise Brokaw", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("Natalie Hershlag", new Source("GIVEN"),
                ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("Laura Elizabeth Dern", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("Monica Louise Brokaw", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("Laura Elizabeth Dern", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("Monica Louise Brokaw", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("Mel Columcille Gerard Gibson", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Date of Birth");
        e1.addFactForBenchmark(new Fact(a), new FactValue("1962-01-17", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("1956-01-03", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("1967-02-10", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("1971-06-30", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("1981-06-09", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("10 February 1967", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("30 June 1971", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a),
                    new FactValue("1967-02-10", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a),
                    new FactValue("1971-06-30", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("1981-06-09", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Place of Birth");
        e1.addFactForBenchmark(new Fact(a), new FactValue("Newmarket", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("Peekskill", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("Los Angeles", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("Cleveland", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("Jerusalem", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("Los Angeles, California, USA", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("Cleveland, Ohio, USA", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("Los Angeles", new Source("GIVEN"),
                    ExtractionType.UNKNOWN));
            e9
                    .addFactForBenchmark(new Fact(a), new FactValue("Cleveland", new Source("GIVEN"),
                            ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a),
                    new FactValue("Jerusalem", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }

        a = d.getAttribute("Height");
        e1.addFactForBenchmark(new Fact(a), new FactValue("187", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e2.addFactForBenchmark(new Fact(a), new FactValue("170", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e3.addFactForBenchmark(new Fact(a), new FactValue("178", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e4.addFactForBenchmark(new Fact(a), new FactValue("170", new Source("GIVEN"), ExtractionType.UNKNOWN));
        e5.addFactForBenchmark(new Fact(a), new FactValue("160", new Source("GIVEN"), ExtractionType.UNKNOWN));
        if (fullSet) {
            e6.addFactForBenchmark(new Fact(a), new FactValue("178", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e7.addFactForBenchmark(new Fact(a), new FactValue("170", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e8.addFactForBenchmark(new Fact(a), new FactValue("178", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e9.addFactForBenchmark(new Fact(a), new FactValue("170", new Source("GIVEN"), ExtractionType.UNKNOWN));
            e10.addFactForBenchmark(new Fact(a), new FactValue("178", new Source("GIVEN"), ExtractionType.UNKNOWN));
        }
        // a = d.getAttribute("Nationality");
        // e1.addFact(new Fact(a), new FactValue("Canadian",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e2.addFact(new Fact(a), new FactValue("American",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // if (fullSet) {
        // e3.addFact(new Fact(a), new FactValue("American",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // e4.addFact(new Fact(a), new FactValue("American",new Source("GIVEN"),ExtractionType.UNKNOWN), true);
        // }
    }

    public void evaluateBenchmarkExtractions() {

        int totalFactNumber = 0;
        int totalFactNumberExpected = 0;
        int totalCorrectFacts = 0;
        int totalAlmostCorrectFacts = 0;
        double precision = 0.0;
        double recall = 0.0;
        double f1 = 0.0;
        double precisionWeak = 0.0;
        double f1Weak = 0.0;
        double f1Total = 0.0;
        double recallWeak = 0.0;
        double foundRatio = 0.0; // ratio of facts found to facts expected
        HashMap<String, Double> extractionTypeEvaluations = new HashMap<String, Double>();
        Integer[] extractionTypes = { ExtractionType.FREE_TEXT_SENTENCE, ExtractionType.COLON_PHRASE,
                ExtractionType.PATTERN_PHRASE, ExtractionType.TABLE_CELL };
        ArrayList<FactValue> highestRankedFactValues = new ArrayList<FactValue>();

        // create log document
        Iterator<Concept> dIt = concepts.iterator();
        while (dIt.hasNext()) {

            Concept cEntry = dIt.next();
            LOGGER.info("Concept: " + cEntry.getName());

            Iterator<Entity> eIt = cEntry.getEntitiesByTrust().iterator();
            while (eIt.hasNext()) {

                Entity eEntry = eIt.next();
                LOGGER.info(" Entity: " + eEntry.getName());

                Iterator<Fact> fIt = eEntry.getFacts().iterator();
                while (fIt.hasNext()) {
                    Fact fEntry = fIt.next();

                    HashSet<Integer> foundCounted = new HashSet<Integer>();
                    if (fEntry.getValues().size() > 0) {

                        ++totalFactNumber;
                        highestRankedFactValues.add(fEntry.getFactValue());

                        if (fEntry.isAbsoluteCorrect()) {
                            ++totalCorrectFacts;
                            LOGGER.info("  " + fEntry.getAttribute().getName() + " considered correct with "
                                    + fEntry.getValue());
                        } else if (fEntry.isAlmostCorrect()) {
                            ++totalAlmostCorrectFacts;
                            LOGGER.info("  " + fEntry.getAttribute().getName() + " considered ALMOST correct with "
                                    + fEntry.getValue());
                        }

                        // calculate extraction type precisions
                        Iterator<FactValue> factValuesIterator = fEntry.getValues().iterator();
                        HashSet<Integer> factExtractionTypesUsed = new HashSet<Integer>();
                        while (factValuesIterator.hasNext()) {
                            FactValue fv = factValuesIterator.next();
                            ArrayList<Integer> extractionTypesUsed = fv.getExtractionTypes(false);
                            factExtractionTypesUsed.addAll(extractionTypesUsed);

                            for (int i = 0; i < extractionTypesUsed.size(); i++) {
                                Double extractions = extractionTypeEvaluations.get(cEntry.getName()
                                        + extractionTypesUsed.get(i) + "extractions");
                                if (extractions == null) {
                                    extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                            + "extractions", 1.0);
                                } else {
                                    extractions += 1.0;
                                    extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                            + "extractions", extractions);
                                }
                                Double extractionsOverall = extractionTypeEvaluations.get("overall"
                                        + extractionTypesUsed.get(i) + "extractions");
                                if (extractionsOverall == null) {
                                    extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i)
                                            + "extractions", 1.0);
                                } else {
                                    extractionsOverall += 1.0;
                                    extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i)
                                            + "extractions", extractionsOverall);
                                }
                                if (!foundCounted.contains(extractionTypesUsed.get(i))) {
                                    Double found = extractionTypeEvaluations.get(cEntry.getName()
                                            + extractionTypesUsed.get(i) + "found");
                                    if (found == null) {
                                        extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                                + "found", 1.0);
                                    } else {
                                        found += 1.0;
                                        extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                                + "found", found);
                                    }
                                    Double foundOverall = extractionTypeEvaluations.get("overall"
                                            + extractionTypesUsed.get(i) + "found");
                                    if (foundOverall == null) {
                                        extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i) + "found",
                                                1.0);
                                    } else {
                                        foundOverall += 1.0;
                                        extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i) + "found",
                                                foundOverall);
                                    }
                                    foundCounted.add(extractionTypesUsed.get(i));
                                }
                                if (fEntry.isCorrect(fv.getValue())) {
                                    Double correct = extractionTypeEvaluations.get(cEntry.getName()
                                            + extractionTypesUsed.get(i) + "correct");
                                    if (correct == null) {
                                        extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                                + "correct", 1.0);
                                    } else {
                                        correct += 1.0;
                                        extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                                + "correct", correct);
                                    }
                                    Double correctOverall = extractionTypeEvaluations.get("overall"
                                            + extractionTypesUsed.get(i) + "correct");
                                    if (correctOverall == null) {
                                        extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i)
                                                + "correct", 1.0);
                                    } else {
                                        correctOverall += 1.0;
                                        extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i)
                                                + "correct", correctOverall);
                                    }
                                }
                            }
                        }
                        if (factExtractionTypesUsed.size() == 1 && fEntry.isCorrect()) {
                            Double only = extractionTypeEvaluations.get(cEntry.getName()
                                    + factExtractionTypesUsed.iterator().next() + "only");
                            if (only == null) {
                                extractionTypeEvaluations.put(cEntry.getName()
                                        + factExtractionTypesUsed.iterator().next() + "only", 1.0);
                            } else {
                                only += 1.0;
                                extractionTypeEvaluations.put(cEntry.getName()
                                        + factExtractionTypesUsed.iterator().next() + "only", only);
                            }
                            Double onlyOverall = extractionTypeEvaluations.get("overall"
                                    + factExtractionTypesUsed.iterator().next() + "only");
                            if (onlyOverall == null) {
                                extractionTypeEvaluations.put("overall" + factExtractionTypesUsed.iterator().next()
                                        + "only", 1.0);
                            } else {
                                onlyOverall += 1.0;
                                extractionTypeEvaluations.put("overall" + factExtractionTypesUsed.iterator().next()
                                        + "only", onlyOverall);
                            }
                        }
                    }
                }
            }

            Iterator<Attribute> aIt = cEntry.getAttributes().iterator();
            while (aIt.hasNext()) {
                aIt.next();
                ++totalFactNumberExpected;
            }
        }

        if (ExtractionProcessManager.getBenchmarkSetSize() == ExtractionProcessManager.BENCHMARK_FULL_SET) {
            totalFactNumberExpected *= 10;
        } else {
            totalFactNumberExpected *= 5;
        }

        precision = MathHelper.round((double) totalCorrectFacts / (double) totalFactNumber, 4);
        recall = MathHelper.round((double) totalCorrectFacts / (double) totalFactNumberExpected, 4);
        f1 = MathHelper.round(2 * precision * recall / (precision + recall), 4);

        precisionWeak = MathHelper.round((double) totalAlmostCorrectFacts / (double) totalFactNumber, 4);
        recallWeak = MathHelper.round((double) totalAlmostCorrectFacts / (double) totalFactNumberExpected, 4);
        f1Weak = MathHelper.round(2 * precisionWeak * recallWeak / (precisionWeak + recallWeak), 4);

        f1Total = MathHelper.round(2 * (precision + precisionWeak) * (recall + recallWeak)
                / (precision + precisionWeak + recall + recallWeak), 4);

        foundRatio = (double) totalFactNumber / (double) totalFactNumberExpected;

        LOGGER.info("\n\n----------------------------------------------------");
        LOGGER.info("Precision: " + precision + " (weak: +" + precisionWeak + ") = " + (precision + precisionWeak));
        LOGGER.info("Recall: " + recall + " (weak: +" + recallWeak + ") = " + (recall + recallWeak));
        LOGGER.info("F1: " + f1 + " (weak: +" + f1Weak + ") = " + f1Total);
        LOGGER.info("Found: " + foundRatio);
        LOGGER.info("-- Extraction Type Evaluation --");

        int setSize = 5;
        if (ExtractionProcessManager.getBenchmarkSetSize() == ExtractionProcessManager.BENCHMARK_FULL_SET) {
            setSize = 10;
        }

        ArrayList<Concept> concepts = getConcepts();
        if (getConcept("overall") == null) {
            concepts.add(new Concept("overall", new KnowledgeManager()));
        }
        int factsExpected = totalFactNumberExpected;
        for (int i = 0; i < extractionTypes.length; i++) {
            Iterator<Concept> cIt2 = concepts.iterator();
            String etypeName = "";
            if (extractionTypes[i] == ExtractionType.FREE_TEXT_SENTENCE) {
                etypeName = "Free Text Sentence";
            } else if (extractionTypes[i] == ExtractionType.COLON_PHRASE) {
                etypeName = "Colon Pattern";
            } else if (extractionTypes[i] == ExtractionType.PATTERN_PHRASE) {
                etypeName = "Pattern Phrase";
            } else if (extractionTypes[i] == ExtractionType.TABLE_CELL) {
                etypeName = "Table";
            }

            LOGGER.info(etypeName + ":");

            while (cIt2.hasNext()) {
                Concept c = cIt2.next();
                factsExpected = setSize * c.getAttributes().size();
                if (c.getName().equalsIgnoreCase("overall")) {
                    factsExpected = totalFactNumberExpected;
                }
                double found = 0.0;
                if (extractionTypeEvaluations.get(c.getName() + extractionTypes[i] + "found") != null) {
                    found = extractionTypeEvaluations.get(c.getName() + extractionTypes[i] + "found");
                }
                double foundPercent = MathHelper.round(found / factsExpected, 3);
                double extractions = 0.0;
                if (extractionTypeEvaluations.get(c.getName() + extractionTypes[i] + "extractions") != null) {
                    extractions = extractionTypeEvaluations.get(c.getName() + extractionTypes[i] + "extractions");
                }
                double correct = 0.0;
                if (extractionTypeEvaluations.get(c.getName() + extractionTypes[i] + "correct") != null) {
                    correct = extractionTypeEvaluations.get(c.getName() + extractionTypes[i] + "correct");
                }
                double correctPercent = MathHelper.round(correct / extractions, 3);
                double only = 0.0;
                if (extractionTypeEvaluations.get(c.getName() + extractionTypes[i] + "only") != null) {
                    only = extractionTypeEvaluations.get(c.getName() + extractionTypes[i] + "only");
                }
                double onlyPercent = MathHelper.round(only / factsExpected, 3);

                LOGGER.info(" " + c.getName());
                LOGGER.info("  found: " + found + " ~" + foundPercent);
                LOGGER.info("  extractions: " + extractions);
                LOGGER.info("  correct: " + correct + " ~" + correctPercent);
                LOGGER.info("  only: " + only + " ~" + onlyPercent);
            }
        }

        LOGGER.info("-- TRUST --");
        LOGGER.info("Trust Method: " + ExtractionProcessManager.getTrustFormula());
        LOGGER.info("Free Text Sentence: " + ExtractionType.getTrust(ExtractionType.FREE_TEXT_SENTENCE));
        LOGGER.info("Colon Pattern: " + ExtractionType.getTrust(ExtractionType.COLON_PHRASE));
        // Logger.getInstance().log("Structured Phrase: "+ExtractionType.getTrust(ExtractionType.STRUCTURED_PHRASE));
        LOGGER.info("Pattern Phrase: " + ExtractionType.getTrust(ExtractionType.PATTERN_PHRASE));
        LOGGER.info("Table: " + ExtractionType.getTrust(ExtractionType.TABLE_CELL));

        // create data for precision and recall tradeoff
        /*
         * StringBuilder as3String = new StringBuilder(); int relativeTrustStepSize = 1; double distributionInterval =
         * 10.0; int currentDistributionInterval =
         * 0; // 0 - 10% int currentDistributionIntervalCount = 0; int currentDistributionIntervalCorrectCount = 0; int
         * totalDistributionIntervalCount = 0; int
         * totalDistributionIntervalCorrectCount = 0; for (int i = 0; i <= 100; i += relativeTrustStepSize) { Double[]
         * data; double precisionValue = 0.0; double
         * recallValue = 0.0; double f1Value = 0.0; // go to next interval if ((double)i % distributionInterval == 0) {
         * as3String.append("p = new Point("+currentDistributionInterval+","+currentDistributionIntervalCount+
         * "); dataPoints4.push(p);").append("\n");
         * as3String.append("p = new Point("+currentDistributionInterval+","+currentDistributionIntervalCorrectCount+
         * "); dataPoints5.push(p);").append("\n");
         * totalDistributionIntervalCount += currentDistributionIntervalCount; currentDistributionIntervalCount = 0;
         * totalDistributionIntervalCorrectCount +=
         * currentDistributionIntervalCorrectCount; currentDistributionIntervalCorrectCount = 0;
         * currentDistributionInterval++; } // count number of fact values
         * with a relative trust higher or equal i int correctFacts = 0; totalFactNumber = 0; Iterator<FactValue>
         * highestRankedFactValuesIterator =
         * highestRankedFactValues.iterator(); while (highestRankedFactValuesIterator.hasNext()) { FactValue fv =
         * highestRankedFactValuesIterator.next(); if
         * (fv.getRelativeTrust()*100 >= (double)i) { totalFactNumber++; if (fv.getFact().isCorrect()) { correctFacts++;
         * } } if ((double)i %
         * distributionInterval == 0 && fv.getRelativeTrust()*100 < distributionInterval *
         * Math.ceil((double)(i+1)/distributionInterval) &&
         * fv.getRelativeTrust()*100 >= distributionInterval * Math.floor((double)(i+1)/distributionInterval)) {
         * currentDistributionIntervalCount++; if
         * (fv.getFact().isCorrect()) { currentDistributionIntervalCorrectCount++; } } } precisionValue =
         * MathHelper.round((double)correctFacts /
         * (double)totalFactNumber,4); recallValue = MathHelper.round((double)correctFacts /
         * (double)totalFactNumberExpected,4); f1Value =
         * MathHelper.round(2*precisionValue*recallValue / (precisionValue + recallValue),4); // precision data = new
         * Double[2]; data[0] = (double) i; data[1] =
         * precisionValue;
         * as3String.append("p = new Point("+data[0]+","+data[1]+"); dataPoints1.push(p);").append("\n"); // recall data
         * = new Double[2];
         * data[0] = (double) i; data[1] = recallValue;
         * as3String.append("p = new Point("+data[0]+","+data[1]+"); dataPoints2.push(p);").append("\n"); // f1
         * data = new Double[2]; data[0] = (double) i; data[1] = f1Value;
         * as3String.append("p = new Point("+data[0]+","+data[1]+"); dataPoints3.push(p);").append("\n");
         * 
         * as3String.append("p = new Point("+i+","+totalDistributionIntervalCount+"); dataPoints6.push(p);").append("\n")
         * ;
         * 
         * as3String.append("p = new Point("+i+","+totalDistributionIntervalCorrectCount+"); dataPoints7.push(p);").append
         * ("\n"); }
         * FileHelper.writeToFile("data/logs/asText_tradeoffdata_"+System.currentTimeMillis()+".txt", as3String);
         */
    }

    // TODO delete
    public Double[] evaluateBenchmarkExtractionsGetPAR() {

        int totalFactNumber = 0;
        int totalFactNumberExpected = 0;
        int totalCorrectFacts = 0;
        int totalAlmostCorrectFacts = 0;
        double precision = 0.0;
        double recall = 0.0;
        // double f1 = 0.0;
        double precisionWeak = 0.0;
        // double f1Weak = 0.0;
        double f1Total = 0.0;
        double recallWeak = 0.0;
        // double foundRatio = 0.0; // ratio of facts found to facts expected
        HashMap<String, Double> extractionTypeEvaluations = new HashMap<String, Double>();
        // Integer[] extractionTypes = { ExtractionType.FREE_TEXT_SENTENCE, ExtractionType.COLON_PHRASE,
        // ExtractionType.PATTERN_PHRASE, ExtractionType.TABLE_CELL };

        // create log document
        Iterator<Concept> dIt = concepts.iterator();
        while (dIt.hasNext()) {

            Concept cEntry = dIt.next();
            LOGGER.info("Concept: " + cEntry.getName());

            Iterator<Entity> eIt = cEntry.getEntitiesByTrust().iterator();
            while (eIt.hasNext()) {

                Entity eEntry = eIt.next();
                LOGGER.info(" Entity: " + eEntry.getName());

                Iterator<Fact> fIt = eEntry.getFacts().iterator();
                while (fIt.hasNext()) {
                    Fact fEntry = fIt.next();

                    HashSet<Integer> foundCounted = new HashSet<Integer>();
                    if (fEntry.getValues().size() > 0) {

                        ++totalFactNumber;

                        if (fEntry.isAbsoluteCorrect()) {
                            ++totalCorrectFacts;
                            LOGGER.info("  " + fEntry.getAttribute().getName() + " considered correct with "
                                    + fEntry.getValue());
                        } else if (fEntry.isAlmostCorrect()) {
                            ++totalAlmostCorrectFacts;
                            LOGGER.info("  " + fEntry.getAttribute().getName() + " considered ALMOST correct with "
                                    + fEntry.getValue());
                        }

                        // calculate extraction type precisions
                        Iterator<FactValue> factValuesIterator = fEntry.getValues().iterator();
                        HashSet<Integer> factExtractionTypesUsed = new HashSet<Integer>();
                        while (factValuesIterator.hasNext()) {
                            FactValue fv = factValuesIterator.next();
                            ArrayList<Integer> extractionTypesUsed = fv.getExtractionTypes(false);
                            factExtractionTypesUsed.addAll(extractionTypesUsed);

                            for (int i = 0; i < extractionTypesUsed.size(); i++) {
                                Double extractions = extractionTypeEvaluations.get(cEntry.getName()
                                        + extractionTypesUsed.get(i) + "extractions");
                                if (extractions == null) {
                                    extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                            + "extractions", 1.0);
                                } else {
                                    extractions += 1.0;
                                    extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                            + "extractions", extractions);
                                }
                                Double extractionsOverall = extractionTypeEvaluations.get("overall"
                                        + extractionTypesUsed.get(i) + "extractions");
                                if (extractionsOverall == null) {
                                    extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i)
                                            + "extractions", 1.0);
                                } else {
                                    extractionsOverall += 1.0;
                                    extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i)
                                            + "extractions", extractionsOverall);
                                }
                                if (!foundCounted.contains(extractionTypesUsed.get(i))) {
                                    Double found = extractionTypeEvaluations.get(cEntry.getName()
                                            + extractionTypesUsed.get(i) + "found");
                                    if (found == null) {
                                        extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                                + "found", 1.0);
                                    } else {
                                        found += 1.0;
                                        extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                                + "found", found);
                                    }
                                    Double foundOverall = extractionTypeEvaluations.get("overall"
                                            + extractionTypesUsed.get(i) + "found");
                                    if (foundOverall == null) {
                                        extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i) + "found",
                                                1.0);
                                    } else {
                                        foundOverall += 1.0;
                                        extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i) + "found",
                                                foundOverall);
                                    }
                                    foundCounted.add(extractionTypesUsed.get(i));
                                }
                                if (fEntry.isCorrect(fv.getValue())) {
                                    Double correct = extractionTypeEvaluations.get(cEntry.getName()
                                            + extractionTypesUsed.get(i) + "correct");
                                    if (correct == null) {
                                        extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                                + "correct", 1.0);
                                    } else {
                                        correct += 1.0;
                                        extractionTypeEvaluations.put(cEntry.getName() + extractionTypesUsed.get(i)
                                                + "correct", correct);
                                    }
                                    Double correctOverall = extractionTypeEvaluations.get("overall"
                                            + extractionTypesUsed.get(i) + "correct");
                                    if (correctOverall == null) {
                                        extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i)
                                                + "correct", 1.0);
                                    } else {
                                        correctOverall += 1.0;
                                        extractionTypeEvaluations.put("overall" + extractionTypesUsed.get(i)
                                                + "correct", correctOverall);
                                    }
                                }
                            }
                        }
                        if (factExtractionTypesUsed.size() == 1 && fEntry.isCorrect()) {
                            Double only = extractionTypeEvaluations.get(cEntry.getName()
                                    + factExtractionTypesUsed.iterator().next() + "only");
                            if (only == null) {
                                extractionTypeEvaluations.put(cEntry.getName()
                                        + factExtractionTypesUsed.iterator().next() + "only", 1.0);
                            } else {
                                only += 1.0;
                                extractionTypeEvaluations.put(cEntry.getName()
                                        + factExtractionTypesUsed.iterator().next() + "only", only);
                            }
                            Double onlyOverall = extractionTypeEvaluations.get("overall"
                                    + factExtractionTypesUsed.iterator().next() + "only");
                            if (onlyOverall == null) {
                                extractionTypeEvaluations.put("overall" + factExtractionTypesUsed.iterator().next()
                                        + "only", 1.0);
                            } else {
                                onlyOverall += 1.0;
                                extractionTypeEvaluations.put("overall" + factExtractionTypesUsed.iterator().next()
                                        + "only", onlyOverall);
                            }
                        }
                    }
                }
            }

            Iterator<Attribute> aIt = cEntry.getAttributes().iterator();
            while (aIt.hasNext()) {
                aIt.next();
                ++totalFactNumberExpected;
            }
        }

        if (ExtractionProcessManager.getBenchmarkSetSize() == ExtractionProcessManager.BENCHMARK_FULL_SET) {
            totalFactNumberExpected *= 10;
        } else {
            totalFactNumberExpected *= 5;
        }

        precision = MathHelper.round((double) totalCorrectFacts / (double) totalFactNumber, 4);
        recall = MathHelper.round((double) totalCorrectFacts / (double) totalFactNumberExpected, 4);
        // f1 = MathHelper.round(2 * precision * recall / (precision + recall), 4);

        precisionWeak = MathHelper.round((double) totalAlmostCorrectFacts / (double) totalFactNumber, 4);
        recallWeak = MathHelper.round((double) totalAlmostCorrectFacts / (double) totalFactNumberExpected, 4);
        // f1Weak = MathHelper.round(2 * precisionWeak * recallWeak / (precisionWeak + recallWeak), 4);

        f1Total = MathHelper.round(2 * (precision + precisionWeak) * (recall + recallWeak)
                / (precision + precisionWeak + recall + recallWeak), 4);

        // foundRatio = totalFactNumber / (double) totalFactNumberExpected;

        Double[] par = new Double[3];
        par[0] = precision + precisionWeak;
        par[1] = recall + recallWeak;
        par[2] = f1Total;

        return par;
    }

    public void fillDomainsForFactExtractionTest() {
        Iterator<Concept> dIt = this.getConcepts().iterator();
        while (dIt.hasNext()) {
            Concept d = dIt.next();
            if (d.getName().equalsIgnoreCase("Country")) {
                d.addEntity(new Entity("Denmark", d));
                d.addEntity(new Entity("Japan", d));
                d.addEntity(new Entity("Cuba", d));
                d.addEntity(new Entity("India", d));
                d.addEntity(new Entity("China", d));
                d.addEntity(new Entity("Ghana", d));
                // d.addEntity(new Entity("Nigeria"));
                // d.addEntity(new Entity("Argentina"));
                // d.addEntity(new Entity("South Africa"));
                // d.addEntity(new Entity("Austria"));
                // d.addEntity(new Entity("Great Britain"));
                // d.addEntity(new Entity("Ireland"));
                // d.addEntity(new Entity("Mexico"));
                // d.addEntity(new Entity("Vietnam"));
                // d.addEntity(new Entity("South Korea"));

            } else if (d.getName().equalsIgnoreCase("City")) {
                d.addEntity(new Entity("Dresden", d));
                d.addEntity(new Entity("Melbourne", d));
                d.addEntity(new Entity("New York", d));
                d.addEntity(new Entity("Glasgow", d));
                d.addEntity(new Entity("Geelong", d));
                d.addEntity(new Entity("Capetown", d));
                d.addEntity(new Entity("Tokyo", d));
                d.addEntity(new Entity("Villahermosa", d));
                d.addEntity(new Entity("Salt Lake City", d));
                d.addEntity(new Entity("Prague", d));
                d.addEntity(new Entity("Plymouth", d));
                d.addEntity(new Entity("Beijing", d));
                d.addEntity(new Entity("Rome", d));
                d.addEntity(new Entity("Bordeaux", d));
                d.addEntity(new Entity("Barcelona", d));
            } else if (d.getName().equalsIgnoreCase("Cell Phone")) {
                d.addEntity(new Entity("Nokia N95", d));
                d.addEntity(new Entity("Motorola MOTOKRZR K1", d));
                d.addEntity(new Entity("Samsung SGH U600", d));
                d.addEntity(new Entity("Motorola V3", d));
                d.addEntity(new Entity("Nokia 7610", d));
                d.addEntity(new Entity("Apple iPhone", d));
                d.addEntity(new Entity("Sony Ericsson W810i", d));
                d.addEntity(new Entity("LG VX8300", d));
                d.addEntity(new Entity("Sharp GX10", d));
                d.addEntity(new Entity("Nokia 3595", d));
                d.addEntity(new Entity("Samsung SGH D800", d));
                d.addEntity(new Entity("Motorola SLVR L6", d));
                d.addEntity(new Entity("T-Mobile Sidekick 3", d));
                d.addEntity(new Entity("Sony Ericsson P900", d));
                d.addEntity(new Entity("Motorola V60", d));
            }
        }

    }

    // TODO take best three? when to end?
    public boolean updateTrust() {
        return updateTrust(true);
    }

    public boolean updateTrust(boolean saveLogs) {

        // find the three highest corroborated facts (difference from first to second fact value)
        FactValue highestCorroborationDifferenceFactValue1 = null;
        FactValue highestCorroborationDifferenceFactValue2 = null;
        FactValue highestCorroborationDifferenceFactValue3 = null;
        double highestCorroborationDifference1 = 0.0;
        double highestCorroborationDifference2 = 0.0;
        double highestCorroborationDifference3 = 0.0;

        for (Concept concept : getConcepts()) {

            // highestCorroborationDifferenceFactValue1 = null;
            // highestCorroborationDifferenceFactValue2 = null;
            // highestCorroborationDifferenceFactValue3 = null;

            for (Entity entity : concept.getEntities()) {

                for (Fact fact : entity.getFacts()) {

                    // look for highest corroboration of facts that have not been reviewed
                    if (factsReviewed.contains(fact)) {
                        continue;
                    }

                    int c = 0;
                    double corroboration1 = 0.0;
                    double corroboration2 = 0.0;
                    FactValue currentMostCorroboratedFactValue = null;

                    // add trust for all values from the first one that are in the same margin and find difference to
                    // the first one out of that margin
                    boolean inMargin = true;
                    for (FactValue factValue : fact.getValues()) {

                        if (!inMargin) {
                            break;
                        }

                        try {
                            if (c == 0) {
                                corroboration1 = factValue.getCorroboration();
                                currentMostCorroboratedFactValue = factValue;
                            } else if (fact.getAttribute().getValueType() == Attribute.VALUE_NUMERIC
                                    && MathHelper.isWithinCorrectnessMargin(Double.valueOf(factValue.getValue()),
                                            Double.valueOf(currentMostCorroboratedFactValue.getValue()),
                                            Fact.CORRECTNESS_MARGIN)) {
                                corroboration1 += factValue.getCorroboration();
                            } else {
                                inMargin = false;
                            }
                            corroboration2 = factValue.getCorroboration();
                            ++c;
                        } catch (Exception e) {
                            LOGGER.error(currentMostCorroboratedFactValue.getValue(), e);
                        }
                    }

                    // in case all values were within margin take difference between Sum xn-1 - xn
                    if (inMargin) {
                        corroboration1 -= corroboration2;
                    }

                    if (c >= 2) {
                        double cDifference = corroboration1 - corroboration2;
                        if (cDifference > highestCorroborationDifference1) {
                            highestCorroborationDifference3 = highestCorroborationDifference2;
                            highestCorroborationDifference2 = highestCorroborationDifference1;
                            highestCorroborationDifference1 = cDifference;
                            highestCorroborationDifferenceFactValue3 = highestCorroborationDifferenceFactValue2;
                            highestCorroborationDifferenceFactValue2 = highestCorroborationDifferenceFactValue1;
                            highestCorroborationDifferenceFactValue1 = currentMostCorroboratedFactValue;

                        } else if (cDifference > highestCorroborationDifference2) {
                            highestCorroborationDifference3 = highestCorroborationDifference2;
                            highestCorroborationDifference2 = cDifference;
                            highestCorroborationDifferenceFactValue3 = highestCorroborationDifferenceFactValue2;
                            highestCorroborationDifferenceFactValue2 = currentMostCorroboratedFactValue;
                        } else if (cDifference > highestCorroborationDifference3) {
                            highestCorroborationDifference3 = cDifference;
                            highestCorroborationDifferenceFactValue3 = currentMostCorroboratedFactValue;
                        }
                    }
                }
            }
        }

        // no more values found to update parameters TODO
        if (highestCorroborationDifferenceFactValue3 == null) {
            return false;
        }

        // update trust for the extraction types used in the three fact values
        ArrayList<FactValue> factValues = new ArrayList<FactValue>();
        factValues.add(highestCorroborationDifferenceFactValue1);
        // factValues.add(highestCorroborationDifferenceFactValue2);
        // factValues.add(highestCorroborationDifferenceFactValue3);

        for (FactValue correctFactValue : factValues) {

            if (correctFactValue == null) {
                continue;
            }
            Fact currentFact = correctFactValue.getFact();
            factsReviewed.add(currentFact);

            if (currentFact.getAttribute().getValueType() == Attribute.VALUE_NUMERIC) {
                NumericFactDistribution.addNumber(currentFact.getID(), Double.valueOf(correctFactValue.getValue()));
            }

            // iterate through all fact values of the fact and update the trust for the extraction types used
            ArrayList<FactValue> currentFactValues = currentFact.getValues(false);
            Iterator<FactValue> currentFactValuesIterator = currentFactValues.iterator();
            while (currentFactValuesIterator.hasNext()) {
                FactValue currentFactValue = currentFactValuesIterator.next();

                ArrayList<Integer> extractionTypesUsed = currentFactValue.getExtractionTypes(false); // TODO test with
                                                                                                     // true and false
                // HashSet<Integer> etUsed = new HashSet<Integer>();
                // Iterator<Integer> etuIterator = extractionTypesUsed.iterator();
                // while (etuIterator.hasNext()) {
                // etUsed.add(etuIterator.next());
                // }
                // etuIterator = etUsed.iterator();
                Iterator<Integer> etuIterator = extractionTypesUsed.iterator();
                while (etuIterator.hasNext()) {
                    Integer extractionType = etuIterator.next();
                    boolean correct = false;

                    try {
                        // extraction types for correct value are given credit
                        if (currentFactValue.getValue().equalsIgnoreCase(correctFactValue.getValue())) {
                            correct = true;
                        }
                        // do also raise trust for extraction types that got the right value within the allowed margin
                        else if (currentFact.getAttribute().getValueType() == Attribute.VALUE_NUMERIC
                                && MathHelper.isWithinCorrectnessMargin(Double.valueOf(currentFactValue.getValue()),
                                        Double.valueOf(correctFactValue.getValue()), Fact.CORRECTNESS_MARGIN)) {
                            correct = true;
                        }

                        // add number of correct number facts to number fact distribution
                        if (correct && currentFact.getAttribute().getValueType() == Attribute.VALUE_NUMERIC) {
                            NumericFactDistribution.addNumber(currentFact.getID(), Double.valueOf(currentFactValue
                                    .getValue()));
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.error(currentFactValue.getValue(), e);
                    }

                    ExtractionType.addExtraction(extractionType, correct);
                    // ExtractionType.addExtractionByType(extractionType,
                    // currentFactValue.getFact().getAttribute().getDomain().getName(), correct);
                }
            }
        }

        // write log files
        if (saveLogs) {
            StringBuilder headText = new StringBuilder();

            headText.append("Extraction Types Trusts").append("\n");
            headText.append("Free Text Sentence: " + ExtractionType.getTrust(ExtractionType.FREE_TEXT_SENTENCE))
                    .append("\n");
            headText.append("Colon Pattern: " + ExtractionType.getTrust(ExtractionType.COLON_PHRASE)).append("\n");
            // headText.append("Structured Phrase: "+ExtractionType.getTrust(ExtractionType.STRUCTURED_PHRASE)).append("\n");
            headText.append("Pattern Phrase: " + ExtractionType.getTrust(ExtractionType.PATTERN_PHRASE)).append("\n");
            headText.append("Table: " + ExtractionType.getTrust(ExtractionType.TABLE_CELL)).append("\n");

            // FactExtractor.getInstance().createFactLog(headText.toString());
            evaluateBenchmarkExtractions();
        }

        try {
            System.out.println(highestCorroborationDifferenceFactValue1.getFact().getAttribute().getName() + " : "
                    + highestCorroborationDifferenceFactValue1.toString());
            System.out.println(highestCorroborationDifferenceFactValue2.getFact().getAttribute().getName() + " : "
                    + highestCorroborationDifferenceFactValue2.toString());
            System.out.println(highestCorroborationDifferenceFactValue3.getFact().getAttribute().getName() + " : "
                    + highestCorroborationDifferenceFactValue3.toString());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return true;
    }

    // save extractions in owl kb and rdb
    public void saveExtractions() {
        PersistenceManager.saveExtractions(this);
    }

    /**
     * Try to connect attributes that might be synonyms. Do not consider manually defined attributes as pairs. Calculate
     * a trust for each attribute pair of a
     * concept and connect top pairs with trust above certain threshold.
     */
    public void calculateAttributeSynonyms() {

        // iterate through all concepts
        Iterator<Concept> conceptIterator = getConcepts().iterator();
        while (conceptIterator.hasNext()) {
            Concept currentConcept = conceptIterator.next();

            // attribute pair with highest trust
            Attribute[] highestTrustPair = new Attribute[2];
            double highestTrust = 0.0;
            ArrayList<Attribute> attributeList = currentConcept.getAttributesAsList(false);

            for (int i = 0; i < attributeList.size() - 1; i++) {
                Attribute attribute1 = attributeList.get(i);

                for (int j = i + 1; j < attributeList.size(); j++) {
                    Attribute attribute2 = attributeList.get(j);

                    // compare only attributes where at least one has been extracted
                    if (!attribute1.isExtracted() && !attribute2.isExtracted()) {
                        continue;
                    }

                    double attributeSynonymTrust = DatabaseManager.getInstance().calculateAttributeSynonymTrust(
                            attribute1, attribute2);

                    if (attributeSynonymTrust > highestTrust) {
                        highestTrustPair[0] = attribute1;
                        highestTrustPair[1] = attribute2;
                        highestTrust = attributeSynonymTrust;
                    }

                    System.out.println("trust for synonyms " + attribute1.getName() + " = " + attribute2.getName()
                            + ": " + attributeSynonymTrust);

                }
            }

            if (highestTrustPair[0] != null && highestTrustPair[1] != null) {
                System.out.println("highest trust in concept " + currentConcept.getName() + " between attributes "
                        + highestTrustPair[0].getName() + " = " + highestTrustPair[1].getName() + " with trust "
                        + highestTrust);
            }

            if (highestTrust >= ATTRIBUTE_SYNONYM_TRUST_THRESHOLD) {
                System.out.println("enter synonyms " + highestTrustPair[0].getName() + ", "
                        + highestTrustPair[1].getName() + " with trust " + highestTrust);
                DatabaseManager.getInstance().addAttributeSynonym(highestTrustPair[0].getID(),
                        highestTrustPair[1].getID(), highestTrust);
            }

        }
    }

    // public int getTotalAttributes() {
    // int totalAttributes = 0;
    // Iterator<Concept> cIt = concepts.iterator();
    // while (cIt.hasNext()) {
    //			
    // Concept cEntry = cIt.next();
    // totalAttributes += cEntry.getAttributes().size();
    // }
    // return totalAttributes;
    // }

    public static void main(String[] a) {

        // add test Concept+Entity
        KnowledgeManager km1 = new KnowledgeManager();
        Concept test = new Concept("car");
        test.addEntity(new Entity("Porsche 911", test));
        km1.addConcept(test);
        km1.saveExtractions();

        // dm.saveExtractions(km);

        /*
         * Concept c1 = new Concept("c1",new KnowledgeManager()); c1.setLastSearched(new
         * Date(System.currentTimeMillis()-9870871)); Concept c2 = new
         * Concept("c2",new KnowledgeManager()); c2.setLastSearched(new Date(System.currentTimeMillis()-9870870));
         * Concept c3 = new Concept("c3",new
         * KnowledgeManager()); ArrayList<Concept> al = new ArrayList<Concept>(); al.add(c1); al.add(c2); al.add(c3);
         * Collections.sort(al,new
         * ConceptDateComparator()); for (int i = 0; i < al.size(); i++) { System.out.println(al.get(i).getName()); }
         */

        // test attribute synonym trust calculation
        KnowledgeManager km = DatabaseManager.getInstance().loadOntology();
        // km.calculateAttributeSynonyms();
        ArrayList<Concept> c = km.getConcepts();
        CollectionHelper.print(c);
        System.out.println("1: " + c.get(0).getID());
        System.exit(1);
        // TODO robert
        // Attribute newSynonym = new Attribute("running time", c.get(0).getAttribute("runtime").getValueType(),
        // c.get(0));
        c.get(0).getAttribute("runtime").addSynonym("running time");
        // TODO c.get(0).getAttribute("runtime").addSynonym(newSynonym);
        DatabaseManager.getInstance().saveExtractions(km);

    }
}