// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B880000000000000005719F5B43C03415CFBACD51C1D2B6B88F8675146AF02AC0F162E37C6F67B49C29294AEC936FDDDB9EF95B49941A0937FE9FDD39B9C9B9C43A5C351C481C01CAEA1FC730F9A47EB93658F22B05A7FCC2752DF5F3B351E04FB006911C908710EFB33DC6F400D62E2099DA53A9D8AF038176A4A56C5A975C3AFED51E97140E012D20F40A7634AD0F3401CC8DEE0900970C7E47CBD4083C2358B389FCBD6995A02B6122185C28B3E57EBE2C60DC93698F363183F483F00153C934C596CF6EAA6330D2A8A1AD16C8A1AFE2CA48A38C3E68423D667343CE36D7925209940624CA855CD231CF7137238BD778E4E72EB5E6DC8595B29F686DD66B35BF540DACAC6F2152C29629B596C205E6DEECF0CD62A4732723A6E92CA674F65911036E6E3AAA05FA8914FB3F7645C74BE791011688331753AEF3E815C527DC6C6B65FB6EB338149601CBCC3850DB9CDF42672D7D3F15DBDC6792553A2D012C1B9EBF302DEB2F80E200000

package lbj;

import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;

public class WordTypeInformation extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public WordTypeInformation() { super("lbj.WordTypeInformation"); }

  @Override
public String getInputType() { return "LbjTagger.NEWord"; }
  @Override
public String getOutputType() { return "discrete%"; }

  @Override
public String[] allowableValues()
  {
    return DiscreteFeature.BooleanValues;
  }

  @Override
public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof NEWord))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'WordTypeInformation(NEWord)' defined on line 133 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == WordTypeInformation.exampleCache) {
        return WordTypeInformation.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    if (Parameters.featuresToUse.containsKey("WordTypeInformation"))
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
      for (; w != last; w = (NEWord) w.next, ++i)
      {
        boolean allCapitalized = true, allDigits = true, allNonLetters = true;
        for (int j = 0; j < w.form.length(); ++j)
        {
          allCapitalized &= Character.isUpperCase(w.form.charAt(j));
          allDigits &= Character.isDigit(w.form.charAt(j));
          allNonLetters &= !Character.isLetter(w.form.charAt(j));
        }
        __id = this.name + "c" + i;
        __value = "" + allCapitalized;
        __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value, valueIndexOf(__value), (short) 2));
        __id = this.name + "d" + i;
        __value = "" + allDigits;
        __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value, valueIndexOf(__value), (short) 2));
        __id = this.name + "p" + i;
        __value = "" + allNonLetters;
        __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value, valueIndexOf(__value), (short) 2));
      }
    }

    WordTypeInformation.exampleCache = __example;
    WordTypeInformation.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'WordTypeInformation(NEWord)' defined on line 133 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "WordTypeInformation".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof WordTypeInformation; }
}

