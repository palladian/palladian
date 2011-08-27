package ws.palladian.extraction.entity.ner;

import junit.framework.Assert;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class DateAndTimeTaggerTest {

	@Test
	public void testDateAndTimeTagging() {
		DateAndTimeTagger datTagger = new DateAndTimeTagger();
		
		Annotations annotations = datTagger.tagDateAndTime("The mayan calendar ends on 21.12.2012, nobody knows what happens after end of 12/2012.");
		CollectionHelper.print(annotations);
		Assert.assertEquals(2, annotations.size());
		Assert.assertEquals(27, annotations.get(0).getOffset());
		Assert.assertEquals(10, annotations.get(0).getLength());
		
	}
	
}
