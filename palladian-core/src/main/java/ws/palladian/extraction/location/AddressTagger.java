package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Quick and dirty address extraction. Currently, streets and house numbers are extracted.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class AddressTagger {

    /** The assigned entity type for street names. */
    public static final String STREET_ANNOTATION_NAME = "STREET";

    /** The assigned entity type for house numbers. */
    public static final String STREET_NR_ANNOTATION_NAME = "STREETNR";

    public static final Pattern STREET_PATTERN = Pattern.compile(
            ".*street$|.*road$|.*avenue$|.*straße$|.*strasse$|.*gasse$|^rue\\s.*|via\\s.*|viale\\s.*|.*straat",
            Pattern.CASE_INSENSITIVE);

    public static List<Annotation> tag(String text) {
        List<Annotation> ret = CollectionHelper.newArrayList();

        // TODO StringTagger is too strict here, e.g. the following candidate is not recognized:
        // Viale di Porta Ardeatine -- use dedicted regex here?
        List<Annotation> annotations = StringTagger.getTaggedEntities(text);

        // step one: match tagged annotations using street pattern
        for (Annotation annotation : annotations) {
            Matcher matcher = STREET_PATTERN.matcher(annotation.getValue());
            if (matcher.matches()) {
                // System.out.println("street : " + annotation.getEntity());
                ret.add(new Annotation(annotation.getStartPosition(), annotation.getValue(), STREET_ANNOTATION_NAME));
            }
        }

        // step two: look for street numbers before or after
        List<Annotation> streetNumbers = CollectionHelper.newArrayList();
        for (Annotation annotation : ret) {
            String regEx = StringHelper.escapeForRegularExpression(annotation.getValue());

            // try number as suffix
            Pattern suffixRegEx = Pattern.compile(regEx + "\\s(\\d+)");
            Matcher matcher = suffixRegEx.matcher(text);
            while (matcher.find()) {
                // System.out.println("suffix: " + matcher.group(1));
                streetNumbers.add(new Annotation(matcher.start(), matcher.group(1), STREET_NR_ANNOTATION_NAME));
            }

            // try number as prefix
            Pattern prefixRegEx = Pattern.compile("(\\d+)\\s" + regEx);
            matcher = prefixRegEx.matcher(text);
            while (matcher.find()) {
                // System.out.println("prefix: " + matcher.group(1));
                streetNumbers.add(new Annotation(matcher.start(), matcher.group(1), STREET_NR_ANNOTATION_NAME));
            }
        }

        // TODO ZIP codes ...

        ret.addAll(streetNumbers);

        // sort by offset
        Collections.sort(ret, new Comparator<Annotation>() {
            @Override
            public int compare(Annotation a0, Annotation a1) {
                return Integer.valueOf(a0.getStartPosition()).compareTo(a1.getStartPosition());
            }
        });

        return ret;
    }

    private AddressTagger() {
        // no instance.
    }

    public static void main(String[] args) {
        String text = FileHelper.readFileToString("/Users/pk/Desktop/LocationLab/LocationExtractionDataset/text2.txt");
        text = HtmlHelper.stripHtmlTags(text);
        List<Annotation> result = tag(text);
        CollectionHelper.print(result);
    }

}
