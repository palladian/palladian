package ws.palladian.classification.page;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.TextInstance;
import ws.palladian.helper.StopWatch;

/**
 * <p>A pool of reusable classifiers. This is still slower than the static DictionaryClassifier.classify. I don't know why, XXX</p>
 * @author David Urbansky
 *
 */
public class DictionaryClassifierPool {

    private final LinkedBlockingQueue<PalladianTextClassifier> freeClassifiers;
    private final List<PalladianTextClassifier> freeClassifiers2;

    public DictionaryClassifierPool(PalladianTextClassifier classifier, int poolSize) {
        StopWatch stopWatch = new StopWatch();
        freeClassifiers = new LinkedBlockingQueue<PalladianTextClassifier>();
        freeClassifiers2 = new ArrayList<PalladianTextClassifier>();

        for (int i = 0; i < poolSize; i++) {

            PalladianTextClassifier copy = (PalladianTextClassifier) classifier.copy();
            copy.setDictionary(classifier.getDictionary());
            copy.setName("DC2_"+i);
            freeClassifiers.add(copy);
            freeClassifiers2.add(copy);

        }

        System.out.println("created pool in " + stopWatch.getElapsedTimeString());
    }

    public synchronized PalladianTextClassifier get() {
        return freeClassifiers2.remove(0);
//        try {
//            return freeClassifiers.take();
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return null;
    }
    
    public synchronized void release(PalladianTextClassifier dc2) {
        freeClassifiers2.add(dc2);
    }
    
    public TextInstance classify(String text) {

        TextInstance result = null;
        
        PalladianTextClassifier dc2;
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