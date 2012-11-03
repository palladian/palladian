package ws.palladian.processing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private final List<PipelineProcessor> pipelineProcessors;
    /**
     * <p>
     * The {@link Pipe}s connecting the {@link PipelineProcessors} of this {@code ProcessingPipeline}.
     * </p>
     */
    private final List<Pipe<?>> pipes;

    /**
     * <p>
     * Creates a new {@code ProcessingPipeline} without any {@code PipelineProcessor}s. Add processors using
     * {@link #add(PipelineProcessor)} to get a functional {@code ProcessingPipeline}.
     * </p>
     */
    public ProcessingPipeline() {
        pipelineProcessors = new ArrayList<PipelineProcessor>();
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
        pipes = new ArrayList<Pipe<?>>(processingPipeline.pipes);
    }

    /**
     * <p>
     * Adds a new processor for execution to this pipeline. The processor is appended as last step.
     * </p>
     * 
     * @param pipelineProcessor The new processor to add.
     */
    public final <T> void add(PipelineProcessor pipelineProcessor) {
        // Begin Convenience Code
        if (!pipelineProcessors.isEmpty()) {
            Port<T> previousOutputPort = (Port<T>)pipelineProcessors.get(pipelineProcessors.size() - 1).getOutputPort(
                    PipelineProcessor.DEFAULT_OUTPUT_PORT_IDENTIFIER);
            if (previousOutputPort != null) {
                Port<T> inputPort = (Port<T>)pipelineProcessor
                        .getInputPort(PipelineProcessor.DEFAULT_INPUT_PORT_IDENTIFIER);
                if (inputPort != null) {
                    pipes.add(new Pipe<T>(previousOutputPort, inputPort));
                }
            }
        }
        // End Convenience Code

        pipelineProcessors.add(pipelineProcessor);
    }

    /**
     * <p>
     * Adds a new {@link PipelineProcessor} to this pipeline. The processor uses the provided {@code pipes} as input
     * {@code Pipe}s.
     * </p>
     * 
     * @param pipelineProcessor The new processor to add.
     * @param pipes The input {@code Pipe}s to use for the new {@code PipelineProcessor}.
     */
    public final void add(PipelineProcessor pipelineProcessor, Pipe<?>... pipes) {
        pipelineProcessors.add(pipelineProcessor);
        for (Pipe<?> pipe : pipes) {
            this.pipes.add(pipe);
        }
    }

    /**
     * <p>
     * Provides the list of all {@code PipelineProcessor}s currently registered at this pipeline. The list is in the
     * same order as the processors are executed beginning from the first and ending with the last.
     * </p>
     * 
     * @return The list of registered {@code PipelineProcessor}s.
     */
    public final List<PipelineProcessor> getPipelineProcessors() {
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
    public <D extends PipelineDocument<?>> D process(D document) throws DocumentUnprocessableException {
        if (!pipelineProcessors.isEmpty()) {
            ((Port<D>)pipelineProcessors.get(0).getInputPorts().get(0)).setPipelineDocument((PipelineDocument<D>)document);

            process();

            List<Port<?>> outputPorts = pipelineProcessors.get(pipelineProcessors.size() - 1).getOutputPorts();

            // Check if default output is available. This might not be the case if a writer was used to process the
            // final data.
            if (outputPorts.isEmpty()) {
                return null;
            } else {
                Port<D> defaultOutputPort = (Port<D>)outputPorts.get(0);
                return (D)defaultOutputPort.getPipelineDocument();
            }
        } else {
            return document;
        }
    }

    /**
     * <p>
     * Starts the processing of this {@code ProcessingPipeline} and runs the whole process exactly once.
     * </p>
     * 
     * @throws DocumentUnprocessableException If the {@link PipelineDocument} is not processable by one of the
     *             {@code PipelineProcessors} of this {@code ProcessingPipeline}.
     * @see {@link #processContinuous()}
     */
    public void process() throws DocumentUnprocessableException {
        Collection<PipelineProcessor> executableProcessors = new ArrayList<PipelineProcessor>(pipelineProcessors);
        Collection<Pipe<?>> executablePipes = new ArrayList<Pipe<?>>(pipes);
        Collection<PipelineProcessor> executedProcessors = new ArrayList<PipelineProcessor>();
        Collection<Pipe<?>> executedPipes = new ArrayList<Pipe<?>>();
        cleanOutputPorts(); // This is necessary if there are results from previous processing runs.

        do {
            executedProcessors.clear();
            executedPipes.clear();

            for (PipelineProcessor processor : executableProcessors) {
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
            resetPipes(executedPipes); // This is necessary so that already executed pipes do not fire on every
                                       // iteration again.
            executableProcessors.removeAll(executedProcessors);
            executablePipes.removeAll(executedPipes);
        } while (!executedProcessors.isEmpty());
        LOGGER.debug("Finished pipeline.");
        notifyProcessorsOfProcessFinished();
    }

    /**
     * <p>
     * A helper function removing the {@link PipelineDocument}s from all output {@link Port}s of all
     * {@link PipelineProcessor}s of this {@code ProcessingPipeline}, so that all {@code PipelineProcessor}s are ready
     * to restart processing documents, even if there result was not fetched by any {@code Pipe}.
     * </p>
     * 
     */
    private void cleanOutputPorts() {
        for (PipelineProcessor processor : pipelineProcessors) {
            for (Port<?> port : processor.getOutputPorts()) {
                port.setPipelineDocument(null);
            }
        }
    }

    /**
     * <p>
     * Runs the pipeline continuous as long as at least one {@link PipelineProcessor} is executable. This is for example
     * true as long as any source {@code PipelineProcessor} still has some input to process.
     * </p>
     * 
     * @throws DocumentUnprocessableException If the process encounters a {@link PipelineDocument} it can not process.
     */
    public void processContinuous() throws DocumentUnprocessableException {
        Collection<PipelineProcessor> executedProcessors = new ArrayList<PipelineProcessor>(pipelineProcessors.size());
        Collection<Pipe<?>> executedPipes = new ArrayList<Pipe<?>>(pipes.size());
        // TODO actually this needs to be done after every complete run of the workflow. However I am currently not able
        // to know when a complete run is over.
        cleanOutputPorts();

        do {
            executedProcessors.clear();
            executedPipes.clear();
            for (PipelineProcessor processor : pipelineProcessors) {
                if (processor.isExecutable()) {
                    executePreProcessingHook(processor);
                    processor.process();
                    executePostProcessingHook(processor);
                    executedProcessors.add(processor);
                }
            }
            for (Pipe<?> pipe : pipes) {
                if (pipe.canFire()) {
                    pipe.transit();
                    executedPipes.add(pipe);
                }
            }
            resetPipes(executedPipes);
        } while (!executedProcessors.isEmpty() || !executedPipes.isEmpty());
        notifyProcessorsOfProcessFinished();
    }

    /**
     * <p>
     * Resets the provided {@code Pipe}s so they can fire again.
     * </p>
     * 
     * @param pipes The {@code Pipe}s to reset.
     */
    private void resetPipes(Collection<Pipe<?>> pipes) {
        for (Pipe<?> pipe : pipes) {
            pipe.clearInput();
        }

    }

    /**
     * <p>
     * A hook method, doing nothing in this basic implementation. Subclasses may overwrite it to add custom behaviour
     * after each run of {@link PipelineProcessor}.
     * </p>
     * 
     * @param processor The {@code PipelineProcessor} that finished running directly before this method was called.
     */
    protected void executePostProcessingHook(final PipelineProcessor processor) {
        // Subclasses should add code they want to run after the execution of every processor here.
        LOGGER.debug("Start processing on " + processor.getClass().getName());
    }

    /**
     * <p>
     * A hook method, doing nothing in this basic implementation. Subclasses may overwrite it to add custom behaviour
     * before each run of {@link PipelineProcessor}.
     * </p>
     * 
     * @param processor The {@code PipelineProcessor} that is about to run after this method returns.
     */
    protected void executePreProcessingHook(final PipelineProcessor processor) {
        // Subclasses should add code they want to run before the execution of every processor here.
        LOGGER.debug("Finished processing on " + processor.getClass().getName());
    }

    @Override
    public final String toString() {
        return pipelineProcessors.toString();
    }

    private void notifyProcessorsOfProcessFinished() {
        for (PipelineProcessor processor : pipelineProcessors) {
            processor.processingFinished();
        }
    }

}
