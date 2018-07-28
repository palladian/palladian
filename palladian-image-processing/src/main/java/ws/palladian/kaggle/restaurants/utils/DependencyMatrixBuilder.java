package ws.palladian.kaggle.restaurants.utils;

import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.MapMatrix;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build the label dependency matrix.
 */
public class DependencyMatrixBuilder {

    public DependencyMatrix buildMatrix() {
        MapMatrix<Integer, Double> matrix = new MapMatrix<>();

        // get order straight
        for (int i = 0; i <= 8; i++) {
            matrix.set(i,i, 0.);
        }

        Bag<Integer> counts = Bag.create();
        List<String> strings = FileHelper.readFileToArray(Config.CONFIG.getString("dataset.yelp.restaurants.train.csv"));
        for (String string : strings) {
            // skip header
            if (string.startsWith("business_id")) {
                continue;
            }
            String[] split = string.split("[, ]");
            for (int i = 1; i < split.length; i++) {
                int label1 = Integer.valueOf(split[i]);
                counts.add(label1);
                for (int j = i; j < split.length; j++) {
                    int label2 = Integer.valueOf(split[j]);

                    Double num = matrix.get(label1, label2);
                    if (num == null) {
                        num = 0.;
                    }
                    num++;
                    matrix.set(label1, label2, num);
                    matrix.set(label2, label1, num);
                }
            }
        }
        System.out.println(matrix.toString());
        FileHelper.writeToFile(Config.CONFIG.getString("dataset.yelp.restaurants.results") +"/matrix.tsv", matrix.toString("\t"));

        Map<Integer, Double> labelPriors = new HashMap<>();
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                if (i == j) {
                    matrix.set(i, j, (double) counts.count(i) / strings.size());
                    labelPriors.put(i, (double) counts.count(i) / strings.size());
                } else {
                    Double num = matrix.get(i, j);
                    num /= counts.count(j);
                    matrix.set(i, j, MathHelper.round(num, 4));
                }
            }
        }

        FileHelper.writeToFile(Config.CONFIG.getString("dataset.yelp.restaurants.results") +"/matrix-asymmetric.tsv", matrix.toString("\t").replace(".", ","));

        return new DependencyMatrix(matrix);
    }

    public static void main(String[] args) {
        new DependencyMatrixBuilder().buildMatrix();
    }
}
