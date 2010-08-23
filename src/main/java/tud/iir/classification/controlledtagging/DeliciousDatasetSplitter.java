package tud.iir.classification.controlledtagging;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Extends DeliciousDatasetReader with random splitting capabilities for evaluation purposes. For now, we use a fixed
 * split 50:50.
 * 
 * @author Philipp Katz
 * 
 */
public abstract class DeliciousDatasetSplitter extends DeliciousDatasetReader {

    /** the train/test split -- true means train, false means test. */
    private List<Boolean> trainTestSplit;
    
    private int trainLimit = 100;
    
    private int testLimit = 100;
    
    public DeliciousDatasetSplitter() {
        calculateSplit();
    }

    /**
     * The split is calculated upon initialization. Call this, to calculate a new split.
     */
    public void calculateSplit() {

        // create split list, use 50:50 split for now
        Random random = new Random();
        trainTestSplit = new ArrayList<Boolean>();

        int totalSize = getTotalSize();
        for (int i = 0; i < totalSize; i++) {
            trainTestSplit.add(random.nextBoolean());
        }

    }

    private int getTotalSize() {
        final int size[] = new int[1];
        read(new DatasetCallback() {
            @Override
            public void callback(DatasetEntry entry) {
                size[0]++;
            }
        });
        return size[0];
    }

    public void read() {

        ///////////////// training ////////////////
        startTrain();
        final int[] data = new int[2]; // 0:index, 1:count
        read(new DatasetCallback() {
            // int index = 0;
            // int count = 0;

            @Override
            public void callback(DatasetEntry entry) {
                if (trainTestSplit.get(data[0]++)) {
                    train(entry, data[0]);
                    if (++data[1] == trainLimit) {
                        stop();
                    }
                    if (data[1] % 100 == 0) {
                        System.out.println("trained " + data[1] + " documents");
                    }
                }
            }
        });
        finishTrain();
        
        if (data[1] < trainLimit) {
            System.err.println("attention: not enough train documents " + data[1]);
        }

        ///////////////// testing ////////////////
        startTest();
        final int[] data2 = new int[2];
        read(new DatasetCallback() {
            // int index = 0;
            // int count = 0;

            @Override
            public void callback(DatasetEntry entry) {
                if (!trainTestSplit.get(data2[0]++)) {
                    test(entry, data2[0]);
                    if (++data2[1] == testLimit) {
                        stop();
                    }
                    if (data2[1] % 100 == 0) {
                        System.out.println("tested " + data2[1] + " documents");
                    }
                }
            }
        });
        finishTest();
        
        if (data2[1] < testLimit) {
            System.err.println("attention: not enough test documents " + data[1]);
        }

    }

    // hook methods for training/testing ////////////////////////
    public abstract void train(DatasetEntry entry, int index);
    public abstract void test(DatasetEntry entry, int index);
    
    // hook methods when testing/training begins/ends ///////////
    public void startTrain() {}
    public void finishTrain() {}
    public void startTest() {}
    public void finishTest() {}
    
    public void setTrainLimit(int trainLimit) {
        this.trainLimit = trainLimit;
    }
    
    public void setTestLimit(int testLimit) {
        this.testLimit = testLimit;
    }

}
