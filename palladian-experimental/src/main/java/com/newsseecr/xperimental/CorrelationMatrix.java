package com.newsseecr.xperimental;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

public class CorrelationMatrix<T> {

    private Bag<Pair<T>> pairs;
    private Bag<T> counts;

    public CorrelationMatrix() {
        pairs = new HashBag<Pair<T>>();
        counts = new HashBag<T>();
    }

    public void add(T firstItem, T secondItem) {
        add(new Pair<T>(firstItem, secondItem));
    }

    public void add(Pair<T> pair) {
        pairs.add(pair);
        counts.add(pair.getFirstItem());
        counts.add(pair.getSecondItem());
    }

    public int getAbsoluteCorrelation(T firstItem, T secondItem) {
        return pairs.getCount(new Pair<T>(firstItem, secondItem));
    }

    public double getRelativeCorrelation(T firstItem, T secondItem) {
        int absoluteCorrelation = getAbsoluteCorrelation(firstItem, secondItem);
        int totalFirst = counts.getCount(firstItem);
        int totalSecond = counts.getCount(secondItem);
        return (double) absoluteCorrelation / (totalFirst + totalSecond - absoluteCorrelation);
    }
    
    
    
    ///////////////
    
//    public List<Pair<T>> getHighestPairs(int limit) {
//        
//        List<Pair<T>> result = new ArrayList<Pair<T>>();
//        
//        Map<Pair<T>, Integer> map = CollectionHelper.toMap(pairs);
//        LinkedHashMap<Pair<T>, Integer> sortedMap = CollectionHelper.sortByValue(map, CollectionHelper.DESCENDING);
//        int count = 0;
//        for(Entry<Pair<T>, Integer> x : sortedMap.entrySet()) {
//            if (count++ == limit) {
//                break;
//            }
//            result.add(x.getKey());
//        }
//        
//        return result;
//        
//    }

}
