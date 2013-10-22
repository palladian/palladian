package ws.palladian.helper.collection;

import java.util.ArrayList;

public class RoundRobinList<E> extends ArrayList<E> {

    private static final long serialVersionUID = 8357139694464153452L;

    private int index = 0;

    public E getNextItem() {
        E item = get(index++);
        if (index >= size()) {
            index = 0;
        }

        return item;
    }

    @Override
    public boolean remove(Object o) {
        Boolean remove = super.remove(o);
        if (remove && index > 0) {
            index--;
        }
        return remove;
    }
}
