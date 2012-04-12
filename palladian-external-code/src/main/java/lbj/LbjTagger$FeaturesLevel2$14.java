// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// discrete% LbjTagger$FeaturesLevel2$14(NEWord word) <- PreviousTag1Level2 && BrownClusterPaths

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import LBJ2.classify.Classifier;
import LBJ2.classify.Feature;
import LBJ2.classify.FeatureVector;


public class LbjTagger$FeaturesLevel2$14 extends Classifier
{
  private static final PreviousTag1Level2 left = new PreviousTag1Level2();
  private static final BrownClusterPaths right = new BrownClusterPaths();

  private static FeatureVector cache;
  private static Object exampleCache;

  public LbjTagger$FeaturesLevel2$14() { super("lbj.LbjTagger$FeaturesLevel2$14"); }

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
      System.err.println("Classifier 'LbjTagger$FeaturesLevel2$14(NEWord)' defined on line 471 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == LbjTagger$FeaturesLevel2$14.exampleCache) {
        return LbjTagger$FeaturesLevel2$14.cache;
    }

    FeatureVector leftVector = left.classify(__example);
    FeatureVector rightVector = right.classify(__example);
    LbjTagger$FeaturesLevel2$14.cache = new FeatureVector();
    for (java.util.Iterator I = leftVector.iterator(); I.hasNext(); )
    {
      Feature lf = (Feature) I.next();
      for (java.util.Iterator J = rightVector.iterator(); J.hasNext(); )
      {
        Feature rf = (Feature) J.next();
        if (lf.equals(rf)) {
            continue;
        }
        LbjTagger$FeaturesLevel2$14.cache.addFeature(lf.conjunction(rf, this));
      }
    }

    LbjTagger$FeaturesLevel2$14.cache.sort();

    LbjTagger$FeaturesLevel2$14.exampleCache = __example;
    return LbjTagger$FeaturesLevel2$14.cache;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'LbjTagger$FeaturesLevel2$14(NEWord)' defined on line 471 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "LbjTagger$FeaturesLevel2$14".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof LbjTagger$FeaturesLevel2$14; }

}

