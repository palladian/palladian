package ws.palladian.helper.normalization;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import ws.palladian.helper.constants.UnitType;

public class UnitNormalizerTest {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testDetectUnit() throws Exception {

        String input;

        input = "230 MB/s";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.BANDWIDTH));

        input = "1 mAh";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.ELECTRIC_CHARGE));

        input = "1 A";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.CURRENT));

        input = "100 kN/m^2";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.PRESSURE));

        input = "100 kN/m²";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.PRESSURE));

        input = "100kN/m²";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("kN/m²"));

        input = "230 Volt";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("Volt"));

        input = "39 hours";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("hours"));

        input = "filter_groess # filter_groess # gross # 39";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.nullValue());

    }
}