package ws.palladian.extraction.pos.filter;

import java.util.List;

public abstract class AbstractTagFilter {
    public AbstractTagFilter(Object o) {
        // TODO Auto-generated constructor stub
    }

    protected abstract List<String> internalFilter(String tag);
}
