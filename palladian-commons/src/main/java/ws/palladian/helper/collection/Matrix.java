package ws.palladian.helper.collection;

import java.util.Set;

/**
 * <p>
 * A data matrix. The data can have arbitrary types for keys (i.e. indices for rows/columns) and cell content.
 * </p>
 * 
 * @author pk
 * 
 * @param <K> Type of the indices.
 * @param <V> Type of the data.
 */
public interface Matrix<K, V> {

    /**
     * An entry (row or column) within a {@link Matrix}.
     * 
     * @author pk
     * 
     * @param <K>
     * @param <V>
     */
    public interface MatrixEntry<K, V> {

        /**
         * @return The vector with the values.
         */
        Vector<K, V> vector();

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
    Vector<K, V> getRow(K y);

    /**
     * <p>
     * Get a column from this matrix.
     * </p>
     * 
     * @param x Key/index of the column, not <code>null</code>.
     * @return The column, or <code>null</code> in case no such column exists.
     */
    Vector<K, V> getColumn(K x);

    /**
     * <p>
     * Note: Implementors should narrow down to a concrete return type, to keep code short.
     * </p>
     * 
     * @return The rows in this matrix.
     */
    Iterable<? extends MatrixEntry<K, V>> rows();

    /**
     * <p>
     * Note: Implementors should narrow down to a concrete return type, to keep code short.
     * </p>
     * 
     * @return The columns in this matrix.
     */
    Iterable<? extends MatrixEntry<K, V>> columns();

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

}
