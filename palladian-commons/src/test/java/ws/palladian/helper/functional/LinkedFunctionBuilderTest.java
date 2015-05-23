package ws.palladian.helper.functional;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.functional.LinkedFunctionBuilder;

public class LinkedFunctionBuilderTest {

    private class StringToIntegerFunction implements Function<String, Integer> {
        @Override
        public Integer compute(String input) {
            return Arrays.asList("zero", "one", "two", "three", "four").indexOf(input.toLowerCase());
        }
    }

    private class NumberSquareRootFunction implements Function<Number, Double> {
        @Override
        public Double compute(Number input) {
            return Math.sqrt(input.doubleValue());
        }
    }

    private class RoundFunction implements Function<Number, Long> {
        @Override
        public Long compute(Number input) {
            return Math.round(input.doubleValue());
        }
    }

    @Test
    public void testLinkedFunction() {
        Function<String, Long> function = LinkedFunctionBuilder //
                .with(new StringToIntegerFunction()) //
                .add(new NumberSquareRootFunction()) //
                .add(new RoundFunction()) //
                .create(); //

        List<String> values = Arrays.asList("four", "two", "one");
        List<Long> convertedValues = CollectionHelper.convertList(values, function);
        assertEquals(Arrays.asList(2l, 1l, 1l), convertedValues);
    }

}
