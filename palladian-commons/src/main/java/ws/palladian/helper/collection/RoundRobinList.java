package ws.palladian.helper.collection;

import java.util.ArrayList;
import java.util.Iterator;

public class RoundRobinList<E> extends ArrayList<E> {

    private static final long serialVersionUID = 8357139694464153452L;

    private int index = 0;

    public E getNextItem() {
        E item = get(index++);
        System.out.println("got item index "+index+" "+size());
        if (index >= size()) {
            index = 0;
        }

        return item;
    }

    @Override
    public boolean remove(Object o) {

        Boolean remove = super.remove(o);
        if(remove) {
            index--;
            System.out.println("removed item index "+index+" "+size());
        }
        return remove;
    }
}
