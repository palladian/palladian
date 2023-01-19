package ws.palladian.kaggle.restaurants.dataset;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.dataset.ImageValue;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.CloseableIteratorAdapter;
import ws.palladian.helper.io.FileHelper;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class YelpKaggleTestsetReader extends AbstractDataset {

    private final List<File> files;

    public YelpKaggleTestsetReader(File testImagePath) {
        Objects.requireNonNull(testImagePath, "testImagePath must not be null");
        files = Arrays.stream(testImagePath.listFiles()).filter(f -> f.getName().endsWith("jpg")).collect(Collectors.toList());
    }

    @Override
    public CloseableIterator<Instance> iterator() {
        return new CloseableIteratorAdapter<>(new AbstractIterator<Instance>() {

            final Iterator<File> fileIterator = files.iterator();

            @Override
            protected Instance getNext() throws Finished {
                if (fileIterator.hasNext()) {
                    File file = fileIterator.next();
                    InstanceBuilder builder = new InstanceBuilder();
                    builder.set("image", new ImageValue(file));
                    builder.set("photoId", FileHelper.getFileName(file.getAbsolutePath()).replace(".jpg", ""));
                    return builder.create(true);
                }
                throw FINISHED;
            }
        });
    }

    @Override
    public FeatureInformation getFeatureInformation() {
        FeatureInformationBuilder builder = new FeatureInformationBuilder();
        builder.set("image", ImageValue.class);
        builder.set("photoId", ImmutableStringValue.class);
        return builder.create();
    }

    @Override
    public long size() {
        return files.size();
    }

}
