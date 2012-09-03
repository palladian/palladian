// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D90915B43C034158FFAC170E869AD22E3A95D727B71154051F93477B3E24B984E6A6735CFFE6296572B749040E683FD772CDD2B43E8C3D93636D5F29D3CAF5DABD26C075E85598F407B8CE15B3DD784939AA52DE70742FC6F548AA6CA1FA9D8CD1D12B5423C22FC3246C3851E7C68A391F20D961F3F0A0DA578C83C879A0C855D8B2C279354ECA6891ABE41A8283AF66E6EFE5E96EAC0DEDB2C7D987E60562FC85DB93A776B38C1D65699C63EF55D137FBE25121189852AE43C1B43A348C8018B8207D1E5306DFF70F00FDF1E9CB363BBAC4078EE8F38E6977CE52C63733DEA7DB7FE473E9DA9C6A6AC55C2E8D71EC7377BFEEEA2C100000

package lbj;

import ws.palladian.external.lbj.StringStatisticsUtils.MyString;
import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;


public class Forms extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public Forms() { super("lbj.Forms"); }

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
      System.err.println("Classifier 'Forms(NEWord)' defined on line 37 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == Forms.exampleCache) {
        return Forms.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    if (Parameters.featuresToUse.containsKey("Forms"))
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
        __id = this.name + i++;
        __value = "" + w.form;
        __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
      }
      for (; w != last; w = (NEWord) w.next)
      {
        __id = this.name + i;
        __value = "" + MyString.normalizeDigitsForFeatureExtraction(w.form);
        __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
        i++;
      }
    }

    Forms.exampleCache = __example;
    Forms.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'Forms(NEWord)' defined on line 37 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "Forms".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof Forms; }
}

