package ws.palladian.preprocessing;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import ws.palladian.helper.StopWatch;

public class PerformanceCheckProcessingPipeline extends ProcessingPipeline {
    
    private static final long serialVersionUID = 1L;
    
    /** cumulates all processing times for each single component. */
    private HashMap<String, Long> cumulatedTimes = new LinkedHashMap<String, Long>();
    
    @Override
    public PipelineDocument process(PipelineDocument document) {
        
        for (PipelineProcessor processor : getPipelineProcessors()) {
            
            StopWatch sw = new StopWatch();
            processor.process(document);
            long elapsedTime = sw.getElapsedTime();
            
            addProcessingTime(processor, elapsedTime);    
        }
        
        return document;
        
    }

    private void addProcessingTime(PipelineProcessor processor, long elapsedTime) {
        String processorName = processor.getClass().getName();
        long cumulatedTime = cumulatedTimes.containsKey(processorName) ? cumulatedTimes.get(processorName) : 0;
        cumulatedTime += elapsedTime;
        cumulatedTimes.put(processorName, cumulatedTime);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("performance overview:").append("\n");
        Set<Entry<String, Long>> entrySet = cumulatedTimes.entrySet();
        long totalTime = 0;
        for (Entry<String, Long> entry : entrySet) {
            sb.append(entry.getKey()).append(" : ");
            sb.append(entry.getValue()).append("\n");
            totalTime += entry.getValue();
        }
        sb.append("total time : ").append(totalTime);
        return sb.toString();
    }

}
