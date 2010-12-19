package tud.iir.extraction.fact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.extraction.ExtractionType;
import tud.iir.extraction.Extractor;
import tud.iir.gui.GUIManager;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Fact;
import tud.iir.knowledge.FactValue;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.persistence.DatabaseManager;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetrieverManager;

// TODO boolean matching problem? run benchmarks!

// TODO merge similar numbers 
// TODO string similarity finder from file
// TODO mixed must have numbers AND word characters (or just numbers and symbols?)
// TODO add entity name in search query?
// TODO boolean values are true when mentioned in tables or specification lists
// TODO string: stringlist, proper noun, separated string
// TODO - for boolean means no, text means yes
// TODO do not take mentions when word is part of another? + screenWRITER - CAPITALism
// TODO sometimes there are several occurrences of the attribute in a paragraph, for now only the first one is taken
// TODO no script but this page doesn't work correctly: http://wikitravel.org/en/Australia
// TODO make weighted occurrences in sentence
// TODO use attribute synonyms
// TODO multi value attributes

/**
 * The FactExtractor class. This class is singleton.
 * 
 * @author David Urbansky
 */
public class FactExtractor extends Extractor {

    /** The instance of this class. */
    private static final FactExtractor INSTANCE = new FactExtractor();
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FactExtractor.class);

    //private HashSet<String> urlsSeenForEntity; // temporarily save the set of urls that have been used for extraction for the current entity (so that the url is
    // not used twice for a similar extraction)
    //private int counter = 1; // for indexing websiteX.html X > 0

    /**
     * The private constructor for this singleton class.
     */
    private FactExtractor() {
        
        // do not analyze any binary files
        addSuffixesToBlackList(Extractor.URL_BINARY_BLACKLIST);
        
        if (GUIManager.isInstanciated()) {
            // instance.logger.addObserver(GUIManager.getInstance());
        }
    }

    public static FactExtractor getInstance() {
        return INSTANCE;
    }

    /**
     * This methods allows it to extract facts (attribute - value pairs) for a given entity name, for which the concept is unknown.
     * 
     * @param entityName The name of the entity, facts are searched for.
     * @return An array of extracted facts.
     */
    public ArrayList<Fact> extractFactsForEntityName(String entityName) {
        LiveFactExtractor lfe = new LiveFactExtractor(entityName);
        return lfe.extractFacts(3);
    }

    /**
     * Start extraction of facts for entities that are fetched from the knowledge base. Continue from last extraction.
     */
    public void startExtraction() {
        startExtraction(true);
    }

    public void startExtraction(boolean continueFromLastExtraction) {

        LOGGER.info("start fact extraction");

        // reset stopped command
        setStopped(false);

        // load concepts and attributes from ontology (and rdb) and to know what to extract
        if (!isBenchmark()) {
            setKnowledgeManager(DatabaseManager.getInstance().loadOntology());
        } else {
            KnowledgeManager km = new KnowledgeManager();
            km.createBenchmarkConcepts();
            km.setCorrectValues();
            setKnowledgeManager(km);
        }

        // loop until exit called
        while (!isStopped()) {

            // concepts
            ArrayList<Concept> concepts = knowledgeManager.getConcepts(true); // TODO? order by date?

            // iterate through all concepts
            for (Concept currentConcept : concepts) {

                System.out.println("Concept: " + currentConcept.getName());

                if (currentConcept.getAttributes().size() == 0) {
                    LOGGER.info("no attributes found for the concept " + currentConcept.getName());
                    continue;
                }

                if (isStopped()) {
                    LOGGER.info("fact extraction process stopped");
                    break;
                }

                // iterate through all entities for current concept
                if (!isBenchmark()) {
                    currentConcept.loadEntities(continueFromLastExtraction);
                }
                ArrayList<Entity> conceptEntities;
                if (continueFromLastExtraction) {
                    conceptEntities = currentConcept.getEntitiesByDate();
                } else {
                    conceptEntities = currentConcept.getEntities();
                }

                // wait for a certain time when no entities were found, then restart
                if (conceptEntities.isEmpty()) {
                    // logger.log("no entities for fact extraction, wait now for "+((WAIT_FOR_ENTITIES_TIMEOUT/1000)/60)+" minutes");
                    LOGGER.info("no entities for fact extraction, continue with next concept");
                    continue;
                    // try {
                    // Thread.sleep(WAIT_FOR_ENTITIES_TIMEOUT);
                    // continue;
                    // } catch (InterruptedException e) {
                    // logger.logError("waiting for entities", e);
                    // }
                }

                extractionThreadGroup = new ThreadGroup("factExtractionThreadGroup");

                // ArrayList<Entity> conceptEntities = currentConcept.getEntitiesByDate();
                for (Entity currentEntity : conceptEntities) {

                    if (isStopped()) {
                        LOGGER.info("fact extraction process stopped");
                        break;
                    }

                    // update live status action
                    ExtractionProcessManager.liveStatus.setCurrentAction("Search for facts for "
                            + currentEntity.getName() + " (" + currentConcept.getName() + ")");

                    currentEntity.setLastSearched(new Date(System.currentTimeMillis()));

                    LOGGER.info("  start fact extraction process for entity " + currentEntity.getName() + " (" + currentEntity.getConcept().getName() + ")");
                    Thread entityThread = new EntityFactExtractionThread(extractionThreadGroup, currentEntity.getSafeName() + "Thread", currentEntity);
                    entityThread.start();

                    LOGGER.info("THREAD STARTED (" + getThreadCount() + "): " + currentEntity.getName());

                    int c = 0;
                    while (getThreadCount() >= MAX_EXTRACTION_THREADS) {
                        LOGGER.info("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + ") " + extractionThreadGroup.activeCount() + ","
                                + extractionThreadGroup.activeGroupCount());
                        if (extractionThreadGroup.activeCount() + extractionThreadGroup.activeGroupCount() == 0) {
                            LOGGER.warn("apparently " + getThreadCount() + " threads have not finished correctly but thread group is empty, continuing...");
                            resetThreadCount();
                            break;
                        }
                        ThreadHelper.sleep(WAIT_FOR_FREE_THREAD_SLOT);
                        if (isStopped()) {
                            c++;
                        }
                        if (c > 25) {
                            LOGGER.info("waited 25 iterations after stop has been called, breaking now");
                            break;
                        }
                    }

                }
            }

            // create log document
            createFactLog();

            // save extraction results after each full loop
            if (!isBenchmark()) {

                // update trust
                int i = 0;
                while (getKnowledgeManager().updateTrust(false)) {
                    LOGGER.debug("update Trust iteration: " + i);
                    i++;
                }

                createFactLog("trust update iterations: " + i);
                getKnowledgeManager().saveExtractions();

            } else {

                getKnowledgeManager().evaluateBenchmarkExtractions();

                // remove sources from facts that are used for several other entities as well
                // removeCrossSources(); ?TODO has not happened yet, that one page is used for different entities?

                if (ExtractionProcessManager.getTrustFormula() > ExtractionProcessManager.SOURCE_TRUST) {

                    int i = 0;
                    while (getKnowledgeManager().updateTrust(false)) {
                        // empty loop
                        System.out.println("update Trust iteration: " + i);
                        i++;
                    }

                    // // series, data, 0:time,1:value
                    // ArrayList<ArrayList<Double[]>> quantities = new ArrayList<ArrayList<Double[]>>();
                    //					
                    // // collect total entity and total fact extraction
                    // ArrayList<Double[]> tableTrust = new ArrayList<Double[]>();
                    // ArrayList<Double[]> phraseTrust = new ArrayList<Double[]>();
                    // ArrayList<Double[]> colonTrust = new ArrayList<Double[]>();
                    // ArrayList<Double[]> freeTextTrust = new ArrayList<Double[]>();
                    //					
                    // Double[] data;
                    // StringBuilder as3String = new StringBuilder();
                    // for (int i = 0; i < 174; i++) {
                    // System.out.println("update Trust iteration: " + i);

                    // Double[] par = getKnowledgeManager().evaluateBenchmarkExtractionsGetPAR();
                    //												
                    // data = new Double[2];
                    // data[0] = (double) i;
                    // data[1] = ExtractionType.getTrust(ExtractionType.TABLE_CELL);
                    // tableTrust.add(data);
                    // as3String.append("p = new Point("+data[0]+","+data[1]+"); dataPoints1.push(p);").append("\n");
                    //						
                    // data = new Double[2];
                    // data[0] = (double) i;
                    // data[1] = ExtractionType.getTrust(ExtractionType.PATTERN_PHRASE);
                    // phraseTrust.add(data);
                    // as3String.append("p = new Point("+data[0]+","+data[1]+"); dataPoints2.push(p);").append("\n");
                    //						
                    // data = new Double[2];
                    // data[0] = (double) i;
                    // data[1] = ExtractionType.getTrust(ExtractionType.COLON_PHRASE);
                    // colonTrust.add(data);
                    // as3String.append("p = new Point("+data[0]+","+data[1]+"); dataPoints3.push(p);").append("\n");
                    //						
                    // data = new Double[2];
                    // data[0] = (double) i;
                    // data[1] = ExtractionType.getTrust(ExtractionType.FREE_TEXT_SENTENCE);
                    // freeTextTrust.add(data);
                    // as3String.append("p = new Point("+data[0]+","+data[1]+"); dataPoints4.push(p);").append("\n");
                    //								
                    // data = new Double[2];
                    // data[0] = (double) i;
                    // data[1] = par[0];
                    // as3String.append("p = new Point("+data[0]+","+data[1]+"); dataPoints5.push(p);").append("\n");
                    //						
                    // data = new Double[2];
                    // data[0] = (double) i;
                    // data[1] = par[1];
                    // as3String.append("p = new Point("+data[0]+","+data[1]+"); dataPoints6.push(p);").append("\n");
                    //						
                    //						
                    // data = new Double[2];
                    // data[0] = (double) i;
                    // data[1] = par[2];
                    // as3String.append("p = new Point("+data[0]+","+data[1]+"); dataPoints7.push(p);").append("\n");

                    // getKnowledgeManager().updateTrust(false);
                    // }

                    getKnowledgeManager().evaluateBenchmarkExtractions();
                    //					
                    // ArrayList<String> seriesNames = new ArrayList<String>();
                    // seriesNames.add("Table");
                    // seriesNames.add("Phrase");
                    // seriesNames.add("Colon");
                    // seriesNames.add("Free Text");
                    // quantities.add(tableTrust);
                    // quantities.add(phraseTrust);
                    // quantities.add(colonTrust);
                    // quantities.add(freeTextTrust);
                    //					
                    // FileHelper.writeToFile("data/test/asText.txt", as3String);
                    //
                    // ChartCreator.createLineChart("data/logs/"+Logger.getInstance().getDateString()+"_learnedTrust.png", quantities, seriesNames,
                    // "Extraction Structure Trust Learning", "Iteration", "Trust", true);
                }

                LOGGER.info("finished benchmark");
                break;
            }
        }

    }

    @Override
    protected void saveExtractions(boolean saveResults) {
        if (saveResults && !isBenchmark()) {
            System.out.println("save extractions now");
            getKnowledgeManager().saveExtractions();
        }
    }

    /**
     * remove sources from facts that are used for several other entities as well
     */
    // private void removeCrossSources() {
    //
    // // 1. collect all sources that were used for more than one entity
    // HashSet<String> usedSources = new HashSet<String>();
    // HashSet<String> crossSources = new HashSet<String>();
    //
    // ArrayList<Concept> concepts = knowledgeManager.getConcepts();
    // Iterator<Concept> rIt = concepts.iterator();
    // while (rIt.hasNext()) {
    // Concept dEntry = rIt.next();
    //
    // Iterator<Entity> r2It = dEntry.getEntities().iterator();
    // while (r2It.hasNext()) {
    // Entity eEntry = r2It.next();
    // HashSet<String> usedSourcesEntity = new HashSet<String>();
    //
    // Iterator<Fact> r4It = eEntry.getFacts().iterator();
    // while (r4It.hasNext()) {
    // Fact fEntry = r4It.next();
    //
    // Iterator<FactValue> r5It = fEntry.getValues().iterator();
    // while (r5It.hasNext()) {
    // FactValue fv = r5It.next();
    //
    // ArrayList<Source> sources = fv.getSources();
    //
    // Iterator<Source> r6It = sources.iterator();
    // while (r6It.hasNext()) {
    // Source sEntry = r6It.next();
    // boolean isNeverUsed = usedSources.add(sEntry.getUrl());
    // boolean isNewForEntity = usedSourcesEntity.add(sEntry.getUrl());
    // if (!isNeverUsed && isNewForEntity) {
    // crossSources.add(sEntry.getUrl());
    // LOGGER.info(sEntry.getUrl() + " was used before " + eEntry.getName());
    // }
    // }
    // }
    // }
    // }
    // }
    //
    // LOGGER.info("cross sources:\n");
    // // CollectionHelper.print(crossSources);
    // Iterator<String> setIterator = crossSources.iterator();
    // while (setIterator.hasNext()) {
    // String entry = setIterator.next();
    // LOGGER.info(entry);
    // }
    //
    // // 2. remove all cross sources for all fact values
    // rIt = concepts.iterator();
    // while (rIt.hasNext()) {
    // Concept dEntry = rIt.next();
    //
    // Iterator<Entity> r2It = dEntry.getEntities().iterator();
    // while (r2It.hasNext()) {
    // Entity eEntry = r2It.next();
    //
    // Iterator<Fact> r4It = eEntry.getFacts().iterator();
    // while (r4It.hasNext()) {
    // Fact fEntry = r4It.next();
    //
    // Iterator<FactValue> r5It = fEntry.getValues().iterator();
    // while (r5It.hasNext()) {
    // FactValue fv = r5It.next();
    //
    // ArrayList<Source> sources = fv.getSources();
    //
    // Sources<Source> removableSources = new Sources<Source>();
    // Iterator<Source> r6It = sources.iterator();
    // while (r6It.hasNext()) {
    // Source sEntry = r6It.next();
    //
    // if (crossSources.contains(sEntry.getUrl()) && !removableSources.contains(sEntry)) {
    // removableSources.add(sEntry);
    // }
    // }
    //
    // r6It = removableSources.iterator();
    // while (r6It.hasNext()) {
    // Source s = r6It.next();
    // LOGGER.info("remove source from " + eEntry.getName() + ", " + fv.getFact().getAttribute().getName() + " : " +
    // s.getUrl());
    // fv.removeSource(s);
    // }
    // }
    // }
    // }
    // }
    // }

    /**
     * Log which facts for which concepts and entities have been extracted.
     */
    public void createFactLog() {
        createFactLog("");
    }

    public void createFactLog(String headText) {

        if (headText.length() > 0) {
            LOGGER.info(headText);
        }

        LOGGER.info("Facts for the following entities were searched:");
        String entityString = "";
        ArrayList<Concept> concepts = knowledgeManager.getConcepts();
        for (Concept currentConcept : concepts) {
            for (Entity entity : currentConcept.getEntities()) {
                entityString += entity.getName() + " | ";
            }
        }
        LOGGER.info(entityString + "\n\n");

        for (Concept currentConcept : concepts) {

            LOGGER.info("\n\n-------------- Concept " + currentConcept.getName() + " --------------");

            for (Entity entity : currentConcept.getEntities()) {

                if (entity.getFacts().size() == 0) {
                    continue;
                }

                LOGGER.info("\n---- Entity " + entity.getName() + " ----");

                for (Fact fact : entity.getFacts()) {

                    if (fact.getValues().size() == 0) {
                        continue;
                    }

                    LOGGER.info("\n---- Fact Candidates for Attribute " + fact.getAttribute().getName() + " ----");

                    Collections.sort(fact.getValues(), new FactValueComparator());

                    for (FactValue factValue : fact.getValues()) {
                        LOGGER.info(factValue.toString());
                    }
                }
            }
        }

        double dataDownloaded = Crawler.getSessionDownloadSize(Crawler.BYTES);
        LOGGER.info("\n\n--------------------------");
        LOGGER.info("Downloaded (all crawlers since start): " + dataDownloaded / 1024 + " KB / " + dataDownloaded / (1024 * 1024) + " MB\n");
        LOGGER.info(SourceRetrieverManager.getInstance().getLogs());

        LOGGER.info("\n\n-- TRUST --");
        LOGGER.info("Trust Method: " + ExtractionProcessManager.getTrustFormula());
        LOGGER.info("Free Text Sentence: " + ExtractionType.getTrust(ExtractionType.FREE_TEXT_SENTENCE));
        LOGGER.info("Colon Pattern: " + ExtractionType.getTrust(ExtractionType.COLON_PHRASE));
        // Logger.getInstance().log("Structured Phrase: "+ExtractionType.getTrust(ExtractionType.STRUCTURED_PHRASE));
        LOGGER.info("Pattern Phrase: " + ExtractionType.getTrust(ExtractionType.PATTERN_PHRASE));
        LOGGER.info("Table: " + ExtractionType.getTrust(ExtractionType.TABLE_CELL));
    }

    /**
     * For attributes with the data type "number" or "date" several occurrences can be extracted and weighted by their distance to the attribute. TODO employ
     * this method and try to improve results with it
     */
    // private void weightedOccurrences() {
    //
    // String a =
    // "iuh ooi ai7zf8 asl:3.9 io asjf has a 4,9% Unemployment RATE asdf Also 4.8% dhfsfsfuhs fasdf though the unemployment rate might fall by 1% the next month. asdf asdf faisodf 3 sadf isado2 234  saidfjasoi sijf sof";
    // String search = "unemployment rate";
    //
    // int currentStartIndex = 0;
    // TreeMap<Integer, String> values = new TreeMap<Integer, String>();
    //
    // int neighborhoodRange = 50;
    //
    // while (true) {
    //
    // int index = a.toLowerCase().indexOf(search.toLowerCase(), currentStartIndex);
    //
    // if (index == -1) {
    // break;
    // }
    //
    // String pageStringCleaned = a.substring(0, currentStartIndex).toLowerCase();
    // pageStringCleaned = pageStringCleaned +
    // a.substring(currentStartIndex).toLowerCase().replace(search.toLowerCase(), "");
    // int startIndex = index - neighborhoodRange;
    //
    // int missingOffset = 0;
    // if (index - neighborhoodRange < 0)
    // missingOffset = Math.abs(index - neighborhoodRange);
    // int keyTermPosition = neighborhoodRange - missingOffset;
    //
    // if (startIndex < 0)
    // startIndex = 0;
    // int endIndex = index + neighborhoodRange;
    // if (endIndex > pageStringCleaned.length())
    // endIndex = pageStringCleaned.length();
    //
    // System.out.println(pageStringCleaned);
    //
    // String neighborhood = pageStringCleaned.substring(startIndex, endIndex);
    // System.out.println(index + " " + keyTermPosition + "_" + neighborhood);
    //
    // java.util.regex.Pattern pat = java.util.regex.Pattern.compile("[(\\s)|:]((\\d){1,}([,|\\.])?){1,}");
    // Matcher m = pat.matcher(neighborhood);
    //
    // m.region(0, neighborhood.length());
    //
    // while (m.find()) {
    // // add fact candidate for entity and attribute, checking whether fact or fact value has been entered already is
    // done in the entity and fact
    // // class respectively
    // // logger.log("found "+m.group());
    // // currentEntity.addFactAndValue(new Fact(currentAttribute),new FactValue(m.group(),new
    // Source(Source.SEMI_STRUCTURED,currentURL)));
    // String valueFound = m.group().trim().replace(":", "");
    // int absoluteFoundPosition = m.start() + currentStartIndex;
    // int distance = Math.abs(keyTermPosition - m.start());
    // System.out.println(valueFound + " at " + m.start() + " distance " + distance + " index " + index + " absolute " +
    // absoluteFoundPosition + " "
    // + currentStartIndex);
    // values.put(distance, valueFound);
    // }
    //
    // currentStartIndex = index + search.length();
    // }
    //
    // // Collections.sort(values,new IndexPositionComparator());
    // Iterator<Map.Entry<Integer, String>> vIt = values.entrySet().iterator();
    // while (vIt.hasNext()) {
    // Map.Entry<Integer, String> entry = vIt.next();
    // System.out.println(entry.getKey() + "_" + entry.getValue());
    // }
    // }

    /**
     * Try to find facts from a table on a web page. Use a set of attribute names to detect the table and the other facts. If no attributes are given facts are
     * tried to be extracted without any prior information.
     * 
     * @param url The URL of the web page.
     * @param attributes A set of attribute names that appear (on the page AND) in the table.
     * @return A list of facts that were found in the table.
     */
    public static ArrayList<Fact> extractFacts(String url, HashSet<Attribute> attributes) {

        if (attributes.size() == 0) {
            LiveFactExtractor lfe = new LiveFactExtractor("HELPER");
            return lfe.extractFacts(url);
        }

        Entity surrogateEntity = new Entity("HELPER", null);
        FactExtractionDecisionTree dt = new FactExtractionDecisionTree(surrogateEntity, url);
        EntityFactExtractionThread efet = new EntityFactExtractionThread(null, "", surrogateEntity);
        efet.setCurrentSource(url);

        // try to find all attributes
        for (Attribute currentAttribute : attributes) {

            dt.setAttribute(currentAttribute);
            HashMap<Attribute, ArrayList<FactString>> factStrings = dt.getFactStrings(currentAttribute);
            LOGGER.info("found " + factStrings.size() + " new attribute candidates, " + factStrings.keySet());

            // iterate through all attributes found
            Iterator<Map.Entry<Attribute, ArrayList<FactString>>> factIterator = factStrings.entrySet().iterator();
            while (factIterator.hasNext()) {
                Map.Entry<Attribute, ArrayList<FactString>> currentEntry = factIterator.next();
                Attribute attribute = currentEntry.getKey();
                ArrayList<FactString> attributeFactStrings = currentEntry.getValue();

                System.out.println("\n" + currentEntry.getKey().getName() + " (" + currentEntry.getKey().getValueTypeName() + ") with "
                        + attributeFactStrings.size() + " values");
                // CollectionHelper.print(currentEntry.getValue());

                // extract the fact values from each of the strings for the current attribute
                for (int j = 0, l = attributeFactStrings.size(); j < l; ++j) {

                    // do not try to extract booleans only from sentences
                    if (attribute.getValueType() == Attribute.VALUE_BOOLEAN
                            && attributeFactStrings.get(j).getExtractionType() == ExtractionType.FREE_TEXT_SENTENCE) {
                        continue;
                    }

                    // extract the value
                    efet.extractValue(surrogateEntity, attributeFactStrings.get(j), attribute);
                }
            }
        }

        return surrogateEntity.getFacts();
    }

    public static ArrayList<Fact> extractFacts(String url) {
        return extractFacts(url, new HashSet<Attribute>());
    }

    /**
     * Example calls of fact extraction functionality. See FactExtractionTest for more tests and usages.
     * 
     * @param args
     */
    public static void main(String[] args) {

        // /////////////////////////////// extract facts ///////////////////////////////////
        // more examples can be found in the FactExtractionTest.java > testDetectTableFacts()
        String url = "http://en.wikipedia.org/wiki/Nokia_N95";

        HashSet<Attribute> seedAttributes = new HashSet<Attribute>();
        // seedAttributes.add(new Attribute("Second camera", Attribute.VALUE_STRING, c));
        // seedAttributes.add(new Attribute("Memory card", Attribute.VALUE_STRING, c));
        // seedAttributes.add(new Attribute("Form factor", Attribute.VALUE_STRING, c));
        ArrayList<Fact> detectedFacts = null;
        detectedFacts = FactExtractor.extractFacts(url, seedAttributes);

        CollectionHelper.print(detectedFacts);
        System.exit(0);
        // //////////////////////////////////////////////////////////////////////////////////////////

        // KnowledgeManager dm = new KnowledgeManager();
        // dm.createBenchmarkConcepts();
        // FactExtractor.getInstance().setKnowledgeManager(dm);
        // FactExtractor.getInstance().startExtraction();

        // /////////////////////////////// test image extraction ///////////////////////////////////
        // // single concepts
        // Concept concept = new Concept("Movie");
        // Attribute attribute = new Attribute("poster", Attribute.VALUE_IMAGE, concept);
        // concept.addAttribute(attribute);
        // Entity entity = new Entity("The Dark Knight",concept);
        // Concept concept = new Concept("Movie");
        // Attribute attribute = new Attribute("scene", Attribute.VALUE_IMAGE, concept);
        // attribute.setValueCount(4);
        // concept.addAttribute(attribute);
        // Entity entity = new Entity("idiocracy",concept);
        // Concept concept = new Concept("Country");
        // Attribute attribute = new Attribute("flag", Attribute.VALUE_IMAGE, concept);
        // concept.addAttribute(attribute);
        // Entity entity = new Entity("Australia",concept);
        // Concept concept = new Concept("Movie");
        // Attribute attribute = new Attribute("poster", Attribute.VALUE_IMAGE, concept);
        // concept.addAttribute(attribute);
        // Entity entity = new Entity("Braveheart",concept);

        Concept concept = new Concept("Movie");
        Attribute attribute = new Attribute("_entity_image_", Attribute.VALUE_IMAGE, concept);
        concept.addAttribute(attribute);
        // Entity entity = new Entity("Nokia N95", concept);

        // FactQuery fq = FactQueryFactory.getInstance().createImageQuery(entity, attribute);
        // FactExtractor.getInstance().extractImages(fq, entity);

        // // test set
        // KnowledgeManager dm = new KnowledgeManager();
        // dm.createBenchmarkConcepts(true);
        // FactExtractor.getInstance().setKnowledgeManager(dm);
        //		
        // ArrayList<Concept> concepts = dm.getConcepts();
        //		
        // // iterate through all concepts
        // Iterator<Concept> conceptIterator = concepts.iterator();
        // while (conceptIterator.hasNext()) {
        // Concept currentConcept = conceptIterator.next();
        // ArrayList<Entity> conceptEntities = currentConcept.getEntities();
        // HashSet<Attribute> currentAttributes = currentConcept.getAttributes();
        //			
        // Iterator<Entity> entityIterator = conceptEntities.iterator();
        // while (entityIterator.hasNext()) {
        // Entity currentEntity = entityIterator.next();
        //				
        // Iterator<Attribute> attributeIterator = currentAttributes.iterator();
        // while (attributeIterator.hasNext()) {
        // Attribute currentAttribute = attributeIterator.next();
        // if (currentAttribute.getValueType() == Attribute.VALUE_IMAGE) {
        // FactQuery fq = FactQueryFactory.getInstance().createImageQuery(currentEntity, currentAttribute);
        // FactExtractor.getInstance().extractImages(fq, currentEntity);
        // }
        // }
        // }
        // }

        System.out.println("stopped");
        // /////////////////////////////////////////////////////////////////////////////////////////

        // Entity e = new Entity("Germany",KnowledgeManager.getInstance().getDomains().get(0));
        // FactQuery fq = FactQueryFactory.getInstance().createFactQueryForSingleAttribute(e, new Attribute("population",Attribute.VALUE_NUMBER),
        // FactQueryFactory.TYPE_Y_OF_X_IS);

        // test single attribute
        // Entity e = new Entity("Mount Everest",KnowledgeManager.getInstance().getDomains().get(0));
        // FactQuery fq = FactQueryFactory.getInstance().createFactQueryForSingleAttribute(e, new Attribute("height",Attribute.VALUE_NUMBER),
        // FactQueryFactory.TYPE_Y_OF_X_IS);

        // test multiple attributes
        // Entity e = new Entity("Australia",KnowledgeManager.getInstance().getDomains().get(0));
        // FactQuery fq = FactQueryFactory.getInstance().createFactQueryForManyAttributes(e, FactQueryFactory.TYPE_X);
        //		
        // System.out.println(fq);
        // FactExtractor.getInstance().extractFromFactPages(fq);
    }
}