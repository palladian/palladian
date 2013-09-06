package ws.palladian.extraction.location;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.Annotated;

public class AnnotationFilter {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationFilter.class);

    private final Set<String> words = CollectionHelper.newHashSet();
    private final Set<String> prefixes = CollectionHelper.newHashSet();
    private final Set<String> suffixes = CollectionHelper.newHashSet();
    private final Set<String> parts = CollectionHelper.newHashSet();

    public AnnotationFilter() {
        this(AnnotationFilter.class.getResourceAsStream("/locationsBlacklist.txt"));
    }

    public AnnotationFilter(InputStream blacklist) {
        FileHelper.performActionOnEveryLine(blacklist, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (line.isEmpty() || line.startsWith("#")) {
                    return;
                }
                if (line.startsWith("*") && line.endsWith("*")) {
                    parts.add(line.substring(1, line.length() - 1));
                } else if (line.startsWith("*")) {
                    suffixes.add(line.substring(1));
                } else if (line.endsWith("*")) {
                    prefixes.add(line.substring(0, line.length() - 1));
                } else {
                    words.add(line);
                }
            }
        });
        LOGGER.debug("Filter dictionary contains {} words, {} parts, {} prefixes, {} suffixes",
                new Object[] {words.size(), parts.size(), prefixes.size(), suffixes.size()});
    }

    public List<Annotated> filter(List<Annotated> annotations) {
        List<Annotated> result = CollectionHelper.newArrayList();
        Set<String> removeFragments = CollectionHelper.newHashSet();
        out: for (Annotated annotation : annotations) {
            for (String part : parts) {
                if (annotation.getValue().contains(part)) {
                    removeFragments.addAll(getParts(annotation.getValue()));
                    continue out;
                }
            }
            for (String prefix : prefixes) {
                if (annotation.getValue().startsWith(prefix)) {
                    removeFragments.addAll(getParts(annotation.getValue()));
                    continue out;
                }
            }
            for (String suffix : suffixes) {
                if (annotation.getValue().endsWith(suffix)) {
                    removeFragments.addAll(getParts(annotation.getValue()));
                    continue out;
                }
            }
        }
        LOGGER.debug("Fragment blacklist: {}", removeFragments);
        for (Annotated annotation : annotations) {
            if (words.contains(annotation.getValue())) {
                LOGGER.debug("Remove by word list: {}", annotation.getValue());
                continue;
            }
            if (StringHelper.containsWord(removeFragments, annotation.getValue())) {
                LOGGER.debug("Remove by fragment: {}", annotation.getValue());
                continue;
            }
            result.add(annotation);
        }
        LOGGER.debug("Filter removed {} annotations", annotations.size() - result.size());
        return result;
    }

    private Set<String> getParts(String value) {
        return new HashSet<String>(Arrays.asList(value.split("\\s")));
    }

}
