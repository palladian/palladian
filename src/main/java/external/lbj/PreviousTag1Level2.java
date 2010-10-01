// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D8E81CA02C030144F756C080DC1C08E1DAD36F4A8878A87E867B5201359C6A6922EFBB96DA8E15616F43FEDC4968F2E920DC170F4773D4BC58EA79B3AB39D552BFCF4D8F21D5C721B950E1035129386FAFA111FCAA22D1A5F4C54374625796C50D6C1F69AF44CFAE484930BE20C4A8F8419D82E474767AE635E13BC0EA5B65E4D6BFC3A1A62FFE94AC071E3678175F010627C401B85A0CA19C4B752EB639AC1DE4F99C6A8728C6C0FF9051DFD8ED593093EDB07503DC04B2100000

package external.lbj;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.learn.*;
import LBJ2.parse.*;
import java.util.*;

import external.LbjTagger.BrownClusters;
import external.LbjTagger.Gazzetteers;
import external.LbjTagger.NEWord;
import external.LbjTagger.Parameters;
import external.lbj.StringStatisticsUtils.*;


public class PreviousTag1Level2 extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public PreviousTag1Level2() { super("lbj.PreviousTag1Level2"); }

  public String getInputType() { return "LbjTagger.NEWord"; }
  public String getOutputType() { return "discrete%"; }

  public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof NEWord))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'PreviousTag1Level2(NEWord)' defined on line 292 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == PreviousTag1Level2.exampleCache) return PreviousTag1Level2.cache;

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
        if (NETaggerLevel2.isTraining)
        {
          __id = this.name + ("-1");
          __value = "" + (((NEWord) w.previous).neLabel);
          __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
        }
        else
        {
          __id = this.name + ("-1");
          __value = "" + (((NEWord) w.previous).neTypeLevel2);
          __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
        }
      }
    }

    PreviousTag1Level2.exampleCache = __example;
    PreviousTag1Level2.cache = __result;

    return __result;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof NEWord))
      {
        System.err.println("Classifier 'PreviousTag1Level2(NEWord)' defined on line 292 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "PreviousTag1Level2".hashCode(); }
  public boolean equals(Object o) { return o instanceof PreviousTag1Level2; }
}

