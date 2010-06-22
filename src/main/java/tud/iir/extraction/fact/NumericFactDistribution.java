package tud.iir.extraction.fact;

import java.util.HashMap;

/**
 * This class keeps track of the distribution of numeric facts.
 * 
 * @author David Urbansky
 */
public class NumericFactDistribution {

    private static HashMap<String, Integer[]> numberPowerDistribution = new HashMap<String, Integer[]>();

    /**
     * @param args
     */
    public static void main(String[] args) {
        addNumber("abc1", 234.223);
        addNumber("abc1", 23422.223);
        addNumber("abc1", 23.223);
        addNumber("abc1", 25.223);
        addNumber("abc1", 0.000234223);
        addNumber("abc1", 0.0000234223);
        System.out.println(getPowerDistributionFactor("abc1", 10.0));
        System.out.println(getPowerDistributionFactor("abc1", 4.0));
    }

    public static void addNumber(String key, double number) {

        if (numberPowerDistribution.get(key) == null) {
            // powers from -5 to 15
            Integer[] powerDistribution = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            numberPowerDistribution.put(key, powerDistribution);
        } else {
            Integer[] currentPowerDistribution = numberPowerDistribution.get(key);

            // get the power of the number
            int power = (int) Math.floor(Math.log10(number)) + 5;
            if (power < 0 || power > 19)
                return;
            currentPowerDistribution[power] = currentPowerDistribution[power] + 1;
            numberPowerDistribution.put(key, currentPowerDistribution);
        }

    }

    public static double getPowerDistributionFactor(String key, double number) {
        int power = (int) Math.floor(Math.log10(number)) + 5;
        return getPowerDistributionFactor(key, power);
    }

    public static double getPowerDistributionFactor(String key, int power) {
        if (numberPowerDistribution.get(key) == null || power < 0 || power > 19) {
            return 1.0;
        }

        Integer[] powerDistribution = numberPowerDistribution.get(key);
        // calculate ratio between power and all powers for that key
        double ratio = 0.0;
        int allPowers = 0;
        for (int i = 0; i < powerDistribution.length; i++) {
            allPowers += powerDistribution[i];
        }

        if (allPowers > 0)
            ratio = powerDistribution[power] / (double) allPowers;

        // calculate factor for power
        return (1.0 + ratio);
    }

}
