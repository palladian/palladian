package ws.palladian.retrieval.wiki;

import java.util.HashSet;
import java.util.Set;

import ws.palladian.helper.functional.Filter;

public final class NamespaceFilter implements Filter<WikiPageReference> {

    private final Set<Integer> acceptedNamespaces;

    public NamespaceFilter(int... acceptedNamespaces) {
        this.acceptedNamespaces = new HashSet<>();
        for (int namespace : acceptedNamespaces) {
            this.acceptedNamespaces.add(namespace);
        }
    }

    @Override
    public boolean accept(WikiPageReference item) {
        return acceptedNamespaces.contains(item.getNamespaceId());
    }

}
