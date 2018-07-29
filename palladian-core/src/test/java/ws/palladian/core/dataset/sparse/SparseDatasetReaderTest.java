//package ws.palladian.core.dataset.sparse;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//
//import org.junit.Test;
//
//import ws.palladian.core.Instance;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.FeatureInformation;
//import ws.palladian.core.value.ImmutableStringValue;
//import ws.palladian.helper.io.ResourceHelper;
//
//public class SparseDatasetReaderTest {
//	@Test
//	public void testReadSparseDataset_withHeader() throws FileNotFoundException {
//		File sparseFile = ResourceHelper.getResourceFile("/sample-dataset.sparse");
//		SparseDatasetReader reader = new SparseDatasetReader(sparseFile);
//
//		assertEquals(13, reader.size());
//
//		FeatureInformation featureInfo = reader.getFeatureInformation();
//		assertEquals(5581, featureInfo.count());
//		assertTrue(featureInfo.getFeatureInformation("people_id").isCompatible(ImmutableStringValue.class));
//
//		Instance instance = reader.iterator().next();
//		assertEquals("ppl_100", instance.getVector().get("people_id").toString());
//	}
//
//	@Test
//	public void testReadSparseDataset_withoutHeader() throws FileNotFoundException {
//		File sparseFile = ResourceHelper.getResourceFile("/sample-dataset-without-header.sparse");
//		SparseDatasetReader reader = new SparseDatasetReader(sparseFile);
//		assertEquals(13, reader.size());
//
//		FeatureInformation featureInfo = reader.getFeatureInformation();
//		assertTrue(featureInfo.getFeatureInformation("0").isCompatible(ImmutableStringValue.class));
//
//		Instance instance = reader.iterator().next();
//		assertEquals("ppl_100", instance.getVector().get("0").toString());
//
//	}
//	
//	@Test
//	public void testBuffer() throws FileNotFoundException {
//		File sparseFile = ResourceHelper.getResourceFile("/sample-dataset.sparse");
//		Dataset dataset = new SparseDatasetReader(sparseFile);
//		Dataset bufferedDataset = dataset.buffer();
//		
//		assertEquals("expected the buffered dataset to be equal the original dataset", dataset, bufferedDataset);
//
//	}
//	
//	@Test
//	public void testReadStrings() throws FileNotFoundException {
//		File sparseFile = ResourceHelper.getResourceFile("/sample-dataset-string-values.sparse");
//		Dataset dataset = new SparseDatasetReader(sparseFile);
//		Instance firstInstance = dataset.iterator().next();
//		assertEquals(4, firstInstance.getVector().size());
//		assertEquals("string", firstInstance.getVector().getNominal("0").getString());
//		assertEquals("string", firstInstance.getVector().getNominal("1").getString());
//		assertEquals("string with spaces", firstInstance.getVector().getNominal("2").getString());
//		assertEquals("string with spaces: and colon", firstInstance.getVector().getNominal("3").getString());
//	}
//
//}
