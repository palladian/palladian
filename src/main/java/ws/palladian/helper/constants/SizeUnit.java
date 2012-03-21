package ws.palladian.helper.constants;


/**
 * <p>
 * Binary size units, inspired by TimeUnit from Java's concurrent package. Conversions between sizes can be done like
 * following: <tt>SizeUnit.MEGABYTES.toBytes(1)</tt>. This converts 1 megabyte to bytes (= 1048576).
 * </p>
 * 
 * @author Philipp Katz
 */
public enum SizeUnit {

    BYTES(0), 
    KILOBYTES(1), 
    MEGABYTES(2), 
    GIGABYTES(3), 
    TERABYTES(4), 
    PETABYTES(5), 
    EXABYTES(6), 
    ZETTABYTES(7), 
    YOTTABYTES(8);

    private final int index;

    private SizeUnit(int index) {
        this.index = index;
    }

    private static final int[] multipliers = { 
            1, // bytes
            1024, // kilobytes
            1024 * 1024, // megabytes
            1024 * 1024 * 1024, // gigabytes
            1024 * 1024 * 1024 * 1024, // terabytes
            1024 * 1024 * 1024 * 1024 * 1024, // petabytes
            1024 * 1024 * 1024 * 1024 * 1024 * 1024, // exabytes
            1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024, // zettabytes
            1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024, // yottabytes
    };

    public long convert(long size, SizeUnit unit) {
        return doConvert(unit.index - index, size);
    }

    private long doConvert(int delta, long size) {
        if (delta < 0) {
            return size / multipliers[-delta];
        } else if (delta > 0) {
            return size * multipliers[delta];
        }
        return size;
    }
    
    public long toBytes(long size) {
        return doConvert(index, size);
    }
    public long toKilobytes(long size) {
        return doConvert(index - KILOBYTES.index, size);
    }
    public long toMegabytes(long size) {
        return doConvert(index - MEGABYTES.index, size);
    }
    public long toGigabytes(long size) {
        return doConvert(index - GIGABYTES.index, size);
    }
    public long toTerabytes(long size) {
        return doConvert(index - TERABYTES.index, size);
    }
    public long toPetabytes(long size) {
        return doConvert(index - PETABYTES.index, size);
    }
    public long toExabytes(long size) {
        return doConvert(index - EXABYTES.index, size);
    }
    public long toZettabytes(long size) {
        return doConvert(index - ZETTABYTES.index, size);
    }
    public long toYottabytes(long size) {
        return doConvert(index - YOTTABYTES.index, size);
    }

}