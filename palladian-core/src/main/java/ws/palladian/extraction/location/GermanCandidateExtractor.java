package ws.palladian.extraction.location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.ClassifyingTagger;
import ws.palladian.extraction.feature.StopWordRemover;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;

public class GermanCandidateExtractor implements ClassifyingTagger {

    private static final Pattern pattern = Pattern.compile("[A-ZÄÖÜ][A-ZÄÖÜa-zäöüß]+([ -][A-ZÄÖÜ][A-ZÄÖÜa-zäöüß]+)*");

    private static final StopWordRemover stopwords = new StopWordRemover(Language.GERMAN);

    @Override
    public List<ClassifiedAnnotation> getAnnotations(String text) {
        List<ClassifiedAnnotation> annotations = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String value = matcher.group();
            if (stopwords.isStopWord(value.toLowerCase())) {
                continue;
            }
            int startPosition = matcher.start();
            annotations.add(new ClassifiedAnnotation(startPosition, value, CategoryEntries.EMPTY));

            // dirty; stripping of -es and -s suffixes; make this smarter
            if (value.endsWith("es")) {
                String trimmedValue = value.substring(0, value.length() - 2);
                annotations.add(new ClassifiedAnnotation(startPosition, trimmedValue, CategoryEntries.EMPTY));
            }
            if (value.endsWith("s")) {
                String trimmedValue = value.substring(0, value.length() - 1);
                annotations.add(new ClassifiedAnnotation(startPosition, trimmedValue, CategoryEntries.EMPTY));
            }
        }
        return annotations;
    }
    
    public static void main(String[] args) throws IOException {
        GermanCandidateExtractor extractor = new GermanCandidateExtractor();
        String text = HtmlHelper.stripHtmlTags(FileHelper
                .readFileToString("/Users/pk/Desktop/tud-loc-2015-de/text20.txt"));
        System.out.println(text);
        List<ClassifiedAnnotation> annotations = extractor.getAnnotations(text);
        CollectionHelper.print(annotations);
    }

}
