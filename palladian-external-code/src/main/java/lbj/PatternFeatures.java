// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D825B5B43C0341EFB278180D23D8E3BBDC7115011470E4C781D7889E9571CD29194A6735CFFEE9C565BBE414A0D2939FE67E3A5A4B2C0A3C3189277E0D8AB54EEA63863B78B971D6A4868E593C8FC0E314E202B9273C7D487369D221276AF9D2231A593E259DB7CD5638E96D02FC98EF8244D6C0A2187DAB6547F0E67B14B1E061620A0B936C469F80E9C919AAA9710B982BE1CE3613E2C9C77C46669558E66AFD0595F4A586309945E0421AF2644F917BC76B24559B52D07834F9CA4D5FBEA01487BCFB5B342AC6FA137954B1D28BACF0AD8CAB4B84FE2A743BE892CD6C4BE3C67A806CBC23ECF0147A13E118FBE7A090C327CE2FF0A588B9112B1495D2BDA93D830D2591505B14BDFA9F6FEAC8D85A65C6DBE87EFDA1DEA24B5762F2A55F83A5C213C03651EF7AAE01EA62D2D34492CB0EE3494FBCBFC33C947C4CF0FFD9DC251380B7F78BCE68D37FADE5D8E9F647B870F681300000

package lbj;

import java.util.StringTokenizer;

import ws.palladian.external.lbj.StringStatisticsUtils.OccurrenceCounter;
import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;


public class PatternFeatures extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public PatternFeatures() { super("lbj.PatternFeatures"); }

  @Override
public String getInputType() { return "LbjTagger.NEWord"; }
  @Override
public String getOutputType() { return "discrete%"; }

  @Override
public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof NEWord))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'PatternFeatures(NEWord)' defined on line 371 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == PatternFeatures.exampleCache) {
        return PatternFeatures.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    if (Parameters.featuresToUse.containsKey("PatternFeatures"))
    {
      OccurrenceCounter typesCounts = new OccurrenceCounter();
      String[] patterns = word.activePatterns.getTokens();
      for (int i = 0; i < patterns.length; i++)
      {
        double count = word.activePatterns.getCount(patterns[i]);
        StringTokenizer st = new StringTokenizer(patterns[i]);
        st.nextToken();
        typesCounts.addToken(st.nextToken(), count);
      }
      String[] types = typesCounts.getTokens();
      double[] weights = new double[types.length];
      int maxId = 0;
      double sum = 0;
      for (int i = 0; i < types.length; i++)
      {
        weights[i] = typesCounts.getCount(types[i]);
        if (weights[i] >= weights[maxId])
        {
          maxId = i;
        }
        sum += weights[i];
      }
      for (int i = 0; i < types.length; i++)
      {
        __id = this.name + types[i];
        __value = "" + weights[i] / sum;
        __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
      }
    }

    PatternFeatures.exampleCache = __example;
    PatternFeatures.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'PatternFeatures(NEWord)' defined on line 371 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "PatternFeatures".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof PatternFeatures; }
}

