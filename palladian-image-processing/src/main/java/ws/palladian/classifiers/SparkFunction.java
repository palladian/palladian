package ws.palladian.classifiers;

import org.apache.spark.api.java.function.Function;
import org.apache.spark.input.PortableDataStream;
import org.nd4j.linalg.dataset.DataSet;
import scala.Tuple2;

/**
 * Created by sky on 08.07.2017.
 */
public class SparkFunction implements Function<Tuple2<String, PortableDataStream>, DataSet> {

    @Override
    public DataSet call(Tuple2<String, PortableDataStream> v1) throws Exception {
        DataSet d = new DataSet();
        d.load(v1._2().open());
        return d;
    }
}
