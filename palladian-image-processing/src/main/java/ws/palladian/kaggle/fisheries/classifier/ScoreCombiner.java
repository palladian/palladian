package ws.palladian.kaggle.fisheries.classifier;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.Instance;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.kaggle.fisheries.utils.Config;

public class ScoreCombiner {
	public static void main(String[] args) throws IOException {
		/* combine(
				"/Users/pk/Desktop/kaggle-fisheries/submission_2016-12-23_17-57-42.csv",
				"/Users/pk/Desktop/kaggle-fisheries/submission_2016-12-22_22-47-37.csv"
				); */
		// scale("/Users/pk/Desktop/kaggle-fisheries/submission_2016-12-23_18-30-36.csv", 1.5f);
		// scale("/Users/pk/Desktop/kaggle-fisheries/submission_2016-12-23_18-30-36.csv", .5f);
		// scale("/Users/pk/Desktop/kaggle-fisheries/submission_2016-12-23_18-30-36.csv", .9f);
		// scale("/Users/pk/Desktop/kaggle-fisheries/submission_2016-12-23_18-30-36.csv", .8f);
		
		scale("/Users/pk/Desktop/kaggle-fisheries/submission_2016-12-23_17-57-42.csv", .9f);
	}
	
	private static final String[] categories = "ALB,BET,DOL,LAG,NoF,OTHER,SHARK,YFT".split(",");

	public static void scale(String file, float power) {
		CsvDatasetReader dataset = CsvDatasetReaderConfig.filePath(file).setFieldSeparator(',').readClassFromLastColumn(false).create();
		
		StringBuilder csv = new StringBuilder();
		csv.append("image,ALB,BET,DOL,LAG,NoF,OTHER,SHARK,YFT\n");

		
		for (Instance instance : dataset) {
			String image = instance.getVector().getNominal("image").getString();
			CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
			for (String category : categories) {
				double probability = Math.pow(instance.getVector().getNumeric(category).getDouble(), power);
				builder.add(category, probability);
			}
			CategoryEntries categoryEntries = builder.create();
			csv.append(image);
			for (String category : categories) {
				csv.append(',');
				csv.append(categoryEntries.getProbability(category));
			}
			csv.append('\n');
		}
		String csvPath = Config.getDataPath("submission_" + DateHelper.getCurrentDatetime() + ".csv").getPath();
		FileHelper.writeToFile(csvPath, csv);
	}

	public static void combine(String... files) {
		
		
		Map<String, CategoryEntriesBuilder> entries = new LazyMap<>(new TreeMap<>(), CategoryEntriesBuilder.FACTORY);
		
		for (String file : files) {
			CsvDatasetReader dataset = CsvDatasetReaderConfig.filePath(file).setFieldSeparator(',').readClassFromLastColumn(false).create();
			for (Instance instance : dataset) {
				String image = instance.getVector().getNominal("image").getString();
				for (String category : categories) {
					double probability = instance.getVector().getNumeric(category).getDouble();
					entries.get(image).add(category, probability);
				}
			}
		}
		
		StringBuilder csv = new StringBuilder();
		csv.append("image,ALB,BET,DOL,LAG,NoF,OTHER,SHARK,YFT\n");
		for (Entry<String, CategoryEntriesBuilder> entry : entries.entrySet()) {
			csv.append(entry.getKey());
			for (String category : categories) {
				csv.append(',');
				csv.append(entry.getValue().create().getProbability(category));
			}
			csv.append('\n');
		}
		// System.out.println(csv);
		
		String csvPath = Config.getDataPath("submission_" + DateHelper.getCurrentDatetime() + ".csv").getPath();
		FileHelper.writeToFile(csvPath, csv);

	}
}
