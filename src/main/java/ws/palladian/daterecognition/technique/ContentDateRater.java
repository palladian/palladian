package ws.palladian.daterecognition.technique;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.SerializationHelper;
import ws.palladian.daterecognition.KeyWords;
import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.helper.Cache;
import ws.palladian.helper.date.DateWekaInstanceFactory;

/**
 *This class evaluates content-dates. <br>
 *Doing this by dividing dates in three parts: Keyword in attribute, in text
 * and no keyword.<br>
 * Each part will be rate different.<br>
 * Part one by keyword classes, see {@link KeyWords#getKeywordPriority(String)}
 * and age. Part two by distance of keyword an date, keyword classes and age.
 * Part three by age.
 * 
 * @author Martin Gregor
 * 
 */
public class ContentDateRater extends TechniqueDateRater<ContentDate> {

	Classifier classifier = null;

	public ContentDateRater(PageDateType dateType) {
		super(dateType);
		loadClasifier();
	}

	private void loadClasifier() {
		String classifierFile;
		String classifierCacheString = "wekaRandomCommitteeObjectModel";
		if (this.dateType.equals(PageDateType.publish)) {
			classifierFile = "/wekaClassifier/pubClassifierFinal.model";
		} else {
			classifierFile = "/wekaClassifier/modClassifierFinal.model";
		}
		try {
			// String modelPath = ContentDate.class.getResource(classifierFile)
			// .getFile();
			// this.classifier = (Classifier)
			// SerializationHelper.read(modelPath);
			this.classifier = (Classifier) Cache.getInstance().getDataObject(
					classifierCacheString);
			if (this.classifier == null) {
				System.out.println("load classifier");
				InputStream stream = ContentDateRater.class
						.getResourceAsStream(classifierFile);
				this.classifier = (Classifier) SerializationHelper.read(stream);
				Cache.getInstance().putDataObject(classifierCacheString,
						this.classifier);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}

	}

	@Override
	public HashMap<ContentDate, Double> rate(ArrayList<ContentDate> list) {

		int pubModCLassifierIndex;
		HashMap<ContentDate, Double> returnDates = new HashMap<ContentDate, Double>();
		DateWekaInstanceFactory dwif = new DateWekaInstanceFactory(
				this.dateType);

		if(this.dateType.equals(PageDateType.publish)){
			pubModCLassifierIndex = 0;
		}else{
			pubModCLassifierIndex = 0;
		}
		
		for (ContentDate date : list) {
//			if (this.dateType.equals(PageDateType.publish) && date.isInUrl()) {
//				returnDates.put(date, 1.0);
//			} else if(date.isInMetaDates()){
//				returnDates.put(date, 0.99);
//			} else{
				Instance instance = dwif.getDateInstanceByArffTemplate(date);
				try {
					double[] dbl = this.classifier
							.distributionForInstance(instance);
					returnDates.put(date, dbl[pubModCLassifierIndex]);
				} catch (Exception e) {
					System.out.println(date.getDateString());
					System.out.println(instance);
					e.printStackTrace();
				}
			}

//		}
		return returnDates;
	}
}
