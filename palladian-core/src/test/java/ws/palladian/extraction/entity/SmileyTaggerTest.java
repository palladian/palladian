package ws.palladian.extraction.entity;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.core.Annotation;

public class SmileyTaggerTest {

    @Test
    public void testSmileyTagger() {
        String text = "This is a nice day :) and the sun shines ;)";
        SmileyTagger smileyTagger = new SmileyTagger();
        List<Annotation> annotations = smileyTagger.getAnnotations(text);
        assertEquals(2, annotations.size());
        assertEquals(19, annotations.get(0).getStartPosition());
        assertEquals(":)", annotations.get(0).getValue());
        assertEquals(41, annotations.get(1).getStartPosition());
        assertEquals(";)", annotations.get(1).getValue());
    }

}
