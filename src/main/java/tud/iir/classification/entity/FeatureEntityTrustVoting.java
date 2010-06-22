package tud.iir.classification.entity;

import tud.iir.helper.DateHelper;

public class FeatureEntityTrustVoting extends EntityTrustVoting implements EntityTrustVotingInterface {

    public FeatureEntityTrustVoting() {
    }

    /**
     * last run with assignSourceTrust("2"); on 24/03/2009 source trust has been assigned in 1952.0 seconds entity trust has been assigned in 273.0 seconds ::::
     * runtime: 2226.0 seconds
     */
    public void runVoting() {
        // calculate trust for all sources
        assignSourceTrust("2");
        // assignSourceTrust("");

        // calculate trust for all entities
        assignEntityTrust();
    }

    /**
     * Calculate the trust for each source: method = "" => Trust(s) = #totalSourcesConnected / #totalEntities method = "2" => Trust(s) = #totalSourcesConnected
     * A source should have high trust if it hosts many entities, that are also hosted on many other pages.
     */
    private void assignSourceTrust(String method) {
        try {
            long t1 = System.currentTimeMillis();
            dbm.runQuery("CALL source_trust_assignment_procedure" + method);
            System.out.println("source trust has been assigned in " + DateHelper.getRuntime(t1) + " seconds");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculate the trust for each entity: Trust(e) = SUM(SourceTrust(s)) * 2^(#extractionTypes).
     */
    private void assignEntityTrust() {
        try {
            long t1 = System.currentTimeMillis();
            dbm.runQuery("CALL entity_trust_assignment_procedure");
            System.out.println("entity trust has been assigned in " + DateHelper.getRuntime(t1) + " seconds");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        long t1 = System.currentTimeMillis();
        FeatureEntityTrustVoting fetv = new FeatureEntityTrustVoting();
        fetv.runVoting();
        DateHelper.getRuntime(t1);

        System.exit(0);
        int conceptID = 13;
        double tt = fetv.findTrustThreshold(conceptID);
        System.out.println("The trust threshold for concept " + conceptID + " is: " + tt);
    }

}