package ws.palladian.helper.collection;

import java.util.Set;

import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.math.NumericMatrix;

/**
 * <p>
 * A data matrix. The data can have arbitrary types for keys (i.e. indices for rows/columns) and cell content. Matrices
 * allow random access to stored values using {@link #get(Object, Object)}. Rows and columns can also be iterated:
 * 
 * <pre>
 * for (MatrixVector&lt;String, Integer&gt; row : matrix.rows()) {
 *     for (VectorEntry&lt;String, Integer&gt; entry : row) {
 *         // row.key() gives the x index
 *         // entry.key() gives the y index
 *         // entry.value() gives the actual value
 *         System.out.println(row.key() + &quot;/&quot; + entry.key() + &quot; : &quot; + entry.value());
 *     }
 * }
 * </pre>
 * 
 * <p>
 * Specific subclasses can specialize the provided {@link VectorEntry}s; a matrix holding double values might actually
 * provide vectors which have functionality to perform vector calculations such as dot product, norm, etc. (this is
 * specified trough the return types in {@link #rows()}, {@link #columns()}, {@link #getRow(Object)}, and
 * {@link #getColumn(Object)}).
 * 
 * <p>
 * <b>Implementation note:</b> In case, a matrix implementation allows faster access in one dimension, one should keep
 * to the following convention: Accessing rows (using {@link #getRow(Object)} or {@link #rows()} ) should always be the
 * <b>faster</b> operation, than accessing columns (using {@link #getColumn(Object)} or {@link #columns()}).
 * 
 * @author pk
 * 
 * @param <K> Type of the indices.
 * @param <V> Type of the data.
 * @see MapMatrix
 * @see NumericMatrix
 * @see CountMatrix
 */
public interface Matrix<K, V> {

    /**
     * An row or column within a {@link Matrix}.
     * 
     * @author pk
     * 
     * @param <K>
     * @param <V>
     */
    public interface MatrixVector<K, V> extends Vector<K, V> {

        /**
         * @return Key of the row/column.
         */
        K key();

    }

    /**
     * <p>
     * Get data at the specified x/y position.
     * </p>
     * 
     * @param x Key/index of the column.
     * @param y Key/index of the row.
     * @return The value.
     */
    V get(K x, K y);

    /**
     * <p>
     * Set data at the specified x/y position.
     * </p>
     * 
     * @param x Key/index of the column.
     * @param y Key/index of the row.
     * @param value The value.
     */
    void set(K x, K y, V value);

    /**
     * @return The column keys in this matrix.
     */
    Set<K> getColumnKeys();

    /**
     * @return The row keys in this matrix.
     */
    Set<K> getRowKeys();

    /**
     * @return The number of columns in this matrix.
     */
    int columnCount();

    /**
     * @return The number of rows in this matrix.
     */
    int rowCount();

    /**
     * @return The size of this matrix (i.e. num rows times num columns).
     */
    int size();

    /**
     * <p>
     * Clears the matrix of all existing entries.
     * </p>
     */
    void clear();

    /**
     * <p>
     * Get a row from this matrix.
     * </p>
     * 
     * @param y Key/index of the row, not <code>null</code>.
     * @return The row, or <code>null</code> in case no such row exists.
     */
    MatrixVector<K, V> getRow(K y);

    /**
     * <p>
     * Get a column from this matrix.
     * </p>
     * 
     * @param x Key/index of the column, not <code>null</code>.
     * @return The column, or <code>null</code> in case no such column exists.
     */
    MatrixVector<K, V> getColumn(K x);

    /**
     * <p>
     * Note: Implementors should narrow down to a concrete return type, to keep code short.
     * </p>
     * 
     * @return The rows in this matrix.
     */
    Iterable<? extends MatrixVector<K, V>> rows();

    /**
     * <p>
     * Note: Implementors should narrow down to a concrete return type, to keep code short.
     * </p>
     * 
     * @return The columns in this matrix.
     */
    Iterable<? extends MatrixVector<K, V>> columns();

    /**
     * <p>
     * Remove a row from this matrix.
     * </p>
     * 
     * @param y Key/index of the row to remove, not <code>null</code>.
     */
    void removeRow(K y);

    /**
     * <p>
     * Remove a column from this matrix.
     * </p>
     * 
     * @param x Key/index of the column to remove, not <code>null</code>.
     */
    void removeColumn(K x);

    /**
     * <p>
     * Get a string representation of this matrix.
     * </p>
     * 
     * @param separator The separator character between columns.
     * @return A string representation of this matrix.
     */
    String toString(String separator);

    /**
     * <p>
     * Determine whether this and the given matrix are "compatible" to each other. This means, they have them same
     * dimensions and exactly the same indices.
     * </p>
     * 
     * @param other The other matrix, not <code>null</code>.
     * @return <code>true</code> in case the matrices are compatible, <code>false</code> otherwise.
     */
    boolean isCompatible(Matrix<K, V> other);

}
