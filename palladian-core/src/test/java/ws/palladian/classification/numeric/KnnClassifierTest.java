package ws.palladian.classification.numeric;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.ClassificationUtils;
import ws.palladian.classification.Instances;
import ws.palladian.classification.NominalInstance;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Tests whether the numerical Knn Classifier works correctly or not.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public class KnnClassifierTest {

	/**
	 * <p>
	 * Tests the typical in memory usage of the Knn classifier. It is trained
	 * with three instances and tried out on one {@link FeatureVector}. In the
	 * end the top class and its absolut relevance need to be correct.
	 * </p>
	 */
	@Test
	public void testKnnClassifier() {

		// create some instances for the vector space
		List<NominalInstance> trainingInstances = new ArrayList<NominalInstance>(
				3);

		NominalInstance trainingInstance1 = new NominalInstance();
		trainingInstance1.targetClass = "A";
		FeatureVector vector1 = new FeatureVector();
		vector1.add(new NumericFeature("f1", 3d));
		vector1.add(new NumericFeature("f2", 4d));
		vector1.add(new NumericFeature("f3", 5d));
		trainingInstance1.featureVector = vector1;
		trainingInstances.add(trainingInstance1);

		NominalInstance trainingInstance2 = new NominalInstance();
		trainingInstance2.targetClass = "A";
		FeatureVector vector2 = new FeatureVector();
		vector2.add(new NumericFeature("f1", 3d));
		vector2.add(new NumericFeature("f2", 6d));
		vector2.add(new NumericFeature("f3", 6d));
		trainingInstance2.featureVector = vector2;
		trainingInstances.add(trainingInstance2);

		NominalInstance trainingInstance3 = new NominalInstance();
		trainingInstance3.targetClass = "B";
		FeatureVector vector3 = new FeatureVector();
		vector3.add(new NumericFeature("f1", 4d));
		vector3.add(new NumericFeature("f2", 4d));
		vector3.add(new NumericFeature("f3", 4d));
		trainingInstance3.featureVector = vector3;
		trainingInstances.add(trainingInstance3);

		// create the KNN classifier and add the training instances
		KnnClassifier knn = new KnnClassifier();
		KnnModel model = knn.learn(trainingInstances);

		FeatureVector newInstance = new FeatureVector();
		newInstance.add(new NumericFeature("f1", 1d));
		newInstance.add(new NumericFeature("f2", 2d));
		newInstance.add(new NumericFeature("f3", 3d));

		// classify
		CategoryEntries result = knn.predict(newInstance, model);

		assertEquals(0.4743704726540487, ClassificationUtils
				.getSingleBestCategoryEntry(result).getAbsoluteRelevance(), 0);
		assertEquals("A", ClassificationUtils
				.getSingleBestCategoryEntry(result).getCategory().getName());
	}

	/**
	 * <p>
	 * Tests whether the {@link KnnClassifier} works correctly on a larger
	 * dataset loaded directly from a CSV file.
	 * </p>
	 * 
	 * @throws FileNotFoundException
	 *             If the input data could not be found.
	 */
	@Test
	public void testKnnClassifierLoadFromFile() throws FileNotFoundException {

		// create the KNN classifier and add the training instances
		KnnClassifier knn = new KnnClassifier(3);
		KnnModel model = knn.learn(ClassificationUtils
				.createInstances(ResourceHelper
						.getResourcePath("/classifier/wineData.txt")));

		// create an instance to classify
		// 13.82;1.75;2.42;14;111;3.88;3.74;.32;1.87;7.05;1.01;3.26;1190;1 =>
		// this is an actual instance from the
		// training data and should therefore also be classified as "1"
		FeatureVector newInstance = new FeatureVector();
		newInstance.add(new NumericFeature("0", 13.82));
		newInstance.add(new NumericFeature("1", 1.75));
		newInstance.add(new NumericFeature("2", 2.42));
		newInstance.add(new NumericFeature("3", 14d));
		newInstance.add(new NumericFeature("4", 111d));
		newInstance.add(new NumericFeature("5", 3.88));
		newInstance.add(new NumericFeature("6", 3.74));
		newInstance.add(new NumericFeature("7", .32));
		newInstance.add(new NumericFeature("8", 1.87));
		newInstance.add(new NumericFeature("9", 7.05));
		newInstance.add(new NumericFeature("10", 1.01));
		newInstance.add(new NumericFeature("11", 3.26));
		newInstance.add(new NumericFeature("12", 1190d));

		// classify
		CategoryEntries result = knn.predict(newInstance, model);

		assertEquals(1.0000000001339825E9, ClassificationUtils
				.getSingleBestCategoryEntry(result).getAbsoluteRelevance(), 0);
		assertEquals("1", ClassificationUtils
				.getSingleBestCategoryEntry(result).getCategory().getName());
	}

	@Test
	public void testKnnClassifierLoadFromFileNormalize()
			throws IOException {

		// create the KNN classifier and add the training instances
		KnnClassifier knn = new KnnClassifier(3);
		KnnModel model = knn.learn(ClassificationUtils
				.createInstances("/home/muthmann/git/palladian/palladian-core/src/test/resources/classifier/wineData.txt"));
//		knn.getTrainingInstances().normalize();
		model.normalize();

        String tempDir = System.getProperty("java.io.tmpdir");
		FileHelper.serialize(model, tempDir+"/testKNN.gz");

		KnnModel loadedModel = FileHelper.deserialize(tempDir + "/testKNN.gz");

		// create an instance to classify
		// 13.82;1.75;2.42;14;111;3.88;3.74;.32;1.87;7.05;1.01;3.26;1190;1 =>
		// this is an actual instance from the
		// training data and should therefore also be classified as "1"
		// UniversalInstance newInstance = new UniversalInstance(null);
		FeatureVector vector = new FeatureVector();
		vector.add(new NumericFeature("f1", 13.82));
		vector.add(new NumericFeature("f2", 1.75));
		vector.add(new NumericFeature("f3", 2.42));
		vector.add(new NumericFeature("f4", 14d));
		vector.add(new NumericFeature("f5", 111d));
		vector.add(new NumericFeature("f6", 3.88));
		vector.add(new NumericFeature("f7", 3.74));
		vector.add(new NumericFeature("f8", .32));
		vector.add(new NumericFeature("f9", 1.87));
		vector.add(new NumericFeature("f10", 7.05));
		vector.add(new NumericFeature("f11", 1.01));
		vector.add(new NumericFeature("f12", 3.26));
		vector.add(new NumericFeature("f13", 1190d));

		// classify
		CategoryEntries result = knn.predict(vector, loadedModel);

		assertEquals(1.0000000054326154E9, ClassificationUtils
				.getSingleBestCategoryEntry(result).getAbsoluteRelevance(), 0);
		assertEquals("1", ClassificationUtils
				.getSingleBestCategoryEntry(result).getCategory().getName());
	}

}