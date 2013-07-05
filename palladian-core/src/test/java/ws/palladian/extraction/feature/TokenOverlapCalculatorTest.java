/**
 * Created on: 20.11.2012 08:06:41
 */
package ws.palladian.extraction.feature;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Tests the whether the {@link TokenOverlapCalculator} works correctly or not.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class TokenOverlapCalculatorTest {

    @Test
    public void test() throws DocumentUnprocessableException {
        TokenOverlapCalculator objectOfClassUnderTest = new TokenOverlapCalculator("jaccard", "firstDocument",
                "secondDocument");

        TextDocument firstDocument = new TextDocument("a b c");
        ListFeature<PositionAnnotation> firstAnnotations = new ListFeature<PositionAnnotation>("firstDocument");
        firstAnnotations.add(new PositionAnnotation("a", 0, 1));
        firstAnnotations.add(new PositionAnnotation("b", 2, 3));
        firstAnnotations.add(new PositionAnnotation("c", 4, 5));
        firstDocument.add(firstAnnotations);

        TextDocument secondDocument = new TextDocument("b c d");
        ListFeature<PositionAnnotation> secondAnnotations = new ListFeature<PositionAnnotation>("secondDocument");
        secondAnnotations.add(new PositionAnnotation("b", 0, 1));
        secondAnnotations.add(new PositionAnnotation("c", 2, 3));
        secondAnnotations.add(new PositionAnnotation("d", 4, 5));
        secondDocument.add(secondAnnotations);

        objectOfClassUnderTest.getInputPorts().get(0).put(firstDocument);
        objectOfClassUnderTest.getInputPorts().get(1).put(secondDocument);

        objectOfClassUnderTest.process();

        TextDocument result = (TextDocument)objectOfClassUnderTest.getOutputPorts().get(0).poll();

        NumericFeature jaccard = result.get(NumericFeature.class, "jaccard");

        Assert.assertThat(jaccard.getValue(), Matchers.is(Matchers.closeTo(0.5d, 0.01)));
    }

}
