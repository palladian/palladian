package tud.iir.news;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tud.iir.control.AllTests;
import tud.iir.news.FeedContentClassifier.FeedContentType;

public class FeedContentClassifierTest {

    FeedContentClassifier classifier = new FeedContentClassifier();

    @Test
    public void testFeedContentClassifier() {

        if (AllTests.ALL_TESTS) {

            // Daring Fireball
            // TODO links to different sites
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType("data/test/feeds/feed9.xml"));

            // Ars Technica / Infinite Loop
            // TODO mixed, some entries contain partial, some full content
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType("data/test/feeds/feed10.xml"));

            // esse est percipi
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType("data/test/feeds/feed11.xml"));

            // Panic Blog
            assertEquals(FeedContentType.FULL, classifier.determineContentType("data/test/feeds/feed12.xml"));

            // Engadget
            // TODO they have additional press releases on some pages, which are not in the feed
            // assertEquals(FeedContentType.FULL, classifier.determineFeedTextType("data/test/feeds/feed13.xml"));

            // Wired / Gadget Lab
            // assertEquals(FeedContentType.FULL, classifier.determineFeedTextType("data/test/feeds/feed14.xml"));

            // Ars Technica / Gears & Gadgets
            // TODO mixed, some entries contain partial, sume full content
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType("data/test/feeds/feed15.xml"));

            // SlashGear
            // TODO they acutally have MORE content in the feed entries, as they contain a "read more" list
            // assertEquals(FeedContentType.FULL, classifier.determineFeedTextType("data/test/feeds/feed16.xml"));

            // heise online
            assertEquals(FeedContentType.NONE, classifier.determineContentType("data/test/feeds/feed17.xml"));

            // Wired Top Stories
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType("data/test/feeds/feed18.xml"));

            // FAZ.NET
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType("data/test/feeds/feed19.xml"));

            // Spreeblick
            assertEquals(FeedContentType.FULL, classifier.determineContentType("data/test/feeds/feed20.xml"));

        }

    }

}
