package ws.palladian.retrieval.wiki;

import java.util.HashSet;
import java.util.Set;

import java.util.function.Predicate;

public final class NamespaceFilter implements Predicate<WikiPageReference> {

    private final Set<Integer> acceptedNamespaces;

    public NamespaceFilter(int... acceptedNamespaces) {
        this.acceptedNamespaces = new HashSet<>();
        for (int namespace : acceptedNamespaces) {
            this.acceptedNamespaces.add(namespace);
        }
    }

    @Override
    public boolean test(WikiPageReference item) {
        return acceptedNamespaces.contains(item.getNamespaceId());
    }

}
