package tud.iir.classification.entity;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.persistence.DatabaseManager;
import tud.iir.reporting.ChartCreator;

public class EntityTrustVoting {

    protected DatabaseManager dbm = null;
    private PreparedStatement psGetEntitiesByTrust = null;

    protected static final Logger logger = Logger.getLogger(EntityTrustVoting.class);

    // get the trust intervals for a concept and the number of entities with a trust >= this trust
    private PreparedStatement psSelectTrustThreshold = null;

    public EntityTrustVoting() {
        dbm = DatabaseManager.getInstance();

        try {
            psGetEntitiesByTrust = dbm.getConnection().prepareStatement("SELECT COUNT(id) FROM entities WHERE  trust > ? AND trust < ? AND conceptID = ?");
            psSelectTrustThreshold = dbm
                    .getConnection()
                    .prepareStatement(
                            "SELECT DISTINCT etv1.trust, (SELECT SUM(etv2.numberOfEntities) FROM `entity_trust_view` etv2 WHERE etv2.trust >= etv1.trust AND conceptID = ?) AS numberOfEntitiesIncremental FROM `entity_trust_view` etv1 WHERE conceptID = ? ORDER BY etv1.trust ASC");
        } catch (SQLException e) {
            logger.error("create prepared statements", e);
        }
    }

    /**
     * Create an entity file. file format: concept: entity total | 1:x,2:y,....
     */
    public void createEntityFile() {

        int sampleSize = 50;
        KnowledgeManager km = dbm.loadOntology();
        ArrayList<Concept> concepts = km.getConcepts();
        // first index is concept, second index is random id of entity of that concept
        Integer[][] randomIDs = new Integer[concepts.size()][sampleSize];

        Iterator<Concept> cIterator = concepts.iterator();
        int c = 0;
        while (cIterator.hasNext()) {
            Concept concept = cIterator.next();
            int conceptID = dbm.getConceptID(concept.getName());
            ArrayList<Integer> ids = new ArrayList<Integer>();

            ResultSet rs = dbm.runQuery("SELECT id FROM `entities` WHERE conceptID = " + conceptID);

            try {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            } catch (SQLException e) {
                logger.error("unknown", e);
            }

            int i = 0;
            while (i < sampleSize) {
                int randomNumber = (int) Math.floor(Math.random() * ids.size());
                randomIDs[c][i] = ids.get(randomNumber);
                i++;
            }
            c++;
        }

        try {
            FileWriter fileWriter = new FileWriter("data/reports/entities.csv");

            String separator = "#.#";
            fileWriter.write("\"Concept\"" + separator + "\"Entity\"" + separator + "\"Total Extractions\"" + separator + "\"CP such as\"" + separator
                    + "\"CP like\"" + separator + "\"CP including\"" + separator + "\"is a CS\"" + separator + "\"list of CP\"" + separator + "\"CS listing\""
                    + separator + "\"browse CP\"" + separator + "\"index of CP\"" + separator + "\"CS index\"" + separator + "\"2 seeds\"" + separator
                    + "\"3 seeds\"" + separator + "\"4 seeds\"" + separator + "\"5 seeds\"\n");
            fileWriter.flush();

            for (int i = 0; i < randomIDs.length; i++) {

                ArrayList<Entity> sampleEntities = new ArrayList<Entity>();
                for (int j = 0; j < randomIDs[i].length; j++) {
                    Entity sampleEntity = dbm.loadEntity(randomIDs[i][j]);
                    sampleEntities.add(sampleEntity);
                }

                Iterator<Entity> eIterator = sampleEntities.iterator();
                while (eIterator.hasNext()) {
                    Entity e = eIterator.next();

                    fileWriter.write("\"" + e.getConcept().getName() + "\"" + separator + "\"" + e.getName() + "\"");

                    Integer[] extractionTypes = new Integer[14];
                    for (int j = 0; j < extractionTypes.length; j++) {
                        extractionTypes[j] = e.getNumberOfExtractions(j + 1);
                    }

                    fileWriter.write(separator + e.getSources().size());

                    for (int k = 0; k < extractionTypes.length; k++) {
                        fileWriter.write(separator + extractionTypes[k]);
                    }
                    fileWriter.write("\n");
                    fileWriter.flush();
                }
            }

            fileWriter.close();
        } catch (IOException e) {
            logger.error("unknown", e);
        }

        System.out.println("entity file written successfully");
    }

    /**
     * file format: concept queryType entity
     */
    public void createEntityFile2() {

        int sampleSize = 100;
        KnowledgeManager km = dbm.loadOntology();
        ArrayList<Concept> concepts = km.getConcepts();

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("data/reports/" + DateHelper.getCurrentDatetime() + "entities.csv");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        String separator = "#.#";
        try {
            Iterator<Concept> cIterator = concepts.iterator();
            while (cIterator.hasNext()) {
                Concept concept = cIterator.next();
                int conceptID = dbm.getConceptID(concept.getName());

                for (int i = 1; i <= 14; i++) {
                    System.out.println(concept.getName() + " " + i);
                    fileWriter.write(concept.getName() + " (" + i + ")\n");
                    ResultSet rs = dbm
                            .runQuery("SELECT entities.name,sources.url,entities_sources.extractionType FROM `entities`,`entities_sources`,`sources` WHERE `entities`.conceptID = "
                                    + conceptID
                                    + " AND `entities`.id = `entities_sources`.entityID AND sources.id = entities_sources.sourceID AND extractionType = "
                                    + i
                                    + " GROUP BY entities.id ORDER BY RAND() LIMIT 0," + sampleSize);

                    try {
                        int c = 0;
                        while (rs.next()) {
                            fileWriter.write("\"" + rs.getString(1).replaceAll("\"", "") + "\"" + separator + "\"" + rs.getString(2) + "\"\n");
                            fileWriter.flush();
                            c++;
                        }
                        ResultSet rs2 = dbm
                                .runQuery("SELECT COUNT(*) FROM (SELECT entities.id FROM `entities`,`entities_sources`,`sources` WHERE `entities`.conceptID = "
                                        + conceptID
                                        + " AND `entities`.id = `entities_sources`.entityID AND sources.id = entities_sources.sourceID AND extractionType = "
                                        + i + " GROUP BY entities.id) AS A");
                        rs2.next();
                        fileWriter.write(c + " SAMPLES out of " + rs2.getInt(1) + "\n");
                        fileWriter.flush();
                    } catch (SQLException e) {
                        logger.error(e.getMessage());
                    }

                    fileWriter.write("\n");
                    fileWriter.flush();
                }

                fileWriter.write("\n\n");
                fileWriter.flush();
            }

            fileWriter.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        logger.info("entity file written successfully");
    }

    public void createEntityTrustChart() {
        // series, data, 0:time,1:value
        ArrayList<ArrayList<Double[]>> quantities = new ArrayList<ArrayList<Double[]>>();

        // collect total entity and total fact extraction
        ArrayList<Double[]> tableTrust = new ArrayList<Double[]>();
        for (int requiredTrust = 310000000; requiredTrust >= 259000; requiredTrust -= 259000) {

            try {
                psGetEntitiesByTrust.setDouble(1, requiredTrust - 259000);
                psGetEntitiesByTrust.setDouble(2, 999999999.9);
                psGetEntitiesByTrust.setInt(3, 1);
                ResultSet rs = dbm.runQuery(psGetEntitiesByTrust);

                Double[] data = new Double[2];
                data[0] = (requiredTrust / 10000.0);
                if (rs.next()) {
                    data[1] = (double) rs.getInt(1);
                    if (data[1] > 0.0) {
                        tableTrust.add(data);
                    }
                } else {
                    data[1] = 0.0001;
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            System.out.println(requiredTrust);
        }

        quantities.add(tableTrust);

        ChartCreator.createLineChart("data/reports/entityTrustVoting.png", quantities, new ArrayList<String>(), "Entity Trust Voting",
                "Absolute Trust Intervalls (258333) in 100000", "Number of Entities (log)", true);
    }

    /**
     * find connection (sources-entities) between two entities (depth first)
     */
    public boolean findEntityConnection(int entityID1, int entityID2, int lastSourceID, HashSet<Integer> usedSources, ArrayList<String> pathArray) {

        if (pathArray.size() == 0) {
            pathArray.add("Entity " + entityID1);
        }

        // get all sources for entityID1
        ResultSet rs = dbm.runQuery("SELECT sourceID FROM entities_sources WHERE entityID = " + entityID1 + " AND sourceID != " + lastSourceID);
        try {
            while (rs.next()) {
                // get all entities from that source
                int currentSourceID = rs.getInt(1);
                if (!usedSources.add(currentSourceID)) {
                    continue;
                }
                ResultSet rs2 = dbm.runQuery("SELECT entityID FROM entities_sources WHERE sourceID = " + currentSourceID + " AND entityID != " + entityID1);

                while (rs2.next()) {
                    int entityIDNew = rs2.getInt(1);
                    pathArray.add("Source " + currentSourceID);
                    pathArray.add("Entity " + entityIDNew);
                    if (entityIDNew == entityID2) {
                        System.out.println("The path is:");

                        usedSources = new HashSet<Integer>();
                        for (int i = 1; i < pathArray.size(); i += 2) {
                            int sourceID = Integer.valueOf(pathArray.get(i).substring(7));
                            int entityID = Integer.valueOf(pathArray.get(i - 1).substring(7));
                            if (sourceID != 3630) {
                                System.out.println("change");
                            }
                            if (usedSources.add(sourceID)) {

                                System.out.println(pathArray.get(i - 1) + ": " + dbm.getEntityName(entityID));
                                System.out.println(pathArray.get(i) + ": " + dbm.getSourceURL(sourceID));
                            }
                        }
                        System.out.println(pathArray.get(pathArray.size() - 1));

                        // CollectionHelper.print(pathArray);
                        return true;
                    }
                    if (findEntityConnection(entityIDNew, entityID2, currentSourceID, usedSources, pathArray))
                        return true;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return false;
    }

    /**
     * Calculate the trust threshold for all entities of the concept with the given id and a trust higher than 0. Find that threshold by looking for the highest
     * gradient in the number of entities dependent on the trust. ^ number of entities | | | |_ _|_ _ _> trust
     * 
     * @param conceptID
     * @return
     */
    protected double findTrustThreshold(int conceptID) {
        double trustThreshold = 0.0;

        try {
            psSelectTrustThreshold.setInt(1, conceptID);
            psSelectTrustThreshold.setInt(2, conceptID);

            int lastNumberOfEntities = -1;
            int highestGradient = 0;

            ResultSet rs = dbm.runQuery(psSelectTrustThreshold);
            while (rs.next()) {
                double trust = rs.getDouble(1);
                int currentNumberOfEntities = rs.getInt(2);

                if (lastNumberOfEntities == -1) {
                    lastNumberOfEntities = currentNumberOfEntities;
                    continue;
                }

                int gradient = lastNumberOfEntities - currentNumberOfEntities;
                if (gradient > highestGradient) {
                    trustThreshold = trust;
                    highestGradient = gradient;
                }

                lastNumberOfEntities = currentNumberOfEntities;
            }

        } catch (SQLException e) {
            logger.error("create psSelectTrustThreshold", e);
        }

        return trustThreshold;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // boolean entity trust voting
        BooleanEntityTrustVoting betv = new BooleanEntityTrustVoting();
        betv.runVoting();

        // gradual entity trust voting
        GradualEntityTrustVoting getv = new GradualEntityTrustVoting();
        getv.runVoting();

        // BooleanEntityTrustVoting betv = new BooleanEntityTrustVoting();
        // betv.runVoting();

        // BooleanEntityTrustVoting betv = new BooleanEntityTrustVoting();
        // betv.runVoting();

    }

}
