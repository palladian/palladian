package ws.palladian.extraction.entity.ner;

import junit.framework.Assert;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class UrlTaggerTest {

	
	@Test
	public void testUrlTagging() {
		UrlTagger urlTagger = new UrlTagger();
		
		Annotations annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com/coolstuff.zip but be aware of the size.");
		Assert.assertEquals(1, annotations.size());
		Assert.assertEquals(26, annotations.get(0).getOffset());
		Assert.assertEquals(40, annotations.get(0).getLength());
		
		annotations = urlTagger.tagUrls("You can download it here: www.cinefreaks.com/coolstuff.zip but be aware of the size.");
		CollectionHelper.print(annotations);
		Assert.assertEquals(1, annotations.size());
		Assert.assertEquals(26, annotations.get(0).getOffset());
		Assert.assertEquals(33, annotations.get(0).getLength());
		
		
	}
}
