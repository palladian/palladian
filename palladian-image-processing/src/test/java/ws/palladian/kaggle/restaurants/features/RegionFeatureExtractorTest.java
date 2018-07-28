package ws.palladian.kaggle.restaurants.features;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.junit.rules.ErrorCollector;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.features.RegionFeatureExtractor;
import ws.palladian.helper.collection.Vector;
import ws.palladian.helper.io.ResourceHelper;

public class RegionFeatureExtractorTest {

	public ErrorCollector collector = new ErrorCollector();

	@Test
	public void testRegionFeatureExtractor() throws FileNotFoundException, IOException {
		BufferedImage image = ImageIO.read(ResourceHelper.getResourceFile("/51612.jpg"));
		FeatureVector result = RegionFeatureExtractor.REGION.extract(image);
//		System.out.println(result);
		// gives NaN values for mean_region_size, main_region_size,
		// main_region_dominance, and main_region_coverage; however, I see a big
		// sweet region in the images center

		for (Vector.VectorEntry<String, Value> ve : result) {
			NumericValue value = (NumericValue) ve.value();
			collector.checkThat(value.getDouble(), not(is(Double.NaN)));
		}

		// same here
		image = ImageIO.read(ResourceHelper.getResourceFile("/339720.jpg"));
		result = RegionFeatureExtractor.REGION.extract(image);
//		System.out.println(result);
		for (Vector.VectorEntry<String, Value> ve : result) {
			NumericValue value = (NumericValue) ve.value();
			collector.checkThat(value.getDouble(), not(is(Double.NaN)));
		}

		// here, we get an infinity value for main_region_coverage
		image = ImageIO.read(ResourceHelper.getResourceFile("/261444.jpg"));
		result = RegionFeatureExtractor.REGION.extract(image);
//		System.out.println(result);
		for (Vector.VectorEntry<String, Value> ve : result) {
			NumericValue value = (NumericValue) ve.value();
			collector.checkThat(value.getDouble(), not(is(Double.NaN)));
		}

	}

}
