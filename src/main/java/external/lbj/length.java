// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000B49CC2E4E2A4D294558C94DCB4F29C0D0F37D0FCF2A415827021A9A063ABA05DA0049D2D2AC30B88E5A5E715EAE1455A6A5B24D20065EB8E20E3000000

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


public class length extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public length() { super("lbj.length"); }

  public String getInputType() { return "LbjTagger.NEWord"; }
  public String getOutputType() { return "discrete"; }

  public String discreteValue(Object __example)
  {
    if (!(__example instanceof NEWord))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'length(NEWord)' defined on line 518 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    NEWord word = (NEWord) __example;
    return "" + (word.form.length());
  }

  public FeatureVector classify(Object example)
  {
    if (example == exampleCache) return cache;
    cache = new FeatureVector(new DiscreteFeature(containingPackage, name, discreteValue(example)));
    exampleCache = example;
    return cache;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof NEWord))
      {
        System.err.println("Classifier 'length(NEWord)' defined on line 518 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "length".hashCode(); }
  public boolean equals(Object o) { return o instanceof length; }
}

