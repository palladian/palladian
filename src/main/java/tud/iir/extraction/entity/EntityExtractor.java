package tud.iir.extraction.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.extraction.ExtractionType;
import tud.iir.extraction.Extractor;
import tud.iir.gui.GUIManager;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.helper.WordNet;
import tud.iir.helper.WordTransformer;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.knowledge.Source;
import tud.iir.knowledge.Sources;
import tud.iir.persistence.DatabaseManager;
import tud.iir.persistence.IndexManager;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;

/**
 * The main class for the entity extraction. Here all three entity extraction techniques are triggered and called with the concept names.
 * 
 * @author David Urbansky
 */
public class EntityExtractor extends Extractor {

    /** the instance of the entity extractor */
    private static EntityExtractor INSTANCE = new EntityExtractor();

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(EntityExtractor.class);

    /** private HashMap<String,String[]> concepts; */
    private ArrayList<Concept> concepts;

    /** limit the number of entity extractions, stop if limit has been reached, -1 means no limit */
    private int extractionLimit = -1;

    /** If true, bulks of extractions are saved automatically to knowledge base. */
    boolean autoSave = true;

    // private HashSet<Pattern> patternCandidates;
    private int currentNumberOfExtractions;
    // private int extractionSaveNumber = 0;

    /**
     * In order to save the extraction status we keep track of the extraction technique, the concepts and the URL stack.
     */
    private EntityExtractionStatus currentExtractionStatus = null;

    /** pattern string like /html/body/div/div/div/div/ul/li/ul/li/a with array of indexes that can change like 1,4,6 (index 1,4 and 6 can change) */
    // private HashMap<String, HashSet<Integer>> patterns;

    private EntityExtractor() {
        currentNumberOfExtractions = 0;
        currentExtractionStatus = new EntityExtractionStatus();
        
        // do not analyze any binary files
        addSuffixesToBlackList(Extractor.URL_BINARY_BLACKLIST);
        //addSuffixesToBlackList(new String[]{"dat"});
        
        if (GUIManager.isInstanciated()) {
            // instance.logger.addObserver(GUIManager.getInstance());
        }
    }

    public static EntityExtractor getInstance() {
        return INSTANCE;
    }

    public void startExtraction(boolean phrase, boolean focusedCrawl, boolean seeds) {
        startExtraction(phrase, focusedCrawl, seeds, true);
    }

    public void startExtraction(boolean phrase, boolean focusedCrawl, boolean seeds, boolean continueFromLastExtraction) {

        LOGGER.info("start entity extraction");

        // reset stopped command
        setStopped(false);

        // loop until exit called
        // while (!isStopped()) {

        // load concepts and attributes from ontology (and rdb) to know what to extract
        if (!isBenchmark()) {
            setKnowledgeManager(DatabaseManager.getInstance().loadOntology());
        }

        // get concepts and sort them by lastSearched (oldest and null first)
        concepts = getKnowledgeManager().getConcepts(continueFromLastExtraction);

        for (int i = 0; i < concepts.size(); i++) {
            LOGGER.debug(concepts.get(i).getName() + " " + concepts.get(i).getLastSearched());
        }

        if (continueFromLastExtraction) {
            if (FileHelper.fileExists("data/status/entityExtractionStatus.ser")) {
                EntityExtractionStatus extractionStatus = (EntityExtractionStatus) FileHelper.deserialize("data/status/entityExtractionStatus.ser");
                if (extractionStatus != null) {
                    currentExtractionStatus = extractionStatus;
                    currentExtractionStatus.setLoaded(true);
                    currentExtractionStatus.setInitialized(false);
                    LOGGER.info("loaded extraction status " + currentExtractionStatus);
                }
            }
        }

        // different entity extraction processes
        if (phrase
                && !isStopped()
                && (continueFromLastExtraction && currentExtractionStatus.getExtractionType() == ExtractionType.ENTITY_PHRASE || !continueFromLastExtraction || !currentExtractionStatus
                        .isLoaded())) {
            LOGGER.info("starting/continuing extraction from phrases");
            extractionFromPhrase();
        }
        if (focusedCrawl
                && !isStopped()
                && (continueFromLastExtraction && currentExtractionStatus.getExtractionType() == ExtractionType.ENTITY_FOCUSED_CRAWL
                        || !continueFromLastExtraction || !currentExtractionStatus.isLoaded())) {
            LOGGER.info("starting/continuing extraction with focused crawling");
            extractionFocusedCrawl();
        }
        if (seeds
                && !isStopped()
                && (continueFromLastExtraction && currentExtractionStatus.getExtractionType() == ExtractionType.ENTITY_SEED || !continueFromLastExtraction || !currentExtractionStatus
                        .isLoaded())) {
            LOGGER.info("starting/continuing extraction with seeds");
            extractionSeeds();
        }

        if (isBenchmark()) {

            // normalizeAllEntities();
            stopExtraction(false);
            getLogger().info(
                    "SAVE EXTRACTIONS | " + " | memory usage (free:" + Runtime.getRuntime().freeMemory() + ",total:" + Runtime.getRuntime().totalMemory()
                            + ",max:" + Runtime.getRuntime().maxMemory() + ")");
            getKnowledgeManager().saveExtractions();
            LOGGER.debug("STOPPED");

        } else {
            // stop logging and save logs
            LOGGER.debug("---------------STOPPED HERE---------------");
            LOGGER.debug("------------------------------------------");
            LOGGER.debug("Total Extractions:");

            for (Concept entry4 : knowledgeManager.getConcepts()) {

                LOGGER.info("\n\n---- Extractions for the Concept " + entry4.getName() + " ----");

                ArrayList<Entity> entityList = entry4.getEntitiesByTrust();
                LOGGER.debug(entityList.toString());
                Collections.sort(entityList, new EntityTrustComparator());
                int s = entityList.size();
                for (int e = 0; e < s; ++e) {
                    Entity entry3 = entityList.get(e);
                    if (entry3.isInitial()) {
                        continue;
                    }
                    LOGGER.debug(entry3.getName() + " : " + entry3.getExtractionCount());
                    LOGGER.info(entry3.getName() + " : " + entry3.getExtractionCount());
                }

                /*
                 * Iterator<Entity> it3 = entry4.getEntities(true).iterator(); while (it3.hasNext()) { Entity entry3 = it3.next(); if (entry3.isInitial())
                 * continue; logger.debug(entry3.getName()+" : "+entry3.getCorroboration()); logger.info(entry3.getName()+" : "+entry3.getCorroboration()); }
                 */

            }

            // create log document
            LOGGER.info(SourceRetrieverManager.getInstance().getLogs());

            // save extraction results after each full loop
            // getKnowledgeManager().saveExtractions();
        }

        // }

    }

    /**
     * Use simple generic patterns to extract entities from unstructured text.
     */
    public void extractionFromPhrase() {
        LOGGER.info("start entity extraction from phrases");

        PhraseExtractor pe = new PhraseExtractor();
        extract(pe);
    }

    /**
     * Focused crawl extraction.
     */
    public void extractionFocusedCrawl() {
        LOGGER.info("start entity extraction with focused crawling");

        FocusedCrawlExtractor fce = new FocusedCrawlExtractor(this);
        extract(fce);
    }

    /**
     * Extraction with seeds.
     */
    public void extractionSeeds() {
        LOGGER.info("start entity extraction with seeds");

        SeedExtractor se = new SeedExtractor(this);
        extract(se);
    }

    /**
     * All entity extraction techniques use this method which handles threads, persistence management, querying, iterating through concepts and synonyms.
     * 
     * @param entityExtractionTechnique The entity extraction technique that should be used for a retrieved URL.
     */
    @SuppressWarnings("unchecked")
    public void extract(EntityExtractionTechnique entityExtractionTechnique) {

        // set the extraction technique that is currently used to save it in the entity extraction status
        currentExtractionStatus.setExtractionType(entityExtractionTechnique.getExtractionTechnique());

        Integer[] patterns = entityExtractionTechnique.getPatterns();
        int patternCount = patterns.length;

        // source retriever settings
        SourceRetriever sr = new SourceRetriever();
        sr.setSource(ExtractionProcessManager.getSourceRetrievalSite());
        sr.setResultCount(ExtractionProcessManager.getSourceRetrievalCount());
        // sr.setResultCount(2);

        extractionThreadGroup = new ThreadGroup("entityExtractionThreadGroup");

        // if we want to continue we need to jump to the concept we stopped at last time, that is, we need to put it to the very first index
        List<Concept> conceptCopies = (ArrayList<Concept>) concepts.clone();
        if (currentExtractionStatus.isLoaded() && !currentExtractionStatus.isInitialized()) {
            for (int i = 0; i < conceptCopies.size(); i++) {
                Concept currentConcept = conceptCopies.get(i);
                if (currentConcept.getName().equalsIgnoreCase(currentExtractionStatus.getCurrentConcept())) {
                    Concept c = concepts.get(0);
                    concepts.set(0, concepts.get(i));
                    concepts.set(i, c);
                    break;
                }
            }
        }
        conceptCopies = null;

        // extract entities for all concepts
        for (Concept currentConcept : concepts) {

            if (isStopped()) {
                break;
            }

            LOGGER.info("setting lastSearched for concept: " + currentConcept.getName());

            // update live status action
            ExtractionProcessManager.liveStatus.setCurrentAction("Use " + entityExtractionTechnique.getName()
                    + " to extract entities for the concept " + currentConcept.getName());

            // save the current concept in the extraction status
            currentExtractionStatus.setCurrentConcept(currentConcept.getName());

            // get synonyms for domain name from WordNet and use original name also
            String[] synonyms;
            if (ExtractionProcessManager.isUseConceptSynonyms()) {
                synonyms = WordNet.getSynonyms(currentConcept.getName(), 3, true);

                // show all synonyms
                for (int a = 0; a < synonyms.length; ++a) {
                    LOGGER.info("Synonyms: " + synonyms[a]);
                }

            } else {
                synonyms = new String[1];
                synonyms[0] = currentConcept.getName();
            }

            // try every synonym
            int synonymCount = synonyms.length;
            for (int s = 0; s < synonymCount; ++s) {

                if (isStopped()) {
                    break;
                }

                String currentConceptSynonym = synonyms[s];

                // try every pattern
                for (int i = 0; i < patternCount; ++i) {

                    if (isStopped()) {
                        break;
                    }

                    // if we want to continue we need to jump to the pattern we stopped at last time
                    if (currentExtractionStatus.isLoaded() && !currentExtractionStatus.isInitialized()) {
                        if (i != currentExtractionStatus.getPatternNumber()) {
                            continue;
                        }
                    }

                    currentExtractionStatus.setPatternNumber(i);

                    EntityQuery eq = entityExtractionTechnique.getEntityQuery(new Concept(currentConceptSynonym), patterns[i]);

                    // get all URLs for all query strings
                    HashSet<String> urls = new HashSet<String>();
                    String[] querySet = eq.getQuerySet();

                    int querySetSize = querySet.length;
                    for (int j = 0; j < querySetSize; ++j) {
                        String queryTerm = querySet[j];
                        if (queryTerm != null) {
                            boolean exact = true;
                            if (eq.getRetrievalExtractionType() == EntityQueryFactory.RETRIEVAL_EXTRACTION_TYPE_SEED) {
                                exact = false;
                            }
                            List<String> retrievedURLs = sr.getURLs(querySet[j], exact);
                            urls.addAll(retrievedURLs);
                        }
                    }

                    LOGGER.info(urls.size() + " urls have been received");

                    // if we want to continue we need to use the URL stack from last time
                    if (currentExtractionStatus.isLoaded() && !currentExtractionStatus.isInitialized()) {
                        urls = currentExtractionStatus.getUrlStack();
                        currentExtractionStatus.setInitialized(true);
                    }

                    // apply current pattern phrase on each retrieved URL
                    HashSet<String> visitedURLs = new HashSet<String>();
                    for (String currentURL : urls) {

                        if (isStopped()) {
                            break;
                        }

                        LOGGER.info("processing page " + currentURL + " (retrieved with query: \"" + StringHelper.getArrayAsString(eq.getQuerySet()) + "\")");

                        if (!isURLallowed(currentURL)) {
                            continue;
                        }

                        // start extraction thread
                        EntityExtractionThread entityExtractionThread = new EntityExtractionThread(extractionThreadGroup, "EntityExtractionThread_"
                                + currentURL, entityExtractionTechnique, eq, currentConcept, currentURL);
                        entityExtractionThread.start();
                        // entityExtractionThread.run();

                        LOGGER.info("THREAD STARTED (" + getThreadCount() + "): " + currentURL);
                        // logger.debug("THREAD STARTED ("+getThreadCount()+"): "+currentURL);

                        int c = 0;
                        while (getThreadCount() >= MAX_EXTRACTION_THREADS) {
                            LOGGER.info("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + ") " + c + "/25, " + extractionThreadGroup.activeCount()
                                    + "," + extractionThreadGroup.activeGroupCount());
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
                        // if (isStopped()) return;

                        // remove URL from stack
                        visitedURLs.add(currentURL);
                    }

                    // save the URL stack in the extraction status
                    urls.removeAll(visitedURLs);
                    currentExtractionStatus.setUrlStack(urls);
                } // iterating through patterns

            } // iterating through synonyms

            // if not stopped but still the entities for the concept have been searched, we can finish it and set a lastSearchedDate
            if (!isStopped()) {
                currentConcept.setLastSearched(new Date(System.currentTimeMillis()));
            }

        } // iterating through concepts

        LOGGER.info("finishing extract method, using another extraction technique in the next run");

        currentExtractionStatus.nextExtractionType();

        // save the concepts in the extraction status
        // TODO ? currentExtractionStatus.setConcepts(concepts);
    }

    public ArrayList<Entity> getExtractions() {
        ArrayList<Entity> extractions = new ArrayList<Entity>();
        Iterator<Concept> conceptIterator = getKnowledgeManager().getConcepts().iterator();
        while (conceptIterator.hasNext()) {
            extractions.addAll(conceptIterator.next().getEntities());
        }
        return extractions;
    }

    public void printExtractions() {
        LOGGER.info("\n###############################################################");
        LOGGER.info("Extractions (" + getExtractions().size() + " unique):");
        Iterator<Entity> entityIterator = getExtractions().iterator();
        while (entityIterator.hasNext()) {
            Entity entry = entityIterator.next();
            LOGGER.info(entry.getName() + " (" + entry.getConcept().getName() + ") : " + entry.getSources().size());
        }
        LOGGER.info(SourceRetrieverManager.getInstance().getLogs());
        // logger.saveLogs("NEE_extractions_notSaved"+extractionSaveNumber+".txt");
    }

    @Override
    protected synchronized void saveExtractions(boolean saveResults) {
        if (saveResults && !isBenchmark()) {
            LOGGER.debug("save extractions now");
            getKnowledgeManager().saveExtractions();
            saveExtractionStatus();
        }
    }

    public void createBenchmarkIndex() {

        ExtractionProcessManager.setBenchmarkType(ExtractionProcessManager.BENCHMARK_ENTITY_EXTRACTION);
        ExtractionProcessManager.setBenchmarkSet(ExtractionProcessManager.GOOGLE_8);

        SourceRetriever sr = new SourceRetriever();
        sr.setSource(SourceRetrieverManager.GOOGLE);
        sr.setResultCount(8);

        Crawler crawler = new Crawler();

        // download and index urls for all concepts
        int counter = 1;
        Iterator<Concept> conceptIterator = getConcepts().iterator();
        while (conceptIterator.hasNext()) {
            Concept currentConcept = conceptIterator.next();
            List<String> retrievedURLs = sr.getURLs(
                    "list of "
                    + WordTransformer.wordToPlural(currentConcept.getName()), SourceRetrieverManager.GOOGLE, true);

            for (int i = 0; i < retrievedURLs.size(); i++) {
                String url = retrievedURLs.get(i);
                String resultID = "listof" + currentConcept.getName().toLowerCase().replaceAll(" ", "");
                String path = IndexManager.getInstance().getIndexPath() + "/";
                String filename = "website" + counter + ".html";

                if (crawler.downloadAndSave(url, path + filename)) {
                    IndexManager.getInstance().writeIndex(filename, url, resultID);
                    LOGGER.info("download and index " + url + " resultID " + resultID);
                } else {
                    LOGGER.info("error when downloading " + url);
                }

                ++counter;
            }

        }
    }

    /**
     * Save the status of the extraction to continue later on.
     */
    private void saveExtractionStatus() {
        FileHelper.serialize(currentExtractionStatus, "data/status/entityExtractionStatus.ser");
    }

    /*
     * public void setBenchmark(boolean benchmark) { setBenchmark(benchmark, DatabaseManager.getInstance().loadOntology()); }
     */

    public boolean isAutoSave() {
        return autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public ArrayList<Concept> getConcepts() {
        if (concepts == null) {
            return getKnowledgeManager().getConcepts();
        }
        return concepts;
    }

    public void setConcepts(ArrayList<Concept> concepts) {
        this.concepts = concepts;
    }

    public void normalizeAllEntities() {
        Iterator<Concept> conceptIterator = getKnowledgeManager().getConcepts().iterator();
        while (conceptIterator.hasNext()) {
            Iterator<Entity> entityIterator = conceptIterator.next().getEntities().iterator();
            while (entityIterator.hasNext()) {
                entityIterator.next().normalizeName();
            }
        }
    }

    public synchronized void addExtraction(Entity newEntity) {

        // logger.info("enter add extraction method");

        // do not enter empty entities
        if (StringHelper.trim(newEntity.getName()).length() == 0) {
            return;
        }

        ArrayList<Concept> concepts = getConcepts();

        // synchronized(concepts) {

        Concept concept = newEntity.getConcept();
        concept.addEntity(newEntity);
        currentNumberOfExtractions++;
        if (currentNumberOfExtractions >= 1000 && isAutoSave()) {
            LOGGER.info("SAVE EXTRACTIONS | timestamp " + System.currentTimeMillis() + " | memory usage (free:" + Runtime.getRuntime().freeMemory() + ",total:"
                    + Runtime.getRuntime().totalMemory() + ",max:" + Runtime.getRuntime().maxMemory() + ")");
            getKnowledgeManager().saveExtractions();

            for (Concept c : concepts) {
                c.clearEntities();
            }

            currentNumberOfExtractions = 0;
            // getLogger().log("SAVED EXTRACTIONS | memory usage (free:"+Runtime.getRuntime().freeMemory()+",max:"+Runtime.getRuntime().maxMemory()+",total:"+Runtime.getRuntime().totalMemory()+")");
        }

        // check if extraction limit has been reached
        if (currentNumberOfExtractions > getExtractionLimit() && getExtractionLimit() != -1) {
            // if (currentNumberOfExtractions > getExtractionLimit()) {
            getLogger().info("Extraction limit has been reached (" + getExtractionLimit() + "), stopping entity extraction process.");
            stopExtraction(true);
        }
        // }
    }

    public int getExtractionLimit() {
        return extractionLimit;
    }

    public void setExtractionLimit(int extractionLimit) {
        this.extractionLimit = extractionLimit;
    }

    public static void main(String[] a) {
        // KnowledgeManager km = DatabaseManager.getInstance().loadOntology();
        // km.getConcept("Nation").setLastSearched(new Date(System.currentTimeMillis()));
        // // get concepts and sort them by lastSearched (oldest and null first)
        // ArrayList<Concept> concepts = km.getConcepts(true);
        //		
        // for (int i = 0; i < concepts.size(); i++) {
        // logger.debug(concepts.get(i).getName()+" "+concepts.get(i).getLastSearched() + " " + concepts.get(i).getSynonymsToString());
        // }

        // create benchmark index
        // KnowledgeManager km = new KnowledgeManager();
        // km.createBenchmarkConcepts();
        // EntityExtractor.getInstance().setKnowledgeManager(km);
        // EntityExtractor.getInstance().setConcepts(km.getConcepts(false));
        // EntityExtractor.getInstance().createBenchmarkIndex();

        // test entity extractions
        // EntityExtractor ee = EntityExtractor.getInstance();
        // Entity newEntity = new Entity("E1",new Concept("C1"));
        // ee.getExtractions().addExtraction(newEntity, EntityQueryFactory.RETRIEVAL_EXTRACTION_TYPE_PHRASE, 4);
        //		
        // newEntity = new Entity("E1",new Concept("C1"));
        // ee.getExtractions().addExtraction(newEntity, EntityQueryFactory.RETRIEVAL_EXTRACTION_TYPE_PHRASE, 4);
        //		
        // newEntity = new Entity("E2",new Concept("C1"));
        // ee.getExtractions().addExtraction(newEntity, EntityQueryFactory.RETRIEVAL_EXTRACTION_TYPE_PHRASE, 4);
        //		
        // newEntity = new Entity("E1",new Concept("C1"));
        // ee.getExtractions().addExtraction(newEntity, EntityQueryFactory.RETRIEVAL_EXTRACTION_TYPE_PHRASE, 2);
        //		
        // newEntity = new Entity("E2",new Concept("C2"));
        // ee.getExtractions().addExtraction(newEntity, EntityQueryFactory.RETRIEVAL_EXTRACTION_TYPE_PHRASE, 4);
        //		
        // EntityExtractions entityExtractions = ee.getExtractions();
        // Iterator<Entry<EntityExtraction, Integer>> i = entityExtractions.getExtractions(true).entrySet().iterator();
        // while (i.hasNext()) {
        // Entry<EntityExtraction, Integer> entry = i.next();
        // logger.debug(entry.getKey().getEntity().getName()+" ("+entry.getKey().getEntity().getConcept().getName()+") : "+entry.getValue()+" ("+entry.getKey().printRetrievalExtractionAndQueryTypes()+")");
        // }

        Sources<Source> hs = new Sources<Source>();
        Source s1 = new Source("www1", 1);
        Source s2 = new Source("www2", 1);
        hs.add(s1);
        LOGGER.debug(s1.equals(s2));
        LOGGER.debug(hs.contains(s2));

        Entity e1 = new Entity("E1", null);
        e1.addSource(new Source("www1", EntityQueryFactory.TYPE_BROWSE_XP));
        Entity e2 = new Entity("E1", null);
        e2.addSource(new Source("www2", EntityQueryFactory.TYPE_BROWSE_XP));
        Entity e3 = new Entity("E1", null);
        e3.addSource(new Source("www1", EntityQueryFactory.TYPE_BROWSE_XP));

        Concept c1 = new Concept("ABC");
        c1.addEntity(e1);
        c1.addEntity(e2);
        c1.addEntity(e3);

        Iterator<Entity> eI = c1.getEntities().iterator();
        while (eI.hasNext()) {
            Entity e = eI.next();
            LOGGER.debug(e.getName());
            Iterator<Source> sI = e.getSources().iterator();
            while (sI.hasNext()) {
                Source s = sI.next();
                LOGGER.debug(" " + s.getUrl() + " " + s.getExtractionType());
            }
        }

        EntityExtractor ee = EntityExtractor.getInstance();
        ee.setKnowledgeManager(new KnowledgeManager());
        ee.getKnowledgeManager().addConcept(new Concept("Country", ee.getKnowledgeManager()));
        Entity newEntity = new Entity("E1", new Concept("Country", ee.getKnowledgeManager()));
        newEntity.addSource(new Source("url1", 4));
        ee.getKnowledgeManager().getConcept("Country").addEntity(newEntity);

        newEntity = new Entity("E1", new Concept("Country", ee.getKnowledgeManager()));
        newEntity.addSource(new Source("url1", 3));
        ee.getKnowledgeManager().getConcept("Country").addEntity(newEntity);

        newEntity = new Entity("E2", new Concept("Country", ee.getKnowledgeManager()));
        newEntity.addSource(new Source("url2", 4));
        ee.getKnowledgeManager().getConcept("Country").addEntity(newEntity);

        newEntity = new Entity("E1", new Concept("Country", ee.getKnowledgeManager()));
        newEntity.addSource(new Source("url2", 4));
        ee.getKnowledgeManager().getConcept("Country").addEntity(newEntity);

        newEntity = new Entity("E2", new Concept("Country", ee.getKnowledgeManager()));
        newEntity.addSource(new Source("url3", 4));
        ee.getKnowledgeManager().getConcept("Country").addEntity(newEntity);

        ArrayList<Entity> entities = ee.getKnowledgeManager().getConcept("Country").getEntities();
        Iterator<Entity> i = entities.iterator();
        while (i.hasNext()) {
            Entity entry = i.next();
            LOGGER.debug(entry.getName() + " (" + entry.getConcept().getName() + ") : " + entry.getSources().size());
        }

    }

}