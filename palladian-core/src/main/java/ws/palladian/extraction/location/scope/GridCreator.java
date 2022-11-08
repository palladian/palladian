package ws.palladian.extraction.location.scope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;

/**
 * See doc-files/GridCreator.png for an explanation.
 * 
 * @author Philipp Katz
 */
public final class GridCreator implements Iterable<GridCell> {

    /** The size in degrees for one cell in the grid (latitude/longitude direction). */
    private final double gridSize;

    /**
     * Instantiate a new {@link GridCreator}.
     * 
     * @param gridSize The length of one grid cell in degrees, greater zero, smaller/maximum 180.
     */
    public GridCreator(double gridSize) {
        Validate.isTrue(gridSize > 0, "gridSize must be greater zero");
        Validate.isTrue(gridSize <= 180, "gridSize must be smaller/equal 180");
        this.gridSize = gridSize;
    }

    /**
     * <p>
     * Get the cell identifier for the given {@link GeoCoordinate}.
     * </p>
     * 
     * @param coordinate The coordinate.
     * @return The cell identifier as string (e.g. <code>(12|23)</code>), or an empty string in case of a
     *         <code>null</code> coordinate.
     * @deprecated Use {@link #getCell(GeoCoordinate)} instead.
     */
    @Deprecated
    public String getCellIdentifier(GeoCoordinate coordinate) {
        if (coordinate == null) {
            return StringUtils.EMPTY;
        }
        return getCell(coordinate).getIdentifier();
    }

    /**
     * <p>
     * Get a grid cell for the given {@link GeoCoordinate}.
     * </p>
     * 
     * @param coordinate The coordinate, not <code>null</code>.
     * @return The grid cell for the given coordinate.
     */
    public GridCell getCell(GeoCoordinate coordinate) {
        Validate.notNull(coordinate, "coordinate must not be null");
        int xId = (int)((coordinate.getLongitude() + 180) / gridSize);
        int yId = (int)((coordinate.getLatitude() + 90) / gridSize);
        // edge cases, make sure the range stays in [0,numCellsX|Y[
        xId = Math.min(xId, getNumCellsX()-1);
        yId = Math.min(yId, getNumCellsY()-1);
        // plus zero to prevent "negative zero" returns
        double lat1 = Math.floor(coordinate.getLatitude() / gridSize) * gridSize + 0.;
        double lat2 = lat1 + gridSize;
        double lng1 = Math.floor(coordinate.getLongitude() / gridSize) * gridSize + 0.;
        double lng2 = lng1 + gridSize;
        return new GridCell(xId, yId, lat1, lat2, lng1, lng2, gridSize);
    }

    /**
     * <p>
     * Get grid cells which are within the given cell. This is useful, if one has a coarse grid and a finer grid, and
     * wants to get the cells within a coarse grid cell.
     * </p>
     * 
     * @param cell The cell, not <code>null</code>.
     * @return All cells which are contained within the cell.
     */
    public List<GridCell> getCells(GridCell cell) {
        Validate.notNull(cell, "cell must not be null");
        List<GridCell> cells = new ArrayList<>();
        for (double lat = cell.lat1; lat < cell.lat2; lat += gridSize) {
            for (double lng = cell.lng1; lng < cell.lng2; lng += gridSize) {
                lat = GeoUtils.normalizeLatitude(lat);
                lng = GeoUtils.normalizeLongitude(lng);
                cells.add(getCell(GeoCoordinate.from(lat, lng)));
            }
        }
        return cells;
    }

    /**
     * <p>
     * Transform back a cell identifier to a {@link GeoCoordinate}, by returning the center of the provided cell.
     * </p>
     * 
     * @param cellIdentifier The cell identifier, in the form <code>(12|23)</code>.
     * @return The center of the given cell identifier, or <code>null</code> in case the cell identifier was
     *         <code>null</code> or empty.
     * @throws IllegalArgumentException In case the cell identifier could not be parsed.
     * @deprecated Use {@link #getCell(String)} instead.
     */
    @Deprecated
    public GeoCoordinate getCoordinate(String cellIdentifier) {
        if (StringUtils.isBlank(cellIdentifier)) {
            return null;
        }
//        String[] values = cellIdentifier.replaceAll("\\(|\\)", "").split("\\|");
//        if (values.length != 2) {
//            throw new IllegalArgumentException("Invalid format: '" + cellIdentifier + "'.");
//        }
//        try {
//            double lng = Integer.valueOf(values[0]) * gridSize - 180 + 0.5 * gridSize;
//            double lat = Integer.valueOf(values[1]) * gridSize - 90 + 0.5 * gridSize;
//            return GeoCoordinate.from(lat, lng);
//        } catch (NumberFormatException e) {
//            throw new IllegalArgumentException("Invalid format: '" + cellIdentifier + "'.");
//        }
        return getCell(cellIdentifier).getCenter();
    }

    /**
     * <p>
     * Parse a cell identifier (such as <code>(12|23)</code>) and return a valid {@link GridCell} instance.
     * </p>
     * 
     * @param cellIdentifier The cell identifier, in the form <code>(12|23)</code>, not <code>null</code>.
     * @return The grid cell for the given identifier.
     * @throws IllegalArgumentException In case the cell identifier could not be parsed.
     */
    public GridCell getCell(String cellIdentifier) {
        Validate.notNull(cellIdentifier, "cellIdentifier must not be null");
        String[] values = split(cellIdentifier);
        if (values.length != 2) {
            throw new IllegalArgumentException("Invalid format: '" + cellIdentifier + "'.");
        }
        try {
            int xId = Integer.parseInt(values[0]);
            int yId = Integer.parseInt(values[1]);
            return getCell(xId, yId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid format: '" + cellIdentifier + "'.");
        }
    }

    static final String[] split(String identifier) {
        if (!identifier.startsWith("(") || !identifier.endsWith(")")) {
            throw new IllegalArgumentException("Invalid format: '" + identifier + "'.");
        }
        int idx = identifier.indexOf('|');
        if (idx == -1) {
            throw new IllegalArgumentException("Invalid format: '" + identifier + "'.");
        }
        return new String[] {identifier.substring(1, idx), identifier.substring(idx + 1, identifier.length() - 1)};
    }

    /**
     * Get a grid cell for the specified X and Y IDs.
     * 
     * @param xId The X id, must be in range [0,numCellsX[
     * @param yId The Y id, must be in range [0,numCellsY[
     * @return The grid cell for the given identifiers.
     * @throws IllegalArgumentException In case the IDs were out of range.
     */
    public GridCell getCell(int xId, int yId) {
        Validate.isTrue(0 <= xId && xId < getNumCellsX(), "xId must be in range [0,%d[, was %d", getNumCellsX(), xId);
        Validate.isTrue(0 <= yId && yId < getNumCellsY(), "yId must be in range [0,%d[, was %d", getNumCellsY(), yId);
        double lng1 = xId * gridSize - 180;
        double lng2 = lng1 + gridSize;
        double lat1 = yId * gridSize - 90;
        double lat2 = lat1 + gridSize;
        return new GridCell(xId, yId, lat1, lat2, lng1, lng2, gridSize);
    }

    /**
     * @return The number of unique cells, which this {@link GridCreator} creates.
     */
    public int getNumCells() {
        return getNumCellsX() * getNumCellsY();
    }

    /**
     * @return The number of cells in vertical direction.
     */
    public int getNumCellsY() {
        return (int)Math.ceil(180 / gridSize);
    }

    /**
     * @return The number of cells in horizontal direction.
     */
    public int getNumCellsX() {
        return (int)Math.ceil(360 / gridSize);
    }

    /**
     * @return The grid size.
     */
    public double getGridSize() {
        return gridSize;
    }

    @Override
    public Iterator<GridCell> iterator() {
        // XXX changed the iteration order; it is a bit stupid that the x coordinates are counted from south to north;
        // is there a reason for doing so? If not, reverse the counting, to avoid future confusions and change back the
        // following iteration code.
        
        final int numX = getNumCellsX();
//        final int numY = getNumCellsY();
        return new AbstractIterator2<GridCell>() {
            private int xId = 0;
//            private int yId = 0;
            private int yId = getNumCellsY()-1;

            @Override
            protected GridCell getNext() {
//                if (yId == numY) {
                if (yId < 0 && xId == 0) {
                    return finished();
                }
                GridCell cell = getCell(xId, yId);
                if (++xId == numX) {
                    xId = 0;
//                    yId++;
                    yId--;
                }
                return cell;
            }
        };
    }

    @Override
    public String toString() {
        return String.format("GridCreator (gridSize=%s°,numCells=%s)", gridSize, getNumCells());
    }

    public static void main(String[] args) {
        for (double gridSize : Arrays.asList(25., 10., 5., 2.5, 1., 0.5, 0.25, 0.1)) {
            System.out.println(new GridCreator(gridSize).toString());
        }
    }

}
