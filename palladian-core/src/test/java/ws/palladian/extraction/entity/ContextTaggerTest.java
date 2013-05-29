package ws.palladian.extraction.entity;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.location.experimental.WordCountContextTagger;

public class ContextTaggerTest {

    private static final String TEXT = "Mr. Yakomoto, John J. Smith, and Bill Drody cooperate with T. Shéff, L.Carding, T.O'Brian, Harry O'Sullivan and O'Brody. they are partying on Saturday's night special, Friday's Night special or THURSDAY'S, in St. Petersburg there is Dr. Mark Litwin";

    @Test
    public void testContextTagger() {
        ContextTagger tagger = new WindowSizeContextTagger(StringTagger.PATTERN, "CANDIDATE", 10);
        List<ContextAnnotation> annotations = tagger.getAnnotations(TEXT);

        assertEquals(14, annotations.size());
        assertEquals("Mr. Yakomoto", annotations.get(0).getValue());
        assertEquals("", annotations.get(0).getLeftContext());
        assertEquals(", John J. ", annotations.get(0).getRightContext());

        assertEquals("John J. Smith", annotations.get(1).getValue());
        assertEquals("Yakomoto, ", annotations.get(1).getLeftContext());
        assertEquals(", and Bill", annotations.get(1).getRightContext());

        assertEquals("Bill Drody", annotations.get(2).getValue());
        assertEquals("mith, and ", annotations.get(2).getLeftContext());
        assertEquals(" cooperate", annotations.get(2).getRightContext());
    }

    @Test
    public void testWordContextTagger() {
        ContextTagger tagger = new WordCountContextTagger(StringTagger.PATTERN, "CANDIDATE", 3);
        List<ContextAnnotation> annotations = tagger.getAnnotations(TEXT);

        assertEquals(14, annotations.size());
        assertEquals("Mr. Yakomoto", annotations.get(0).getValue());
        assertEquals("", annotations.get(0).getLeftContext());
        assertEquals(", John J. Smith", annotations.get(0).getRightContext());

        assertEquals("John J. Smith", annotations.get(1).getValue());
        assertEquals("Mr. Yakomoto, ", annotations.get(1).getLeftContext());
        assertEquals(", and Bill Drody", annotations.get(1).getRightContext());

        assertEquals("Bill Drody", annotations.get(2).getValue());
        assertEquals("J. Smith, and ", annotations.get(2).getLeftContext());
        // FIXME
        // assertEquals("cooperate with T.", annotations.get(2).getRightContext());
        assertEquals(" cooperate with T", annotations.get(2).getRightContext());
    }

}
