package ws.palladian.helper.normalization;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class UnitNormalizerTest {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testDetectUnit() throws Exception {

        String input;

        input = "230 Volt";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("Volt"));

        input = "39 hours";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("hours"));

        input = "filter_groess # filter_groess # gross # 39";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.nullValue());

    }
}