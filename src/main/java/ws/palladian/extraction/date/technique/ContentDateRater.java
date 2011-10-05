package ws.palladian.extraction.date.technique;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.configuration.PropertiesConfiguration;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.SerializationHelper;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.helper.Cache;
import ws.palladian.helper.ConfigHolder;
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

    public static final String DATE_CLASSIFIER_IDENTIFIER = "wekaRandomCommitteeObjectModel";

	public ContentDateRater(PageDateType dateType) {
		super(dateType);
		loadClasifier();
	}

	private void loadClasifier() {
        final PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

		String classifierFile;
		if (this.dateType.equals(PageDateType.publish)) {
            classifierFile = config.getString("models.root") + config.getString("models.palladian.date.published");
            // classifierFile = "/wekaClassifier/pubClassifierFinal.model";
		} else {
            classifierFile = config.getString("models.root") + config.getString("models.palladian.date.modified");
            // classifierFile = "/wekaClassifier/modClassifierFinal.model";
		}
		try {
			// String modelPath = ContentDate.class.getResource(classifierFile)
			// .getFile();
			// this.classifier = (Classifier)
			// SerializationHelper.read(modelPath);
            this.classifier = (Classifier) Cache.getInstance().getDataObject(DATE_CLASSIFIER_IDENTIFIER);
			if (this.classifier == null) {
				System.out.println("load classifier");
				InputStream stream = ContentDateRater.class
						.getResourceAsStream(classifierFile);
				this.classifier = (Classifier) SerializationHelper.read(stream);
                Cache.getInstance().putDataObject(DATE_CLASSIFIER_IDENTIFIER, this.classifier);
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
			if (this.dateType.equals(PageDateType.publish) && date.isInUrl()) {
				returnDates.put(date, 1.0);
			} else{
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

		}
		return returnDates;
	}
}
