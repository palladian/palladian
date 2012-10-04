package ws.palladian.classification.page;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import ws.palladian.helper.StopWatch;

/**
 * <p>A pool of reusable classifiers. This is still slower than the static DictionaryClassifier.classify. I don't know why, XXX</p>
 * @author David Urbansky
 *
 */
public class DictionaryClassifierPool {

    private final LinkedBlockingQueue<DictionaryClassifier> freeClassifiers;
    private final List<DictionaryClassifier> freeClassifiers2;

    public DictionaryClassifierPool(DictionaryClassifier classifier, int poolSize) {
        StopWatch stopWatch = new StopWatch();
        freeClassifiers = new LinkedBlockingQueue<DictionaryClassifier>();
        freeClassifiers2 = new ArrayList<DictionaryClassifier>();

        for (int i = 0; i < poolSize; i++) {

            DictionaryClassifier copy = (DictionaryClassifier) classifier.copy();
            copy.setDictionary(classifier.getDictionary());
            copy.setName("DC2_"+i);
            freeClassifiers.add(copy);
            freeClassifiers2.add(copy);

        }

        System.out.println("created pool in " + stopWatch.getElapsedTimeString());
    }

    public synchronized DictionaryClassifier get() {
        return freeClassifiers2.remove(0);
//        try {
//            return freeClassifiers.take();
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return null;
    }
    
    public synchronized void release(DictionaryClassifier dc2) {
        freeClassifiers2.add(dc2);
    }
    
    public TextInstance classify(String text) {

        TextInstance result = null;
        
        DictionaryClassifier dc2;
        try {
            dc2 = freeClassifiers.take();
            result = dc2.classify(text);
            freeClassifiers.offer(dc2);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       
        return result;
    }

    public static void main(String[] args) {

    }

}