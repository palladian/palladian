/**
 * Created on: 03.12.2012 21:16:43
 */
package ws.palladian.extraction.pos.filter;

import java.util.List;

/**
 * <p>
 * A Tag filter is able to filter certain PoS tags and exchange them for different PoS tags. This is helpful if the
 * model you are working on contains a very fine grained tag set but you only need a coarse grained one.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public interface TagFilter {
    /**
     * <p>
     * Filters the provided tag accoring to this {@link TagFilter}s rules.
     * </p>
     * 
     * @param tag The tag to filter.
     * @return A list containing the filtered tag or a list of tags resulting from the input tag.
     */
    List<String> filter(String tag);
}
