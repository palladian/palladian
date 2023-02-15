//package ws.palladian.kaggle.restaurants.features.imagenet;
//
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//
//import javax.imageio.ImageIO;
//
//import org.junit.Assume;
//import org.junit.BeforeClass;
//import org.junit.Ignore;
//import org.junit.Test;
//
//import ws.palladian.core.FeatureVector;
//import ws.palladian.core.value.NumericValue;
//import ws.palladian.core.value.Value;
//import ws.palladian.helper.collection.Vector.VectorEntry;
//import ws.palladian.helper.io.ResourceHelper;
//import ws.palladian.kaggle.restaurants.utils.Config;
//
//@Ignore
//public class ImageNetFeatureExtractorTest {
//	private static File pathToPython;
//	private static File pathToGraph;
//	@BeforeClass
//	public static void getPaths() {
//		try {
//			pathToPython = Config.getFilePath("python.path");
//			pathToGraph = Config.getFilePath("graphdeph.path");
//		} catch (IllegalArgumentException e) {
//			Assume.assumeTrue("property python.path and/or graphdeph.path not present", false);
//		}
//	}
//	@Test
//	public void testImageNetFeatureExtractor() throws FileNotFoundException, IOException {
//		String tensorName = ImageNetFeatureExtractor.TENSOR_SOFTMAX;
//		// String tensorName = ImageNetFeatureExtractor.TENSOR_POOL_3;
//		ImageNetFeatureExtractor extractor = new ImageNetFeatureExtractor(pathToPython, pathToGraph, tensorName);
//		BufferedImage image = ImageIO.read(ResourceHelper.getResourceFile("/911-Turbo-Typ-964.jpeg"));
////		BufferedImage image = ImageIO.read(ResourceHelper.getResourceFile("/mp900440290.jpg"));
////		BufferedImage image = ImageIO.read(ResourceHelper.getResourceFile("/cat-01.jpg"));
////		BufferedImage image = ImageIO.read(ResourceHelper.getResourceFile("/file_23262_entlebucher-mountain-dog-460x290.jpg"));
//		FeatureVector featureVector = extractor.extract(image);
//		String label = null;
//		double max = Double.MIN_VALUE;
//		for (VectorEntry<String, Value> entry : featureVector) {
//			NumericValue numericValue = (NumericValue) entry.value();
//			if (numericValue.getDouble() > max) {
//				max = numericValue.getDouble();
//				label = entry.key();
//			}
//		}
//		//System.out.println(featureVector);
//		//System.out.println(featureVector.size());
//		System.out.println(label +  ": " + max);
//		
//		
//		extractor.close();
//	}
//
//}
