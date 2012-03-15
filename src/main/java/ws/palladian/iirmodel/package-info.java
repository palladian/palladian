/**
 * <h2>Item Stream Model</h2>
 * <p>
 * This package contains a consolidated model for Internet Information Retrieval tasks on streams of items. Item streams
 * are common over the whole web in the form of web forum threads, Facebook, Twitter or web feeds. Each of these sources
 * provides one or several streams of items and are called stream sources. The items are known under several names. Most
 * popular are posts or contributions in a web forum, tweets for twitter, status updates on Facebook or messages on RSS
 * or Atom feeds.
 * </p>
 * 
 * <p>
 * Items are ordered within their stream building either a linear or a treelike structure. See this packages class
 * diagram to get a grasp on the classes and their relations.
 * </p>
 * 
 * <p>
 * A typical instantiation of the model containing crawled data from a SourceForge project with different sources like
 * fourms, issue trackers, mailing lists, and Git repository could look like:
 * </p>
 * 
 * <pre>
 *       StreamGroup : SourceforgeNet
 *          |
 *          + StreamGroup : phpMyAdmin
 *              |
 *              + StreamGroup : Forum
 *              |   + StreamGroup : Help
 *              |       + ItemStream : Thread 1
 *              |       |   + Item 1
 *              |       |   + Item 2
 *              |       |   + Item 3
 *              |       + ItemStream : Thread 2
 *              |       + ItemStream : Thread 3
 *              |
 *              + StreamGroup : Tracker
 *              |   + StreamGroup : Bugs
 *              |   |   + ItemStream : Ticket 1
 *              |   |   + ItemStream : Ticket 2
 *              |   |   + ItemStream : Ticket 3
 *              |   + StreamGroup : Feature Requests
 *              |       + ItemStream : Ticket 1
 *              |       + ItemStream : Ticket 2
 *              |       + ItemStream : Ticket 3
 *              |
 *              + StreamGroup : MailingLists
 *              |   + ItemStream : Development
 *              |   + ItemStream : News
 *              |
 *              + ItemStream : Git
 * </pre>
 * 
 * <h2>Labeler Model</h2>
 * <p>
 * In addition the package contains classes to hold meta data about the {@link ws.palladian.iirmodel.Item}s and
 * {@link ws.palladian.iirmodel.ItemStream}s. This meta data mostly concerns with people or systems providing
 * {@link ws.palladian.iirmodel.Label}s and {@link ws.palladian.iirmodel.ItemRelation}s for
 * {@code ws.palladian.iirmodel.Item}s. They are represented by the class {@link ws.palladian.iirmodel.Labeler}.
 * </p>
 * <p>
 * Each {@code ws.palladian.iirmodel.Label} as well as each {@code ws.palladian.iirmodel.ItemRelation} has a type
 * represented by {@link ws.palladian.iirmodel.LabelType} and {@link ws.palladian.iirmodel.RelationType}.
 * {@code ws.palladian.iirmodel.Label}s define the meaning of an {@code Item}. For example a
 * {@code ws.palladian.iirmodel.Label} may carry the information that a certain {@code ws.palladian.iirmodel.Item} is a
 * news item or a question. Similarily {@code ws.palladian.iirmodel.ItemRelation}s may be of types like "part-of" or
 * "duplicate".
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 3.0
 * @since 1.0
 */
package ws.palladian.iirmodel;