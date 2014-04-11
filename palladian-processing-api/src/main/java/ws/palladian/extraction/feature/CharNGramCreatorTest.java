package ws.palladian.extraction.feature;

import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.token.AbstractTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

public class CharNGramCreatorTest {

    @Test
    public void testNGramCreation() throws DocumentUnprocessableException {

        CharNGramCreator charNGramCreator = new CharNGramCreator(1, 5);
        String text = "The quick brown fox.";
        TextDocument document = new TextDocument(text);
        charNGramCreator.processDocument(document);
        List<PositionAnnotation> annotations = document.get(ListFeature.class,
                AbstractTokenizer.PROVIDED_FEATURE);

        for (PositionAnnotation annotation : annotations) {
            assertThat(annotation.getValue().length(), greaterThanOrEqualTo(1));
            assertThat(annotation.getValue().length(), lessThanOrEqualTo(5));
        }

        assertEquals(90, annotations.size());

        charNGramCreator = new CharNGramCreator(1, 5, true, Integer.MAX_VALUE);
        document = new TextDocument(text);
        charNGramCreator.processDocument(document);
        annotations = document.get(ListFeature.class, AbstractTokenizer.PROVIDED_FEATURE);
        assertEquals(87, annotations.size());

        charNGramCreator = new CharNGramCreator(1, 5, false, 10);
        document = new TextDocument(text);
        charNGramCreator.processDocument(document);
        annotations = document.get(ListFeature.class, AbstractTokenizer.PROVIDED_FEATURE);
        assertEquals(10, annotations.size());

    }

}
