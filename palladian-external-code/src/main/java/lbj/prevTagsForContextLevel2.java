// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000592916F63D030168FFAC1598926502491FD695FB03D04889AC4C286F16A80979C53838B6F83B3B25D4BFFEC9DE6A51DE0D48A295DC5E9B7FED8FD3249AF52CD11ED752B37F1C2D9953E1F7BFB0CB74D7C9DCFCFAD25B0B2E340C9EB687005B08CE2529C52A74275C205AF180D556FB93C2A1E669AC8BFC8BEC62782A311228281F0AE514F52C8AE03B831A48B2F4AC477357C747C5638BA1B8FEAE21EB43DC04486A1C33B30B4213AD48F7B14F0FD78EA58528C41323D2141C9EC0E81E8E887AEB0E6A14BE212FC55036159E645DFF21BC4499C85266550B0F2BC86F7371528B868F34B487CDB23F9548B0156707F9A33690FA095395CFD3B87B296D70E31EDE47A3DD37AA2FC383D846F98CE99C8EAA0D86A3FF38B290A09452BBD7D3A2449B8EBB0CF5384DE6B4881B56E7EC1A57849630A05EA22E8493101789E78F2AB42C6553E5953E25BC7596A5BBCF886094A7B45C0E07E6593CC4CEE29BFAB09D6B59DF986EFB5F82E3F58CB54DC291EAA15B3C725A34A111C88D33065BEB3C431734FE768FC608F8701C4C94F45033A58138905FDE6FB4E0A66B379D1AF8E2D5850CDF521667BAFB9DE07C822F8FC162FD72C72321636F4EF25C8B3D95A7520ED04695B678B5D8267497BE5AE4397B732CFBF3CDE674C033400000

package lbj;

import ws.palladian.external.lbj.StringStatisticsUtils.OccurrenceCounter;
import ws.palladian.external.lbj.Tagger.NEWord;
import ws.palladian.external.lbj.Tagger.Parameters;
import LBJ2.classify.Classifier;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.RealFeature;

public class prevTagsForContextLevel2 extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public prevTagsForContextLevel2() { super("lbj.prevTagsForContextLevel2"); }

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
      System.err.println("Classifier 'prevTagsForContextLevel2(NEWord)' defined on line 328 of LbjTagger.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == prevTagsForContextLevel2.exampleCache) {
        return prevTagsForContextLevel2.cache;
    }

    NEWord word = (NEWord) __example;
    FeatureVector __result = new FeatureVector();
    String __id;

    if (Parameters.featuresToUse.containsKey("prevTagsForContext"))
    {
      int i, j;
      NEWord w = word;
      String[] words = new String[3];
      OccurrenceCounter[] count = new OccurrenceCounter[3];
      for (i = 0; i <= 2 && w != null; ++i)
      {
        count[i] = new OccurrenceCounter();
        words[i] = w.form;
        w = (NEWord) w.next;
      }
      w = word.previousIgnoreSentenceBoundary;
      for (i = 0; i < 1000 && w != null; i++)
      {
        for (j = 0; j < words.length; j++)
        {
          if (words[j] != null && w.form.equals(words[j]))
          {
            if (NETaggerLevel2.isTraining)
            {
              if (Parameters.prevPredictionsLevel2RandomGenerator.useNoise())
              {
                count[j].addToken(Parameters.prevPredictionsLevel2RandomGenerator.randomLabel());
              }
              else
              {
                count[j].addToken(w.neLabel);
              }
            }
            else
            {
              count[j].addToken(w.neTypeLevel2);
            }
          }
        }
        w = w.previousIgnoreSentenceBoundary;
      }
      for (j = 0; j < count.length; j++)
      {
        if (count[j] != null)
        {
          String[] all = count[j].getTokens();
          for (i = 0; i < all.length; i++)
          {
            __id = this.name + j + "_" + all[i];
            __result.addFeature(new RealFeature(this.containingPackage, __id, count[j].getCount(all[i]) / count[j].totalTokens));
          }
        }
      }
    }

    prevTagsForContextLevel2.exampleCache = __example;
    prevTagsForContextLevel2.cache = __result;

    return __result;
  }

  @Override
public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) {
        if (!(examples[i] instanceof NEWord))
          {
            System.err.println("Classifier 'prevTagsForContextLevel2(NEWord)' defined on line 328 of LbjTagger.lbj received '" + examples[i].getClass().getName() + "' as input.");
            new Exception().printStackTrace();
            System.exit(1);
          }
    }

    return super.classify(examples);
  }

  @Override
public int hashCode() { return "prevTagsForContextLevel2".hashCode(); }
  @Override
public boolean equals(Object o) { return o instanceof prevTagsForContextLevel2; }
}

