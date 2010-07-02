package tud.iir.classification.entity;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import tud.iir.helper.DateHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.KnowledgeManager;

public class GradualEntityTrustVoting extends EntityTrustVoting implements EntityTrustVotingInterface {

    public GradualEntityTrustVoting() {
    }

    /**
     * UPDATE sources SET voting = 0,entityTrust = 0; UPDATE entities SET voting = 0,trust = 0,class = null;
     * ------------------------------------------------------- for concept Movie entity "Snow Dogs" has been found for concept Mobile Phone entity "Nokia" has
     * been found for concept Notebook entity "Acer" has been found for concept Car entity "Audi" has been found for concept City entity "Boston" has been found
     * for concept Song entity "Close To You" has been found for concept Country entity "India" has been found for concept Actor entity "Tommy Lee Jones" has
     * been found for concept Sport entity "Golf" has been found run voting... 242 entities_sources were affected by page voting 79956 sources were affected by
     * entity voting run voting... 671 entities_sources were affected by page voting 199432 sources were affected by entity voting run voting... 553
     * entities_sources were affected by page voting 49030 sources were affected by entity voting run voting... 34 entities_sources were affected by page voting
     * 3901 sources were affected by entity voting run voting... 2 entities_sources were affected by page voting 30 sources were affected by entity voting run
     * voting... 1 entities_sources were affected by page voting 26 sources were affected by entity voting run voting... 0 entities_sources were affected by
     * page voting 0 sources were affected by entity voting 0 of 24021 sources have entity trust 1 0 of 427360 entities have entity trust 1 stopped, runtime:
     * 5304 seconds
     * 
     * */
    public void runVoting() {

        KnowledgeManager km = dbm.loadOntology();
        ArrayList<Concept> concepts = km.getConcepts();

        Iterator<Concept> cIterator = concepts.iterator();
        while (cIterator.hasNext()) {
            Concept concept = cIterator.next();
            int conceptID = dbm.getConceptID(concept.getName());

            ResultSet rs = dbm
                    .runQuery("SELECT entities.name,entities.id,concepts.name,COUNT(entities.id) FROM entities,entities_sources,concepts WHERE entities.id = entities_sources.entityID AND concepts.id = "
                            + conceptID
                            + " AND entities.conceptID = concepts.id AND (SELECT COUNT(DISTINCT extractionType) FROM entities_sources AS es WHERE entityID = entities.id AND extractionType < 11) > 2 GROUP BY entities.id HAVING COUNT(entities.id)>1 ORDER BY COUNT(entities.id) DESC");
            // ResultSet rs = dbm.runQuery("SELECT entities.name,entities.id FROM entities WHERE entities.id = 1");
            try {
                // get highest trusted entity
                int trustedEntityID = 0;
                if (rs.next()) {
                    trustedEntityID = rs.getInt(2);
                    dbm.runUpdate("UPDATE entities SET voting = 1 WHERE id = " + trustedEntityID);
                    System.out.println("for concept " + rs.getString(3) + " entity \"" + rs.getString(1) + "\" has been found");
                }
                rs.close();

                // assign 0.8 to all pages that this entity has been extracted from
                ResultSet rs2 = dbm.runQuery("SELECT sourceID FROM entities_sources WHERE entityID = " + trustedEntityID);
                while (rs2.next()) {
                    int sourceID = rs2.getInt(1);
                    dbm.runUpdate("UPDATE sources SET entityTrust = 0.8 WHERE id = " + sourceID);
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
            affectRows += gradualEntityTrustVotingPage();
            affectRows += gradualEntityTrustVotingEntity();
        }

        // calculate trust for all entities as the sum of the trusts of their sources
        try {
            dbm.runQuery("CALL final_source_voting_procedure");
            System.out.println("final source voting finished");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The websites vote for the entities that were extracted from them.
     * 
     * @return The number of sources that voted.
     */
    private int gradualEntityTrustVotingPage() {
        int affectedRows = 0;
        try {
            dbm.getConnection().setAutoCommit(false);
            Statement stmt = dbm.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT source_voting_function()");
            if (rs.next()) {
                affectedRows = rs.getInt(1);
            }
            rs.close();
            if (!dbm.getConnection().getAutoCommit()) {
                dbm.getConnection().commit();
                dbm.getConnection().setAutoCommit(true);
            }
            System.out.println(affectedRows + " sources voted");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return affectedRows;
    }

    /**
     * The entities vote for the websites they were extracted from.
     * 
     * @return The number of entities that voted.
     */
    private int gradualEntityTrustVotingEntity() {
        int affectedRows = 0;
        try {
            dbm.getConnection().setAutoCommit(false);
            Statement stmt = dbm.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT entity_voting_function()");
            if (rs.next()) {
                affectedRows = rs.getInt(1);
            }
            rs.close();
            if (!dbm.getConnection().getAutoCommit()) {
                dbm.getConnection().commit();
                dbm.getConnection().setAutoCommit(true);
            }
            System.out.println(affectedRows + " entities voted");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return affectedRows;
    }

    /**
     * for concept Movie entity "Snow Dogs" has been found for concept Mobile Phone entity "Nokia" has been found for concept Notebook entity "Acer" has been
     * found for concept Car entity "Audi" has been found for concept City entity "Boston" has been found for concept Song entity "Close To You" has been found
     * for concept Country entity "India" has been found for concept Actor entity "Tommy Lee Jones" has been found for concept Sport entity "Golf" has been
     * found run voting... 242 sources voted 79947 entities voted run voting... 671 sources voted 199432 entities voted run voting... 553 sources voted 49030
     * entities voted run voting... 34 sources voted 3901 entities voted run voting... 2 sources voted 30 entities voted run voting... 1 sources voted 26
     * entities voted run voting... 0 sources voted 0 entities voted final source voting finished stopped, runtime: 3937 seconds ###### after removing indices
     * on voting and trust: 1565 seconds
     */
    public static void main(String[] args) {

        long t1 = System.currentTimeMillis();
        GradualEntityTrustVoting getv = new GradualEntityTrustVoting();
        getv.runVoting();
        DateHelper.getRuntime(t1);

        System.exit(0);

        for (int i = 1; i <= 18; i++) {
            int conceptID = i;
            double tt = getv.findTrustThreshold(conceptID);
            System.out.println("The trust threshold for concept " + conceptID + " is: " + tt);
        }
    }

}