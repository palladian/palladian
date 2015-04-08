package ws.palladian.extraction.location;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.RegExTagger;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;

public class GermanCandidateExtractor extends RegExTagger {

    private static final Pattern pattern = Pattern.compile("[A-ZÄÖÜ][A-ZÄÖÜa-zäöüß]+([ -][A-ZÄÖÜ][A-ZÄÖÜa-zäöüß]+)*");

    public GermanCandidateExtractor() {
        super(pattern, "candidate");
    }

    public static void main(String[] args) throws IOException {
        GermanCandidateExtractor extractor = new GermanCandidateExtractor();
        String text = HtmlHelper.stripHtmlTags(FileHelper
                .readFileToString("/Users/pk/Desktop/tud-loc-2015-de/text2.txt"));
        System.out.println(text);
        List<Annotation> annotations = extractor.getAnnotations(text);
        CollectionHelper.print(annotations);
    }

}
