package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class SparseMatrix<V> implements Matrix<Integer, V>, Serializable {

    private static final long serialVersionUID = 1L;

    final ArrayList<Integer> colIdx = new ArrayList<Integer>();
    final ArrayList<Integer> rowPtr = new ArrayList<Integer>(Arrays.asList(0));
    final ArrayList<V> data = new ArrayList<V>();

    @Override
    public V get(Integer x, Integer y) {
        if (y > rowPtr.size() - 2) {
            return null;
        }
        for (int idx = rowPtr.get(y); idx < rowPtr.get(y + 1); idx++) {
            if (x == colIdx.get(idx)) {
                return data.get(idx);
            }
        }
        return null;
    }

    @Override
    public void set(Integer x, Integer y, V value) {
        if (y + 2 > rowPtr.size()) {
            grow(rowPtr, y - rowPtr.size() + 2);
        }
        int idx;
        for (idx = rowPtr.get(y); idx < rowPtr.get(y + 1); idx++) {
            if (colIdx.get(idx) == x) {
                data.set(idx, value);
                return;
            }
            if (colIdx.get(idx) > x) {
                idx++;
                break;
            }
        }
        colIdx.add(idx, x);
        data.add(idx, value);
        for (int i = y + 1; i < rowPtr.size(); i++) {
            rowPtr.set(i, rowPtr.get(i) + 1);
        }
    }

    @Override
    public Set<Integer> getKeysX() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Integer> getKeysY() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int sizeX() {
        int size = 0;
        for (int index : colIdx) {
            size = Math.max(size, index);
        }
        return size + 1;
    }

    @Override
    public int sizeY() {
        return rowPtr.size() - 1;
    }

    @Override
    public String asCsv() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void clear() {
        colIdx.clear();
        rowPtr.clear();
        data.clear();
    }

    private void grow(ArrayList<Integer> rowPtr, int by) {
        Integer last = rowPtr.get(rowPtr.size() - 1);
        for (int i = 0; i < by; i++) {
            rowPtr.add(last);
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("colIdx=").append(colIdx);
        stringBuilder.append(", rowPtr=").append(rowPtr);
        stringBuilder.append(", data=").append(data);
        return stringBuilder.toString();
    }

    @Override
    public List<Pair<Integer, V>> getRow(Integer y) {
        List<Pair<Integer, V>> row = CollectionHelper.newArrayList();
        for (int idx = rowPtr.get(y); idx < rowPtr.get(y + 1); idx++) {
            row.add(Pair.of(colIdx.get(idx), data.get(idx)));
        }
        return row;
    }

    @Override
    public List<Pair<Integer, V>> getColumn(Integer x) {
        List<Pair<Integer, V>> column = CollectionHelper.newArrayList();
        for (int y = 0; y < sizeY(); y++) {
            V value = get(x, y);
            if (value != null) {
                column.add(Pair.of(y, value));
            }
        }
        return column;
    }

}
