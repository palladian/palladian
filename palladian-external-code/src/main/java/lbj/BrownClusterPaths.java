// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B880000000000000005609FDB43C03017CFF59370E8698D02E3A96D705C7214A0A2E3C8D348DEAD5A479C8BB4DE44CFFDD4ADECD8D8042CD5EBFDFCDF85B1E511A7CB687427DBD7A6B36F845A6FB1E4E5F9F3D1DA1AF075A09760F306A284A4D4A7B1C14C2B24DEB324E777F1C8275ECA7D6C2FB0E7723BB0ADC2D4320CA703A0E0468260AFD04BA9DF41828AC114262487BA0C04E50CD1CC7E3A4EAA00BD5BDA2012C44ED4EB9A5D47885A5CDB750FB76C9708C60E4F27748F56C57C74A569D04BEF41574DDF3B450540F4C22AEC5C1B8646CB97236BE5C216717AE0A93DD40BCA1D7948599D3272DBC04DD6A78183CA6A91B9D602F1DE2B54B5BFD48C80111EC86913CC27F3E7F2A956CEDC8011F907EF0D2CE48FAEC100000

package lbj;

import ws.palladian.external.lbj.Tagger.BrownClusters;
import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;


public class BrownClusterPaths extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public BrownClusterPaths() { super("lbj.BrownClusterPaths"); }

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
      System.err.println("Classifier 'BrownClusterPaths(NEWord)' defined on line 56 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == BrownClusterPaths.exampleCache) {
        return BrownClusterPaths.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    if (Parameters.featuresToUse.containsKey("BrownClusterPaths"))
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
        String[] paths = BrownClusters.getPrefixes(w.form);
        for (int j = 0; j < paths.length; j++)
        {
          __id = this.name + i;
          __value = "" + paths[j];
          __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
        }
        i++;
      }
    }

    BrownClusterPaths.exampleCache = __example;
    BrownClusterPaths.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'BrownClusterPaths(NEWord)' defined on line 56 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "BrownClusterPaths".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof BrownClusterPaths; }
}

