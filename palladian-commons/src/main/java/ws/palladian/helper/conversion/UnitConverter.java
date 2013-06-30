package ws.palladian.helper.conversion;

import java.util.Arrays;
import java.util.Collection;

import ws.palladian.helper.constants.TemperatureUnit;
import ws.palladian.helper.normalization.UnitNormalizer;

/**
 * <p>
 * Convert units.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class UnitConverter {

    /**
     * <p>
     * Convert an amount from one unit to another.
     * </p>
     * 
     * @param amount The amount to convert.
     * @param fromUnit The source unit.
     * @param toUnit The target unit.
     * @return The converted amount.
     */
    public static Double convert(Double amount, String fromUnit, String toUnit) {
        if (UnitNormalizer.getUnitType(fromUnit) == UnitNormalizer.UNIT_TEMPERATURE) {
            return convertTemperature(amount, fromUnit, toUnit);
        }

        double normalizedAmount = UnitNormalizer.getNormalizedNumber(amount, fromUnit);
        double divisor = UnitNormalizer.unitLookup(toUnit);
        return normalizedAmount / divisor;
    }

    public static Double convertTemperature(Double amount, String fromUnit, String toUnit) {
        
        TemperatureUnit from = TemperatureUnit.getByName(fromUnit);
        TemperatureUnit to = TemperatureUnit.getByName(toUnit);

        if (from == null || to == null || amount == null) {
            return null;
        }
        
        // celsius to fahrenheit
        if (from == TemperatureUnit.CELSIUS && to == TemperatureUnit.FAHRENHEIT) {
            amount = (amount * 9 / 5) + 32;
        }
        
        // celsius to kelvin
        if (from == TemperatureUnit.CELSIUS && to == TemperatureUnit.KELVIN) {
            amount += 273.15;
        }
        
        // fahrenheit to celsius
        if (from == TemperatureUnit.FAHRENHEIT && to == TemperatureUnit.CELSIUS) {
            amount = (amount - 32) * 5 / 9;
        }
        
        // fahrenheit to kelvin
        if (from == TemperatureUnit.FAHRENHEIT && to == TemperatureUnit.KELVIN) {
            amount = ((amount - 32) * 5 / 9) + 273.15;
        }
        
        // kelvin to celsius
        if (from == TemperatureUnit.KELVIN && to == TemperatureUnit.CELSIUS) {
            amount -= 273.15;

        }
        
        // kelvin to fahrenheit
        if (from == TemperatureUnit.KELVIN && to == TemperatureUnit.FAHRENHEIT) {
            amount = ((amount - 273.15) * 9 / 5) + 32;
        }

        return amount;
    }
    
    public static AmountUnit bestFitConvert(Double amount, String fromUnit, Collection<String> possibleUnits) {
        double normalizedAmount = UnitNormalizer.getNormalizedNumber(amount, fromUnit);

        // the divisor should be close to one to get the best fit
        double bestMatchDivisor = 0;
        double lowestMatchDivisor = 999999999;
        String bestMatchUnit = "";
        String lowestMatchUnit = "";

        for (String toUnit : possibleUnits) {
            double divisor = UnitNormalizer.unitLookup(toUnit);
            if (divisor > bestMatchDivisor && divisor < normalizedAmount) {
                bestMatchDivisor = divisor;
                bestMatchUnit = toUnit;
            } else if (divisor < lowestMatchDivisor) {
                lowestMatchDivisor = divisor;
                lowestMatchUnit = toUnit;
            }
        }

        if (bestMatchUnit.isEmpty()) {
            bestMatchUnit = lowestMatchUnit;
            bestMatchDivisor = lowestMatchDivisor;
        }

        double calculatedAmount = normalizedAmount / bestMatchDivisor;
        // if (!MathHelper.isWithinRange(calculatedAmount, 1, 0.1) && !bestMatchUnit.endsWith("s")) {
        // bestMatchUnit += "s";
        // }

        AmountUnit amountUnit = new AmountUnit();
        amountUnit.setAmount(calculatedAmount);
        amountUnit.setUnit(bestMatchUnit);

        return amountUnit;
    }

    public static void main(String[] args) {
        System.out.println(UnitConverter.convert(2., "liters", "ounces"));
        System.out.println(UnitConverter.convert(2., "kg", "lb"));
        System.out.println(UnitConverter.bestFitConvert(600., "g", Arrays.asList("lb", "ounces")).getCombined());
    }

}
