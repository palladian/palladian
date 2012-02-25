package ws.palladian.iirmodel.helper;

import ws.palladian.iirmodel.Item;
import ws.palladian.iirmodel.ItemStream;
import ws.palladian.iirmodel.StreamGroup;

/**
 * <p>
 * Default {@link StreamVisitor} implementation. Override what you want, sucker!
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public class DefaultStreamVisitor implements StreamVisitor {

    @Override
    public void visitItemStream(ItemStream itemStream, int depth) {

    }

    @Override
    public void visitStreamGroup(StreamGroup streamGroup, int depth) {

    }

    @Override
    public void visitItem(Item item, int depth) {

    }

}
