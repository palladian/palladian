package ws.palladian.extraction.entity;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.processing.features.Annotated;

public class UrlTaggerTest {

    @Test
    public void testUrlTagging() {
        UrlTagger urlTagger = new UrlTagger();
        List<Annotated> annotations = urlTagger
                .tagUrls("You can download it here: cinefreaks.com/coolstuff.zip but be aware of the size.");
        assertEquals(1, annotations.size());
        assertEquals(26, annotations.get(0).getStartPosition());
        assertEquals(28, annotations.get(0).getValue().length());

    }
}
