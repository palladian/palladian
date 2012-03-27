// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000DA29BDA43C040168F556C085634CE251DBAE1401F002A411CA8712E5C63D94BB639DDAB331ADA5BFEEE63D4525BA80A21848DD9F7EF9F666230D41B346CD783D421D33421DDB870BE60035FFA01AD578508E40488DBB55E456E59E842BD13A1DFAA85B53771F8033498F29BA49EA686CA3C335482A22F37F1AEDE4A48124DA60F71FDB9EF3F756B1A74EBB9313662D6EC272E4B1E4D880E251BA410D93BE041F6AF5840517EE01076CE45CC6D53127336367A60AAC30458D73120880E74886D680076A9543CF9795C81B29856D5AE9DB72491B53CA4B1AB6C9B80A44B0A865C3D3471A35C244626D562354334E190F6AEF8026520DE5074D2FF96770E8B50154A3C2717BB96072054490D01240397B00A95177ACBF4CE766341D830F2C519F27BB0D1EFEA01D7B58AB05FF8A4F96A4FF5FBAD26BB58641D2CA1662AC13590DEFA3A8AA3C1F1B5D2C5F68CB01F3AE7A53CAF7ED102470352014300000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;

public class Affixes extends Classifier {
  private static FeatureVector cache;
  private static Object exampleCache;

  public Affixes() { super("lbj.Affixes"); }

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
      System.err.println("Classifier 'Affixes(NEWord)' defined on line 161 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == Affixes.exampleCache) {
        return Affixes.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    if (!Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.DualTokenizationScheme) && !Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.LbjTokenizationScheme))
    {
      System.out.println("Fatal error at Affixes feature extractor: unknown tokenization scheme: " + Parameters.LbjTokenizationScheme);
      System.exit(0);
    }
    if (Parameters.featuresToUse.containsKey("Affixes"))
    {
      int N = word.form.length();
      for (int i = 3; i <= 4; ++i)
      {
        if (word.form.length() > i)
        {
          __id = this.name + "p|";
          __value = "" + word.form.substring(0, i);
          __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
        }
      }
      for (int i = 1; i <= 4; ++i)
      {
        if (word.form.length() > i)
        {
          __id = this.name + "s|";
          __value = "" + word.form.substring(N - i);
          __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
        }
      }
      if (Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.DualTokenizationScheme))
      {
        for (int i = 0; i < word.parts.length; i++)
        {
          __id = this.name + "part" + i;
          __value = "" + word.parts[i];
          __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
        }
      }
    }

    Affixes.exampleCache = __example;
    Affixes.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'Affixes(NEWord)' defined on line 161 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "Affixes".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof Affixes; }
}

