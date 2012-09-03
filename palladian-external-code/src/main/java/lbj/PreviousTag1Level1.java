// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D8E8BCA02C030154F75EA501A95430DDA5D5675A49E2A2EA36B36904C4523D7054CF773D788E25606657FC9B7B2DC5A3A696B8CD15FAB9E8B0557C72AE9C4C1669E5A175160FF406F11E90D73489B27AEEE117C2F64AADEC1171DC999469D8D6596BC74A13C0E757180131B6B58E40F192E03B8399D938C7CA97C6E00BD91326D6BC25F68A9CD2394A6E2C9FE0D6BE920C469901441708D12C57FA0C7D624A5A39AB29940F2091F1EF31A26C70D2593193FDB1BB11312CB2100000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;

public class PreviousTag1Level1 extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public PreviousTag1Level1() { super("lbj.PreviousTag1Level1"); }

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
      System.err.println("Classifier 'PreviousTag1Level1(NEWord)' defined on line 199 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == PreviousTag1Level1.exampleCache) {
        return PreviousTag1Level1.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    if (Parameters.featuresToUse.containsKey("PreviousTag1"))
    {
      int i;
      NEWord w = word;
      if (w.previous != null)
      {
        if (NETaggerLevel1.isTraining)
        {
          __id = this.name + "-1";
          __value = "" + ((NEWord) w.previous).neLabel;
          __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
        }
        else
        {
          __id = this.name + "-1";
          __value = "" + ((NEWord) w.previous).neTypeLevel1;
          __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
        }
      }
    }

    PreviousTag1Level1.exampleCache = __example;
    PreviousTag1Level1.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'PreviousTag1Level1(NEWord)' defined on line 199 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "PreviousTag1Level1".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof PreviousTag1Level1; }
}

