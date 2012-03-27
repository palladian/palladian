// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000DA59F6B43C03016CFBAC9305A5AEAEF750735019902A8A0E4C713C7116BB571613999C5DA346FDDD4ADD4D59D856AD7358B4FE97E9BF5F821D8C4C6280C7741B771942A13164C59CB24649A6431CD5BE959EE1466F11243BE0F90CBF01C303DCE51905B98BF3DEC6BA723817759426C5A9B1C1705B70D8D3ED57676E63F08A581A330940C1EC067B103377B59B48D601CC09D26A1B16E7A1B4CF0A604F596806AA3E0D48D78DADA2AF73EC046A244302A88BB082B98BA7A623929DC934D37F9C2E196C77EA253F3E65FA7EE69DCDC3FDD7ECBA7ACEBF742DC562D97106A5B3E88F5591AB2D8F692A4A28BBDA6882FA56B4217A17B7C32C8314ACF0D401EC8CCE75916136D0D2D953B850AC48606BE82273881BDE8676E80A6B753BF4BD2D91EB0C9AF74E5AA4525058E248D1F41292262A896D14F27CC7C4748ABA38CDF5909B020D701BC23F60C6D70BE25570870B2107140AF006956ED08F3F7CEF79DE0752BDC769F06DF25CF188AA8D8D1DA145BEC2CA4A2F6ABB3547F9291ED3CAE6377CB291795AAF09E24DEFB87BA8DBE9CF9897D9D0F25D798D974153AC75ECCEAEAC60C5024099BB2ACDD961AB3C9C71A71234CC2C700000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.RealFeature;


public class level1AggregationFeatures extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public level1AggregationFeatures() { super("lbj.level1AggregationFeatures"); }

  @Override
public String getInputType() { return "LbjTagger.NEWord"; }
  @Override
public String getOutputType() { return "real%"; }

  @Override
public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof NEWord))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'level1AggregationFeatures(NEWord)' defined on line 403 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == level1AggregationFeatures.exampleCache) {
        return level1AggregationFeatures.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;

    if (Parameters.featuresToUse.containsKey("PredictionsLevel1"))
    {
      int i = 0;
      NEWord w = word, last = (NEWord) word.next;
      for (i = 0; i < 2 && last != null; ++i)
      {
        last = (NEWord) last.next;
      }
      for (i = 0; i > -2 && w.previous != null; --i)
      {
        w = (NEWord) w.previous;
      }
      do
      {
        String[] arr = w.mostFrequentLevel1TokenInEntityType.getTokens();
        for (int k = 0; k < arr.length; k++)
        {
          __id = this.name + i + "1" + arr[k];
          __result.addFeature(new RealFeature(this.containingPackage, __id, w.mostFrequentLevel1TokenInEntityType.getCount(arr[k]) / w.mostFrequentLevel1TokenInEntityType.totalTokens));
        }
        arr = w.mostFrequentLevel1SuperEntityType.getTokens();
        for (int k = 0; k < arr.length; k++)
        {
          __id = this.name + i + "2" + arr[k];
          __result.addFeature(new RealFeature(this.containingPackage, __id, w.mostFrequentLevel1SuperEntityType.getCount(arr[k]) / w.mostFrequentLevel1SuperEntityType.totalTokens));
        }
        arr = w.mostFrequentLevel1ExactEntityType.getTokens();
        for (int k = 0; k < arr.length; k++)
        {
          __id = this.name + i + "3" + arr[k];
          __result.addFeature(new RealFeature(this.containingPackage, __id, w.mostFrequentLevel1ExactEntityType.getCount(arr[k]) / w.mostFrequentLevel1ExactEntityType.totalTokens));
        }
        arr = w.mostFrequentLevel1Prediction.getTokens();
        for (int k = 0; k < arr.length; k++)
        {
          __id = this.name + i + "4" + arr[k];
          __result.addFeature(new RealFeature(this.containingPackage, __id, w.mostFrequentLevel1Prediction.getCount(arr[k]) / w.mostFrequentLevel1Prediction.totalTokens));
        }
        arr = w.mostFrequentLevel1PredictionType.getTokens();
        for (int k = 0; k < arr.length; k++)
        {
          __id = this.name + i + "5" + arr[k];
          __result.addFeature(new RealFeature(this.containingPackage, __id, w.mostFrequentLevel1PredictionType.getCount(arr[k]) / w.mostFrequentLevel1PredictionType.totalTokens));
        }
        arr = w.mostFrequentLevel1NotOutsidePrediction.getTokens();
        for (int k = 0; k < arr.length; k++)
        {
          __id = this.name + i + "6" + arr[k];
          __result.addFeature(new RealFeature(this.containingPackage, __id, w.mostFrequentLevel1NotOutsidePrediction.getCount(arr[k]) / w.mostFrequentLevel1NotOutsidePrediction.totalTokens));
        }
        arr = w.mostFrequentLevel1NotOutsidePredictionType.getTokens();
        for (int k = 0; k < arr.length; k++)
        {
          __id = this.name + i + "7" + arr[k];
          __result.addFeature(new RealFeature(this.containingPackage, __id, w.mostFrequentLevel1NotOutsidePredictionType.getCount(arr[k]) / w.mostFrequentLevel1NotOutsidePredictionType.totalTokens));
        }
        i++;
        w = (NEWord) w.next;
      }      while (w != last);

    }

    level1AggregationFeatures.exampleCache = __example;
    level1AggregationFeatures.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'level1AggregationFeatures(NEWord)' defined on line 403 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "level1AggregationFeatures".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof level1AggregationFeatures; }
}

