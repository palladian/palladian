package tud.iir.classification;



public class FastWordCorrelationMatrixTest extends WordCorrelationMatrixTest {
    
    @Override
    protected WordCorrelationMatrix getMatrix() {
        return new FastWordCorrelationMatrix();
    }

}
