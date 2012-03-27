// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000DA09F3F62C03015CFBAC90909C6502541F658E45575A3419A302607393285D06B4767491AAC77FE163F7A4B50130B8D2DDB7FEEDFC624DDC01ACD86A97D4A7BE5CCF5EDD1550D1F12166368F287D2BC6980D698FCEA5B10902A3B08720B8DDF5958C9E5AC8AB6995B32016C6003CEA9C49FA954C8C958D6ED0ADA3C648481341CA5699464B4029BEAAA58BF44B2E467FDE78F046C6D2CC8873560584EA8EF6F2157E52AE6A2159A4ED2E079B280BB087E6AD386D812EE0F79EDB920B7E832187E912C02D29107F42BD2DCA0E1F7CAE8F792E8A948783881C50DD4A6ED1FD9369AB7069AF71B4D53C25FB1BAFF62DA6598CD8200000

package lbj;

import ws.palladian.external.lbj.StringStatisticsUtils.OccurrenceCounter;
import ws.palladian.external.lbj.Tagger.NEWord;
import LBJ2.classify.Classifier;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.RealFeature;


public class charNgrams extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public charNgrams() { super("lbj.charNgrams"); }

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
      System.err.println("Classifier 'charNgrams(NEWord)' defined on line 487 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == charNgrams.exampleCache) {
        return charNgrams.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;

    OccurrenceCounter grams2 = new OccurrenceCounter();
    OccurrenceCounter grams3 = new OccurrenceCounter();
    for (int i = 0; i < word.form.length() - 2; i++)
    {
      grams2.addToken(word.form.substring(i, i + 2));
    }
    for (int i = 0; i < word.form.length() - 3; i++)
    {
      grams3.addToken(word.form.substring(i, i + 3));
    }
    String[] tokens = grams2.getTokens();
    for (int i = 0; i < tokens.length; i++)
    {
      __id = this.name + "grams2" + tokens[i];
      __result.addFeature(new RealFeature(this.containingPackage, __id, grams2.getCount(tokens[i]) / grams2.totalTokens));
    }
    tokens = grams3.getTokens();
    for (int i = 0; i < tokens.length; i++)
    {
      __id = this.name + "grams3" + tokens[i];
      __result.addFeature(new RealFeature(this.containingPackage, __id, grams3.getCount(tokens[i]) / grams3.totalTokens));
    }

    charNgrams.exampleCache = __example;
    charNgrams.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'charNgrams(NEWord)' defined on line 487 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "charNgrams".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof charNgrams; }
}

