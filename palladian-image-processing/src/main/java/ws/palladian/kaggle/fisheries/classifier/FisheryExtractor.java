//package ws.palladian.kaggle.fisheries.classifier;
//
//import static ws.palladian.kaggle.restaurants.features.BoundsFeatureExtractor.BOUNDS;
//import static ws.palladian.kaggle.restaurants.features.ColorFeatureExtractor.COLOR;
//import static ws.palladian.kaggle.restaurants.features.color.HSB.BRIGHTNESS;
//import static ws.palladian.kaggle.restaurants.features.color.HSB.HUE;
//import static ws.palladian.kaggle.restaurants.features.color.HSB.SATURATION;
//import static ws.palladian.kaggle.restaurants.features.color.Luminosity.LUMINOSITY;
//import static ws.palladian.kaggle.restaurants.features.color.RGB.BLUE;
//import static ws.palladian.kaggle.restaurants.features.color.RGB.GREEN;
//import static ws.palladian.kaggle.restaurants.features.color.RGB.RED;
//import static ws.palladian.kaggle.restaurants.features.descriptors.MopsDescriptorExtractor.MOPS;
//import static ws.palladian.kaggle.restaurants.features.descriptors.SiftDescriptorExtractor.SIFT;
//import static ws.palladian.kaggle.restaurants.features.descriptors.SurfDescriptorExtractor.SURF;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.DefaultDataset;
//import ws.palladian.helper.math.MathHelper;
//import ws.palladian.kaggle.fisheries.utils.Config;
//import ws.palladian.kaggle.restaurants.Extractor;
//import ws.palladian.kaggle.restaurants.dataset.DirectoryDatasetReader;
//import ws.palladian.kaggle.restaurants.features.FeatureExtractor;
//import ws.palladian.kaggle.restaurants.features.PoiFeatureExtractor;
//import ws.palladian.kaggle.restaurants.features.StatisticsFeatureExtractor;
//import ws.palladian.kaggle.restaurants.features.imagenet.ImageNetFeatureExtractor;
//
//public class FisheryExtractor {
//	public static void main(String[] args) throws IOException {
//		DirectoryDatasetReader trainingDataReader = new DirectoryDatasetReader(Config.getTrainingPath());
//		
//		List<FeatureExtractor> extractors = new ArrayList<>();
//		// extractors.add(new StatisticsFeatureExtractor(LUMINOSITY, RED, GREEN, BLUE, HUE, SATURATION, BRIGHTNESS));
//		// extractors.add(BOUNDS);
////		Collection<File> vocabularyFiles = MathHelper.sample(trainingDataReader.getImageFiles(), 100);
////		extractors.add(PoiFeatureExtractor.buildVocabulary(SIFT, vocabularyFiles));
////		extractors.add(COLOR);
////		extractors.add(PoiFeatureExtractor.buildVocabulary(MOPS, trainingDataReader.getImageFiles()));
////		extractors.add(PoiFeatureExtractor.buildVocabulary(SURF, trainingDataReader.getImageFiles()));
//		
//		File pathToPython = Config.getFilePath("python.path");
//		File pathToGraph = Config.getFilePath("graphdeph.path");
//		extractors.add(new ImageNetFeatureExtractor(pathToPython, pathToGraph, ImageNetFeatureExtractor.TENSOR_SOFTMAX));
//		extractors.add(new ImageNetFeatureExtractor(pathToPython, pathToGraph, ImageNetFeatureExtractor.TENSOR_POOL_3));
//
////		Dataset trainingSet = new DefaultDataset(trainingDataReader);
////		File trainingCsvOutput = new File("/Users/pk/Desktop/training_inception_features.csv");
////		Extractor.run(trainingSet, trainingCsvOutput, extractors);
//
//		Dataset testingSet = new DefaultDataset(new DirectoryDatasetReader(Config.getTestStg1Path()));
//		File testingCsvOutput = new File("/Users/pk/Desktop/testing_inception_features.csv");
//		Extractor.run(testingSet, testingCsvOutput, extractors);
//
//	}
//}
