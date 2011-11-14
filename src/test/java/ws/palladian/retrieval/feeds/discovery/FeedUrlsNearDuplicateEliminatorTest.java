package ws.palladian.retrieval.feeds.discovery;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class FeedUrlsNearDuplicateEliminatorTest {

    @Test
    public void testFeedUrlsNearDuplicateEliminator() {

        List<String> urls = new ArrayList<String>();
        urls.add("http://kidneydiettips.davitablogs.com/?feed=rss2");
        urls.add("http://kidneydiettips.davitablogs.com/?feed=atom");
        List<String> deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://kidneydiettips.davitablogs.com/?feed=atom"));

        urls = new ArrayList<String>();
        urls.add("http://en.citizendium.org/wiki?title=Special:RecentChanges&feed=rss");
        urls.add("http://en.citizendium.org/wiki?title=Special:RecentChanges&feed=atom");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://en.citizendium.org/wiki?title=Special:RecentChanges&feed=atom"));

        urls = new ArrayList<String>();
        urls.add("http://www.gaucin.com/en.feed?type=rss");
        urls.add("http://www.gaucin.com/en.feed?type=atom");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://www.gaucin.com/en.feed?type=atom"));

        urls = new ArrayList<String>();
        urls.add("http://fukui-oozora.jugem.jp/?mode=rss");
        urls.add("http://fukui-oozora.jugem.jp/?mode=atom");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://fukui-oozora.jugem.jp/?mode=atom"));

        urls = new ArrayList<String>();
        urls.add("http://hob3photography.smugmug.com/hack/feed.mg?Type=nickname&Data=hob3photography&format=atom10");
        urls.add("http://hob3photography.smugmug.com/hack/feed.mg?Type=nickname&Data=hob3photography&format=rss200");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(
                "http://hob3photography.smugmug.com/hack/feed.mg?Type=nickname&Data=hob3photography&format=atom10",
                deDup.get(0));

        urls = new ArrayList<String>();
        urls.add("http://riadzany.blogspot.com/feeds/posts/default");
        urls.add("http://riadzany.blogspot.com/feeds/posts/default?alt=rss");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://riadzany.blogspot.com/feeds/posts/default?alt=rss"));

        urls = new ArrayList<String>();
        urls.add("http://www.example.com/feed/");
        urls.add("http://www.example.com/feed/index.html");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://www.example.com/feed/index.html"));

        // Sandro: would be a useful improvement to detect this
        // urls = new ArrayList<String>();
        // urls.add("http://www.example.com/feed/index.html");
        // urls.add("http://www.example.com/feed/atom/index.html");
        // deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        // assertEquals(1, deDup.size());
        // assertEquals(true, deDup.contains("http://www.example.com/feed/atom/index.html"));

        urls = new ArrayList<String>();
        urls.add("http://www.organicfacts.net/feed/rss.html");
        urls.add("http://www.organicfacts.net/feed/atom.html");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://www.organicfacts.net/feed/atom.html"));

        urls = new ArrayList<String>();
        urls.add("http://prevent-swine-flu.com/feed/");
        urls.add("http://prevent-swine-flu.com/feed/atom/");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://prevent-swine-flu.com/feed/atom/"));

        urls = new ArrayList<String>();
        urls.add("http://pigai.livejournal.com/data/rss");
        urls.add("http://pigai.livejournal.com/data/atom");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://pigai.livejournal.com/data/atom"));

        urls = new ArrayList<String>();
        urls.add("http://www.sdli.org/index.php/sdli/rss_2.0/");
        urls.add("http://www.sdli.org/index.php/sdli/atom/");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://www.sdli.org/index.php/sdli/atom/"));

        urls = new ArrayList<String>();
        urls.add("http://williamsport-ohio.apartmenthomeliving.com/feed.rss");
        urls.add("http://williamsport-ohio.apartmenthomeliving.com/feed.atom");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://williamsport-ohio.apartmenthomeliving.com/feed.atom"));

        urls = new ArrayList<String>();
        urls.add("http://bluewaterjrhawks.com/feed.xml");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://bluewaterjrhawks.com/feed.xml"));

        urls = new ArrayList<String>();
        urls.add("http://www.bluewaterfl.com/feed/");
        urls.add("http://www.bluewaterfl.com/home/feed/");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(2, deDup.size());
        assertEquals(true, deDup.contains("http://www.bluewaterfl.com/feed/"));
        assertEquals(true, deDup.contains("http://www.bluewaterfl.com/home/feed/"));

        urls = new ArrayList<String>();
        urls.add("http://www.thefreedictionary.com/_/WoD/rss.aspx?type=article");
        urls.add("http://www.thefreedictionary.com/_/WoD/rss.aspx?type=history");
        urls.add("http://www.thefreedictionary.com/_/WoD/rss.aspx?type=birthday");
        urls.add("http://www.thefreedictionary.com/_/WoD/rss.aspx?type=quote");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(4, deDup.size());

        urls = new ArrayList<String>();
        urls.add("http://easterndiocese.org/news");
        urls.add("http://easterndiocese.org/news/index.rss?cid=1");
        urls.add("http://easterndiocese.org/news/index.rss?cid=2");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(2, deDup.size());
        assertEquals(true, deDup.contains("http://easterndiocese.org/news/index.rss?cid=1"));
        assertEquals(true, deDup.contains("http://easterndiocese.org/news/index.rss?cid=2"));

        // problematic feeds that contain several notations of rss within the URL
        urls = new ArrayList<String>();
        // contains rss + rss + + rss + rss20
        urls.add("http://www.br-online.de/br/jsp/global/funktion/rssexport/rssExport.jsp?rssType=rss20&contentId=/index.xml&bereich=HP");
        // contains rss + rss + + rss + atom
        urls.add("http://www.br-online.de/br/jsp/global/funktion/rssexport/rssExport.jsp?rssType=atom&contentId=/index.xml&bereich=HP");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(
                true,
                deDup.contains("http://www.br-online.de/br/jsp/global/funktion/rssexport/rssExport.jsp?rssType=atom&contentId=/index.xml&bereich=HP"));

        urls = new ArrayList<String>();
        // contains rss
        urls.add("http://womenpaintersse.blogspot.com/feeds/posts/default");
        // contains rss + rss
        urls.add("http://womenpaintersse.blogspot.com/feeds/posts/default?alt=rss");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://womenpaintersse.blogspot.com/feeds/posts/default?alt=rss"));

        urls = new ArrayList<String>();
        // contains rss + RSS
        urls.add("http://www.erlangen.de/de/contentXXL/services/newsexport/rss.asmx/GetRSSNewsByTabId?tabid=1");
        urls.add("http://www.erlangen.de/de/contentXXL/services/newsexport/rss.asmx/GetRSSNewsByTabId?tabid=3");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(2, deDup.size());
        assertEquals(
                true,
                deDup.contains("http://www.erlangen.de/de/contentXXL/services/newsexport/rss.asmx/GetRSSNewsByTabId?tabid=1"));
        assertEquals(
                true,
                deDup.contains("http://www.erlangen.de/de/contentXXL/services/newsexport/rss.asmx/GetRSSNewsByTabId?tabid=3"));

        urls = new ArrayList<String>();
        // contains rss
        urls.add("http://awoundedwarriorssecondchance.org/feed/");
        // conatins rss + atom
        urls.add("http://awoundedwarriorssecondchance.org/feed/atom/");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://awoundedwarriorssecondchance.org/feed/atom/"));

        // contains several formats each as single words (not contained in word like ...worrieRSSecond...)
        urls = new ArrayList<String>();
        // conatins rss + atom
        urls.add("http://www.alghad.com/index.php/rss/section/0.atom");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(1, deDup.size());
        assertEquals(true, deDup.contains("http://www.alghad.com/index.php/rss/section/0.atom"));

        // TODO: we currently cannot detect feeds contains several formats each as single words
        // urls = new ArrayList<String>();
        // // contains rss + RSS
        // urls.add("http://66.147.244.199/~monaromo/rss.php?action=newblogs&type=atom");
        // urls.add("http://66.147.244.199/~monaromo/rss.php?action=newblogs&type=rss");
        // deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        // assertEquals(1, deDup.size());
        // assertEquals(true,
        // deDup.contains("http://66.147.244.199/~monaromo/rss.php?action=newblogs&type=atom"));

        // ignore feeds which contain session ids
        urls = new ArrayList<String>();
        urls.add("http://fgsd.des.schoolfusion.us/modules/cms/announceRss.phtml?sessionid=c4e953bd5ed6802d8220af0b25c6645d");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(0, deDup.size());

        urls = new ArrayList<String>();
        urls.add("http://www.carbootsaledirectory.co.uk/.xml/?type=rss;PHPSESSID=5ebef52c07aa2776b125cd2c1ebdb3ea");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        assertEquals(0, deDup.size());

        // remove case duplicates
        urls = new ArrayList<String>();
        urls.add("http://tanny.ica.com/ica/tko/tkoblog.nsf/blog.rss");
        urls.add("http://tanny.ica.com/ICA/TKO/tkoblog.nsf/blog.rss");
        deDup = FeedUrlsNearDuplicateEliminator.deDuplicate(urls);
        System.out.println(deDup);
        assertEquals(1, deDup.size());

    }

}
