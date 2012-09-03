package ws.palladian.classification;

import ws.palladian.classification.FastWordCorrelationMatrix;
import ws.palladian.classification.WordCorrelationMatrix;



public class FastWordCorrelationMatrixTest extends WordCorrelationMatrixTest {
    
    @Override
    protected WordCorrelationMatrix getMatrix() {
        return new FastWordCorrelationMatrix();
    }

}
