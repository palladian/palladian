package tud.iir.classification.entity;

import tud.iir.knowledge.Entity;

/**
 * The URNS assessment model for (un)supervised information extraction. Single URN model with simplified assumptions (Poisson) as described in
 * "A Probabilistic Model of Redundancy in Information Extraction, 2006".
 * "Redundancy in Web-scale Information Extraction: Probabilistic Model and Experimental Results, 2008" (page 34 and following)
 * 
 * @author David Urbansky
 * 
 */
public class Urns {

    /**
     * Calculate probability from single URN with Poisson model. p(x € C | x appears k times in n draws) = 1 ---------------------- 1 + |E|/|C| * (pe/pc)^k *
     * e^(n*(pc-pe)) p = precision pc = p/|C| pe = p/|E|
     * 
     * @return
     */
    private double calculateProbability(int e, int c, double p, int k, int n) {

        double probability = 0.0;

        double ec = (double) e / (double) c;
        double pc = p / c;
        double pe = (1.0 - p) / e;
        double pepc = pe / pc;

        double denominator = 1 + (ec * Math.pow(pepc, k) * Math.pow(Math.E, n * (pc - pe)));
        probability = 1.0 / denominator;

        return probability;
    }

    public boolean classify(Entity entity) {
        // TODO parameters
        // if (calculateProbability(e, c, p, k, n) > 0.5) return true;

        return false;
    }

    public static void main(String[] args) {
        double p = new Urns().calculateProbability(2000, 2000, 0.9, 3, 10000);
        p = new Urns().calculateProbability(200, 200, 0.5, 2, 400);
        System.out.println(p);
    }

}