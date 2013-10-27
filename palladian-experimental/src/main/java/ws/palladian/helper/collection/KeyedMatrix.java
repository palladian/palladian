//package ws.palladian.helper.collection;
//
//import java.io.Serializable;
//import java.util.Collections;
//import java.util.List;
//import java.util.Set;
//
//import org.apache.commons.lang3.tuple.Pair;
//
//public class KeyedMatrix<K, V> implements Matrix<K, V>, Serializable {
//
//    private static final long serialVersionUID = 1L;
//    
//    private final Matrix<Integer, V> matrix;
//    private final BidiMap<K, Integer> rowNames;
//    private final BidiMap<K, Integer> colNames;
//
//    /**
//     * @param matrix
//     */
//    public KeyedMatrix(Matrix<Integer, V> matrix) {
//        this.matrix = matrix;
//        this.rowNames = new BidiMap<K, Integer>();
//        this.colNames = new BidiMap<K, Integer>();
//    }
//    
//    public KeyedMatrix(Matrix<Integer, V> matrix, KeyedMatrix<K, V> keys) {
//        this.matrix = matrix;
//        this.rowNames = keys.rowNames;
//        this.colNames = keys.colNames;
//    }
//
//    @Override
//    public V get(K x, K y) {
//        Integer xIdx = rowNames.get(x);
//        if (xIdx == null) {
//            return null;
//        }
//        Integer yIdx = colNames.get(y);
//        if (yIdx == null) {
//            return null;
//        }
//        return matrix.get(xIdx, yIdx);
//    }
//
//    @Override
//    public void set(K x, K y, V value) {
//        Integer xIdx = rowNames.get(x);
//        if (xIdx == null) {
//            xIdx = rowNames.size();
//            rowNames.put(x, xIdx);
//        }
//        Integer yIdx = colNames.get(y);
//        if (yIdx == null) {
//            yIdx = colNames.size();
//            colNames.put(y, yIdx);
//        }
//        matrix.set(xIdx, yIdx, value);
//    }
//
//    @Override
//    public Set<K> getKeysX() {
//        return rowNames.keySet();
//    }
//
//    @Override
//    public Set<K> getKeysY() {
//        return colNames.keySet();
//    }
//
//    @Override
//    public int sizeY() {
//        return colNames.size();
//    }
//
//    @Override
//    public int sizeX() {
//        return rowNames.size();
//    }
//
//    @Override
//    public String asCsv() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public void clear() {
//        matrix.clear();
//        rowNames.clear();
//        colNames.clear();
//    }
//
//    @Override
//    public List<Pair<K, V>> getRow(K y) {
//        Integer yIdx = rowNames.get(y);
//        if (yIdx == null) {
//            return Collections.emptyList();
//        }
//        List<Pair<Integer, V>> temp = matrix.getRow(yIdx);
//        List<Pair<K, V>> result = CollectionHelper.newArrayList();
//        for (Pair<Integer, V> pair : temp) {
//            result.add(Pair.of(colNames.getKey(pair.getKey()), pair.getValue()));
//        }
//        return result;
//    }
//
//    @Override
//    public List<Pair<K, V>> getColumn(K x) {
//        Integer xIdx = rowNames.get(x);
//        if (xIdx == null) {
//            return Collections.emptyList();
//        }
//        List<Pair<Integer, V>> temp = matrix.getRow(xIdx);
//        List<Pair<K, V>> result = CollectionHelper.newArrayList();
//        for (Pair<Integer, V> pair : temp) {
//            result.add(Pair.of(colNames.getKey(pair.getKey()), pair.getValue()));
//        }
//        return result;
//    }
//}
