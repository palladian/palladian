package ws.palladian.kaggle.restaurants.features.descriptors;

import java.util.Collection;

import ij.process.ImageProcessor;
import mpicbg.ij.SIFT;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;

public class SiftDescriptorExtractor extends MpicbgDescriptorExtractor {
	public static final SiftDescriptorExtractor SIFT = new SiftDescriptorExtractor();

	private SiftDescriptorExtractor() {
		// singleton
	}

	@Override
	protected Collection<Feature> extract(ImageProcessor processor) {
		SIFT sift = new SIFT(new FloatArray2DSIFT(new FloatArray2DSIFT.Param()));
		return sift.extractFeatures(processor);
	}
	
	@Override
	public String toString() {
		return "SIFT";
	}
}
