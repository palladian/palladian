/**
 * Created on: 19.12.2010 21:18:14
 */
package ws.palladian.iirmodel;

/**
 * The type of a question describes its semantic meaning in a broader category of stream item types. Currently supported
 * types are oriented at the SAP Developer Network post status types. They are:
 * <dl>
 * <dt>QUESTION:</dt>
 * <dd>A question formulated as an item in an item stream. In a forum thread this is often the first posting.</dd>
 * <dt>CORRECT_ANSWER:</dt>
 * <dd>An item marked as a correct answer by someone who created a question item.</dd>
 * <dt>VERY_HELPFUL:</dt>
 * <dd>An item marked as very helpful by someone who created a question item.</dd>
 * <dt>HELPFUL:</dt>
 * <dd>An item marked as helpful by someone who created a question item.</dd>
 * <dt>UNCERTAIN_ANSWER:</dt>
 * <dd>An item that might be an answer but was not marked by someone who created a question item.</dd>
 * <dt>OTHER:</dt>
 * <dd>All other items.</dd>
 * </dl>
 * 
 * @author Klemens Muthmann
 * @version 2.0
 * @since 1.0
 */
public enum ItemType {
    QUESTION, OTHER, CORRECT_ANSWER, UNCERTAIN_ANSWER, HELPFUL, VERY_HELPFUL;
}
