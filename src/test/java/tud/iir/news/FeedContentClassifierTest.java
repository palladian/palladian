package tud.iir.news;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tud.iir.control.AllTests;

public class FeedContentClassifierTest {

    FeedContentClassifier classifier = new FeedContentClassifier(new FeedStoreDummy());

    @Test
    public void testFeedContentClassifier() {

        if (AllTests.ALL_TESTS) {

            // Daring Fireball
            // TODO links to different sites
            assertEquals(Feed.TEXT_TYPE_PARTIAL, classifier.determineFeedTextType("data/test/feeds/feed9.xml"));

            // Ars Technica / Infinite Loop
            // TODO mixed, some entries contain partial, some full content
            assertEquals(Feed.TEXT_TYPE_PARTIAL, classifier.determineFeedTextType("data/test/feeds/feed10.xml"));

            // esse est percipi
            assertEquals(Feed.TEXT_TYPE_PARTIAL, classifier.determineFeedTextType("data/test/feeds/feed11.xml"));

            // Panic Blog
            assertEquals(Feed.TEXT_TYPE_FULL, classifier.determineFeedTextType("data/test/feeds/feed12.xml"));

            // Engadget
            // TODO they have additional press releases on some pages, which are not in the feed
            // assertEquals(Feed.TEXT_TYPE_FULL, classifier.determineFeedTextType("data/test/feeds/feed13.xml"));

            // Wired / Gadget Lab
            assertEquals(Feed.TEXT_TYPE_FULL, classifier.determineFeedTextType("data/test/feeds/feed14.xml"));

            // Ars Technica / Gears & Gadgets
            // TODO mixed, some entries contain partial, sume full content
            assertEquals(Feed.TEXT_TYPE_PARTIAL, classifier.determineFeedTextType("data/test/feeds/feed15.xml"));

            // SlashGear
            // TODO they acutally have MORE content in the feed entries, as they contain a "read more" list
            // assertEquals(Feed.TEXT_TYPE_FULL, classifier.determineFeedTextType("data/test/feeds/feed16.xml"));

            // heise online
            assertEquals(Feed.TEXT_TYPE_NONE, classifier.determineFeedTextType("data/test/feeds/feed17.xml"));

            // Wired Top Stories
            assertEquals(Feed.TEXT_TYPE_PARTIAL, classifier.determineFeedTextType("data/test/feeds/feed18.xml"));

            // FAZ.NET
            assertEquals(Feed.TEXT_TYPE_PARTIAL, classifier.determineFeedTextType("data/test/feeds/feed19.xml"));

            // Spreeblick
            assertEquals(Feed.TEXT_TYPE_FULL, classifier.determineFeedTextType("data/test/feeds/feed20.xml"));

        }

    }

}
