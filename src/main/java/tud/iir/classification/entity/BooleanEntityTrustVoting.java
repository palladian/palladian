package tud.iir.classification.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import tud.iir.knowledge.Concept;
import tud.iir.knowledge.KnowledgeManager;

public class BooleanEntityTrustVoting extends EntityTrustVoting implements EntityTrustVotingInterface {

    private PreparedStatement psSelectTrustedEntitySources;
    private PreparedStatement psSelectTrustedEntities;
    private PreparedStatement psUpdateEntityTrustSources;
    private PreparedStatement psUpdateEntityTrustEntities;

    public BooleanEntityTrustVoting() {
        super();
        try {
            psSelectTrustedEntitySources = dbm.getConnection().prepareStatement("SELECT id FROM sources WHERE entityTrust = ?");
            psSelectTrustedEntities = dbm.getConnection().prepareStatement("SELECT id FROM entities WHERE trust = ?");
            psUpdateEntityTrustSources = dbm.getConnection().prepareStatement(
                    "UPDATE entities,entities_sources SET entities.trust = ? WHERE entities.id = entities_sources.entityID AND sourceID = ?");
            psUpdateEntityTrustEntities = dbm
                    .getConnection()
                    .prepareStatement(
                            "UPDATE sources,entities_sources SET sources.entityTrust = 1 WHERE sources.id = entities_sources.sourceID AND sources.entityTrust != 1 AND entityID = ?");
        } catch (SQLException e) {
            Logger.getRootLogger().error("create prepared statements", e);
        }
    }

    /**
     * Boolean trust voting. for concept Movie entity "The Incredibles" has been found for concept Mobile Phone entity "Nokia" has been found for concept
     * Notebook entity "Acer" has been found for concept Car entity "Audi" has been found for concept Song entity "Close To You" has been found for concept City
     * entity "Boston" has been found for concept Country entity "India" has been found for concept Sport entity "Golf" has been found for concept Actor entity
     * "Tommy Lee Jones" has been found run voting... 79956 entities_sources were affected by page voting 616 sources were affected by entity voting run
     * voting... 199588 entities_sources were affected by page voting 548 sources were affected by entity voting run voting... 48927 entities_sources were
     * affected by page voting 29 sources were affected by entity voting run voting... 3848 entities_sources were affected by page voting 2 sources were
     * affected by entity voting run voting... 30 entities_sources were affected by page voting 1 sources were affected by entity voting run voting... 26
     * entities_sources were affected by page voting 0 sources were affected by entity voting run voting... 0 entities_sources were affected by page voting 0
     * sources were affected by entity voting 1503 of 1910 sources have entity trust 1 332375 of 427360 entities have entity trust 1 stopped, runtime: 4140
     * seconds
     */
    public void runVoting() {

        KnowledgeManager km = dbm.loadOntology();
        ArrayList<Concept> concepts = km.getConcepts();

        Iterator<Concept> cIterator = concepts.iterator();
        while (cIterator.hasNext()) {
            Concept concept = cIterator.next();
            int conceptID = dbm.getConceptID(concept.getName());

            ResultSet results = dbm
                    .runQuery("SELECT entities.name,entities.id,concepts.name,COUNT(entities.id) FROM entities,entities_sources,concepts WHERE entities.id = entities_sources.entityID AND concepts.id = "
                            + conceptID
                            + " AND entities.conceptID = concepts.id AND (SELECT COUNT(DISTINCT extractionType) FROM entities_sources AS es WHERE entityID = entities.id AND extractionType < 11) > 2 GROUP BY entities.id HAVING COUNT(entities.id)>1 ORDER BY COUNT(entities.id) DESC");
            try {
                // get highest trusted entity
                int trustedEntityID = 0;
                if (results.next()) {
                    trustedEntityID = results.getInt(2);
                    System.out.println("for concept " + results.getString(3) + " entity \"" + results.getString(1) + "\" has been found");
                }
                results.close();

                // mark all pages that this entity has been extracted from as correct
                ResultSet rs2 = dbm.runQuery("SELECT sourceID FROM entities_sources WHERE entityID = " + trustedEntityID);
                while (rs2.next()) {
                    int sourceID = rs2.getInt(1);
                    dbm.runUpdate("UPDATE sources SET entityTrust = 1 WHERE id = " + sourceID);
                }
                rs2.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        int affectRows = 1;
        while (affectRows > 0) {
            System.out.println("run voting...");
            affectRows = 0;
            affectRows += booleanEntityTrustVotingPage();
            affectRows += booleanEntityTrustVotingEntity();
        }

        try {
            ResultSet rs1 = dbm.runQuery("SELECT COUNT(id) FROM sources");
            ResultSet rs2 = dbm.runQuery("SELECT COUNT(id) FROM sources WHERE entityTrust = 1");
            if (rs1.next() && rs2.next()) {
                System.out.println(rs2.getInt(1) + " of " + rs1.getInt(1) + " sources have entity trust 1");
            }
            rs1.close();
            rs2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ResultSet rs1 = dbm.runQuery("SELECT COUNT(id) FROM entities");
            ResultSet rs2 = dbm.runQuery("SELECT COUNT(id) FROM entities WHERE trust = 1");
            if (rs1.next() && rs2.next()) {
                System.out.println(rs2.getInt(1) + " of " + rs1.getInt(1) + " entities have entity trust 1");
            }
            rs1.close();
            rs2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int booleanEntityTrustVotingPage() {
        int affectedRows = 0;

        try {
            psSelectTrustedEntitySources.setDouble(1, 1.0);
            ResultSet rs = dbm.runQuery(psSelectTrustedEntitySources);
            while (rs.next()) {
                int sourceID = rs.getInt(1);
                psUpdateEntityTrustSources.setDouble(1, 1.0);
                psUpdateEntityTrustSources.setInt(2, sourceID);
                affectedRows += dbm.runUpdate(psUpdateEntityTrustSources);
            }
            rs.close();
            System.out.println(affectedRows + " entities_sources were affected by page voting");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return affectedRows;
    }

    private int booleanEntityTrustVotingEntity() {
        int affectedRows = 0;
        try {
            psSelectTrustedEntities.setDouble(1, 1.0);
            ResultSet rs = dbm.runQuery(psSelectTrustedEntities);
            while (rs.next()) {
                int entityID = rs.getInt(1);
                psUpdateEntityTrustEntities.setInt(1, entityID);
                affectedRows += dbm.runUpdate(psUpdateEntityTrustEntities);
            }
            rs.close();
            System.out.println(affectedRows + " sources were affected by entity voting");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return affectedRows;
    }
}