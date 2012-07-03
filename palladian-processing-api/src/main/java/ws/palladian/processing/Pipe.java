/**
 * Created on: 05.06.2012 21:00:56
 */
package ws.palladian.processing;

import org.apache.commons.lang3.Validate;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class Pipe<T> {
    /**
     * <p>
     * 
     * </p>
     */
    private final Port<T> inputPort;
    /**
     * <p>
     * 
     * </p>
     */
    private final Port<T> outputPort;

    /**
     * <p>
     * 
     * </p>
     * 
     * @param inputPort
     * @param outputPort
     */
    public Pipe(final Port<T> inputPort, final Port<T> outputPort) {
        super();
        Validate.notNull(inputPort);
        Validate.notNull(outputPort);

        this.inputPort = inputPort;
        this.outputPort = outputPort;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public void transit() {
        Validate.notNull(inputPort.getPipelineDocument());
        outputPort.setPipelineDocument(inputPort.getPipelineDocument());
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @return
     */
    public Boolean canFire() {
        return inputPort.getPipelineDocument() != null;
    }

    @Override
    public String toString() {
        return "Pipe [inputPort=" + inputPort + ", outputPort=" + outputPort + "]";
    }

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public void clearInput() {
        this.inputPort.setPipelineDocument(null);
    }
}
