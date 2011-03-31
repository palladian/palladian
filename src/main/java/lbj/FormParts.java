// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000DC29D5B6BD034168FFACB53CA8C89884779172BB96B181BF060DE8E549E586EE942A4D69239E87E6A56FFD77C65BD464B36713A03C02B4A3F5FE97E8EAD6CA2013DB1CCD786EB890C15D7E3DB0F1EA1D9C2936A32CD3CE20A4D1885D432EB12A66F734ECED916BEDD955B2A6843D7F6D4D1FDF2D9F047C6229AD37F3113D9F3B09C37C1E12EF52FE7CF6BE792D6E229FC6B199A1DEB56DB906D17DE456373C6A60580E30C0FEA36C28C07B18047BC1C45CE3C40DABB17EB378D7548834189023418FBAC82F1530DDA5653693FFC104EE50E34DD87EEFB642D5977C6CAB8F186B2A554C5CC6024FA0CDFE119C542463EC039162DA71250B4809285CC03A3A4BFAC7B2B6A3F3EBB093E3CB19C358E620DF0BEBD852AB59DA90AA3C1CC0EADABEE5EB5CB398F7B3C0585641BCBCB24D622724F3347A73DFEA488208AEB8EA5E67C5ACF6AB374D539B52FA4E6B8267AF39CB90E3D635A6D241A135BDB3A31BB4BC1582EC313E3D43A5180AE927E5EAFA46A234E81D8AF1FCFEDADE5F5639DB723745E31419DE3781E4E11DF904C4768FFC93415CB0C125B4639D4EBF50EBEA755F53400000

package lbj;

import ws.palladian.external.lbj.StringStatisticsUtils.MyString;
import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.FeatureVector;


public class FormParts extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public FormParts() { super("lbj.FormParts"); }

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
      System.err.println("Classifier 'FormParts(NEWord)' defined on line 76 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == FormParts.exampleCache) {
        return FormParts.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    if (!Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.DualTokenizationScheme) && !Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.LbjTokenizationScheme))
    {
      System.out.println("Fatal error at FormParts feature extractor: unknown tokenization scheme: " + Parameters.LbjTokenizationScheme);
      System.exit(0);
    }
    if (Parameters.featuresToUse.containsKey("Forms") && Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.DualTokenizationScheme))
    {
      __id = this.name + "0";
      __value = "" + word.form;
      __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
      int i = -1;
      int count = -1;
      NEWord w = (NEWord) word.previous;
      while (w != null && i >= -2)
      {
        String[] lastParts = w.parts;
        for (int j = 0; j < lastParts.length; j++)
        {
          __id = this.name + count;
          __value = "" + MyString.normalizeDigitsForFeatureExtraction(lastParts[j]);
          __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
          count--;
        }
        w = (NEWord) w.previous;
        i--;
      }
      i = 1;
      count = 1;
      w = (NEWord) word.next;
      while (w != null && i <= 2)
      {
        String[] lastParts = w.parts;
        for (int j = 0; j < lastParts.length; j++)
        {
          __id = this.name + count;
          __value = "" + MyString.normalizeDigitsForFeatureExtraction(lastParts[j]);
          __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value));
          count++;
        }
        w = (NEWord) w.next;
        i++;
      }
    }

    FormParts.exampleCache = __example;
    FormParts.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'FormParts(NEWord)' defined on line 76 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "FormParts".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof FormParts; }
}

