package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

/**
 * <p>
 * Quick and dirty address extraction. Currently, streets and house numbers are extracted.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class AddressTagger implements Tagger {

    public static final Pattern STREET_PATTERN = Pattern
            .compile(
            // suffix rules
                    "[A-Za-z]+(?:\\s[A-Za-z]+)?(?:\\sstreet$|\\sroad$|\\savenue$|\\save\\.|boulevard$|straße$|strasse$|gasse$|straat|\\sdrive|\\sst\\.|\\strafficway)|"
                            +
                            // prefix rules
                            "(?:^rue\\s.+|via\\s.+|viale\\s.+)[A-Za-z]+(?:\\s[A-Za-z]+)?", Pattern.CASE_INSENSITIVE);

    @Override
    public List<LocationAnnotation> getAnnotations(String text) {
        List<LocationAnnotation> ret = CollectionHelper.newArrayList();

        // TODO StringTagger is too strict here, e.g. the following candidate is not recognized:
        // Viale di Porta Ardeatine -- use dedicted regex here?
        Annotations<ContextAnnotation> annotations = StringTagger.getTaggedEntities(text);
        // CollectionHelper.print(annotations);

        // step one: match tagged annotations using street pattern
        for (Annotation annotation : annotations) {
            String value = annotation.getValue();

            // street names must consist of four tokens maximum
            if (value.split("\\s").length > 4) {
                continue;
            }

            // XXX ugly; in case of "Bla St", check, if following character is a . and extend annotation, as the . was
            // swallowed by StringTagger
            if (value.endsWith(" St") && text.length() >= annotation.getEndPosition()
                    && text.charAt(annotation.getEndPosition()) == '.') {
                value += ".";
            }

            Matcher matcher = STREET_PATTERN.matcher(value);
            if (matcher.matches()) {
                // System.out.println("street : " + annotation.getEntity());
                Annotation newAnnotation = new ImmutableAnnotation(annotation.getStartPosition(), value,
                        LocationType.STREET.toString());
                ret.add(new LocationAnnotation(newAnnotation, new ImmutableLocation(0, value, LocationType.STREET,
                        null, null)));
            }
        }

        // step two: look for street numbers before or after
        List<LocationAnnotation> streetNumbers = CollectionHelper.newArrayList();
        for (Annotation annotation : ret) {
            String regEx = Pattern.quote(annotation.getValue());

            // try number as suffix
            Pattern suffixRegEx = Pattern.compile(regEx + "\\s(\\d+)");
            Matcher matcher = suffixRegEx.matcher(text);
            while (matcher.find()) {
                // System.out.println("suffix: " + matcher.group(1));
                Annotation newAnnotation = new ImmutableAnnotation(matcher.start(1), matcher.group(1),
                        LocationType.STREETNR.toString());
                streetNumbers.add(new LocationAnnotation(newAnnotation, new ImmutableLocation(0, matcher.group(1),
                        LocationType.STREETNR, null, null)));
            }

            // try number as prefix
            Pattern prefixRegEx = Pattern.compile("(\\d+)\\s" + regEx);
            matcher = prefixRegEx.matcher(text);
            while (matcher.find()) {
                // System.out.println("prefix: " + matcher.group(1));
                Annotation newAnnotation = new ImmutableAnnotation(matcher.start(1), matcher.group(1),
                        LocationType.STREETNR.toString());
                streetNumbers.add(new LocationAnnotation(newAnnotation, new ImmutableLocation(0, matcher.group(1),
                        LocationType.STREETNR, null, null)));
            }
        }

        // TODO ZIP codes ...

        ret.addAll(streetNumbers);

        // sort by offset
        Collections.sort(ret);

        return ret;
    }

}
