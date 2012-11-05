package ws.palladian.processing;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * <p>
 * A specialized pipeline that registers the time necessary to run each {@link PipelineProcessor}.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.1.7
 */
public class PerformanceCheckProcessingPipeline extends ProcessingPipeline {

    private static final long serialVersionUID = 1L;

    /** Store cumulated processing times for each single PipelineProcessor. */
    private final HashMap<String, Long> cumulatedTimes = new LinkedHashMap<String, Long>();
    private long started;

    @Override
    protected void executePreProcessingHook(final PipelineProcessor processor) {
        super.executePreProcessingHook(processor);
        started = System.currentTimeMillis();
    }

    @Override
    protected void executePostProcessingHook(final PipelineProcessor processor) {
        long elapsedTime = System.currentTimeMillis() - started;
        addProcessingTime(processor, elapsedTime);
        super.executePostProcessingHook(processor);
    }

    /**
     * <p>
     * Registers the time taken by one processor.
     * </p>
     * 
     * @param processor The processor to register.
     * @param elapsedTime The time the processor took.
     */
    private void addProcessingTime(PipelineProcessor processor, long elapsedTime) {
        String processorName = processor.getClass().getName();
        long cumulatedTime = cumulatedTimes.containsKey(processorName) ? cumulatedTimes.get(processorName) : 0;
        cumulatedTime += elapsedTime;
        cumulatedTimes.put(processorName, cumulatedTime);
    }

    /**
     * <p>
     * Retrieve a report with performance statistics for each involved {@link PipelineProcessor} in this
     * {@link PerformanceCheckProcessingPipeline}.
     * </p>
     * 
     * @return
     */
    public String getStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("performance overview:").append("\n");
        Set<Entry<String, Long>> entrySet = cumulatedTimes.entrySet();
        long totalTime = 0;
        for (Entry<String, Long> entry : entrySet) {
            stats.append(entry.getKey()).append(" : ");
            stats.append(entry.getValue()).append("\n");
            totalTime += entry.getValue();
        }
        stats.append("total time : ").append(totalTime);
        return stats.toString();
    }

}
