// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B880000000000000005AF813B02C030158FFAC3B02433891A3A5D1D9C22E0517E8DE5B4026229B6D222EF7735B59AB8E227073D77FDB775A6E2C353437CED3D55BB6937557291D59C421FE63747EB4471690CA618B3475887FACBA37831FC2B225D4BE9837770629583BD82D697B47B83AF4D54244F7B6B18E41F6926DF217AF27672F232F89DA16B5364C8961F8D2406264C75A77B9097539F1E109A937F1A196B57F003956244B8422C21397F77280969235722392E1023140F7882FBD5868A6DBD6C9723176D09D38100000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;

public class PreviousTag2Level2 extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public PreviousTag2Level2() { super("lbj.PreviousTag2Level2"); }

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
      System.err.println("Classifier 'PreviousTag2Level2(NEWord)' defined on line 309 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == PreviousTag2Level2.exampleCache) {
        return PreviousTag2Level2.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    if (Parameters.featuresToUse.containsKey("PreviousTag2"))
    {
      int i;
      NEWord w = word;
      if (w.previous != null)
      {
        if (((NEWord) w.previous).previous != null)
        {
          if (NETaggerLevel2.isTraining)
          {
            __id = this.name + "-2";
            __value = "" + ((NEWord) ((NEWord) w.previous).previous).neLabel;
            __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
          }
          else
          {
            __id = this.name + "-2";
            __value = "" + ((NEWord) ((NEWord) w.previous).previous).neTypeLevel2;
            __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
          }
        }
      }
    }

    PreviousTag2Level2.exampleCache = __example;
    PreviousTag2Level2.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'PreviousTag2Level2(NEWord)' defined on line 309 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "PreviousTag2Level2".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof PreviousTag2Level2; }
}

