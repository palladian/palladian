package tud.iir.web.feeds;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tud.iir.control.AllTests;
import tud.iir.web.feeds.FeedContentClassifier.FeedContentType;

public class FeedContentClassifierTest {

    FeedContentClassifier classifier = new FeedContentClassifier();

    @Test
    public void testFeedContentClassifier() {

        if (AllTests.ALL_TESTS) {

            // Daring Fireball
            // TODO links to different sites
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType(FeedContentClassifierTest.class.getResource("/feeds/feed9.xml").getFile()));

            // Ars Technica / Infinite Loop
            // TODO mixed, some entries contain partial, some full content
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType(FeedContentClassifierTest.class.getResource("/feeds/feed10.xml").getFile()));

            // esse est percipi
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType(FeedContentClassifierTest.class.getResource("/feeds/feed11.xml").getFile()));

            // Panic Blog
            assertEquals(FeedContentType.FULL, classifier.determineContentType(FeedContentClassifierTest.class.getResource("/feeds/feed12.xml").getFile()));

            // Engadget
            // TODO they have additional press releases on some pages, which are not in the feed
            // assertEquals(FeedContentType.FULL, classifier.determineFeedTextType("data/test/feeds/feed13.xml"));

            // Wired / Gadget Lab
            // assertEquals(FeedContentType.FULL, classifier.determineFeedTextType("data/test/feeds/feed14.xml"));

            // Ars Technica / Gears & Gadgets
            // TODO mixed, some entries contain partial, sume full content
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType(FeedContentClassifierTest.class.getResource("/feeds/feed15.xml").getFile()));

            // SlashGear
            // TODO they acutally have MORE content in the feed entries, as they contain a "read more" list
            // assertEquals(FeedContentType.FULL, classifier.determineFeedTextType("data/test/feeds/feed16.xml"));

            // heise online
            assertEquals(FeedContentType.NONE, classifier.determineContentType(FeedContentClassifierTest.class.getResource("/feeds/feed17.xml").getFile()));

            // Wired Top Stories
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType(FeedContentClassifierTest.class.getResource("/feeds/feed18.xml").getFile()));

            // FAZ.NET
            assertEquals(FeedContentType.PARTIAL, classifier.determineContentType(FeedContentClassifierTest.class.getResource("/feeds/feed19.xml").getFile()));

            // Spreeblick
            assertEquals(FeedContentType.FULL, classifier.determineContentType(FeedContentClassifierTest.class.getResource("/feeds/feed20.xml").getFile()));

        }

    }

}
