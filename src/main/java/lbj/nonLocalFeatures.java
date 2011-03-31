// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B8800000000000000055E81BA02C04C068F552B807741B83BDA3888E427B838349E0786A7E1C190CD5A8384FDDD8D2A0E2904EFFEFF8464F96504C476EB9F4724F2366C26CD1FAC9FE0F4D161AD53CB0E2293258EA78149920BB93BA30ACE3527C49EFAE6B181833898420159DD43ABAD5AA5724A02F0DB4559551714A28B44D5CE16BF31BB9F9AF83F0C32989F22A2F906A739B0003C19B000000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import LBJ2.classify.Classifier;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.RealFeature;

public class nonLocalFeatures extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public nonLocalFeatures() { super("lbj.nonLocalFeatures"); }

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
      System.err.println("Classifier 'nonLocalFeatures(NEWord)' defined on line 190 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == nonLocalFeatures.exampleCache) {
        return nonLocalFeatures.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;

    String[] feats = word.getAllNonlocalFeatures();
    for (int i = 0; i < feats.length; i++)
    {
      __id = this.name + feats[i];
      __result.addFeature(new RealFeature(this.containingPackage, __id, word.getNonLocFeatCount(feats[i])));
    }

    nonLocalFeatures.exampleCache = __example;
    nonLocalFeatures.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'nonLocalFeatures(NEWord)' defined on line 190 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "nonLocalFeatures".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof nonLocalFeatures; }
}

