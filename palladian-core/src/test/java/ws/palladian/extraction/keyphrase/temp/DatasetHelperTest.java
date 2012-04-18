package ws.palladian.extraction.keyphrase.temp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItems;

import java.io.File;
import java.util.Iterator;

import org.junit.Test;

/**
 * @author Philipp Katz
 */
public class DatasetHelperTest {

    @Test
    public void testCrossValidation() {
        Dataset2 dataset = new Dataset2();
        DatasetItem item1 = new DatasetItem(new File("file1.txt"), "true");
        DatasetItem item2 = new DatasetItem(new File("file2.txt"), "false");
        DatasetItem item3 = new DatasetItem(new File("file3.txt"), "true");
        DatasetItem item4 = new DatasetItem(new File("file4.txt"), "false");
        DatasetItem item5 = new DatasetItem(new File("file5.txt"), "false");
        DatasetItem item6 = new DatasetItem(new File("file6.txt"), "false");
        DatasetItem item7 = new DatasetItem(new File("file7.txt"), "true");
        DatasetItem item8 = new DatasetItem(new File("file8.txt"), "true");
        dataset.add(item1);
        dataset.add(item2);
        dataset.add(item3);
        dataset.add(item4);
        dataset.add(item5);
        dataset.add(item6);
        dataset.add(item7);
        dataset.add(item8);

        Iterator<Dataset2[]> iterator = DatasetHelper.crossValidate(dataset, 3);
        assertTrue(iterator.hasNext());
        Dataset2[] fold1 = iterator.next();
        assertThat(fold1[0], hasItems(item4, item5, item6, item7, item8));
        assertThat(fold1[1], hasItems(item1, item2, item3));

        assertTrue(iterator.hasNext());
        Dataset2[] fold2 = iterator.next();
        assertThat(fold2[0], hasItems(item1, item2, item3, item7, item8));
        assertThat(fold2[1], hasItems(item4, item5, item6));
        
        assertTrue(iterator.hasNext());
        Dataset2[] fold3 = iterator.next();
        assertThat(fold3[0], hasItems(item1, item2, item3, item4, item5, item6));
        assertThat(fold3[1], hasItems(item7, item8));
        
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testCalculatePartitionsSizes() {
        assertArrayEquals(new int[] {3, 3, 3}, DatasetHelper.calculatePartitionSizes(9, 3));
        assertArrayEquals(new int[] {3, 3, 2}, DatasetHelper.calculatePartitionSizes(8, 3));
        assertArrayEquals(new int[] {3, 2, 2}, DatasetHelper.calculatePartitionSizes(7, 3));
    }

}
