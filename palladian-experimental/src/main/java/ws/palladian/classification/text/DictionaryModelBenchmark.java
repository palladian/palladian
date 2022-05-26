package ws.palladian.classification.text;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import ws.palladian.classification.text.evaluation.TextDatasetIterator;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;

public class DictionaryModelBenchmark {

    private static final String trainFile = "/Users/pk/Desktop/20newsgroups-18828/index_split1.txt";
    private static final String testFile = "/Users/pk/Desktop/20newsgroups-18828/index_split2.txt";
    private static final FeatureSetting featureSetting = FeatureSettingBuilder.chars(3, 6).maxTerms(1000).create();
    private static final File BENCHMARK_RESULT_CSV = new File("dictionaryModelBenchmark.csv");

    public static void main(String[] args) throws IOException {
        // run tests with 512 MB heap
        benchmark(new DictionaryTrieModel.Builder());
        benchmark(new LuceneDictionaryModel.Builder(FileHelper.getTempFile()));
    }

    public static void benchmark(DictionaryBuilder builder) {
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting, builder);
        TextDatasetIterator trainIterator = new TextDatasetIterator(trainFile, " ", true);
        TextDatasetIterator testIterator = new TextDatasetIterator(testFile, " ", true);

        DictionaryModel model = null;
        try {
            StringBuilder resultBuilder = new StringBuilder();
            resultBuilder.append(builder.toString()).append(';');

            StopWatch stopWatch = new StopWatch();
            model = classifier.train(trainIterator);
            resultBuilder.append(stopWatch.getElapsedTime(true)).append(';');

            stopWatch = new StopWatch();
            ConfusionMatrix evaluationResult = ClassifierEvaluation.evaluate(classifier, testIterator, model);
            resultBuilder.append(stopWatch.getElapsedTime(true)).append(';');
            
            resultBuilder.append(evaluationResult.getAccuracy()).append('\n');

            FileHelper.appendFile(BENCHMARK_RESULT_CSV.getAbsolutePath(), resultBuilder);
        } catch (Exception e) {
            e.printStackTrace();
            FileHelper.appendFile(BENCHMARK_RESULT_CSV.getAbsolutePath(), builder.toString() + ";" + e.toString() + ";\n");
        } finally {
            if (model instanceof Closeable) {
                FileHelper.close((Closeable)model);
            }
            gc();
        }
    }

    /**
     * This method guarantees that garbage collection is
     * done unlike <code>{@link System#gc()}</code>
     */
    public static void gc() {
        Object obj = new Object();
        WeakReference<Object> ref = new WeakReference<Object>(obj);
        obj = null;
        while (ref.get() != null) {
            System.gc();
        }
    }

}
