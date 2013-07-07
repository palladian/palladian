package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotated;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * Quick and dirty address extraction. Currently, streets and house numbers are extracted.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class AddressTagger implements Tagger {

    public static final Pattern STREET_PATTERN = Pattern.compile(
                    ".*street$|.*road$|.*avenue$|.*ave\\.|.*boulevard$|.*straße$|.*strasse$|.*gasse$|^rue\\s.*|via\\s.*|viale\\s.*|.*straat",
            Pattern.CASE_INSENSITIVE);

    @Override
    public List<LocationAnnotation> getAnnotations(String text) {
        List<LocationAnnotation> ret = CollectionHelper.newArrayList();

        // TODO StringTagger is too strict here, e.g. the following candidate is not recognized:
        // Viale di Porta Ardeatine -- use dedicted regex here?
        Annotations<ContextAnnotation> annotations = StringTagger.getTaggedEntities(text);

        // step one: match tagged annotations using street pattern
        for (Annotated annotation : annotations) {
            Matcher matcher = STREET_PATTERN.matcher(annotation.getValue());
            if (matcher.matches()) {
                // System.out.println("street : " + annotation.getEntity());
                Annotated annotated = new Annotation(annotation.getStartPosition(), annotation.getValue(),
                        LocationType.STREET.toString());
                ret.add(new LocationAnnotation(annotated, new ImmutableLocation(0, annotated.getValue(),
                        LocationType.STREET, null, null, null)));
            }
        }

        // step two: look for street numbers before or after
        List<LocationAnnotation> streetNumbers = CollectionHelper.newArrayList();
        for (Annotated annotation : ret) {
            String regEx = StringHelper.escapeForRegularExpression(annotation.getValue());

            // try number as suffix
            Pattern suffixRegEx = Pattern.compile(regEx + "\\s(\\d+)");
            Matcher matcher = suffixRegEx.matcher(text);
            while (matcher.find()) {
                // System.out.println("suffix: " + matcher.group(1));
                Annotated annotated = new Annotation(matcher.start(1), matcher.group(1),
                        LocationType.STREETNR.toString());
                streetNumbers.add(new LocationAnnotation(annotated, new ImmutableLocation(0, matcher.group(1),
                        LocationType.STREETNR, null, null, null)));
            }

            // try number as prefix
            Pattern prefixRegEx = Pattern.compile("(\\d+)\\s" + regEx);
            matcher = prefixRegEx.matcher(text);
            while (matcher.find()) {
                // System.out.println("prefix: " + matcher.group(1));
                Annotated annotated = new Annotation(matcher.start(1), matcher.group(1),
                        LocationType.STREETNR.toString());
                streetNumbers.add(new LocationAnnotation(annotated, new ImmutableLocation(0, matcher.group(1),
                        LocationType.STREETNR, null, null, null)));
            }
        }

        // TODO ZIP codes ...

        ret.addAll(streetNumbers);
        
        // sort by offset
        Collections.sort(ret);

        return ret;
    }

}
