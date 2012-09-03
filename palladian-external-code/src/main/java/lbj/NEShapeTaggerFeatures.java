// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D60915B43C034158FFAC1B0E868AD23C743BAF63F54995072E3A48DE6B60A623294DA22EF77F6A657EA49042CD3937EB312E845B7E8DCAF1E54D186BAA968CDD09A0D932F9E66DF46DDE1D3F620BAC1F90D532DA4935FA418C9F2AE1D9B5BF8E9A8D95314963EF69E32D4669898011126204B4CF2D15E0D0718659F03E0215B578453F8B490D85598B4C261F3693B216AB6B598C23D19736E6C7EA816E2C0DB7098FA907EA19F0C9EB8383A73D6B3F74A5E9F04BEFF3AE8EBF3694670762698C9A9369A81FB9AF2C7CF08AC1DEB227CDCBC873E9C872E784684E995E407583536CE994CFED53371769721765662E776773717697217656887695C387D7300A6D38F5C1200000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.RealFeature;


public class NEShapeTaggerFeatures extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public NEShapeTaggerFeatures() { super("lbj.NEShapeTaggerFeatures"); }

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
      System.err.println("Classifier 'NEShapeTaggerFeatures(NEWord)' defined on line 529 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == NEShapeTaggerFeatures.exampleCache) {
        return NEShapeTaggerFeatures.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;

    if (Parameters.featuresToUse.containsKey("NEShapeTaggerFeatures"))
    {
      int i;
      NEWord w = word, last = word;
      for (i = 0; i <= 2 && last != null; ++i)
      {
        last = (NEWord) last.next;
      }
      for (i = 0; i > -2 && w.previous != null; --i)
      {
        w = (NEWord) w.previous;
      }
      for (; w != last; w = (NEWord) w.next)
      {
        if (w.shapePredPer > 0)
        {
          __id = this.name + i + "_Per";
          __result.addFeature(new RealFeature(this.containingPackage, __id, w.shapePredPer));
        }
        if (w.shapePredOrg > 0)
        {
          __id = this.name + i + "_Org";
          __result.addFeature(new RealFeature(this.containingPackage, __id, w.shapePredOrg));
        }
        if (w.shapePredLoc > 0)
        {
          __id = this.name + i + "_Loc";
          __result.addFeature(new RealFeature(this.containingPackage, __id, w.shapePredLoc));
        }
        i++;
      }
    }

    NEShapeTaggerFeatures.exampleCache = __example;
    NEShapeTaggerFeatures.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'NEShapeTaggerFeatures(NEWord)' defined on line 529 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "NEShapeTaggerFeatures".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof NEShapeTaggerFeatures; }
}

