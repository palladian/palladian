//package ws.palladian.helper.collection;
//
//import java.io.Serializable;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Set;
//
//import org.apache.commons.lang3.tuple.Pair;
//
//public class ArrayMatrix<V> implements Matrix<Integer, V>, Serializable {
//    
//    private static final long serialVersionUID = 1L;
//    
//    private final Object[] data;
//    private final int xSize;
//    private final int ySize;
//    
//    public ArrayMatrix(Matrix<Integer, V> matrix) {
//        xSize = matrix.sizeX();
//        ySize = matrix.sizeY();
//        data = new Object[xSize * ySize];
//        Set<Integer> yKeys = matrix.getKeysY();
//        for (Integer yKey : yKeys) {
//            List<Pair<Integer, V>> row = matrix.getRow(yKey);
//            for (Pair<Integer, V> rowEntry : row) {
//                set(rowEntry.getKey(), yKey, rowEntry.getValue());
//            }
//        }
//    }
//    
//    public ArrayMatrix(int xSize, int ySize) {
//        this.xSize = xSize;
//        this.ySize = ySize;
//        this.data = new Object[xSize * ySize];
//    }
//
//    @SuppressWarnings("unchecked")
//    @Override
//    public V get(Integer x, Integer y) {
//        return (V)data[x * ySize + y];
//    }
//
//    @Override
//    public void set(Integer x, Integer y, V value) {
//        data[x * ySize + y] = value;
//    }
//
//    @Override
//    public Set<Integer> getKeysX() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Set<Integer> getKeysY() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public int sizeY() {
//        return ySize;
//    }
//
//    @Override
//    public int sizeX() {
//        return xSize;
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
//        Arrays.fill(data, null);
//    }
//
//    @Override
//    @SuppressWarnings("unchecked")
//    public List<Pair<Integer, V>> getRow(Integer y) {
//        List<Pair<Integer, V>> result = CollectionHelper.newArrayList();
//        for (int x = 0; x < xSize; x++) {
//            V value = (V)data[y * xSize + x];
//            if (value != null) {
//                result.add(Pair.of(x, value));
//            }
//        }
//        return result;
//    }
//
//    @SuppressWarnings("unchecked")
//    @Override
//    public List<Pair<Integer, V>> getColumn(Integer x) {
//        List<Pair<Integer, V>> result = CollectionHelper.newArrayList();
//        for (int y = 0; y < ySize; x++) {
//            V value = (V)data[y * xSize + x];
//            if (value != null) {
//                result.add(Pair.of(y, value));
//            }
//        }
//        return result;
//    }
//
//}
