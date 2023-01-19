package ws.palladian.clustering;

import weka.clusterers.SimpleKMeans;
import weka.core.*;
import ws.palladian.helper.collection.CollectionHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class WekaKMeansClusterer implements Clusterer {
    private final int numClusters;

    public WekaKMeansClusterer(int numClusters) {
        this.numClusters = numClusters;
    }

    @Override
    public Collection<Cluster> cluster(Iterable<double[]> vectors) {
        List<double[]> vectorList = CollectionHelper.newArrayList(vectors);
        if (vectorList.isEmpty()) {
            return Collections.emptyList();
        }
        int numAttributes = vectorList.get(0).length;
        FastVector attInfo = new FastVector(numAttributes);
        for (int attributeIdx = 0; attributeIdx < numAttributes; attributeIdx++) {
            attInfo.addElement(new Attribute(String.valueOf(attributeIdx)));
        }
        Instances instances = new Instances("data", attInfo, vectorList.size());
        for (double[] vector : vectorList) {
            Instance instance = new SparseInstance(numAttributes);
            for (int attributeIdx = 0; attributeIdx < numAttributes; attributeIdx++) {
                instance.setValue(attributeIdx, vector[attributeIdx]);
            }
            instances.add(instance);
        }
        try {
            SimpleKMeans kMeans = new SimpleKMeans();
            kMeans.setNumClusters(numClusters);
            kMeans.buildClusterer(instances);
            Instances centroids = kMeans.getClusterCentroids();
            double[] clusterSizes = kMeans.getClusterSizes();
            Collection<Cluster> clusters = new ArrayList<>();
            for (int instanceIdx = 0; instanceIdx < centroids.numInstances(); instanceIdx++) {
                Instance instance = centroids.instance(instanceIdx);
                double[] attributes = new double[numAttributes];
                for (int attributeIdx = 0; attributeIdx < numAttributes; attributeIdx++) {
                    attributes[attributeIdx] = instance.value(attributeIdx);
                }
                clusters.add(new ImmutableCluster(attributes, (int) clusterSizes[instanceIdx]));
            }
            return clusters;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
