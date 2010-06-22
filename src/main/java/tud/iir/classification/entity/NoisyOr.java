package tud.iir.classification.entity;

import tud.iir.knowledge.Entity;

/**
 * The Noisy-Or formula for assessment of (un)supervised information extraction. Noisy-Or as described in
 * "A Probabilistic Model of Redundancy in Information Extraction, 2006".
 * 
 * @author David
 * 
 */
public class NoisyOr {

    /**
     * Calculate probability using noisy-or probability. p(x elementOf C | x appears k times) = 1 - (1 - p)^k p = precision
     * 
     * @return
     */
    private double calculateProbability(double p, int k) {

        double probability = 0.0;

        probability = 1.0 - Math.pow(1.0 - p, k);

        return probability;
    }

    public boolean classify(Entity entity) {
        // TODO calculate p and k
        double p = 0.0;
        int k = 0;
        if (calculateProbability(p, k) > 0.5)
            return true;
        return false;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        double p = new NoisyOr().calculateProbability(0.9, 3);
        System.out.println(p);
    }

}