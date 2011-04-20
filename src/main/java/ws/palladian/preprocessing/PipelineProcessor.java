package ws.palladian.preprocessing;

import java.io.Serializable;

public interface PipelineProcessor extends Serializable {

    void process(PipelineDocument document);

}
