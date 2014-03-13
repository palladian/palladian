package ws.palladian.classification.text;

import java.util.Iterator;
import java.util.Queue;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;

public final class NGramWrapperIterator extends AbstractIterator<String> {

    private final Iterator<String> wrapped;
    private final int minLength;
    private final int maxLength;
    private final Queue<String> tokenQueue;

    private int currentLength;

    public NGramWrapperIterator(Iterator<String> wrapped, int minLength, int maxLength) {
        Validate.notNull(wrapped, "wrapped must not be null");
        Validate.isTrue(minLength > 0, "minLength must be greater zero");
        Validate.isTrue(maxLength >= minLength, "maxLength must be greater/equal zero");
        this.wrapped = wrapped;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.tokenQueue = CollectionHelper.newLinkedList();
        this.currentLength = minLength;
    }

    @Override
    protected String getNext() throws Finished {
        while (tokenQueue.size() < maxLength && wrapped.hasNext()) {
            tokenQueue.add(wrapped.next());
        }
        if (currentLength <= maxLength && currentLength <= tokenQueue.size()) {
            return createNGram();
        } else if (tokenQueue.size() > minLength) {
            currentLength = minLength;
            tokenQueue.poll();
            if (wrapped.hasNext()) {
                tokenQueue.add(wrapped.next());
            }
            return createNGram();
        } else {
            throw FINISHED;
        }
    }

    private String createNGram() {
        System.out.println("currentLength=" + currentLength + " queue=" + tokenQueue);
        StringBuilder builder = new StringBuilder();
        Iterator<String> queueIterator = tokenQueue.iterator();
        int length = minLength;
        boolean space = false;
        while (queueIterator.hasNext() && length <= currentLength) {
            String current = queueIterator.next();
            if (length >= minLength) {
                if (space) {
                    builder.append(' ');
                } else {
                    space = true;
                }
                builder.append(current);
            }
            length++;
        }
        currentLength++;
        return builder.toString();
    }

    public static void main(String[] args) {
        Iterator<String> iterator = new TokenIterator("the quick brown fox");
        iterator = new NGramWrapperIterator(iterator, 1, 2);
        CollectionHelper.print(iterator);
    }

}
