/**
 * Created on: 20.11.2012 08:06:41
 */
package ws.palladian.extraction.feature;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
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
        firstDocument.getFeatureVector().add(new PositionAnnotation("firstDocument", 0, 1, "a"));
        firstDocument.getFeatureVector().add(new PositionAnnotation("firstDocument", 2, 3, "b"));
        firstDocument.getFeatureVector().add(new PositionAnnotation("firstDocument", 4, 5, "c"));

        TextDocument secondDocument = new TextDocument("b c d");
        secondDocument.getFeatureVector().add(new PositionAnnotation("secondDocument", 0, 1, "b"));
        secondDocument.getFeatureVector().add(new PositionAnnotation("secondDocument", 2, 3, "c"));
        secondDocument.getFeatureVector().add(new PositionAnnotation("secondDocument", 4, 5, "d"));

        objectOfClassUnderTest.getInputPorts().get(0).put(firstDocument);
        objectOfClassUnderTest.getInputPorts().get(1).put(secondDocument);

        objectOfClassUnderTest.process();

        TextDocument result = (TextDocument)objectOfClassUnderTest.getOutputPorts().get(0).poll();

        NumericFeature jaccard = result.getFeatureVector().getFeature(NumericFeature.class, "jaccard");

        Assert.assertThat(jaccard.getValue(), Matchers.is(Matchers.closeTo(0.5d, 0.01)));
    }

}
