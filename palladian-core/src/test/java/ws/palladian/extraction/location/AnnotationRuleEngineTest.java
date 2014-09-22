package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.helper.io.ResourceHelper;

public class AnnotationRuleEngineTest {

    @Test
    public void testRuleEngine() throws FileNotFoundException {
        InputStream ruleFile = ResourceHelper.getResourceStream("/ruleEngine.rules");
        AnnotationRuleEngine ruleEngine = new AnnotationRuleEngine(ruleFile);
        String sampleText = "English is the language of the world. There is a place called English Lake in Indiana. Mr. Smith speaks English. It is common within Great Britain to speak English, Mr. John Smith commented. John likes marvel cakes. Let's make a trip to Miami, yes Miami is nice! The incredibly beautiful River Neckar flows through Stuttgart. And the Neckar also flows through Heilbronn. USA, US, GB, XY, yeah.";
        List<Annotation> annotations = StringTagger.INSTANCE.getAnnotations(sampleText);
        List<ClassifiedAnnotation> result = ruleEngine.apply(sampleText, annotations);
        // CollectionHelper.print(result);
        assertEquals(17, result.size());
        Iterator<ClassifiedAnnotation> iterator = result.iterator();
        assertEquals(null, iterator.next().getTag());
        assertEquals(null, iterator.next().getTag());
        assertEquals(null, iterator.next().getTag());
        assertEquals(null, iterator.next().getTag());
        assertEquals("LOC", iterator.next().getTag());
        assertEquals(null, iterator.next().getTag());
        assertEquals("LOC", iterator.next().getTag());
        assertEquals("LOC", iterator.next().getTag());
        assertEquals(null, iterator.next().getTag());
        assertEquals("LOC", iterator.next().getTag());
        assertEquals(null, iterator.next().getTag());
        assertEquals(null, iterator.next().getTag());
        assertEquals("LOC", iterator.next().getTag());
        assertEquals(null, iterator.next().getTag());
        assertEquals(null, iterator.next().getTag());
        assertEquals(null, iterator.next().getTag());
    }

}
