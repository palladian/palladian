package ws.palladian.retrieval.wikipedia;

import java.util.Set;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;

public final class NamespaceFilter implements Filter<WikipediaPageReference> {

    private final Set<Integer> acceptedNamespaces;

    public NamespaceFilter(int... acceptedNamespaces) {
        this.acceptedNamespaces = CollectionHelper.newHashSet();
        for (int namespace : acceptedNamespaces) {
            this.acceptedNamespaces.add(namespace);
        }
    }

    @Override
    public boolean accept(WikipediaPageReference item) {
        return acceptedNamespaces.contains(item.getNamespaceId());
    }

}
