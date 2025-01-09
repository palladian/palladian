package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.extraction.entity.TaggingFormat;

public class BertNerTest {

    private final Path modelDirectory = Paths.get("/Users/pk/temp/Huggingface_Java/raw-files/onnx/model.onnx");
    private final Path tokenizerJson = Paths.get("/Users/pk/temp/Huggingface_Java/raw-files/tokenizer.json");

    @Test
    @Ignore
    public void testBertNer() throws IOException {

        // original text:
        // Most day-trippers head for the largest island of the group, Inis More, with
        // its spectacular Dun Aengus fort, a pre-Christian monument built in the 2nd
        // Century BC

        // make it a harder by using nonsense names to prevent usage of dictionaries
        var text = "Most day-trippers head for the largest island of the group, Xonix Emor, with its spectacular Pom Pombo fort, a pre-Christian monument built in the 2nd Century BC by Elong Muskat.";

        try (var ner = BertNer.loadFrom(modelDirectory, tokenizerJson)) {
            var annotations = ner.getAnnotations(text);
            var textWithTags = NerHelper.tag(text, annotations, TaggingFormat.XML);
            assertEquals(
                    "Most day-trippers head for the largest island of the group, <LOC>Xonix Emor</LOC>, with its spectacular <LOC>Pom Pombo</LOC> fort, a <MISC>pre-Christian</MISC> monument built in the 2nd Century <MISC>BC</MISC> by <PER>Elong Muskat</PER>.",
                    textWithTags);
        }
    }

    @Test
    @Ignore
    public void testBertNer_errorCases_US() throws IOException {
        var text = "In an era when advertisers often hired fine artists to add a touch of class to their campaigns, the “least commercial artist in the U.S.”";
        try (var ner = BertNer.loadFrom(modelDirectory, tokenizerJson)) {
            var annotations = ner.getAnnotations(text);
            var textWithTags = NerHelper.tag(text, annotations, TaggingFormat.XML);
            assertEquals(
                    "In an era when advertisers often hired fine artists to add a touch of class to their campaigns, the “least commercial artist in the <LOC>U.S.</LOC>”",
                    textWithTags);
        }
    }

}
