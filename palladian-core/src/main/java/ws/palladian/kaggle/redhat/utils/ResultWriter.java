package ws.palladian.kaggle.redhat.utils;

import java.io.File;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.kaggle.redhat.classifier.ClassifierWithModel;

public class ResultWriter {
	public static void writeClassificationResults(ClassifierWithModel<?> classifier, Dataset dataset, File file) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("activityId;peopleGroup1;classified;actual\n");
		for (Instance instance : dataset) {
			CategoryEntries result = classifier.classify(instance.getVector());
			double probability = result.getCategory("1").getProbability();
			String peopleGroup1 = instance.getVector().getNominal("people_group_1").getString();
			String activityId = instance.getVector().getNominal("activity_id").getString();
			buffer.append(activityId).append(';');
			buffer.append(peopleGroup1).append(';');
			buffer.append(probability).append(';');
			buffer.append(instance.getCategory()).append('\n');
		}
		FileHelper.writeToFile(file.getAbsolutePath(), buffer);
	}
}
