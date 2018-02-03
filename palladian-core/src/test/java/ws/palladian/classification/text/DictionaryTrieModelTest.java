package ws.palladian.classification.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class DictionaryTrieModelTest extends AbstractDictionaryModelTest {

	@Test
	public void testBackwardsCompatibility() throws FileNotFoundException, IOException {
		DictionaryModel model_v1 = FileHelper
				.deserialize(ResourceHelper.getResourcePath("/model/testDictionaryTrieModel_v1.ser"));
		assertTrue(model_v1.equals(model));
	}

	@Test
	public void testPruning() {
		DictionaryTrieModel.Builder builder = new DictionaryTrieModel.Builder();
		builder.addDictionary(model);
		builder.setPruningStrategy(PruningStrategies.termCount(2));
		model = builder.create();
		assertEquals(4, model.getNumEntries());
		assertEquals(3, model.getNumUniqTerms());
		assertEquals(5, model.getTermCounts().getCount(CATEGORY_1));
		assertEquals(4, model.getTermCounts().getCount(CATEGORY_2));
	}

}
