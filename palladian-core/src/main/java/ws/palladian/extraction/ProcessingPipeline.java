package ws.palladian.extraction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
/**
 * <p>
 * A pipeline handling information processing components implemented by {@link PipelineProcessor}s to process
 * {@link PipelineDocument}s.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 3.0
 * @since 0.0.8
 */
public class ProcessingPipeline implements Serializable {

    /**
     * <p>
     * Unique number used to identify serialized versions of this object. This value change only but each time the
     * serialized schema of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -6173687204106619909L;

	/** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ProcessingPipeline.class);

    /**
     * <p>
     * The processors this pipeline will execute as ordered by this list from the first to the last.
     * </p>
     */
    private final List<PipelineProcessor<?>> pipelineProcessors;
    private final List<Pipe<?>> pipes;

    /**
     * <p>
     * Creates a new {@code ProcessingPipeline} without any {@code PipelineProcessor}s. Add processors using
     * {@link #add(PipelineProcessor)} to get a functional {@code ProcessingPipeline}.
     * </p>
     */
    public ProcessingPipeline() {
        pipelineProcessors = new ArrayList<PipelineProcessor<?>>();
        pipes = new ArrayList<Pipe<?>>();
    }
    
    /**
     * <p>
     * Creates a new {@link ProcessingPipeline} with the {@link PipelineProcessor}s from the supplied
     * {@link ProcessingPipeline}. The newly created {@link ProcessingPipeline} will use the instances of the
     * {@link PipelineProcessor}s from the supplied {@link ProcessingPipeline}. In other words, a <i>shallow copy</i> of
     * the workflow is created, where {@link PipelineProcessor}s share their states.
     * </p>
     * 
     * @param processingPipeline The {@link ProcessingPipeline} from which the {@link PipelineProcessor}s will be added
     *            to the newly created instance.
     */
    public ProcessingPipeline(ProcessingPipeline processingPipeline) {
        pipelineProcessors = new ArrayList<PipelineProcessor>(processingPipeline.getPipelineProcessors());
    }

    /**
     * <p>
     * Adds a new processor for execution to this pipeline. The processor is appended as last step.
     * </p>
     * 
     * @param pipelineProcessor The new processor to add.
     */
    public final void add(PipelineProcessor<?> pipelineProcessor) {
        // Begin Convenience Code
        if (!pipelineProcessors.isEmpty()) {
            List<Port<?>> previousOutputPorts = pipelineProcessors.get(pipelineProcessors.size() - 1).getOutputPorts();
            if (!previousOutputPorts.isEmpty()) {
                Port<?> previousOutputPort = previousOutputPorts.get(0);

                Port<?> inputPort = pipelineProcessor.getInputPorts().get(0);
                if ("defaultInput".equals(inputPort.getName()) && "defaultOutput".equals(previousOutputPort.getName())) {
                    add(new Pipe(previousOutputPort, inputPort));
                }
            }
        }
        // End Convenience Code

        pipelineProcessors.add(pipelineProcessor);
    }

    public final void add(Pipe<?> transition) {
        pipes.add(transition);
    }

    /**
     * <p>
     * Provides the list of all {@code PipelineProcessor}s currently registered at this pipeline. The list is in the
     * same order as the processors are executed beginning from the first and ending with the last.
     * </p>
     * 
     * @return The list of registered {@code PipelineProcessor}s.
     */
    public final List<PipelineProcessor<?>> getPipelineProcessors() {
        return Collections.unmodifiableList(pipelineProcessors);
    }

    /**
     * <p>
     * Starts processing of the provided document in this pipeline, running it through all currently registered
     * processors.
     * </p>
     * 
     * @param document The document to process.
     * @return The processed document is returned. This should be the same instance as provided. However this is not
     *         guaranteed. The returned document contains all features and modified representations created by the
     *         pipeline.
     */
    // Convenience Method
    public <T> PipelineDocument<T> process(PipelineDocument<T> document) throws DocumentUnprocessableException {

        ((Port<T>)pipelineProcessors.get(0).getInputPorts().get(0)).setPipelineDocument(document);

        process();

        return (PipelineDocument<T>)pipelineProcessors.get(pipelineProcessors.size() - 1).getOutputPorts().get(0)
                .getPipelineDocument();
    }

    public void process() throws DocumentUnprocessableException {
        Collection<PipelineProcessor<?>> executableProcessors = new ArrayList<PipelineProcessor<?>>(pipelineProcessors);
        Collection<Pipe<?>> executablePipes = new ArrayList<Pipe<?>>(pipes);
        Collection<PipelineProcessor<?>> executedProcessors = new ArrayList<PipelineProcessor<?>>();
        Collection<Pipe<?>> executedPipes = new ArrayList<Pipe<?>>();

        do {
            executedProcessors.clear();
            executedPipes.clear();

            for (PipelineProcessor<?> processor : executableProcessors) {
                if (processor.isExecutable()) {
                    executePreProcessingHook(processor);
                    processor.process();
                    executePostProcessingHook(processor);
                    executedProcessors.add(processor);
                }
            }
            for (Pipe<?> pipe : executablePipes) {
                if (pipe.canFire()) {
                    pipe.transit();
                    executedPipes.add(pipe);
                }
            }
            executableProcessors.removeAll(executedProcessors);
            executablePipes.removeAll(executedPipes);
        } while (!executedProcessors.isEmpty());
    }

    protected void executePostProcessingHook(final PipelineProcessor<?> processor) {
        // Subclasses should add code they want to run after the execution of every processor here.
    }

    protected void executePreProcessingHook(final PipelineProcessor<?> processor) {
        // Subclasses should add code they want to run before the execution of every processor here.
    }
    
    @Override
    public final String toString() {
        return pipelineProcessors.toString();
    }

}
