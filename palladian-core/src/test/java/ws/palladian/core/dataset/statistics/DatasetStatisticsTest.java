package ws.palladian.core.dataset.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.statistics.DatasetStatistics.ValueStatistics;
import ws.palladian.helper.io.ResourceHelper;

public class DatasetStatisticsTest {
	@Test
	public void testDatasetStatistics() throws FileNotFoundException {
		Builder configBuilder = CsvDatasetReaderConfig.filePath(ResourceHelper.getResourceFile("/classifier/saheart.csv"));
		configBuilder.readHeader(true);
		configBuilder.setFieldSeparator(",");
		Dataset dataset = configBuilder.create();

		DatasetStatistics statistics = new DatasetStatistics(dataset);

		ValueStatistics statistics1 = statistics.getValueStatistics("Sbp");
		assertTrue(statistics1 instanceof NumericValueStatistics);
		NumericValueStatistics numericValueStatistics = (NumericValueStatistics) statistics1;
		assertEquals(0, numericValueStatistics.getNumNullValues());
		assertEquals(138.33, numericValueStatistics.getMean(), 0.01);
		assertEquals(101, numericValueStatistics.getMin(), 0.01);
		assertEquals(218, numericValueStatistics.getMax(), 0.01);

		ValueStatistics statistics2 = statistics.getValueStatistics("Famhist");
		assertTrue(statistics2 instanceof NominalValueStatistics);
		NominalValueStatistics nominalValueStatistics = (NominalValueStatistics) statistics2;
		assertEquals(0, nominalValueStatistics.getNumNullValues());
		assertEquals(2, nominalValueStatistics.getNumUniqueValues(), 0.01);
		assertEquals(270, nominalValueStatistics.getCount("Absent"));
		assertEquals(192, nominalValueStatistics.getCount("Present"));

		// System.out.println(statistics);
	}

}
