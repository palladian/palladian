package ws.palladian.features;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Wrap an arbitrary FeatureExtractor to allow extracting features in a grid.
 *
 * @author Philipp Katz
 */
public class LocalFeatureExtractor implements FeatureExtractor {
    private final FeatureExtractor wrapped;
    private final int divisions;

    public LocalFeatureExtractor(int divisions, FeatureExtractor wrapped) {
        if (divisions < 2) {
            throw new IllegalArgumentException("divisions must be at least 2, but was " + divisions);
        }
        this.wrapped = Objects.requireNonNull(wrapped);
        this.divisions = divisions;
    }

    @Override
    public FeatureVector extract(BufferedImage image) {
        // TODO following block is copied from GridSimilarityExtractor
        int cellWidth = image.getWidth() / divisions;
        int cellHeight = image.getHeight() / divisions;
        BufferedImage[] cells = new BufferedImage[divisions * divisions];
        for (int xIdx = 0; xIdx < divisions; xIdx++) {
            for (int yIdx = 0; yIdx < divisions; yIdx++) {
                int x = xIdx * cellWidth;
                int y = yIdx * cellHeight;
                cells[xIdx * divisions + yIdx] = image.getSubimage(x, y, cellWidth, cellHeight);
            }
        }

        InstanceBuilder instanceBuilder = new InstanceBuilder();
        for (int i = 0; i < cells.length; i++) {
            FeatureVector cellVector = wrapped.extract(cells[i]);
            for (VectorEntry<String, Value> entry : cellVector) {
                String cellKey = "cell-" + (i + 1) + "/" + (divisions * divisions) + "-" + entry.key();
                instanceBuilder.set(cellKey, entry.value());
            }
        }
        return instanceBuilder.create();
    }
}
