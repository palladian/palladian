package ws.palladian.extraction.entity;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.processing.features.Annotation;

public class TwitterTaggerTest {

    private static final String TWEET_TEXT = "@cnni shame on u USA shame on u #obama all what's happened in syria because of http://t.co/fRQo66khtW  #usa #uk #bah #ksa #uae";

    @Test
    public void testTwitterTagger() {
        TwitterTagger tagger = TwitterTagger.INSTANCE;
        List<Annotation> annotations = tagger.getAnnotations(TWEET_TEXT);
        assertEquals(7, annotations.size());
        assertEquals(0, annotations.get(0).getStartPosition());
        assertEquals(5, annotations.get(0).getEndPosition());
        assertEquals("@cnni", annotations.get(0).getValue());
        assertEquals(122, annotations.get(6).getStartPosition());
        assertEquals(126, annotations.get(6).getEndPosition());
        assertEquals("#uae", annotations.get(6).getValue());
    }

}
