package ws.palladian.helper.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * Temperature units.
 * </p>
 * 
 * @author David Urbansky
 */
public enum TemperatureUnit {

    CELSIUS("celsius", "degrees celsius", "° celsius", "°celsius", "°c"),

    FAHRENHEIT("fahrenheit", "degrees fahrenheit", "° fahrenheit", "°fahrenheit", "°f"),

    KELVIN("kelvin", "K");

    private Set<String> names = new HashSet<String>();

    private TemperatureUnit(String... names) {
        this.names.addAll(Arrays.asList(names));
    }

    public Set<String> getNames() {
        return names;
    }

    public static boolean isCelsius(String unit) {
        for (String name : CELSIUS.names) {
            if (name.equalsIgnoreCase(unit)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFahrenheit(String unit) {
        for (String name : FAHRENHEIT.names) {
            if (name.equalsIgnoreCase(unit)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isKelvin(String unit) {
        for (String name : KELVIN.names) {
            if (name.equalsIgnoreCase(unit)) {
                return true;
            }
        }
        return false;
    }

    public static TemperatureUnit getByName(String unit) {
        if (isCelsius(unit)) {
            return CELSIUS;
        }
        if (isFahrenheit(unit)) {
            return FAHRENHEIT;
        }
        if (isKelvin(unit)) {
            return KELVIN;
        }

        return null;
    }
}