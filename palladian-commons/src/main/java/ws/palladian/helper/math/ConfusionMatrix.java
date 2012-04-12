package ws.palladian.helper.math;


public class ConfusionMatrix extends Matrix {
    
    private static final long serialVersionUID = -6053171287857722193L;

    public ConfusionMatrix() {
        super();
    }

    public void increment(Object x, Object y) {

        Number count = (Number)get(x, y);
        if (count == null) {
            set(x, y, new Integer(1));
        } else {
            count = count.intValue() + 1;
            set(x, y, count);
        }

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        boolean headWritten = false;
        
        // iterate through all rows (y)
        for (String yKey : keysY) {
        
            // write table head
            if (!headWritten) {
                builder.append("\t");

                for (String key : keysX) {
                    builder.append(key).append("\t");
                }
                builder.append("<Precision>\n");
                
                headWritten = true;
            }
            
            builder.append(yKey).append("\t");
            
            // iterate through all columns (x)
            double rowSum = 0;
            double correctAssignments = 0;
            for (String xKey : keysX) {
              
                Number obj = (Number) get(xKey,yKey);
                if (obj == null) {
                    obj = 0;
                }
                rowSum += obj.doubleValue();
                if (xKey.equals(yKey)) {
                    correctAssignments = obj.doubleValue();
                }
                
                builder.append(obj).append("\t");
            }
            builder.append(MathHelper.round(correctAssignments/rowSum, 4)).append("\t");
            
            builder.append("\n");
        }
        
        builder.append("<Recall>\t");
        for (String xKey : keysX) {
            
            double correctAssignments = 0;
            double columnSum = calculateColumnSum(get(xKey));
            
            Number v = ((Number)get(xKey, xKey));
            if (v == null) {
                v = 0;
            }
            correctAssignments = v.doubleValue();
            
            builder.append(MathHelper.round(correctAssignments/columnSum, 4)).append("\t");
        }

        return builder.toString();
    }
    
    @Override
    public String asCsv() {
        return toString().replace("\t", ";");
    }
    
    
    public static void main(String[] args) {

        ConfusionMatrix confusionMatrix2 = new ConfusionMatrix();
        confusionMatrix2.set("A", "A", 2);
        confusionMatrix2.set("B", "B", 3);
        confusionMatrix2.set("A", "B", 4);
        confusionMatrix2.set("B", "A", 5);
        System.out.println(confusionMatrix2);

    }

    
}
