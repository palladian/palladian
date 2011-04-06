package ws.palladian.retrieval.feeds.discovery;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import ws.palladian.retrieval.feeds.discovery.FeedUrlsNearDuplicateEliminator;

public class FeedUrlsNearDuplicateEliminatorTest {

    @Test
    public void testFeedUrlsNearDuplicateEliminator() {

        List<String> urls = new ArrayList<String>();
        urls.add("http://kidneydiettips.davitablogs.com/?feed=rss2");
        urls.add("http://kidneydiettips.davitablogs.com/?feed=atom");
        List<String> deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://kidneydiettips.davitablogs.com/?feed=atom", deDup.get(0));

        urls = new ArrayList<String>();
        urls.add("http://en.citizendium.org/wiki?title=Special:RecentChanges&feed=rss");
        urls.add("http://en.citizendium.org/wiki?title=Special:RecentChanges&feed=atom");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://en.citizendium.org/wiki?title=Special:RecentChanges&feed=atom", deDup.get(0));
        
        urls = new ArrayList<String>();
        urls.add("http://www.gaucin.com/en.feed?type=rss");
        urls.add("http://www.gaucin.com/en.feed?type=atom");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://www.gaucin.com/en.feed?type=atom", deDup.get(0));
        
        urls = new ArrayList<String>();        
        urls.add("http://fukui-oozora.jugem.jp/?mode=rss");
        urls.add("http://fukui-oozora.jugem.jp/?mode=atom");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://fukui-oozora.jugem.jp/?mode=atom", deDup.get(0));

        urls = new ArrayList<String>();
        urls.add("http://hob3photography.smugmug.com/hack/feed.mg?Type=nickname&Data=hob3photography&format=atom10");
        urls.add("http://hob3photography.smugmug.com/hack/feed.mg?Type=nickname&Data=hob3photography&format=rss200");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://hob3photography.smugmug.com/hack/feed.mg?Type=nickname&Data=hob3photography&format=atom10", deDup.get(0));

        urls = new ArrayList<String>();
        urls.add("http://riadzany.blogspot.com/feeds/posts/default");
        urls.add("http://riadzany.blogspot.com/feeds/posts/default?alt=rss");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://riadzany.blogspot.com/feeds/posts/default?alt=rss", deDup.get(0));

        urls = new ArrayList<String>();
        urls.add("http://www.organicfacts.net/feed/rss.html");
        urls.add("http://www.organicfacts.net/feed/atom.html");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://www.organicfacts.net/feed/atom.html", deDup.get(0));
        
        urls = new ArrayList<String>();
        urls.add("http://prevent-swine-flu.com/feed/");
        urls.add("http://prevent-swine-flu.com/feed/atom/");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://prevent-swine-flu.com/feed/atom/", deDup.get(0));
        
        urls = new ArrayList<String>();
        urls.add("http://pigai.livejournal.com/data/rss");
        urls.add("http://pigai.livejournal.com/data/atom");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://pigai.livejournal.com/data/atom", deDup.get(0));
        
        urls = new ArrayList<String>();
        urls.add("http://www.sdli.org/index.php/sdli/rss_2.0/");
        urls.add("http://www.sdli.org/index.php/sdli/atom/");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://www.sdli.org/index.php/sdli/atom/", deDup.get(0));
        
        urls = new ArrayList<String>();
        urls.add("http://williamsport-ohio.apartmenthomeliving.com/feed.rss");
        urls.add("http://williamsport-ohio.apartmenthomeliving.com/feed.atom");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://williamsport-ohio.apartmenthomeliving.com/feed.atom", deDup.get(0));
        
        urls = new ArrayList<String>();
        urls.add("http://bluewaterjrhawks.com/feed.xml");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(1, deDup.size());
        Assert.assertEquals("http://bluewaterjrhawks.com/feed.xml", deDup.get(0));
        
        urls = new ArrayList<String>();
        urls.add("http://www.bluewaterfl.com/feed/");
        urls.add("http://www.bluewaterfl.com/home/feed/");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(2, deDup.size());
        Assert.assertEquals("http://www.bluewaterfl.com/feed/", deDup.get(0));
        Assert.assertEquals("http://www.bluewaterfl.com/home/feed/", deDup.get(1));
        
        urls = new ArrayList<String>();
        urls.add("http://www.thefreedictionary.com/_/WoD/rss.aspx?type=article");
        urls.add("http://www.thefreedictionary.com/_/WoD/rss.aspx?type=history");
        urls.add("http://www.thefreedictionary.com/_/WoD/rss.aspx?type=birthday");
        urls.add("http://www.thefreedictionary.com/_/WoD/rss.aspx?type=quote");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        Assert.assertEquals(4, deDup.size());
        
    }

}
