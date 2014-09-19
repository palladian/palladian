package ws.palladian.extraction.location.experimental;

import static ws.palladian.extraction.location.experimental.PatternAnalyzer.Direction.LEFT;
import static ws.palladian.extraction.location.experimental.PatternAnalyzer.Direction.RIGHT;
import static ws.palladian.helper.functional.Filters.and;
import static ws.palladian.helper.functional.Functions.LOWERCASE;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.DictionaryBuilder;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.DictionaryModel.DictionaryEntry;
import ws.palladian.classification.text.DictionaryTrieModel;
import ws.palladian.classification.text.PruningStrategies;
import ws.palladian.core.Annotation;
import ws.palladian.core.CategoryEntries;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

public class PatternAnalyzer {

    public static enum Direction {
        LEFT, RIGHT
    }

    /**
     * 
     * @param inputFile File with the XML annotated entities.
     * @param outputFile File to which the results will be written.
     * @param direction Specify whether to extract left/right contexts.
     * @param size The maximum size of the contexts in words (1-n are actually extracted).
     * @param minCount The minimum number of occurrences for a context.
     * @param minProb The minimum probability for a context.
     * @param categories
     */
    public static void getPatterns(File inputFile, File outputPath, final Direction direction, final int size,
            int minCount, double minProb, String... categories) {
        Validate.notNull(inputFile, "inputFile must not be null");
        Validate.notNull(outputPath, "outputPath must not be null");
        Validate.notNull(direction, "direction must not be null");
        Validate.isTrue(size > 0, "size must be greater zero");
        Validate.isTrue(minCount > 0, "minCount must be greater zero");
        Validate.isTrue(minProb > 0, "minProb must be greater zero");
        final DictionaryBuilder builder = new DictionaryTrieModel.Builder();
        final int numLines = FileHelper.getNumberOfLines(inputFile);
        final Set<String> categorySet = categories.length != 0 ? CollectionHelper.newHashSet(categories) : null;
        final ProgressMonitor monitor = new ProgressMonitor();
        monitor.startTask(null, numLines);
        FileHelper.performActionOnEveryLine(inputFile, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                monitor.increment();
                if (line.startsWith("=-DOCSTART-")) {
                    return;
                }
                line = StringHelper.replaceProtectedSpace(line);
                line = StringHelper.normalizeQuotes(line);
                Annotations<Annotation> annotations = FileFormatParser.getAnnotationsFromXmlText(line);
                String cleanText = HtmlHelper.stripHtmlTags(line);
                for (Annotation annotation : annotations) {
                    if (categorySet == null || categorySet.contains(annotation.getTag())) {
                        List<String> context;
                        if (direction == LEFT) {
                            context = NerHelper.getLeftContexts(annotation, cleanText, size);
                        } else {
                            context = NerHelper.getRightContexts(annotation, cleanText, size);
                        }
                        context = CollectionHelper.convertList(context, LOWERCASE);
                        builder.addDocument(context, annotation.getTag());
                    }
                }
            }
        });
        Set<Filter<CategoryEntries>> pruningStrategies = CollectionHelper.newHashSet();
        pruningStrategies.add(new PruningStrategies.TermCountPruningStrategy(minCount));
        pruningStrategies.add(new PruningStrategies.MinProbabilityPruningStrategy(minProb));
        builder.setPruningStrategy(and(pruningStrategies));
        DictionaryModel dictionary = builder.create();
        System.out.println(dictionary);
        File outputFile = new File(outputPath, "contexts_" + direction + "_" + System.currentTimeMillis() + ".txt");
        for (DictionaryEntry dictionaryEntry : dictionary) {
            String tag = dictionaryEntry.getCategoryEntries().getMostLikelyCategory();
            String line;
            if (direction == LEFT) {
                line = dictionaryEntry.getTerm() + " *\t" + tag + "\n";
            } else {
                line = "* " + dictionaryEntry.getTerm() + "\t" + tag + "\n";
            }
            FileHelper.appendFile(outputFile.getPath(), line);
        }
    }

    public static void main(String[] args) throws Exception {
        // File inputFile = new File("/Users/pk/Desktop/allCleansed.xml");
        File inputFile = new File("/Users/pk/Desktop/Wikipedia-EN-entity-dataset/annotations-combined.xml");
        File outputPath = new File("/Users/pk/Desktop");
        Direction direction = RIGHT;
        getPatterns(inputFile, outputPath, direction, /* size */3, /* minCount */50, /* minProb */0.9);
    }

}
