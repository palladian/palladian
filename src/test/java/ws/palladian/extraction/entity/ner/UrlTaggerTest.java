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
		Assert.assertEquals(39, annotations.get(0).getLength());
		
		annotations = urlTagger.tagUrls("You can download it here: www.cinefreaks.com/coolstuff.zip but be aware of the size.");
		Assert.assertEquals(1, annotations.size());
		Assert.assertEquals(26, annotations.get(0).getOffset());
		Assert.assertEquals(32, annotations.get(0).getLength());
		
		annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com/");
		Assert.assertEquals(1, annotations.size());
		Assert.assertEquals(26, annotations.get(0).getOffset());
		Assert.assertEquals(26, annotations.get(0).getLength());
		
        annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com.");
        Assert.assertEquals(1, annotations.size());
        Assert.assertEquals(26, annotations.get(0).getOffset());
        Assert.assertEquals(25, annotations.get(0).getLength());

        annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com?");
        Assert.assertEquals(1, annotations.size());
        Assert.assertEquals(26, annotations.get(0).getOffset());
        Assert.assertEquals(25, annotations.get(0).getLength());

        annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com! Or somewhere else");
        Assert.assertEquals(1, annotations.size());
        Assert.assertEquals(26, annotations.get(0).getOffset());
        Assert.assertEquals(25, annotations.get(0).getLength());

        annotations = urlTagger
                .tagUrls("You can download it here: http://www.cinefreaks.com. This is the next sentence");
        Assert.assertEquals(1, annotations.size());
        Assert.assertEquals(26, annotations.get(0).getOffset());
        Assert.assertEquals(25, annotations.get(0).getLength());

        annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com, this is the next...");
        Assert.assertEquals(1, annotations.size());
        Assert.assertEquals(26, annotations.get(0).getOffset());
        Assert.assertEquals(25, annotations.get(0).getLength());

		annotations = urlTagger.tagUrls("http://www.google.com/search?tbm=isch&hl=en&source=hp&biw=1660&bih=751&q=alfred+neuman+mad+magazine&gbv=2&aq=1s&aqi=g1g-s1g-sx1&aql=&oq=alfred+newman+m");
		Assert.assertEquals(1, annotations.size());
		Assert.assertEquals(0, annotations.get(0).getOffset());
		Assert.assertEquals(151, annotations.get(0).getLength());

		CollectionHelper.print(annotations);		
		
	}
}
