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
public final class Pipe {

    private final Port inputPort;

    private final Port outputPort;

    /**
     * @param inputPort
     * @param outputPort
     */
    public Pipe(Port inputPort, Port outputPort) {
        Validate.notNull(inputPort);
        Validate.notNull(outputPort);
        this.inputPort = inputPort;
        this.outputPort = outputPort;
    }

    public void transit() {
        Validate.notNull(inputPort.getPipelineDocument());
        outputPort.setPipelineDocument(inputPort.getPipelineDocument());
    }

    public boolean canFire() {
        return inputPort.getPipelineDocument() != null;
    }

    @Override
    public String toString() {
        return "Pipe [inputPort=" + inputPort + ", outputPort=" + outputPort + "]";
    }

    public void clearInput() {
        inputPort.setPipelineDocument(null);
    }
    
}
