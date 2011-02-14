/**
 * Classes from this package provide functionality to work with ATOM and RSS web feeds. The central class is the {@code
 * Feed} class representing a single feed with several {@code FeedItem} objects as children. To get the content of a
 * feed into a {@code Feed} object use the {@code FeedDownloader}. The {@code FeedDownloader} is like a factory for
 * feeds and trys to fetch a feeds content directly over the internet using Palladians {@link tud.iir.web.Crawler} and
 * the ROME framework. To save the retrieved cotnent use an instance of
 * {@link tud.iir.web.feeds.persistence.FeedDatabase}.
 */
package tud.iir.web.feeds;