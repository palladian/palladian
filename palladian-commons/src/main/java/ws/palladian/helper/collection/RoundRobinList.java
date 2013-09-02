package ws.palladian.helper.collection;

import java.util.ArrayList;

public class RoundRobinList<E> extends ArrayList<E> {

    private static final long serialVersionUID = 8357139694464153452L;

    private int index = 0;

    public synchronized E getNextItem() {
        E item = get(index++);
        if (index >= size()) {
            index = 0;
        }

        return item;
    }

}
