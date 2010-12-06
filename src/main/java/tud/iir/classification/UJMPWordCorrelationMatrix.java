package tud.iir.classification;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.enums.ValueType;
import org.ujmp.core.exceptions.MatrixException;


/**
 * 
 * 
 * 
 * http://www.ujmp.org/
 * http://www.ujmp.org/forum/viewtopic.php?f=3&t=206
 * http://www.ujmp.org/forum/viewtopic.php?f=3&t=165
 * 
 * @author Philipp Katz
 * 
 */
@SuppressWarnings("serial")
public class UJMPWordCorrelationMatrix extends WordCorrelationMatrix {

    /** Matrix with absolute correlations. */
    private Matrix absMatrix = MatrixFactory.sparse(ValueType.LONG, 0, 0);

    /** Matrix with relative correlations. */
    private Matrix relMatrix = MatrixFactory.sparse(ValueType.DOUBLE, 0, 0);

    @Override
    public void updatePair(String word1, String word2) {

        // get positions of the words in the matrix; if not yet present, add row/column
        // return numeric row/column positions.
        long word1Pos = getOrAdd(word1);
        long word2Pos = getOrAdd(word2);

        // increment absolute count by 1
        absMatrix.setAsInt(absMatrix.getAsInt(word2Pos, word1Pos) + 1, word2Pos, word1Pos);
        absMatrix.setAsInt(absMatrix.getAsInt(word1Pos, word2Pos) + 1, word1Pos, word2Pos);

    }

    private long getOrAdd(String word) {
        long wordPos = absMatrix.getRowForLabel(word);

        if (wordPos == -1) {

            wordPos = absMatrix.getSize(1);

            absMatrix.setSize(wordPos + 1, wordPos + 1);
            absMatrix.setRowLabel(wordPos, word);
            absMatrix.setColumnLabel(wordPos, word);
        }

        return wordPos;
    }

    @Override
    public WordCorrelation getCorrelation(String word1, String word2) {

        WordCorrelation result = null;

        long word1Pos = absMatrix.getRowForLabel(word1);
        long word2Pos = absMatrix.getRowForLabel(word2);
        int absCor = absMatrix.getAsInt(word1Pos, word2Pos);

        if (absCor > 0) {
            double relCor = relMatrix.getAsDouble(word1Pos, word2Pos);
            result = new WordCorrelation(new Term(word1), new Term(word2));
            result.setAbsoluteCorrelation(absCor);
            result.setRelativeCorrelation(relCor);
        }

        return result;

    }

    @Override
    public List<WordCorrelation> getCorrelations(String word, int minCooccurrences) {

        List<WordCorrelation> result = new ArrayList<WordCorrelation>();

        // get the row with the specified word,
        // the Matrix object below is a "view", which represents the row.
        long wordPos = absMatrix.getRowForLabel(word);
        Matrix row = absMatrix.selectRows(Ret.LINK, wordPos);

        for (long[] pos : row.availableCoordinates()) {
            int absCor = row.getAsInt(pos);
            if (absCor >= minCooccurrences && absCor != 0) {
                String word2 = row.getColumnLabel(pos[1]);
                double relCor = relMatrix.getAsDouble(pos);
                WordCorrelation correlation = new WordCorrelation(new Term(word), new Term(word2));
                correlation.setAbsoluteCorrelation(absCor);
                correlation.setRelativeCorrelation(relCor);
                result.add(correlation);
            }
        }

        return result;
    }

    @Override
    public Set<WordCorrelation> getCorrelations() {
        Set<WordCorrelation> correlations = new HashSet<WordCorrelation>();

        for (long[] pos : absMatrix.availableCoordinates()) {
            int absCor = absMatrix.getAsInt(pos);
            if (absCor != 0) {
                String word1 = absMatrix.getRowLabel(pos[0]);
                String word2 = absMatrix.getColumnLabel(pos[1]);
                double relCor = relMatrix.getAsDouble(pos);
                WordCorrelation correlation = new WordCorrelation(new Term(word1), new Term(word2));
                correlation.setAbsoluteCorrelation(absCor);
                correlation.setRelativeCorrelation(relCor);
                correlations.add(correlation);
            }
        }

        return correlations;
    }

    @Override
    public void makeRelativeScores() {

        Matrix rowSums = absMatrix.sum(Ret.LINK, Matrix.COLUMN, true);
        System.out.println("relCalculation: " + rowSums.getSize()[0] + " " + rowSums.getSize()[1]);
        relMatrix = MatrixFactory.sparse(absMatrix.getSize());
        
        int i = 0;

        for (long[] pos : absMatrix.availableCoordinates()) {
            System.out.println("pos : " + pos[0] + " / " + pos[1] + " / " + i++);
            double absolute = absMatrix.getAsDouble(pos);
            int rowSum1 = rowSums.getAsInt(pos[0], 0);
            int rowSum2 = rowSums.getAsInt(pos[1], 0);
            double relativeCorrelation = absolute / (rowSum1 + rowSum2 - absolute);
            relMatrix.setAsDouble(relativeCorrelation, pos);
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("absolute Matrix:\n").append(absMatrix).append("\n");
        sb.append("relative Matrix:\n").append(relMatrix);
        return sb.toString();
    }
    
    public void showGui() {
        absMatrix.showGUI();
    }
    
    public void saveMatrizes() {
        try {
            
            absMatrix.exportToFile("absMatrix.csv");
            // relMatrix.exportToFile("relMatrix");
            
        } catch (MatrixException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    public static void main(String[] args) {

        Matrix matrix = MatrixFactory.sparse(ValueType.INT, 5, 5);

        matrix.setColumnLabel(0, "one");
        matrix.setColumnLabel(1, "two");
        matrix.setColumnLabel(2, "three");
        matrix.setColumnLabel(3, "four");
        matrix.setColumnLabel(4, "five");

        matrix.setSize(10, 10);

        matrix.setColumnLabel(5, "six");
        matrix.setColumnLabel(6, "seven");
        matrix.setColumnLabel(7, "eight");
        matrix.setColumnLabel(8, "nine");
        matrix.setColumnLabel(9, "ten");

        System.out.println(matrix);

        // bug in library version 0.2.5 ...
        // column label is not assigned, if matrix is resized
        // wrote mail to developer 2010-10-23
        // until bug is fixed, we use version 0.2.3
        System.out.println(matrix.getColumnLabel(0)); // returns "one"
        System.out.println(matrix.getColumnLabel(5)); // return null in version 0.2.5
        
        
        // test
        
        matrix.setAsInt(10, 6,6);
        matrix.setAsInt(10, 8,7);

        
        Iterable<long[]> availableCoordinates = matrix.availableCoordinates();
        for (long[] c : availableCoordinates) {
            System.out.println(c[0] + " " + c[1]);
        }

    }

}
