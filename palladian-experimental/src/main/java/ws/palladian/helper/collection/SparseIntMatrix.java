package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class SparseIntMatrix implements Matrix<Integer, Integer>, Serializable {

    private static final long serialVersionUID = 1L;
    
    int[] colIdx = new int[0];
    int[] rowPtr = new int[] {0};
    int[] data = new int[0];

    @Override
    public Integer get(Integer x, Integer y) {
        for (int idx = rowPtr[x]; idx < rowPtr[x + 1]; idx++) {
            if (y == colIdx[idx]) {
                return data[idx];
            }
        }
        return null;
    }

    @Override
    public void set(Integer x, Integer y, Integer value) {
        if (x + 2 > rowPtr.length) {
            rowPtr = grow(rowPtr, x - rowPtr.length + 2);
        }
        int idx;
        for (idx = rowPtr[x]; idx < rowPtr[x + 1]; idx++) {
            if (colIdx[idx] == y) {
                data[idx] = value;
                return;
            }
            if (colIdx[idx] > y) {
                idx++;
                break;
            }
        }
        colIdx = insert(colIdx, y, idx);
        data = insert(data, value, idx);
        for (int i = x + 1; i < rowPtr.length; i++) {
            rowPtr[i] = rowPtr[i] + 1;
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
    public int sizeY() {
        int size = 0;
        for (int index : colIdx) {
            size = Math.max(size, index);
        }
        return size + 1;
    }

    @Override
    public int sizeX() {
        return rowPtr.length - 1;
    }

    @Override
    public String asCsv() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void clear() {
        colIdx = new int[0];
        rowPtr = new int[] {0};
        data = new int[0];
    }

    private static int[] grow(int array[], int by) {
        int dest[] = new int[array.length + by];
        System.arraycopy(array, 0, dest, 0, array.length);
        for (int i = array.length; i < array.length + by; i++) {
            dest[i] = dest[array.length - 1];
        }
        return dest;
    }

    private static int[] insert(int array[], int item, int at) {
        int dest[] = new int[array.length + 1];
        System.arraycopy(array, 0, dest, 0, at);
        dest[at] = item;
        System.arraycopy(array, at, dest, at + 1, array.length - at);
        return dest;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("colIdx=").append(Arrays.toString(colIdx));
        stringBuilder.append(", rowPtr=").append(Arrays.toString(rowPtr));
        stringBuilder.append(", data=").append(Arrays.toString(data));
        return stringBuilder.toString();
    }

    @Override
    public List<Pair<Integer, Integer>> getRow(Integer y) {
        List<Pair<Integer, Integer>> row = CollectionHelper.newArrayList();
        for (int idx = rowPtr[y]; idx < rowPtr[y + 1]; idx++) {
            row.add(Pair.of(colIdx[idx], data[idx]));
        }
        return row;
    }

    @Override
    public List<Pair<Integer, Integer>> getColumn(Integer x) {
        List<Pair<Integer, Integer>> column = CollectionHelper.newArrayList();
        for (int y = 0; y < sizeY(); y++) {
            Integer value = get(x, y);
            if (value != null) {
                column.add(Pair.of(y, value));
            }
        }
        return column;
    }

}
