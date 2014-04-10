package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;

public class ReservoirSamplerTest {

    @Test
    public void testReservoirSampler() {
        Collection<Integer> sampling = ReservoirSampler.sample(new AbstractIterator<Integer>() {
            int counter = 0;

            @Override
            protected Integer getNext() throws Finished {
                if (counter >= 1000) {
                    throw FINISHED;
                }
                return counter++;
            }
        }, 100);
        assertEquals(100, sampling.size());
    }

}
