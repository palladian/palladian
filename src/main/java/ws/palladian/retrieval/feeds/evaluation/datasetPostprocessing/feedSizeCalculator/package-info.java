/**
 * Package to calculate the feed sizes of every single poll, based on a dataset initially created by
 * {@link ws.palladian.retrieval.feeds.evaluation.DatasetCreator}.
 * Feed sizes (amount of data to be transferred, ignoring features such as http compression) are not calculated in
 * realtime when creating the dataset (e.g. TUDCS6) for performance reasons.
 * 
 * All calculated feed sizes are stored in database table feed_polls, column responseSize.
 */
package ws.palladian.retrieval.feeds.evaluation.datasetPostprocessing.feedSizeCalculator;