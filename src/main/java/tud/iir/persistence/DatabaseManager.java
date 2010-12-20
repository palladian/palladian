package tud.iir.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.extraction.LiveStatus;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Extractable;
import tud.iir.knowledge.Fact;
import tud.iir.knowledge.FactValue;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.knowledge.QA;
import tud.iir.knowledge.Snippet;
import tud.iir.knowledge.Source;

/**
 * The DatabaseManager writes and reads data to the database.
 * 
 * @author David Urbansky
 * @author Christopher Friedrich
 * @author Philipp Katz
 * @author Martin Werner
 */
public class DatabaseManager {

    /** the instance of the DatabaseManager */
    private final static DatabaseManager INSTANCE = new DatabaseManager();

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);

    /** the configuration file can be found under config/db.conf */
    private static PropertiesConfiguration config;

    private Connection connection;
    private PreparedStatement psConceptCheck;
    private PreparedStatement psConceptSynonymCheck;
    private PreparedStatement psAttributeSynonymCheck;
    private PreparedStatement psAttributeConceptCheck;
    private PreparedStatement psGetLastSearchedConcept;
    private PreparedStatement psGetLastSearchedAttribute;
    private PreparedStatement psGetLastSearchedEntity;
    private PreparedStatement psInsertConcept;
    private PreparedStatement psUpdateConcept;
    private PreparedStatement psAttributeCheck;
    private PreparedStatement psInsertAttribute;
    private PreparedStatement psUpdateAttribute;
    private PreparedStatement psEntityCheck;
    private PreparedStatement psGetEntityName;
    private PreparedStatement psInsertEntity;
    private PreparedStatement psUpdateEntity;
    private PreparedStatement psLoadEntities1;
    private PreparedStatement psLoadEntities2;
    private PreparedStatement psLoadEntity;
    private PreparedStatement psDeleteEmptyEntities;
    private PreparedStatement psGetEntityIDsByName;
    private PreparedStatement psGetAttributeID;
    private PreparedStatement psGetAttributeExtractedAt;
    private PreparedStatement psFactCheck;
    private PreparedStatement psInsertFact;
    private PreparedStatement psSnippetCheck;
    private PreparedStatement psInsertSnippet;
    private PreparedStatement psAttributeSourceCheck;
    private PreparedStatement psInsertAttributeSource;
    private PreparedStatement psGetEntitySources;
    private PreparedStatement psEntitySourceCheck;
    private PreparedStatement psInsertEntitySource;
    private PreparedStatement psFactSourceCheck;
    private PreparedStatement psInsertFactSource;
    private PreparedStatement psSourceCheck;
    private PreparedStatement psInsertSource;
    private PreparedStatement psGetSourceURL;
    private PreparedStatement psGetSeeds;
    private PreparedStatement psLastInsertID;
    private PreparedStatement psInsertAssessmentInstance;
    private PreparedStatement psGetEntitiesForSource;
    private PreparedStatement psGetEntitiesForExtractionType;
    private PreparedStatement psGetExtractionTypesForSource;
    private PreparedStatement psGetSourcesForExtractionType;
    private PreparedStatement psGetPMI;
    private PreparedStatement psGetConcepts;
    private PreparedStatement psGetConceptSynonyms;

    private PreparedStatement psSetTestField;

    // question and answers
    private PreparedStatement psAddQ;
    private PreparedStatement psAddA;

    // status
    private PreparedStatement psUpdateExtractionStatus;
    private PreparedStatement psCheckCleanExtractionStatus;
    private PreparedStatement psCleanExtractionStatus;
    private PreparedStatement psGetExtractionStatusDownloadedBytes;


    // TODO in entities domainID instead of conceptID (or no cascade or best new table that connects an entity with all
    // synonyms) because deleting a
    // concept (one synonym) might lead to deletion of all entities for that concept
    /**
     * Instantiates a new database manager.
     */
    private DatabaseManager() {
        try {
            config = new PropertiesConfiguration("config/db.conf");
            config.setThrowExceptionOnMissing(true);
        } catch (ConfigurationException e) {
            // LOGGER.error(e.getMessage());
            // Attention: using the "eager" singleton idiom we must not use the class logger, as it has not yet been
            // instantiated at this point; use the RootLogger instead.
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    /**
     * Gets the single instance of DatabaseManager.
     * 
     * @return single instance of DatabaseManager
     */
    public static DatabaseManager getInstance() {
        try {
            if (INSTANCE.connection == null || INSTANCE.connection != null && INSTANCE.connection.isClosed()) {
                INSTANCE.establishConnection();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage());
        }
        return INSTANCE;
    }

    /**
     * Load DB driver, establish DB connection, prepare statements.
     * 
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private void establishConnection() throws SQLException, ClassNotFoundException {

        Class.forName(config.getString("db.driver"));
        String url = "jdbc:" + config.getString("db.type") + "://" + config.getString("db.host") + ":"
                + config.getString("db.port") + "/" + config.getString("db.name");
        url += "?useServerPrepStmts=false&cachePrepStmts=false";
        connection = DriverManager.getConnection(url, config.getString("db.username"), config.getString("db.password"));

        psLastInsertID = connection.prepareStatement("SELECT LAST_INSERT_ID()");
        // only get the "master" concepts, not the synonyms -- Philipp, 2010-07-02
        // psGetConcepts = connection.prepareStatement("SELECT * FROM `concepts`");
        psGetConcepts = connection
                .prepareStatement("SELECT * FROM `concepts` WHERE id NOT IN (SELECT conceptID2 FROM concept_synonyms)");
        psGetConceptSynonyms = connection
                .prepareStatement("SELECT * FROM `concepts`, `concept_synonyms` WHERE conceptID1 = ? AND conceptID2 = concepts.id");

        psConceptCheck = connection.prepareStatement("SELECT id FROM `concepts` WHERE name = ?");
        psConceptSynonymCheck = connection
                .prepareStatement("SELECT id FROM concept_synonyms WHERE (conceptID1 = ? AND conceptID2 = ?) OR (conceptID1 = ? AND conceptID2 = ?)");
        psAttributeSynonymCheck = connection
                .prepareStatement("SELECT id FROM attribute_synonyms WHERE (attributeID1 = ? AND attributeID2 = ?) OR (attributeID1 = ? AND attributeID2 = ?)");

        psAttributeConceptCheck = connection
                .prepareStatement("SELECT id FROM attributes_concepts WHERE attributeID = ? AND conceptID = ?");

        psGetLastSearchedConcept = connection.prepareStatement("SELECT lastSearched FROM concepts WHERE name = ?");
        psGetLastSearchedAttribute = connection.prepareStatement("SELECT lastSearched FROM attributes WHERE name = ?");
        psGetLastSearchedEntity = connection.prepareStatement("SELECT lastSearched FROM entities WHERE name = ?");
        psInsertConcept = connection.prepareStatement("INSERT INTO `concepts` SET name = ?, lastSearched = ?");
        psUpdateConcept = connection.prepareStatement("UPDATE `concepts` SET lastSearched = ? WHERE id = ?");

        psAttributeCheck = connection.prepareStatement("SELECT id FROM `attributes` WHERE name = ?");
        psInsertAttribute = connection
                .prepareStatement("INSERT INTO `attributes` SET name = ?,trust = ?, lastSearched = ?");
        psUpdateAttribute = connection.prepareStatement("UPDATE `attributes` SET lastSearched = ? WHERE id = ?");

        psEntityCheck = connection.prepareStatement("SELECT id FROM `entities` WHERE name = ? AND conceptID = ?");
        psGetEntityName = connection.prepareStatement("SELECT name FROM `entities` WHERE id = ?");
        psInsertEntity = connection
                .prepareStatement("INSERT INTO `entities` SET name = ?,trust = ?,conceptID = ?,lastSearched = ?");
        psUpdateEntity = connection.prepareStatement("UPDATE `entities` SET lastSearched = ? WHERE id = ?");
        psLoadEntities1 = connection
                .prepareStatement("SELECT id,name,lastSearched FROM `entities` WHERE conceptID = ? ORDER BY name ASC LIMIT ?,?");
        psLoadEntities2 = connection
                .prepareStatement("SELECT id,name,lastSearched FROM `entities` WHERE conceptID = ? ORDER BY lastSearched ASC LIMIT ?,?");
        psLoadEntity = connection
                .prepareStatement("SELECT `entities`.name AS entityName,`entities`.lastSearched,`concepts`.name AS conceptName FROM `entities`,`concepts` WHERE `entities`.conceptID = `concepts`.id AND `entities`.id = ?");
        psDeleteEmptyEntities = connection.prepareStatement("DELETE FROM entities WHERE LENGTH(TRIM(name)) = 0");

        psGetEntityIDsByName = connection.prepareStatement("SELECT id FROM `entities` WHERE name = ?");
        psGetAttributeID = connection.prepareStatement("SELECT id FROM `attributes` WHERE name = ?");
        psGetAttributeExtractedAt = connection.prepareStatement("SELECT extractedAt FROM `attributes` WHERE name = ?");

        psFactCheck = connection
                .prepareStatement("SELECT id FROM `facts` WHERE entityID = ? AND attributeID = ? AND value = ?");
        psInsertFact = connection
                .prepareStatement("INSERT INTO `facts` SET entityID = ?,attributeID = ?,value = ?,trust = ?");

        psSnippetCheck = connection.prepareStatement("SELECT id FROM `snippets` WHERE entityID = ? AND text = ?");
        psInsertSnippet = connection
                .prepareStatement("INSERT INTO `snippets` SET entityID = ?, sourceID = ?, text = ?");

        psAttributeSourceCheck = connection
                .prepareStatement("SELECT id FROM `attributes_sources` WHERE attributeID = ? AND sourceID = ?");
        psInsertAttributeSource = connection
                .prepareStatement("INSERT INTO `attributes_sources` SET attributeID = ?,sourceID = ?,extractionType = ?");
        psGetEntitySources = connection
                .prepareStatement("SELECT sources.id,url,extractionType FROM `entities`,`entities_sources`,`sources` WHERE `entities`.id = `entities_sources`.entityID AND `entities_sources`.sourceID = `sources`.id  AND `entities`.id = ?");
        psEntitySourceCheck = connection
                .prepareStatement("SELECT id FROM `entities_sources` WHERE entityID = ? AND sourceID = ? AND extractionType = ?");
        psInsertEntitySource = connection
                .prepareStatement("INSERT INTO `entities_sources` SET entityID = ?,sourceID = ?,extractionType = ?");
        psFactSourceCheck = connection
                .prepareStatement("SELECT id FROM `facts_sources` WHERE factID = ? AND sourceID = ?");
        psInsertFactSource = connection
                .prepareStatement("INSERT INTO `facts_sources` SET factID = ?,sourceID = ?,extractionType = ?");
        psSourceCheck = connection.prepareStatement("SELECT id FROM `sources` WHERE url = ?");
        psInsertSource = connection.prepareStatement("INSERT INTO `sources` SET url = ?");
        psGetSourceURL = connection.prepareStatement("SELECT url FROM `sources` WHERE id = ?");

        psInsertAssessmentInstance = connection
                .prepareStatement("INSERT INTO training_samples SET conceptID = ?, entityID = ?, class = ?");

        psGetSeeds = connection
                .prepareStatement("SELECT entities.name FROM `entities`,`entities_sources` WHERE `entities`.conceptID = ? AND `entities`.id = `entities_sources`.entityID GROUP BY entityID ORDER BY COUNT(entityID) DESC LIMIT 0,2000");
        psGetEntitiesForSource = connection
                .prepareStatement("SELECT training_samples.entityID FROM `training_samples`,`entities_sources` WHERE training_samples.entityID = entities_sources.entityID AND `entities_sources`.sourceID = ?");
        psGetEntitiesForExtractionType = connection
                .prepareStatement("SELECT training_samples.entityID FROM `training_samples`,`entities_sources` WHERE training_samples.entityID = entities_sources.entityID AND `entities_sources`.extractionType = ? AND training_samples.conceptID = ?");
        psGetExtractionTypesForSource = connection
                .prepareStatement("SELECT extractionType FROM `training_samples`,`entities_sources` WHERE training_samples.entityID = entities_sources.entityID AND `entities_sources`.sourceID = ? AND training_samples.conceptID = ?");
        psGetSourcesForExtractionType = connection
                .prepareStatement("SELECT entities_sources.sourceID FROM `training_samples`,`entities_sources` WHERE training_samples.entityID = entities_sources.entityID AND `entities_sources`.extractionType = ? AND training_samples.conceptID = ?");

        // psSetEntityTrust = connection.prepareStatement("UPDATE entities SET trust = ? WHERE id = ?");

        psGetPMI = connection.prepareStatement("SELECT value FROM facts WHERE entityID = ? AND attributeID = ?");

        psSetTestField = connection.prepareStatement("UPDATE training_samples SET test = ? WHERE entityID = ?");

        // question and answers
        psAddQ = connection.prepareStatement("INSERT INTO `questions` SET sourceID = ?, question = ?");
        psAddA = connection.prepareStatement("INSERT INTO `answers` SET answer = ?, questionID = ?");

        // status
        psUpdateExtractionStatus = connection
                .prepareStatement("INSERT INTO live_status SET percent = ?, timeLeft= ?, currentPhase = ?, currentAction = ?, logExcerpt = ?, moreText1 = ?, moreText2 = ?, downloadedBytes = ?, updatedAt = NOW()");
        psCheckCleanExtractionStatus = connection.prepareStatement("SELECT COUNT(id) FROM live_status");
        psCleanExtractionStatus = connection.prepareStatement("TRUNCATE live_status");
        psGetExtractionStatusDownloadedBytes = connection
                .prepareStatement("SELECT downloadedBytes FROM live_status WHERE id IN (SELECT MAX(id) FROM live_status)");

    }

    /**
     * Return the connection.
     * 
     * @return
     */
    public Connection getConnection() {
        return connection;
    }
    /**
     * Update ontology.
     */
    public void updateOntology() {
        updateOntology("");
    }

    /**
     * Write the concepts and their attributes (defined in the ontology) in the database.
     * 
     * @param filePath the file path
     */
    public void updateOntology(String filePath) {

        KnowledgeManager knowledgeManager;

        // load the ontology in the KnowledgeManager
        if (filePath.equals("")) {
            knowledgeManager = OntologyManager.getInstance().loadOntology();
        } else {
            knowledgeManager = OntologyManager.getInstance().loadOntologyFile(filePath);
        }

        // enter concept and attribute names in the database
        ArrayList<Concept> concepts = knowledgeManager.getConcepts();
        Iterator<Concept> conceptIterator = concepts.iterator();
        while (conceptIterator.hasNext()) {
            Concept concept = conceptIterator.next();

            // enter concept and its synonyms
            int conceptID = addConcept(concept);
            HashSet<String> conceptSynonyms = concept.getSynonyms();
            Iterator<String> conceptSynonymsIterator = conceptSynonyms.iterator();
            while (conceptSynonymsIterator.hasNext()) {
                String conceptSynonym = conceptSynonymsIterator.next();
                Concept synonymConcept = new Concept(conceptSynonym, knowledgeManager);
                int conceptID2 = addConcept(synonymConcept);
                addConceptSynonym(conceptID, conceptID2);
            }

            Iterator<Attribute> attributesIterator = concept.getAttributes(false).iterator();
            while (attributesIterator.hasNext()) {
                Attribute attribute = attributesIterator.next();
                // String attributeName = attribute.getName();
                // double attributeTrust = attribute.getTrust();

                // enter attribute and its synonyms
                int attributeID = addAttribute(attribute, conceptID);
                HashSet<String> attributeSynonyms = attribute.getSynonyms();
                Iterator<String> attributeSynonymsIterator = attributeSynonyms.iterator();
                while (attributeSynonymsIterator.hasNext()) {
                    String attributeSynonym = attributeSynonymsIterator.next();
                    attribute.setName(attributeSynonym);
                    int attributeID2 = addAttribute(attribute, conceptID);
                    addAttributeSynonym(attributeID, attributeID2, 1.0);
                }
            }
        }
    }

    /**
     * Update ontology.
     * 
     * @param knowledgeManager the knowledge manager
     */
    public void updateOntology(KnowledgeManager knowledgeManager) {

        // enter concept and attribute names in the database
        ArrayList<Concept> concepts = knowledgeManager.getConcepts();
        Iterator<Concept> conceptIterator = concepts.iterator();
        while (conceptIterator.hasNext()) {
            Concept concept = conceptIterator.next();

            // enter concept and its synonyms
            int conceptID = addConcept(concept);
            HashSet<String> conceptSynonyms = concept.getSynonyms();
            Iterator<String> conceptSynonymsIterator = conceptSynonyms.iterator();
            while (conceptSynonymsIterator.hasNext()) {
                String conceptSynonym = conceptSynonymsIterator.next();
                Concept synonymConcept = new Concept(conceptSynonym, knowledgeManager);
                int conceptID2 = addConcept(synonymConcept);
                addConceptSynonym(conceptID, conceptID2);
            }

            Iterator<Attribute> attributesIterator = concept.getAttributes(false).iterator();
            while (attributesIterator.hasNext()) {
                Attribute attribute = attributesIterator.next();
                // String attributeName = attribute.getName();
                // double attributeTrust = attribute.getTrust();

                // enter attribute and its synonyms
                // System.out.println("adding attribute: " + attribute.getName());
                int attributeID = addAttribute(attribute, conceptID);
                HashSet<String> attributeSynonyms = attribute.getSynonyms();
                Iterator<String> attributeSynonymsIterator = attributeSynonyms.iterator();
                while (attributeSynonymsIterator.hasNext()) {
                    String attributeSynonym = attributeSynonymsIterator.next();
                    attribute.setName(attributeSynonym);
                    int attributeID2 = addAttribute(attribute, conceptID);
                    addAttributeSynonym(attributeID, attributeID2, 1.0);
                }
            }
        }
    }

    /**
     * Load ontology.
     * 
     * @return the knowledge manager
     */
    public KnowledgeManager loadOntology() {
        return loadOntology("");
    }

    /**
     * Load the ontology saved in the database into the KnowledgeManager. Update the ontology for the database first
     * from the owl ontology.
     * 
     * @param filePath the file path
     * @return the knowledge manager
     */
    public KnowledgeManager loadOntology(String filePath) {
        updateOntology(filePath);
        KnowledgeManager knowledgeManager;

        if (filePath.equals("")) {
            knowledgeManager = OntologyManager.getInstance().loadOntology();
        } else {
            knowledgeManager = OntologyManager.getInstance().loadOntologyFile(filePath);
        }

        // add lastSearched and extractedAt information from DB to concepts and attributes
        ArrayList<Concept> concepts = knowledgeManager.getConcepts();
        Iterator<Concept> conceptIterator = concepts.iterator();
        while (conceptIterator.hasNext()) {

            Concept concept = conceptIterator.next();
            String dateString = getLastSearched(concept.getName(), "concepts");

            java.util.Date lastSearched = null;
            if (dateString != null) {
                lastSearched = new java.util.Date(Timestamp.valueOf(dateString).getTime());
            }
            concept.setLastSearched(lastSearched);
            concept.setID(getConceptID(concept.getName()));
            // System.out.println("loaded concept with " + concept.getName() + "lastSearched " +
            // concept.getLastSearched());

            Iterator<Attribute> attributesIterator = concept.getAttributes(false).iterator();
            while (attributesIterator.hasNext()) {
                Attribute attribute = attributesIterator.next();
                lastSearched = null;
                java.util.Date extractedAt = null;
                int id = getAttributeID(attribute.getName());
                dateString = getLastSearched(attribute.getName(), "attributes");
                if (dateString != null) {
                    lastSearched = new java.util.Date(Timestamp.valueOf(dateString).getTime());
                }
                dateString = getAttributeExtractedAt(attribute.getName());
                if (dateString != null) {
                    extractedAt = new java.util.Date(Timestamp.valueOf(dateString).getTime());
                }
                attribute.setID(id);
                attribute.setLastSearched(lastSearched);
                attribute.setExtractedAt(extractedAt);
            }
        }

        ArrayList<Concept> concepts1 = knowledgeManager.getConcepts();
        Iterator<Concept> conceptIterator2 = concepts1.iterator();
        while (conceptIterator2.hasNext()) {
            Concept concept = conceptIterator2.next();
            LOGGER.info("concept: " + concept.getName() + " " + concept.getLastSearched());
            // Iterator<Entity> entityIterator = concept.getEntities().iterator();
            // while (entityIterator.hasNext()) {
            // Entity entity = entityIterator.next();
            // System.out.println("  entity: "+entity.getLastSearched());
            // }
            /*
             * Iterator<Attribute> attributesIterator = concept.getAttributes().iterator(); while
             * (attributesIterator.hasNext()) {
             * System.out.println("  attribute: "+attributesIterator.next().getName()); }
             */
        }

        return knowledgeManager;
    }

    // TODO load sources for each entity as well / done but not tested
    /**
     * Load entities (names and lastSearched only) for a specific concept.
     * 
     * @param concept the concept
     * @param number Number of Entities to return.
     * @param offset An offset value.
     * @param continueFromLastExtraction the continue from last extraction
     * @return An array of entities.
     */
    public ArrayList<Entity> loadEntities(Concept concept, int number, int offset, boolean continueFromLastExtraction) {
        ArrayList<Entity> entities = new ArrayList<Entity>();

        // get id of concept
        int conceptID = getConceptID(concept.getName());

        ResultSet rs = null;
        try {
            if (continueFromLastExtraction) {
                psLoadEntities2.setInt(1, conceptID);
                psLoadEntities2.setInt(2, offset);
                psLoadEntities2.setInt(3, number);
                rs = runQuery(psLoadEntities2);
            } else {
                psLoadEntities1.setInt(1, conceptID);
                psLoadEntities1.setInt(2, offset);
                psLoadEntities1.setInt(3, number);
                rs = runQuery(psLoadEntities1);
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        try {
            while (rs.next()) {
                String entityName = rs.getString("name");
                String dateString = rs.getString("lastSearched");
                int entityID = rs.getInt("id");
                java.util.Date lastSearched = null;
                if (dateString != null) {
                    lastSearched = new java.util.Date(Timestamp.valueOf(dateString).getTime());
                }
                Entity e = new Entity(entityName, concept);
                e.setID(entityID);
                e.setLastSearched(lastSearched);

                entities.add(e);
            }
            rs.close();

            // get and add sources
            Iterator<Entity> entitiesIterator = entities.iterator();
            while (entitiesIterator.hasNext()) {
                Entity e = entitiesIterator.next();
                psGetEntitySources.setInt(1, e.getID());
                rs = runQuery(psGetEntitySources);
                while (rs.next()) {
                    Source s = new Source(rs.getString("url"), rs.getInt("extractionType"));
                    e.addSource(s);
                }
                rs.close();
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return entities;
    }

    /**
     * Load concepts.
     * 
     * @return the array list
     */
    public ArrayList<Concept> loadConcepts() {

        ArrayList<Concept> concepts = new ArrayList<Concept>();

        ResultSet rs = null;

        rs = runQuery(psGetConcepts);
        try {
            while (rs.next()) {
                int id = rs.getInt("id");
                String conceptName = rs.getString("name");
                Concept concept = new Concept(conceptName);
                concept.setID(id);
                concept.setSynonyms(loadConceptSynonyms(concept));
                concepts.add(concept);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return concepts;
    }

    private HashSet<String> loadConceptSynonyms(Concept concept) {
        HashSet<String> result = new HashSet<String>();
        try {
            psGetConceptSynonyms.setInt(1, concept.getID());
            ResultSet rs = runQuery(psGetConceptSynonyms);
            while (rs.next()) {
                String synonymName = rs.getString("name");
                result.add(synonymName);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return result;
    }

    /**
     * Load evaluation entities.
     * 
     * @param concept the concept
     * @return the array list
     */
    public ArrayList<Entity> loadEvaluationEntities(Concept concept) {
        ArrayList<Entity> entities = new ArrayList<Entity>();

        int conceptID = getConceptID(concept.getName());
        String query = "SELECT entityID,class,test FROM `training_samples` WHERE conceptID = " + conceptID
                + " ORDER BY RAND()";

        ResultSet rs = runQuery(query);
        try {
            while (rs.next()) {
                Entity e = loadEntity(rs.getInt(1));
                e.setTrust(Math.floor(rs.getInt(2) + 0.5));
                if (rs.getInt(3) == 1) {
                    e.setType(Extractable.TESTING);
                    // System.out.println("1 add testing entity " + e.getName() + " | " + e.getTrust());
                } else {
                    e.setType(Extractable.TRAINING);
                    // System.out.println("2 add training entity " + e.getName() + " | " + e.getTrust());
                }
                entities.add(e);
            }
            rs.close();

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return entities;
    }

    /**
     * Load entity.
     * 
     * @param entityID the entity id
     * @return the entity
     */
    public Entity loadEntity(int entityID) {
        Entity entity = null;

        try {
            psLoadEntity.setInt(1, entityID);
        } catch (SQLException e1) {
            LOGGER.error(e1.getMessage());
        }

        ResultSet rs = runQuery(psLoadEntity);
        try {
            while (rs.next()) {
                String entityName = rs.getString("entityName");
                String dateString = rs.getString("lastSearched");
                java.util.Date lastSearched = null;
                if (dateString != null) {
                    lastSearched = new java.util.Date(Timestamp.valueOf(dateString).getTime());
                }
                entity = new Entity(entityName, new Concept(rs.getString("conceptName")));
                entity.setID(entityID);
                entity.setLastSearched(lastSearched);
            }
            rs.close();

            // get and add sources
            psGetEntitySources.setInt(1, entityID);
            rs = runQuery(psGetEntitySources);
            while (rs.next()) {
                Source s = new Source(rs.getString("url"), rs.getInt("extractionType"));
                s.setID(rs.getInt(1));
                entity.addSource(s);
            }
            rs.close();

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return entity;
    }

    /**
     * Save instance knowledge (entities, their facts(also MIOs), their snippets and their sources). If entries exist,
     * link them.
     * 
     * @param knowledgeManager The knowledgeManager.
     */
    public void saveExtractions(KnowledgeManager knowledgeManager) {

        long t1 = System.currentTimeMillis();

        // FactValue fv = new FactValue("Canberra",new
        // Source(Source.SEMI_STRUCTURED,"www.www.com"),ExtractionType.COLON_PHRASE);
        // System.out.println(knowledgeManager.getConcept("Country").getEntity("Australia"));
        // knowledgeManager.getConcept("Country")
        // .getEntity("Australia")
        // .addFactAndValue(new Fact(new Attribute("Capital",Attribute.VALUE_STRING)), fv);

        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        ArrayList<Concept> concepts = knowledgeManager.getConcepts();
        // synchronized(concepts) {

        for (Concept concept : concepts) {
            // int conceptID = getConceptID(concept.getName());

            int conceptID = addConcept(concept);

            // save all concept's synonyms
            HashSet<String> synonyms = concept.getSynonyms();
            for (String synonym : synonyms) {
                int synonymID = addConcept(new Concept(synonym));
                addConceptSynonym(conceptID, synonymID);
            }

            for (Entity entity : concept.getEntities()) {                

                // do not enter entities with a too short name
                entity.normalizeName();
                if (entity.getName().length() <= 1) {
                    continue;
                }
                
                // add entity to database
                int entityID = addEntity(entity, conceptID);

                // add source(s) for that entity
                for (Source entitySource : entity.getSources()) {
                    addEntitySource(entitySource, entityID);
                }

                // add snippets for that entity
                for (Snippet snippet : entity.getSnippets()) {
                    addSnippet(snippet);
                }

                for (Fact fact : entity.getFacts()) {

                    Attribute attribute = fact.getAttribute();

                    // add attribute to database
                    int attributeID = addAttribute(attribute, conceptID);

                    // add source(s) for that attribute
                    for (Source attributeSource : attribute.getSources()) {
                        addAttributeSource(attributeSource, attributeID);
                    }

                    // special case for saving mios
                    if (attribute.getName().contains("mio")) {

                        List<FactValue> factValues = fact.getValues();
                        for (FactValue factValue : factValues) {

                            int factID = addFact(factValue, entityID, attributeID, factValue.getTrust());
                          
                            // error occurred, continue
                            if (factID == -1) {
                                continue;
                            }

                            for (Source factSource : factValue.getSources()) {
                                addFactSource(factSource, factID);
                            }
                        }

                    } else {

                        // add first three fact values with highest trust or more if value count is higher
                        ArrayList<FactValue> highTrustFactValues = fact.getValues(true,
                                Math.max(3, fact.getAttribute().getValueCount()));
                        for (int i = 0, l = highTrustFactValues.size(); i < l; i++) {
                            FactValue highTrustFV = highTrustFactValues.get(i);
                            int factID = addFact(highTrustFV, entityID, attributeID);

                            // error occurred, continue
                            if (factID == -1) {
                                continue;
                            }

                            // add source(s) for that value
                            for (Source fvSource : highTrustFV.getSources()) {
                                addFactSource(fvSource, factID);
                            }
                        }
                    }
                }
            }
        }

        // }

        // batch update (better performance)
        try {

            // delete empty entities
            runUpdate(psDeleteEmptyEntities);

            if (!connection.getAutoCommit()) {
                connection.commit();
                connection.setAutoCommit(true);
            }
            long ms = System.currentTimeMillis() - t1;

            // System.out.println(t2 - t1);
            LOGGER.debug("saved extractions in " + ms + " ms");

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Gets the seeds.
     * 
     * @param concept the concept
     * @param maxNumber the max number
     * @return the seeds
     */
    public ArrayList<String> getSeeds(Concept concept, int maxNumber) {
        ArrayList<String> seeds = new ArrayList<String>();

        int conceptID = getConceptID(concept.getName());
        try {
            psGetSeeds.setInt(1, conceptID);
            ResultSet rs = runQuery(psGetSeeds);
            while (rs.next() && seeds.size() < maxNumber) {
                seeds.add(rs.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return seeds;
    }

    /**
     * Gets the pMI.
     * 
     * @param entityID the entity id
     * @param attributeID the attribute id
     * @return the pMI
     */
    public double getPMI(int entityID, int attributeID) {
        double pmi = 0;
        try {
            psGetPMI.setInt(1, entityID);
            psGetPMI.setInt(2, attributeID);
            ResultSet rs = runQuery(psGetPMI);
            if (rs.next()) {
                pmi = rs.getDouble(1);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return pmi;
    }

    /**
     * Gets the benchmark pm is.
     * 
     * @return the benchmark pm is
     */
    public HashMap<String, Double> getBenchmarkPMIs() {
        HashMap<String, Double> benchmarkPMIs = new HashMap<String, Double>();

        ResultSet rs = runQuery("SELECT entityID, attributeID, value FROM facts WHERE id > 0 AND id <= 100000");
        try {
            while (rs.next()) {
                benchmarkPMIs.put(rs.getString(1) + rs.getString(2), rs.getDouble(3));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return benchmarkPMIs;
    }

    /**
     * *************************************************************************************************
     * Reading and writing from the database.
     * *************************************************************************************************.
     */
    
    /**
     * Check whether extraction status must be cleaned.
     */
    public void checkCleanExtractionStatus() {
        ResultSet rs = runQuery(psCheckCleanExtractionStatus);
        try {
            if (rs.next()) {
                int rowCount = rs.getInt(1);

                // if there are more than 100 lines in the live status, delete all of them
                if (rowCount > 100) {
                    runUpdate(psCleanExtractionStatus);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("could not get row count of live status");
        }
    }

    /**
     * @param extractionPhase The extraction phase.
     * @param progress The progress.
     * @param logExcerpt The log excerpt.
     * @param downloadedBytes The downloaded bytes.
     */

    public void updateExtractionStatus(LiveStatus liveStatus) {
        try {
            psUpdateExtractionStatus.setDouble(1, liveStatus.getPercent());
            psUpdateExtractionStatus.setString(2, liveStatus.getTimeLeft());
            psUpdateExtractionStatus.setString(3, liveStatus.getCurrentPhase());
            psUpdateExtractionStatus.setString(4, liveStatus.getCurrentAction());
            psUpdateExtractionStatus.setString(5, liveStatus.getLogExcerpt());
            psUpdateExtractionStatus.setString(6, liveStatus.getMoreText1());
            psUpdateExtractionStatus.setString(7, liveStatus.getMoreText2());
            psUpdateExtractionStatus.setLong(8, liveStatus.getDownloadedBytes());
            runUpdate(psUpdateExtractionStatus);
        } catch (SQLException e) {
            LOGGER.error("could not update live status, " + e.getMessage());
        }
    }

    /**
     * Gets the extraction status downloaded bytes.
     * 
     * @return the extraction status downloaded bytes
     */
    public long getExtractionStatusDownloadedBytes() {
        try {
            ResultSet rs = runQuery(psGetExtractionStatusDownloadedBytes);
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOGGER.error("could not get the number of downloaded bytes, " + e.getMessage());
        }
        return 0;
    }

    /**
     * Set the test field in training_samples database for a certain entity.
     * 
     * @param entityID the entity id
     * @param test the test
     */
    public void setTestField(int entityID, boolean test) {
        try {
            if (test) {
                psSetTestField.setInt(1, 1);
            } else {
                psSetTestField.setInt(1, 0);
            }
            psSetTestField.setInt(2, entityID);
            runUpdate(psSetTestField);

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Gets the entities for source.
     * 
     * @param sourceID the source id
     * @return the entities for source
     */
    public HashSet<Integer> getEntitiesForSource(int sourceID) {
        HashSet<Integer> entityIDs = new HashSet<Integer>();
        try {
            psGetEntitiesForSource.setInt(1, sourceID);
            ResultSet rs = runQuery(psGetEntitiesForSource);
            while (rs.next()) {
                entityIDs.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return entityIDs;
    }

    /**
     * Gets the entities for extraction type.
     * 
     * @param extractionType the extraction type
     * @param concept the concept
     * @return the entities for extraction type
     */
    public HashSet<Integer> getEntitiesForExtractionType(int extractionType, Concept concept) {
        HashSet<Integer> entityIDs = new HashSet<Integer>();
        try {
            int conceptID = getConceptID(concept.getName());
            psGetEntitiesForExtractionType.setInt(1, extractionType);
            psGetEntitiesForExtractionType.setInt(2, conceptID);
            ResultSet rs = runQuery(psGetEntitiesForExtractionType);
            while (rs.next()) {
                entityIDs.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return entityIDs;
    }

    /**
     * Gets the extraction types for source.
     * 
     * @param sourceID the source id
     * @param concept the concept
     * @return the extraction types for source
     */
    public HashSet<Integer> getExtractionTypesForSource(int sourceID, Concept concept) {
        HashSet<Integer> extractionTypes = new HashSet<Integer>();
        try {
            int conceptID = getConceptID(concept.getName());
            psGetExtractionTypesForSource.setInt(1, sourceID);
            psGetExtractionTypesForSource.setInt(2, conceptID);
            ResultSet rs = runQuery(psGetExtractionTypesForSource);
            while (rs.next()) {
                extractionTypes.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return extractionTypes;
    }

    /**
     * Gets the sources for extraction type.
     * 
     * @param extractionType the extraction type
     * @param concept the concept
     * @return the sources for extraction type
     */
    public HashSet<Integer> getSourcesForExtractionType(int extractionType, Concept concept) {
        HashSet<Integer> sourceIDs = new HashSet<Integer>();
        try {
            int conceptID = getConceptID(concept.getName());
            psGetSourcesForExtractionType.setInt(1, extractionType);
            psGetSourcesForExtractionType.setInt(2, conceptID);
            ResultSet rs = runQuery(psGetSourcesForExtractionType);
            while (rs.next()) {
                sourceIDs.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return sourceIDs;
    }

    /**
     * Adds the q as.
     * 
     * @param qas the qas
     */
    public void addQAs(List<QA> qas) {
        try {
            for (QA qa : qas) {
                // add source for the question
                int sourceID = 0;
                for (Source qaSource : qa.getSources()) {
                    sourceID = addSource(qaSource);
                }

                psAddQ.setInt(1, sourceID);
                psAddQ.setString(2, qa.getQuestion());
                int questionAdded = runUpdate(psAddQ);

                if (questionAdded > -1) {
                    int questionID = getLastInsertID();

                    // add answers to answers table
                    ArrayList<String> answers = qa.getAnswers();
                    for (String answer : answers) {
                        psAddA.setString(1, answer);
                        psAddA.setInt(2, questionID);
                        runUpdate(psAddA);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Add a concept to the database, if it exists already, update it.
     * 
     * @param concept the concept
     * @return The id of the added concept or of the existing concept.
     */
    private int addConcept(Concept concept) {
        int entryID = -1;
        try {
            psConceptCheck.setString(1, concept.getName());
            entryID = entryExists(psConceptCheck);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        String lastSearched = null;
        if (concept.getLastSearched() != null) {
            lastSearched = new java.sql.Timestamp(concept.getLastSearched().getTime()).toString();
        }
        // insert or update
        if (entryID == -1) {
            // runUpdate("INSERT INTO `concepts` SET name = '" + concept.getName() + "',lastSearched = " +
            // lastSearched);
            try {
                psInsertConcept.setString(1, concept.getName());
                psInsertConcept.setString(2, lastSearched);
                runUpdate(psInsertConcept);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
            return getLastInsertID();
        } else {
            // System.out.println("update concept " + concept.getName() + " to " + concept.getLastSearched());
            if (lastSearched != null) {
                try {
                    psUpdateConcept.setString(1, lastSearched);
                    psUpdateConcept.setInt(2, entryID);
                    runUpdate(psUpdateConcept);
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage());
                }
            }

        }
        return entryID;
    }

    /**
     * Add a concept synonym pair.
     * 
     * @param conceptID1 Concept id 1.
     * @param conceptID2 Concept id 2.
     * @return The id of the added synonym.
     */
    private int addConceptSynonym(int conceptID1, int conceptID2) {

        try {
            psConceptSynonymCheck.setInt(1, conceptID1);
            psConceptSynonymCheck.setInt(2, conceptID2);
            psConceptSynonymCheck.setInt(3, conceptID2);
            psConceptSynonymCheck.setInt(4, conceptID1);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        int entryID = entryExists(psConceptSynonymCheck);
        if (entryID == -1) {
            runUpdate("INSERT INTO `concept_synonyms` SET conceptID1 = " + conceptID1 + ",conceptID2 = " + conceptID2);
            return getLastInsertID();
        }
        return entryID;
    }

    /**
     * Add an attribute for a concept, if it exists already update it.
     * 
     * @param attribute the attribute
     * @param conceptID The id of the concept.
     * @return The id of the added attribute.
     */
    private int addAttribute(Attribute attribute, int conceptID) {
        try {
            psAttributeCheck.setString(1, attribute.getName());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        int entryID = entryExists(psAttributeCheck);
        String lastSearched = null;
        if (attribute.getLastSearched() != null) {
            lastSearched = new java.sql.Timestamp(attribute.getLastSearched().getTime()).toString();
        }

        // insert or update
        if (entryID == -1) {
            try {
                psInsertAttribute.setString(1, attribute.getName());
                psInsertAttribute.setDouble(2, attribute.getTrust());
                psInsertAttribute.setString(3, lastSearched);
                runUpdate(psInsertAttribute);
                int attributeID = getLastInsertID();
                // runUpdate("INSERT INTO `attributes` SET name = ?,trust = " + attribute.getTrust() +
                // ",lastSearched = " + lastSearched +
                // ",extractedAt = " + extractedAt,attribute.getName());
                runUpdate("INSERT INTO `attributes_concepts` SET attributeID = " + attributeID + ",conceptID = "
                        + conceptID);
                return attributeID;
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        } else {

            // check whether the relation exists already
            try {
                psAttributeConceptCheck.setInt(1, entryID);
                psAttributeConceptCheck.setInt(2, conceptID);
            } catch (SQLException e1) {
                LOGGER.error(e1.getMessage());
            }

            int relationID = entryExists(psAttributeConceptCheck);
            if (relationID == -1) {
                runUpdate("INSERT INTO `attributes_concepts` SET attributeID = " + entryID + ",conceptID = "
                        + conceptID);
            }

            try {
                psUpdateAttribute.setString(1, lastSearched);
                psUpdateAttribute.setInt(2, entryID);
                runUpdate(psUpdateAttribute);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
            /*
             * if (lastSearched != null) runUpdate("UPDATE `attributes` SET lastSearched = " + lastSearched +
             * " WHERE id = " + entryID); if (extractedAt !=
             * null) runUpdate("UPDATE `attributes` SET extractedAt = " + extractedAt + " WHERE id = " + entryID);
             */

        }
        return entryID;
    }

    /**
     * Add an attribute synonym pair.
     * 
     * @param attributeID1 Attribute id 1.
     * @param attributeID2 Attribute id 2.
     * @param trust The trust in the connection.
     * @return The id of the added attribute synonym.
     */
    public int addAttributeSynonym(int attributeID1, int attributeID2, double trust) {
        try {
            psAttributeSynonymCheck.setInt(1, attributeID1);
            psAttributeSynonymCheck.setInt(2, attributeID2);
            psAttributeSynonymCheck.setInt(3, attributeID2);
            psAttributeSynonymCheck.setInt(4, attributeID1);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        int entryID = entryExists(psAttributeSynonymCheck);
        if (entryID == -1) {
            runUpdate("INSERT INTO `attribute_synonyms` SET attributeID1 = " + attributeID1 + ",attributeID2 = "
                    + attributeID2 + ",trust = " + trust);
            return getLastInsertID();
        }
        return entryID;
    }

    /**
     * Calculate attribute synonym trust.
     * 
     * @param attribute1 the attribute1
     * @param attribute2 the attribute2
     * @return the double
     */
    public double calculateAttributeSynonymTrust(Attribute attribute1, Attribute attribute2) {

        // get number of matching values between attributes (only first of three valus per fact is taken)
        /*
         * SELECT COUNT(*) FROM (SELECT entityID,attributeID,value,trust FROM `facts` WHERE attributeID = 7 GROUP BY
         * entityID,attributeID) AS A1 INNER JOIN
         * (SELECT entityID,attributeID,value,trust FROM `facts` WHERE attributeID = 2 GROUP BY entityID,attributeID) AS
         * A2 ON A1.value = A2.value AND
         * A1.entityID = A2.entityID
         */

        // get total number of possible matches between attributes (only first of three valus per fact is taken)
        /*
         * SELECT COUNT(*) FROM (SELECT entityID FROM `facts` WHERE attributeID = 7 GROUP BY entityID,attributeID) AS
         * A1,(SELECT entityID FROM `facts` WHERE
         * attributeID = 2 GROUP BY entityID,attributeID) AS A2
         */

        // calculate trust directly on database
        // System.out.println("check SELECT (10000*(SELECT COUNT(*) FROM (SELECT entityID,attributeID,value,trust FROM `facts` WHERE attributeID = "+attribute1.getID()+" GROUP BY entityID,attributeID) AS A1 INNER JOIN (SELECT entityID,attributeID,value,trust FROM `facts` WHERE attributeID = "+attribute2.getID()+" GROUP BY entityID,attributeID) AS A2 ON A1.value = A2.value)/(SELECT COUNT(*) FROM (SELECT entityID FROM `facts` WHERE attributeID = "+attribute1.getID()+" GROUP BY entityID,attributeID) AS A1,(SELECT entityID FROM `facts` WHERE attributeID = "+attribute2.getID()+" GROUP BY entityID,attributeID) AS A2)) AS Trust");
        // //"SELECT ((SELECT COUNT(*) FROM (SELECT entityID,attributeID,value,trust FROM `facts` WHERE attributeID = "+attribute1.getID()+" GROUP BY entityID,attributeID) AS A1 INNER JOIN (SELECT entityID,attributeID,value,trust FROM `facts` WHERE attributeID = "+attribute2.getID()+" GROUP BY entityID,attributeID) AS A2 ON A1.value = A2.value)/(SELECT COUNT(*) FROM (SELECT entityID FROM `facts` WHERE attributeID = "+attribute1.getID()+" GROUP BY entityID,attributeID) AS A1,(SELECT entityID FROM `facts` WHERE attributeID = "+attribute2.getID()+" GROUP BY entityID,attributeID) AS A2)) AS Trust"
        ResultSet rs = runQuery("SELECT ((SELECT COUNT(*) FROM (SELECT entityID,attributeID,value,trust FROM `facts` WHERE attributeID = "
                + attribute1.getID()
                + " GROUP BY entityID,attributeID) AS A1 INNER JOIN (SELECT entityID,attributeID,value,trust FROM `facts` WHERE attributeID = "
                + attribute2.getID()
                + " GROUP BY entityID,attributeID) AS A2 ON A1.value = A2.value AND A1.entityID = A2.entityID)/(SELECT COUNT(*) FROM (SELECT entityID FROM `facts` WHERE attributeID = "
                + attribute1.getID()
                + " GROUP BY entityID,attributeID) AS A1,(SELECT entityID FROM `facts` WHERE attributeID = "
                + attribute2.getID()
                + " GROUP BY entityID,attributeID) AS A2 WHERE A1.entityID = A2.entityID)) AS Trust");

        try {
            if (rs.next()) {
                return rs.getDouble("Trust");
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return 0.0;
    }

    /**
     * Gets the entity name.
     * 
     * @param entityID the entity id
     * @return the entity name
     */
    public String getEntityName(int entityID) {
        try {
            psGetEntityName.setInt(1, entityID);
        } catch (SQLException e1) {
            LOGGER.error(e1.getMessage());
        }
        ResultSet rs = runQuery(psGetEntityName);
        try {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return "unknown";
    }

    /**
     * Gets the entity i ds by name.
     * 
     * @param entityName the entity name
     * @return the entity i ds by name
     */
    public HashSet<Integer> getEntityIDsByName(String entityName) {

        HashSet<Integer> entityIDs = new HashSet<Integer>();

        try {
            psGetEntityIDsByName.setString(1, entityName);
            ResultSet rs = runQuery(psGetEntityIDsByName);
            if (rs.next()) {
                entityIDs.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return entityIDs;
    }

    /**
     * Adds the assessment instance.
     * 
     * @param conceptID the concept id
     * @param entityID the entity id
     * @param classValue the class value
     */
    public void addAssessmentInstance(int conceptID, int entityID, int classValue) {
        try {
            psInsertAssessmentInstance.setInt(1, conceptID);
            psInsertAssessmentInstance.setInt(2, entityID);
            psInsertAssessmentInstance.setInt(3, classValue);
            runUpdate(psInsertAssessmentInstance);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Add an entity to the database, if it exists already, update it.
     * 
     * @param entity the entity
     * @param conceptID The id of the concept of the entity.
     * @return The id of the added entity.
     */
    private int addEntity(Entity entity, int conceptID) {

        int entryID = -1;
        try {
            psEntityCheck.setString(1, entity.getName());
            psEntityCheck.setInt(2, conceptID);

            // entities can belong to different concepts, therefore check name and concept
            entryID = entryExists(psEntityCheck);

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        // int entryID = entryExists("SELECT id FROM `entities` WHERE name = '" + entity.getName() +"' AND conceptID = "
        // + conceptID);

        String lastSearched = null;
        if (entity.getLastSearched() != null) {
            lastSearched = new java.sql.Timestamp(entity.getLastSearched().getTime()).toString();
        }

        // insert or update
        if (entryID == -1) {
            try {
                psInsertEntity.setString(1, entity.getName());
                psInsertEntity.setDouble(2, entity.getTrust());
                psInsertEntity.setInt(3, conceptID);
                psInsertEntity.setString(4, lastSearched);
                // psInsertEntity.addBatch();
                runUpdate(psInsertEntity);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
            return getLastInsertID();
        } else {
            try {
                psUpdateEntity.setString(1, lastSearched);
                psUpdateEntity.setInt(2, entryID);
                // psUpdateEntity.addBatch();
                runUpdate(psUpdateEntity);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        }

        return entryID;
    }

    /**
     * Add a snippet and source to DB.
     * 
     * @param snippet The snippet.
     * @return The id of the added snippet.
     */
    private int addSnippet(Snippet snippet) {

        int entryID = getSnippetID(snippet);

        if (entryID == -1) {

            String url = snippet.getWebResult().getSource().getUrl();
            int sourceID = getSourceID(url);

            if (sourceID == -1) {
                sourceID = addSource(new Source(url));
            }

            try {
                psInsertSnippet.setInt(1, snippet.getEntity().getID());
                psInsertSnippet.setInt(2, sourceID);
                psInsertSnippet.setString(3, snippet.getText());

                if (runUpdate(psInsertSnippet) == -1) {
                    return -1;
                }
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
                return -1;
            }
            return getLastInsertID();
        }
        return entryID;
    }

    /**
     * Add a fact value (the fact in the facts table and the value in the values table).
     * 
     * @param factValue The fact value.
     * @param entityID The entity id.
     * @param attributeID The attribute id.
     * @return The id of the added fact.
     */
    public int addFact(FactValue factValue, int entityID, int attributeID) {

        int entryID = -1;
        try {
            psFactCheck.setInt(1, entityID);
            psFactCheck.setInt(2, attributeID);
            psFactCheck.setString(3, factValue.getValue());
            entryID = entryExists(psFactCheck);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        // int entryID = entryExists("SELECT id FROM `facts` WHERE entityID = " + entityID +" AND attributeID = " +
        // attributeID + " AND value = '" +
        // factValue.getValue() + "'")

        if (entryID == -1) {

            try {
                psInsertFact.setInt(1, entityID);
                psInsertFact.setInt(2, attributeID);
                psInsertFact.setString(3, factValue.getValue());
                psInsertFact.setDouble(4, 1.0);// factValue.getRelativeTrust());

                if (runUpdate(psInsertFact) == -1) {
                    return -1;
                }
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
                return -1;
            }
            return getLastInsertID();
        }
        return entryID;
    }

    /**
     * Add a factValue (especially for MIOs).
     * 
     * @param factValue the fact value
     * @param entityID the entity id
     * @param attributeID the attribute id
     * @param trust the trust
     * @return the int
     */
    public int addFact(FactValue factValue, int entityID, int attributeID, double trust) {

        int entryID = -1;
        try {
            psFactCheck.setInt(1, entityID);
            psFactCheck.setInt(2, attributeID);
            psFactCheck.setString(3, factValue.getValue());
            entryID = entryExists(psFactCheck);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        if (entryID == -1) {

            try {
                psInsertFact.setInt(1, entityID);
                psInsertFact.setInt(2, attributeID);
                psInsertFact.setString(3, factValue.getValue());
                psInsertFact.setDouble(4, trust);

                if (runUpdate(psInsertFact) == -1) {
                    return -1;
                }
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
                return -1;
            }
            return getLastInsertID();
        }
        return entryID;
    }

    /**
     * Add an extraction source.
     * 
     * @param source The source.
     * @param attributeID The attribute id.
     * @return The id of the added source.
     */
    private int addAttributeSource(Source source, int attributeID) {
        try {
            psSourceCheck.setString(1, source.getUrl());
        } catch (SQLException e1) {
            LOGGER.error(e1.getMessage());
        }
        int entryID = entryExists(psSourceCheck);
        int sourceID = -1;

        if (entryID == -1) {
            try {
                psInsertSource.setString(1, source.getUrl());
                runUpdate(psInsertSource);
                sourceID = getLastInsertID();
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        } else {
            sourceID = entryID;
        }

        entryID = -1;
        try {
            psAttributeSourceCheck.setInt(1, attributeID);
            psAttributeSourceCheck.setInt(2, sourceID);
            entryID = entryExists(psAttributeSourceCheck);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        if (entryID == -1) {
            try {
                psInsertAttributeSource.setInt(1, attributeID);
                psInsertAttributeSource.setInt(2, sourceID);
                psInsertAttributeSource.setInt(3, source.getExtractionType());
                // psInsertFactSource.addBatch();
                runUpdate(psInsertAttributeSource);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        }

        return sourceID;
    }

    /**
     * Add an extraction source.
     * 
     * @param source The source.
     * @param factID The fact id.
     * @return The id of the added source.
     */
    private int addFactSource(Source source, int factID) {
        int entryID = -1;
        int sourceID = addSource(source);

        try {
            psFactSourceCheck.setInt(1, factID);
            psFactSourceCheck.setInt(2, sourceID);
            entryID = entryExists(psFactSourceCheck);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        if (entryID == -1) {
            try {
                psInsertFactSource.setInt(1, factID);
                psInsertFactSource.setInt(2, sourceID);
                psInsertFactSource.setInt(3, source.getExtractionType());
                // psInsertFactSource.addBatch();
                runUpdate(psInsertFactSource);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        }

        return sourceID;
    }

    /**
     * Adds the entity source.
     * 
     * @param source the source
     * @param entityID the entity id
     * @return the int
     */
    private int addEntitySource(Source source, int entityID) {
        int entryID = -1;
        int sourceID = addSource(source);

        try {
            psEntitySourceCheck.setInt(1, entityID);
            psEntitySourceCheck.setInt(2, sourceID);
            psEntitySourceCheck.setInt(3, source.getExtractionType());
            entryID = entryExists(psEntitySourceCheck);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        if (entryID == -1) {
            try {
                psInsertEntitySource.setInt(1, entityID);
                psInsertEntitySource.setInt(2, sourceID);
                psInsertEntitySource.setInt(3, source.getExtractionType());
                // psInsertEntitySource.addBatch();
                runUpdate(psInsertEntitySource);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        }

        return sourceID;
    }

    /**
     * Adds the source.
     * 
     * @param source the source
     * @return the int
     */
    private int addSource(Source source) {
        int entryID = -1;
        try {
            psSourceCheck.setString(1, source.getUrl());
            entryID = entryExists(psSourceCheck);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        int sourceID = -1;

        if (entryID == -1) {
            try {
                psInsertSource.setString(1, source.getUrl());
                runUpdate(psInsertSource);
                sourceID = getLastInsertID();
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        } else {
            sourceID = entryID;
        }

        return sourceID;
    }

    /**
     * Gets the source url.
     * 
     * @param sourceID the source id
     * @return the source url
     */
    public String getSourceURL(int sourceID) {
        try {
            psGetSourceURL.setInt(1, sourceID);
        } catch (SQLException e1) {
            LOGGER.error(e1.getMessage());
        }
        ResultSet rs = runQuery(psGetSourceURL);
        try {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return "unknown";
    }

    /**
     * Gets the snippet id.
     * 
     * @param snippet the snippet
     * @return the snippet id
     */
    public int getSnippetID(Snippet snippet) {

        int entryID = -1;

        try {
            psSnippetCheck.setInt(1, snippet.getEntity().getID());
            psSnippetCheck.setString(2, snippet.getText());
            entryID = entryExists(psSnippetCheck);
        } catch (SQLException e1) {
            LOGGER.error(e1.getMessage());
        }

        return entryID;
    }

    /**
     * Snippet exists.
     * 
     * @param snippet the snippet
     * @return true, if successful
     */
    public boolean snippetExists(Snippet snippet) {
        if (getSnippetID(snippet) > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check whether a specified table and field has the entry already.
     * 
     * @param statement the statement
     * @return The id of the existing record or -1 if entry was not found.
     */
    /*
     * private int entryExists_(String entry, String table, String field) { ResultSet rs = runQuery("SELECT id FROM `" +
     * table + "` WHERE " + field + " = ?",
     * entry); try { int id = -1; if (rs.first()) { id = rs.getInt(1); } rs.close(); return id; } catch (SQLException e)
     * { logger.error(e.getMessage()); }
     * return -1; }
     */

    /*
     * private int entryExists_(int id1, int id2, String table, String field1, String field2) { ResultSet rs =
     * runQuery("SELECT id FROM `" + table + "` WHERE ("
     * + field1 + " = " + id1 + " AND " + field2 + " = " + id2 + ") OR (" + field2 + " = " + id1 + " AND " + field1 +
     * " = " + id2 + ")"); try { int id = -1; if
     * (rs.first()) { id = rs.getInt(1); } rs.close(); return id; } catch (SQLException e) {
     * logger.error(e.getMessage()); } catch (NullPointerException e) {
     * logger.error(e.getMessage()); } return -1; }
     */

    private int entryExists(PreparedStatement statement) {
        ResultSet rs = runQuery(statement);
        try {
            if (rs != null) {
                if (rs.first()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
        }
        return -1;
    }

    /**
     * Gets the last insert id.
     * 
     * @return the last insert id
     */
    public int getLastInsertID() {

        int lastInsertID = -1;

        try {
            ResultSet rs = runQuery(psLastInsertID);
            rs.next();
            lastInsertID = Integer.valueOf(rs.getString(1));
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return lastInsertID;
    }

    /**
     * Gets the concept id.
     * 
     * @param conceptName the concept name
     * @return the concept id
     */
    public int getConceptID(String conceptName) {

        try {
            psConceptCheck.setString(1, conceptName);
            ResultSet rs = runQuery(psConceptCheck);
            rs.next();
            return Integer.valueOf(rs.getString(1));
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return -1;
    }

    /**
     * Gets the attribute id.
     * 
     * @param attributeName the attribute name
     * @return the attribute id
     */
    private int getAttributeID(String attributeName) {
        try {
            psGetAttributeID.setString(1, attributeName);
        } catch (SQLException e1) {
            LOGGER.error(e1.getMessage());
        }
        ResultSet rs = runQuery(psGetAttributeID);

        try {
            rs.next();
            return Integer.valueOf(rs.getString(1));
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return -1;
    }

    /**
     * Gets the source id.
     * 
     * @param sourceURL the source url
     * @return the source id
     */
    private int getSourceID(String sourceURL) {
        try {
            psSourceCheck.setString(1, sourceURL);
        } catch (SQLException e1) {
            LOGGER.error(e1.getMessage());
        }
        return entryExists(psSourceCheck);
    }

    /**
     * Gets the attribute extracted at.
     * 
     * @param attributeName the attribute name
     * @return the attribute extracted at
     */
    private String getAttributeExtractedAt(String attributeName) {
        try {
            psGetAttributeExtractedAt.setString(1, attributeName);
        } catch (SQLException e1) {
            LOGGER.error(e1.getMessage());
        }
        ResultSet rs = runQuery(psGetAttributeExtractedAt);

        try {
            rs.next();
            return rs.getString(1);
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return "";
    }

    /**
     * Gets the total concepts number.
     * 
     * @return the total concepts number
     */
    public int getTotalConceptsNumber() {
        int totalConcepts = 0;

        ResultSet rs = runQuery("SELECT COUNT(id) FROM `concepts` WHERE id NOT IN (SELECT conceptID2 FROM `concept_synonyms`)");
        try {
            if (rs.next()) {
                totalConcepts = rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return totalConcepts;
    }

    /**
     * Gets the total attributes number.
     * 
     * @return the total attributes number
     */
    public int getTotalAttributesNumber() {
        int totalAttributes = 0;

        ResultSet rs = runQuery("SELECT COUNT(id) FROM `attributes` WHERE id NOT IN (SELECT attributeID2 FROM `attribute_synonyms`)");
        try {
            if (rs.next()) {
                totalAttributes = rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return totalAttributes;
    }

    /**
     * Gets the total entities number.
     * 
     * @return the total entities number
     */
    public int getTotalEntitiesNumber() {
        return getTotalEntitiesNumber("");
    }

    /**
     * Gets the total entities number.
     * 
     * @param conceptName the concept name
     * @return the total entities number
     */
    public int getTotalEntitiesNumber(String conceptName) {
        int totalEntities = 0;

        String query = "SELECT COUNT(id) FROM `entities`";
        if (conceptName.length() > 0) {
            query += " WHERE conceptID = " + getConceptID(conceptName);
        }
        ResultSet rs = runQuery(query);
        try {
            if (rs.next()) {
                totalEntities = rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return totalEntities;
    }

    /**
     * Total number of facts (only one per entity-attribute).
     * 
     * @return The total number of facts.
     */
    public int getTotalFactsNumber() {
        return getTotalFactsNumber("");
    }

    /**
     * Gets the total facts number.
     * 
     * @param conceptName the concept name
     * @return the total facts number
     */
    public int getTotalFactsNumber(String conceptName) {
        int totalFacts = 0;

        String query = "SELECT COUNT(c) FROM (SELECT COUNT(id) AS c FROM `facts`";
        if (conceptName.length() > 0) {
            query += " WHERE entityID IN (SELECT id FROM `entities` WHERE conceptID = " + getConceptID(conceptName)
                    + ")";
        }
        query += " GROUP BY entityID,attributeID) AS t";

        ResultSet rs = runQuery(query);
        try {
            if (rs.next()) {
                totalFacts = rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return totalFacts;
    }

    /**
     * Gets the total sources number.
     * 
     * @return the total sources number
     */
    public int getTotalSourcesNumber() {
        int totalSources = 0;

        ResultSet rs = runQuery("SELECT COUNT(id) FROM `sources`");
        try {
            if (rs.next()) {
                totalSources = rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return totalSources;
    }

    /**
     * Run query.
     * 
     * @param query the query
     * @return the result set
     */
    public ResultSet runQuery(String query) {
        return runQuery(query, "");
    }

    /**
     * Run query.
     * 
     * @param statement the statement
     * @return the result set
     */
    public ResultSet runQuery(PreparedStatement statement) {
        ResultSet rs = null;

        try {
            rs = statement.executeQuery();
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
        }

        return rs;
    }

    /**
     * Run query.
     * 
     * @param query the query
     * @param text the text
     * @return the result set
     */
    public ResultSet runQuery(String query, String text) {
        ResultSet rs = null;
        PreparedStatement ps = null;
        Statement stmt = null;
        try {
            if (text.length() > 0) {
                // ps = connection.prepareStatement(query,ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
                // ps.setFetchSize(Integer.MIN_VALUE);
                ps = connection.prepareStatement(query);
                ps.setString(1, text);
                rs = ps.executeQuery();
            } else {
                // stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
                // stmt.setFetchSize(Integer.MIN_VALUE);
                stmt = connection.createStatement();
                rs = stmt.executeQuery(query);
            }
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return rs;
    }

    /**
     * Run query.
     * 
     * @param query the query
     * @param texts the texts
     * @return the result set
     */
    public ResultSet runQuery(String query, String[] texts) {
        ResultSet rs = null;

        try {
            if (texts.length > 0) {
                java.sql.PreparedStatement ps = connection.prepareStatement(query);
                for (int i = 1; i <= texts.length; i++) {
                    ps.setString(i, texts[i - 1]);
                }
                rs = ps.executeQuery();
            } else {
                Statement stmt = connection.createStatement();
                rs = stmt.executeQuery(query);
            }
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return rs;
    }

    /**
     * Execute a prepared statement.
     * 
     * @param preparedStatement The prepared statement.
     * @return the int
     */
    public int runUpdate(PreparedStatement preparedStatement) {
        int result = 0;
        try {
            result = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            return -1;
        }
        return result;
    }

    /**
     * Run update.
     * 
     * @param update the update
     * @return the int
     */
    public int runUpdate(String update) {
        return runUpdate(update, "");
    }

    /**
     * Run update.
     * 
     * @param update the update
     * @param text the text
     * @return the int
     */
    public int runUpdate(String update, String text) {
        int result = 0;
        try {
            // Statement stmt = connection.createStatement();
            if (text.length() > 0) {
                java.sql.PreparedStatement ps = connection.prepareStatement(update);
                ps.setString(1, text);
                result = ps.executeUpdate();
                ps.close();
            } else {
                Statement stmt = connection.createStatement();
                result = stmt.executeUpdate(update);
                stmt.close();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return result;
    }

    /**
     * Run update.
     * 
     * @param update the update
     * @param texts the texts
     * @return the int
     */
    public int runUpdate(String update, String[] texts) {
        int result = 0;
        try {
            if (texts.length > 0) {
                java.sql.PreparedStatement ps = connection.prepareStatement(update);
                for (int i = 1; i <= texts.length; i++) {
                    ps.setString(i, texts[i - 1]);
                }
                result = ps.executeUpdate();
                ps.close();
            } else {
                Statement stmt = connection.createStatement();
                result = stmt.executeUpdate(update);
                stmt.close();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return result;
    }

    /**
     * Gets the last searched.
     * 
     * @param name the name
     * @param table the table
     * @return the last searched
     */
    private String getLastSearched(String name, String table) {

        String fieldValue = "";

        try {

            ResultSet rs = null;

            if (table.equalsIgnoreCase("concepts")) {
                psGetLastSearchedConcept.setString(1, name);
                rs = runQuery(psGetLastSearchedConcept);
            } else if (table.equalsIgnoreCase("attributes")) {
                psGetLastSearchedAttribute.setString(1, name);
                rs = runQuery(psGetLastSearchedAttribute);
            } else if (table.equalsIgnoreCase("entities")) {
                psGetLastSearchedEntity.setString(1, name);
                rs = runQuery(psGetLastSearchedEntity);
            }

            rs.next();
            fieldValue = rs.getString(1);
            rs.close();

        } catch (SQLException e) {
            LOGGER.error("could not retrieve lastSearched for concept " + name + ", " + e.getMessage());
        }

        return fieldValue;

    }

    /**
     * *************************************************************************************************
     * interaction
     * *************************************************************************************************.
     */

    /**
     * Deletes all domains, concepts, attributes that are not in the ontology anymore (foreign key cascade). It also
     * deletes all facts etc. that refer to them
     * (trigger / lookup).
     */
    public void cleanUnusedOntologyElements() {
        // HashSet<Integer> deleteDomainIDs = new HashSet<Integer>();
        HashSet<Integer> deleteConceptIDs = new HashSet<Integer>();
        HashSet<Integer> deleteAttributeIDs = new HashSet<Integer>();

        // get all ids from the database first
        try {
            ResultSet rs = runQuery("SELECT id FROM `concepts`");
            while (rs.next()) {
                deleteConceptIDs.add(rs.getInt(1));
            }

            rs = runQuery("SELECT id FROM `attributes`");
            while (rs.next()) {
                deleteAttributeIDs.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        // remove all ids from deleteIDs that are still in the ontology
        KnowledgeManager knowledgeManager = OntologyManager.getInstance().loadOntology();

        ArrayList<Concept> concepts = knowledgeManager.getConcepts();
        Iterator<Concept> conceptIterator = concepts.iterator();
        while (conceptIterator.hasNext()) {
            Concept concept = conceptIterator.next();

            try {
                psConceptCheck.setString(1, concept.getName());
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
            int conceptID = entryExists(psConceptCheck);
            if (conceptID > -1) {
                deleteConceptIDs.remove(conceptID);
            }

            // find ids for concepts and their synonyms
            HashSet<String> conceptSynonyms = concept.getSynonyms();
            Iterator<String> conceptSynonymsIterator = conceptSynonyms.iterator();
            while (conceptSynonymsIterator.hasNext()) {
                String conceptSynonym = conceptSynonymsIterator.next();
                try {
                    psConceptCheck.setString(1, conceptSynonym);
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage());
                }
                conceptID = entryExists(psConceptCheck);
                if (conceptID > -1) {
                    deleteConceptIDs.remove(conceptID);
                }
            }

            Iterator<Attribute> attributesIterator = concept.getAttributes().iterator();
            while (attributesIterator.hasNext()) {
                Attribute attribute = attributesIterator.next();

                try {
                    psAttributeCheck.setString(1, attribute.getName());
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage());
                }
                int attributeID = entryExists(psAttributeCheck);
                if (attributeID > -1) {
                    deleteAttributeIDs.remove(attributeID);
                }

                HashSet<String> attributeSynonyms = attribute.getSynonyms();
                Iterator<String> attributeSynonymsIterator = attributeSynonyms.iterator();
                while (attributeSynonymsIterator.hasNext()) {
                    String attributeSynonym = attributeSynonymsIterator.next();
                    try {
                        psAttributeCheck.setString(1, attributeSynonym);
                    } catch (SQLException e) {
                        LOGGER.error(e.getMessage());
                    }
                    attributeID = entryExists(psAttributeCheck);
                    if (attributeID > -1) {
                        deleteAttributeIDs.remove(attributeID);
                    }
                }
            }
        }

        // remove all elements that have reference to deleteIDs
        // remove concept references
        Iterator<Integer> deleteConceptsIterator = deleteConceptIDs.iterator();
        while (deleteConceptsIterator.hasNext()) {
            int conceptID = deleteConceptsIterator.next();

            // delete from concepts
            runUpdate("DELETE FROM `concepts` WHERE id = " + conceptID);
        }

        // remove attribute references
        Iterator<Integer> deleteAttributesIterator = deleteAttributeIDs.iterator();
        while (deleteAttributesIterator.hasNext()) {
            int attributeID = deleteAttributesIterator.next();

            // delete from attributes
            runUpdate("DELETE FROM `attributes` WHERE id = " + attributeID);
        }

    }

    /**
     * Clear complete database.
     */
    public void clearCompleteDatabase() {
        String query;
        query = "TRUNCATE `attributes`";
        runUpdate(query);
        query = "TRUNCATE `attribute_synonyms`";
        runUpdate(query);
        query = "TRUNCATE `attributes_concepts`";
        runUpdate(query);
        query = "TRUNCATE `attributes_sources`";
        runUpdate(query);
        query = "TRUNCATE `concepts`";
        runUpdate(query);
        query = "TRUNCATE `concept_synonyms`";
        runUpdate(query);
        query = "TRUNCATE `domains`";
        runUpdate(query);
        query = "TRUNCATE `domain_synonyms`";
        runUpdate(query);
        query = "TRUNCATE `entities`";
        runUpdate(query);
        query = "TRUNCATE `facts`";
        runUpdate(query);
        query = "TRUNCATE `sources`";
        runUpdate(query);
        query = "TRUNCATE `facts_sources`";
        runUpdate(query);
        query = "TRUNCATE `entities_sources`";
        runUpdate(query);
        query = "TRUNCATE `questions`";
        runUpdate(query);
        query = "TRUNCATE `answers`";
        runUpdate(query);
    }

    /**
     * Test procedure.
     */
    public void testProcedure() {
        try {
            Statement stmt = connection.createStatement();
            // ResultSet rs = stmt.executeQuery("CALL source_voting_procedure");
            ResultSet rs = stmt.executeQuery("SELECT source_voting_function()");
            if (rs.next()) {
                System.out.println(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("finished");
    }

    /**
     * Sql script to grab the worst performing indexes in the whole server. Source:
     * http://forge.mysql.com/tools/tool.php?id=85
     * 
     */
    public void getWorstIndices() {
        runQuery("SELECT t.TABLE_SCHEMA AS `db`" + ", t.TABLE_NAME AS `table`" + ", s.INDEX_NAME AS `index name`"
                + ", s.COLUMN_NAME AS `field name`" + ", s.SEQ_IN_INDEX `seq in index`"
                + ", s2.max_columns AS `# cols`" + ", s.CARDINALITY AS `card`" + ", t.TABLE_ROWS AS `est rows`"
                + ", ROUND(((s.CARDINALITY / IFNULL(t.TABLE_ROWS, 0.01)) * 100), 2) AS `sel %`"
                + "FROM INFORMATION_SCHEMA.STATISTICS s" + "INNER JOIN INFORMATION_SCHEMA.TABLES t"
                + "ON s.TABLE_SCHEMA = t.TABLE_SCHEMA" + "AND s.TABLE_NAME = t.TABLE_NAME" + "INNER JOIN (" + "SELECT"
                + "TABLE_SCHEMA" + ", TABLE_NAME" + ", INDEX_NAME" + ", MAX(SEQ_IN_INDEX) AS max_columns"
                + "FROM INFORMATION_SCHEMA.STATISTICS" + "WHERE TABLE_SCHEMA != 'mysql'"
                + "GROUP BY TABLE_SCHEMA, TABLE_NAME, INDEX_NAME" + ") AS s2" + "ON s.TABLE_SCHEMA = s2.TABLE_SCHEMA"
                + "AND s.TABLE_NAME = s2.TABLE_NAME" + "AND s.INDEX_NAME = s2.INDEX_NAME"
                + "WHERE t.TABLE_SCHEMA != 'mysql'" + // filter out the mysql system
                // DB
                "AND t.TABLE_ROWS > 10" + // only tables with some rows
                "AND s.CARDINALITY IS NOT NULL" + // need at least one non-NULL value in the field
                "AND (s.CARDINALITY / IFNULL(t.TABLE_ROWS, 0.01)) < 1.00" + // selectivity < 1.0 b/c unique indexes are
                // perfect anyway
                "ORDER BY `sel %`, s.TABLE_SCHEMA, s.TABLE_NAME" + // switch to `sel %` DESC for best non-unique indexes
                "LIMIT 20;");
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {

        // DatabaseManager.getInstance().clearCompleteDatabase();
        //
        // KnowledgeManager km = new KnowledgeManager();
        // Concept c = new Concept("smartphone");
        // c.addSynonym("cell phone");
        // c.addSynonym("cellular phone");
        // c.addSynonym("mobile phone");
        // c.addSynonym("handphone");
        // c.addEntity(new Entity("Apple iPhone"));
        // km.addConcept(c);
        //
        // c = new Concept("car");
        // km.addConcept(c);
        //
        // km.saveExtractions();
        // DatabaseManager.getInstance().updateOntology(km);

        // ArrayList<Concept> concepts = DatabaseManager.getInstance().loadConcepts();
        // for (Concept concept : concepts) {
        // System.out.println(concept);
        // }

        // snippet testing //

        /*
         * Entity entity = new Entity("iPhone 3GS", new Concept("Mobile Phone")); entity.setID(1); WebResult webresult =
         * new
         * WebResult(SourceRetrieverManager.YAHOO, 45, "http://www.slashgear.com/iphone-3gs-reviews-2648062/"); Snippet
         * snippet = new Snippet( entity,
         * webresult,
         * "The iPhone 3GS striking physical similarity to the last-gen 3G came as a mild disappointment back at the WWDC, but we've come to appreciate the stability.  It underscores the evolutionary, rather than revolutionary, nature of this update and, perhaps more importantly, it means accessories acquired for the iPhone 3G will still have a place with the new 3GS."
         * ); SnippetFeatureExtractor.setFeatures(snippet); int snippetID =
         * DatabaseManager.getInstance().addSnippet(snippet); if (snippetID > 0) { try {
         * DatabaseManager.getInstance().psUpdateSnippet.setDouble(1, 3.0);
         * DatabaseManager.getInstance().psUpdateSnippet.setInt(2, snippetID);
         * DatabaseManager.getInstance().runUpdate(DatabaseManager.getInstance().psUpdateSnippet); } catch (SQLException
         * e) { e.getStackTrace(); } }
         */

        // Connection con = DatabaseManager.getInstance().getConnection();
        // System.out.println(DatabaseManager.getInstance().getTotalConceptsNumber());

        //
        // //System.out.println("URL: " + con.getCatalog());
        // System.out.println("Connection: " + con);
        //
        // Statement stmt = con.createStatement();
        //
        // //stmt.executeUpdate("CREATE TABLE t1 (id int,name varchar(255))");
        // //stmt.executeUpdate("INSERT INTO t1 SET name='abc'");
        // stmt.executeUpdate("INSERT INTO `values` SET value='abc'");
        // System.out.println(DatabaseManager.getInstance().getLastInsertID());
        // con.close();
        //
        // DatabaseManager.getInstance().clearCompleteDatabase();
        // domainManager.createBenchmarkConcepts();
        // KnowledgeManager domainManager = new KnowledgeManager();
        // domainManager.createBenchmarkConcepts();
        // DatabaseManager.getInstance().updateOntology();
        // DatabaseManager.getInstance().saveExtractions(domainManager);

        // DatabaseManager.getInstance().loadOntology();

        // try {
        // ArrayList<Entity> al = DatabaseManager.getInstance().loadEntities(new Concept("Country"),2, 2, false);
        // for (int i = 0; i < al.size(); i++) {
        // System.out.println(al.get(i).getName() + al.get(i).getLastSearched());
        // }
        // } catch (Exception e) {
        //
        // e.printStackTrace();
        // }

        // String s = "'a'`'sdfa ";
        // DatabaseManager.getInstance().runUpdate("INSERT INTO t1 set name = ?",s);

        // DatabaseManager.getInstance().runUpdate("INSERT INTO feeds SET url = ?","sdfcxv");

        // DatabaseManager.getInstance().clearCompleteDatabase();
        // DatabaseManager.getInstance().updateOntology();
        // KnowledgeManager dm = DatabaseManager.getInstance().loadOntology();
        // System.out.println("-----------------------------------");
        // ArrayList<Concept> concepts = dm.getConcepts();
        // for (int i = 0; i < concepts.size(); i++) {
        // System.out.println(concepts.get(i).getName()+" syns: "+concepts.get(i).getSynonymsToString());
        // HashSet<Attribute> attributes = concepts.get(i).getAttributes();
        // Iterator<Attribute> ai = attributes.iterator();
        // while (ai.hasNext()) {
        // Attribute a = ai.next();
        // System.out.println(" "+a.getName()+" "+a.getConcept());
        // }
        // }

        // Date lastSearched = new java.sql.Date(new java.util.Date(System.currentTimeMillis()).getTime());
        // System.out.println(lastSearched);
        // java.util.Date d = new java.util.Date(System.currentTimeMillis());
        // Timestamp ds = new java.sql.Timestamp(d.getTime());
        // System.out.println(d+"_"+ds);
        // DatabaseManager.getInstance().runUpdate("INSERT INTO `entities` SET name = 'te',trust = 0.1,conceptID = 4,lastSearched = '"
        // + ds + "'");
        // //
        //
        // Concept concept = new Concept("Actor");
        // String dateString = DatabaseManager.getInstance().getField("lastSearched","concepts","name = '" +
        // concept.getName() + "'");
        //
        // java.util.Date lastSearched = null;
        // if (dateString != null) lastSearched = new java.util.Date(Timestamp.valueOf(dateString).getTime());
        // concept.setLastSearched(lastSearched);
        // System.out.println(lastSearched+" "+concept.getLastSearched());

        // DatabaseManager.getInstance().cleanUnusedOntologyElements();

        // DatabaseManager.getInstance().clearCompleteDatabase();

        // System.out.println(DatabaseManager.getInstance().entryExists("Country", "concepts", "name"));
        // DatabaseManager.getInstance().addConcept("Country");

        // create sample of entities
        // DatabaseManager.getInstance().createEntityFile2();

        /*
         * long t1 = System.currentTimeMillis(); DatabaseManager.getInstance().booleanEntityTrustVoting(); long t2 =
         * System.currentTimeMillis();
         * System.out.println("stopped, runtime: "+((t2-t1)/1000)+" seconds");
         */

        /*
         * long t1 = System.currentTimeMillis(); DatabaseManager.getInstance().gradualEntityTrustVoting(); long t2 =
         * System.currentTimeMillis(); double hours =
         * (t2-t1)/(1000*60*60); System.out.println("stopped, runtime: "+((t2-t1)/1000)+" seconds ("+hours+"h)");
         */

        // DatabaseManager.getInstance().createEntityTrustChart();

        // DatabaseManager.getInstance().findEntityConnection(12,1,0,new HashSet<Integer>(),new ArrayList<String>());
        // DatabaseManager.getInstance().findEntityConnection(6,13,0,new HashSet<Integer>(),new ArrayList<String>());
        // DatabaseManager.getInstance().findEntityConnection(14,2,0,new HashSet<Integer>(),new ArrayList<String>());
        // DatabaseManager.getInstance().findEntityConnection(12,11,0,new HashSet<Integer>(),new ArrayList<String>());

        // DatabaseManager.getInstance().findEntityConnection(154180,151864,0,new HashSet<Integer>(),new
        // ArrayList<String>());
        // DatabaseManager.getInstance().findEntityConnection(153977,151912,0,new HashSet<Integer>(),new
        // ArrayList<String>());
        // DatabaseManager.getInstance().gradualEntityTrustVoting();

        // int r =
        // DatabaseManager.getInstance().runUpdate("UPDATE sources SET entityTrust = 0.5 WHERE id = 1 AND entityTrust != 0.5");
        // System.out.println(r);

        // long t1 = System.currentTimeMillis();
        //
        // // insert performance test
        // DatabaseManager dbm = DatabaseManager.getInstance();
        // dbm.connection.setAutoCommit(false);
        //
        // java.sql.PreparedStatement ps = null;
        // java.sql.PreparedStatement ps2 = null;
        // java.sql.PreparedStatement ps3 = null;
        // try {
        // ps = dbm.connection.prepareStatement("SELECT id FROM `t1` WHERE name = ? AND foreignKeyField = ?");
        // ps2 = dbm.connection.prepareStatement("INSERT INTO t1 SET name = ?, foreignKeyField = ?");
        // ps3 = dbm.connection.prepareStatement("UPDATE t1 SET name = ?, foreignKeyField = ?");
        // } catch (SQLException e) {
        // e.printStackTrace();
        // }
        //
        // for (int i = 0; i < 3000; i++) {
        //
        // int randomNumber = (int) Math.floor((Math.random()*10000));
        //
        // //java.sql.PreparedStatement ps = null;
        // int entryID = -1;
        // try {
        // // ps = dbm.connection.prepareStatement("SELECT id FROM `t1` WHERE name = ? AND foreignKeyField = " +
        // randomNumber);
        // ps.setString(1, "vbydferwer"+randomNumber);
        // ps.setInt(2, randomNumber);
        //
        // // entities can belong to different concepts, therefore check name and concept
        // //entryID = dbm.entryExists(ps);
        //
        // } catch (SQLException e) {
        // e.printStackTrace();
        // }
        //
        // // insert or update
        // if (entryID == -1) {
        // ps2.setString(1, "asdfvcxyv"+randomNumber);
        // ps2.setInt(2, randomNumber);
        // dbm.runUpdate(ps2);
        // //System.out.println(dbm.getLastInsertID());
        // //ps2.addBatch();
        // // dbm.runUpdate("INSERT INTO t1 SET name = '"+"asdfxc"+randomNumber+"', foreignKeyField = " + randomNumber);
        // } else {
        // ps3.setString(1, "hcvhxa"+randomNumber);
        // ps3.setInt(2, randomNumber);
        // dbm.runUpdate(ps3);
        // //ps3.addBatch();
        // // dbm.runUpdate("UPDATE t1 SET name = '"+"asdfxc"+randomNumber+"', foreignKeyField = " + randomNumber);
        // }
        // }
        //
        // //ps2.executeBatch();
        // //ps3.executeBatch();
        // dbm.connection.commit();
        // long t2 = System.currentTimeMillis();
        //
        // System.out.println(t2-t1);
        //
        // // 44 839 with autocommit=yes
        // // 1 877 with autocommit=no
        //
        // // 3000: 3601,2948 with batch
        // // 3000: 6304,5414 without batch
        // // 3000: 125125 without autocommit
        //
        // long t3 = System.currentTimeMillis();
        // DatabaseManager dbm2 = DatabaseManager.getInstance();
        //
        // PreparedStatement ps5 =
        // dbm.connection.prepareStatement("SELECT id FROM `t1` WHERE name = ? AND foreignKeyField > ?");
        //
        // for (int i = 0; i < 100; i++) {
        // int randomNumber = (int) Math.floor((Math.random()*10000));
        // ps5.setString(1, "reztstrz"+randomNumber);
        // ps5.setInt(2, (int)(randomNumber / 10));
        // dbm2.runQuery(ps5);
        // }
        //
        // long t4 = System.currentTimeMillis();
        //
        // System.out.println(t4-t3);
    }
}