package com.uprizer.sensearray.freetools.stats;

import com.google.common.collect.AbstractIterator;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;

/**
 * Select a random sampling of elements from a dataset of unknown size
 *
 * An implementation of the algorithm described here:
 * http://gregable.com/2007/10/reservoir-sampling.html
 *
 * @param <T> The type of the elements to be sampled
 * @author Ian Clarke <ian@uprizer.com>
 */
public class ReservoirSampler<T> implements Serializable {
    private static final long serialVersionUID = -6796342106531559434L;
    final Object[] samples;
    private final int sampleSize;
    private volatile int sampleCount = 0;
    private final Random random;

    public ReservoirSampler(final int sampleSize) {
        this.sampleSize = sampleSize;
        samples = new Object[sampleSize];
        this.random = new Random();
    }

    public synchronized void addSample(final T s) {
        sampleCount++;
        if (sampleCount <= sampleSize) {
            samples[sampleCount - 1] = s;
        } else {
            if (random.nextDouble() < ((double) sampleSize) / (double) sampleCount) {
                samples[random.nextInt(samples.length)] = s;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Iterable<T> getSamples() {
        return new Iterable<T>() {

            public Iterator<T> iterator() {
                return new AbstractIterator<T>() {

                    int pos = 0;

                    @Override
                    protected T computeNext() {
                        if (pos >= samples.length || samples[pos] == null)
                            return endOfData();
                        return (T) samples[pos++];
                    }
                };
            }

        };
    }
}