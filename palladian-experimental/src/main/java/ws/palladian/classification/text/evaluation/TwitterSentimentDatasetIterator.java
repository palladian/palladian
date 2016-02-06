package ws.palladian.classification.text.evaluation;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.io.CsvHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineIterator;
import ws.palladian.helper.nlp.StringHelper;

public final class TwitterSentimentDatasetIterator implements Iterable<Instance> {

    /**
     * The normalization options as described in "<a href="http://s3.eddieoz.com/docs/sentiment_analysis/
     * Twitter_Sentiment_Classification_using_Distant_Supervision
     * .pdf">Twitter Sentiment Classification using Distant Supervision</a>"; Alec Go;
     * Richa Bhayani; Lei Huang; 2009.
     */
    public enum NormalizationOptions {
        /** Replace query terms by 'QUERY_TERM'. */
        QUERY_TERM,
        /** Replace user names by 'USERNAME'. */
        USER_NAMES,
        /** Replace links by 'URL'. */
        LINKS,
        /** Normalize repeated letters in words (e.g. 'huuuuuuungry' becomes 'huungry'). */
        REPEATED_LETTERS
    }

    private final File datasetFile;
    private final Set<NormalizationOptions> options;
    private final int numLines;

    public TwitterSentimentDatasetIterator(File datasetFile, NormalizationOptions... options) {
        Validate.notNull(datasetFile, "datasetFile must not be null");
        this.datasetFile = datasetFile;
        this.options = CollectionHelper.newHashSet(options);
        this.numLines = FileHelper.getNumberOfLines(datasetFile);
    }

    public TwitterSentimentDatasetIterator(File datasetFile) {
        this(datasetFile, NormalizationOptions.values());
    }

    @Override
    public Iterator<Instance> iterator() {
        LineIterator lineIterator = new LineIterator(datasetFile);
        final ProgressMonitor monitor = new ProgressMonitor();
        monitor.startTask(getClass().getSimpleName(), numLines);
        Function<String, Instance> converter = new Function<String, Instance>() {
            @Override
            public Instance compute(String input) {
                List<String> split = CsvHelper.splitCsvLine(input, ',');
                if (split.size() != 6) {
                    throw new IllegalStateException("Expected six columns, got " + split.size() + " in '" + input + "'");
                }
                String category = split.get(0);
                String text = split.get(5);
                if (options.contains(NormalizationOptions.QUERY_TERM)) {
                    String queryTerm = split.get(3);
                    text = StringHelper.replaceWord(queryTerm, "QUERY_TERM", text);
                }
                if (options.contains(NormalizationOptions.USER_NAMES)) {
                    text = text.replaceAll("@[^\\s]+", "USERNAME");
                }
                if (options.contains(NormalizationOptions.LINKS)) {
                    text = text.replaceAll("https?://[^\\s]+", "URL");
                }
                if (options.contains(NormalizationOptions.REPEATED_LETTERS)) {
                    text = text.replaceAll("(\\w)\\1{3,}", "$1$1");
                }
                monitor.increment();
                return new InstanceBuilder().setText(text).create(category);
            }
        };
        return CollectionHelper.convert(lineIterator, converter);
    }

    public static void main(String[] args) {
        File trainData = new File("/Users/pk/Downloads/trainingandtestdata/training.1600000.processed.noemoticon.csv");
        // File testData = new File("/Users/pk/Downloads/trainingandtestdata/testdata.manual.2009.06.14.csv");
        TwitterSentimentDatasetIterator dataset = new TwitterSentimentDatasetIterator(trainData);
        CollectionHelper.print(dataset);
    }

}
