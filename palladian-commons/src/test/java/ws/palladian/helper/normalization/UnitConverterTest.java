package ws.palladian.helper.normalization;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.helper.conversion.UnitConverter;

/**
 * Test cases for the conversion of units.
 * 
 * @author David Urbansky
 */
public class UnitConverterTest {

    @Test
    public void testConvertUnit() {
        assertEquals(32., UnitConverter.convert(0., "celsius", "fahrenheit"), 0.01);
        assertEquals(273.15, UnitConverter.convert(0., "celsius", "kelvin"), 0.01);
        assertEquals(0., UnitConverter.convert(32., "fahrenheit", "celsius"), 0.01);
        assertEquals(273.15, UnitConverter.convert(32., "fahrenheit", "kelvin"), 0.01);
        assertEquals(0., UnitConverter.convert(273.15, "kelvin", "celsius"), 0.01);
        assertEquals(32., UnitConverter.convert(273.15, "kelvin", "fahrenheit"), 0.01);

        assertEquals(3.53, UnitConverter.convert(100., "mL", "ounces"), 0.01);
    }

}