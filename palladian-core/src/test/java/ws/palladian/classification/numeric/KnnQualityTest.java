/**
 * Created on: 03.10.2012 11:29:50
 */
package ws.palladian.classification.numeric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since
 * 
 */
@RunWith(Parameterized.class)
@Ignore
public class KnnQualityTest {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{
                "/home/alaak/git/palladian/palladian-core/src/test/resources/10000qa-featuresetQAO.csv", 0.613}});
    }

    private String datasetPath;

    private Double expectedAccuracy;

    private static final Double EPSILON = 0.003;

    public KnnQualityTest(String datasetPath, Double expectedAccuracy) {
        super();
        this.datasetPath = datasetPath;
        this.expectedAccuracy = expectedAccuracy;
    }

    @Test
    public void test() {
        // create the KNN classifier and add the training instances
        KnnClassifier knn = new KnnClassifier();

        Instances<UniversalInstance> dataset = createInstances(datasetPath);
        List<UniversalInstance> datasetList = new ArrayList<UniversalInstance>(dataset);
        List<UniversalInstance> trainingList = drawRandomSubset(datasetList, 60, 47L);
        Instances<UniversalInstance> trainingSet = new Instances<UniversalInstance>();
        trainingSet.addAll(trainingList);
        trainingSet.normalize();
        knn.setTrainingInstances(trainingSet);

        List<UniversalInstance> testSet = new ArrayList<UniversalInstance>(dataset);
        testSet.removeAll(trainingList);

        int truePositives = 0;
        // classify
        for (UniversalInstance instance : testSet) {
            knn.classify(instance);
            CategoryEntry entry = instance.getMainCategoryEntry();

            if (entry.getCategory().getName().equals(instance.getInstanceCategoryName())) {
                truePositives++;
            }
        }

        double result = Double.valueOf(truePositives) / testSet.size();

        Assert.assertThat(expectedAccuracy, Matchers.is(Matchers.closeTo(result, EPSILON)));
    }

    private static <T> List<T> drawRandomSubset(final List<T> list, final int trainingFraction, final long seed) {
        Random rnd = new Random(seed);
        int m = (trainingFraction * list.size()) / 100;
        for (int i = 0; i < list.size(); i++) {
            int pos = i + rnd.nextInt(list.size() - i);
            T tmp = list.get(pos);
            list.set(pos, list.get(i));
            list.set(i, tmp);
        }
        return list.subList(0, m);
    }

    private static Instances<UniversalInstance> createInstances(String filePath) {
        List<String> trainingLines = FileHelper.readFileToArray(filePath);
        trainingLines.remove(0);

        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();
        UniversalInstance instance = null;
        List<Double> features = null;

        for (String trainingLine : trainingLines) {
            String[] parts = trainingLine.split(",");

            instance = new UniversalInstance(instances);
            features = new ArrayList<Double>();

            for (int f = 1; f < parts.length; f++) {
                features.add(Double.valueOf(parts[f]));
            }

            instance.setNumericFeatures(features);
            instance.setClassNominal(true);
            // instance.setInstanceClass(parts[parts.length - 1]);
            instance.setInstanceCategory(parts[0]);
            instances.add(instance);
        }

        return instances;
    }
}
