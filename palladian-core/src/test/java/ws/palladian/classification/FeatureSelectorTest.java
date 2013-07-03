/**
 * Created on: 20.11.2012 08:33:41
 */
package ws.palladian.classification;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.classification.featureselection.AverageMergingStrategy;
import ws.palladian.classification.featureselection.ChiSquaredFeatureRanker;
import ws.palladian.classification.featureselection.FeatureDetails;
import ws.palladian.classification.featureselection.FeatureRanking;
import ws.palladian.classification.featureselection.FeatureRanker;
import ws.palladian.classification.featureselection.InformationGainFeatureSelector;
import ws.palladian.classification.featureselection.RoundRobinMergingStrategy;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.SparseFeature;

/**
 * <p>
 * Tests whether the {@link FeatureRanker} works correctly or nor.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class FeatureSelectorTest {
    private List<Instance> fixture;

    @Before
    public void setUp() {
        FeatureVector fv1 = new FeatureVector();
        FeatureVector fv2 = new FeatureVector();
        FeatureVector fv3 = new FeatureVector();

        ListFeature<SparseFeature<String>> listFeature1 = new ListFeature<SparseFeature<String>>("testfeature");
        listFeature1.add(new SparseFeature<String>("a"));
        listFeature1.add(new SparseFeature<String>("b"));
        listFeature1.add(new SparseFeature<String>("c"));
        listFeature1.add(new SparseFeature<String>("a"));
        listFeature1.add(new SparseFeature<String>("d"));
        fv1.add(listFeature1);

        ListFeature<SparseFeature<String>> listFeature2 = new ListFeature<SparseFeature<String>>("testfeature");
        listFeature2.add(new SparseFeature<String>("a"));
        listFeature2.add(new SparseFeature<String>("b"));
        listFeature2.add(new SparseFeature<String>("c"));
        fv2.add(listFeature2);

        ListFeature<SparseFeature<String>> listFeature3 = new ListFeature<SparseFeature<String>>("testfeature");
        listFeature3.add(new SparseFeature<String>("d"));
        listFeature3.add(new SparseFeature<String>("e"));
        listFeature3.add(new SparseFeature<String>("f"));
        fv3.add(listFeature3);

        Instance instance1 = new Instance("c1", fv1);
        Instance instance2 = new Instance("c1", fv2);
        Instance instance3 = new Instance("c2", fv3);

        fixture = new ArrayList<Instance>(3);
        fixture.add(instance1);
        fixture.add(instance2);
        fixture.add(instance3);
    }

    @After
    public void tearDown() {
        fixture = null;
    }

    @Test
    public void testChiSquareFeatureSelection() {
        FeatureRanker featureSelector = new ChiSquaredFeatureRanker(new AverageMergingStrategy());

        FeatureRanking ranking = featureSelector.rankFeatures(fixture);
        // System.out.println(ranking);

        assertThat(ranking.getAll().get(5).getValue(), is("d"));
        assertThat(ranking.getAll().get(5).getScore(), is(closeTo(0.75, 0.0001)));
        assertThat(ranking.getAll().get(4).getValue(), is("a"));
        assertThat(ranking.getAll().get(4).getScore(), is(closeTo(3.0, 0.0001)));
        assertThat(ranking.getAll().get(3).getValue(), is("c"));
        assertThat(ranking.getAll().get(3).getScore(), is(closeTo(3.0, 0.0001)));
        assertThat(ranking.getAll().get(2).getValue(), is("b"));
        assertThat(ranking.getAll().get(2).getScore(), is(closeTo(3.0, 0.0001)));
        assertThat(ranking.getAll().get(1).getValue(), is("e"));
        assertThat(ranking.getAll().get(1).getScore(), is(closeTo(3.0, 0.0001)));
        assertThat(ranking.getAll().get(0).getValue(), is("f"));
        assertThat(ranking.getAll().get(0).getScore(), is(closeTo(3.0, 0.0001)));
    }

    @Test
    public void testChiSquaredRoundRobinMerge() throws Exception {
        FeatureRanker featureSelector = new ChiSquaredFeatureRanker(new RoundRobinMergingStrategy());

        FeatureRanking ranking = featureSelector.rankFeatures(fixture);
        // System.out.println(ranking);

        assertThat(ranking.getAll().get(5).getValue(), is("d"));
        assertThat(ranking.getAll().get(5).getScore(), is(closeTo(1.0, 0.0001)));
        assertThat(ranking.getAll().get(4).getValue(), is("a"));
        assertThat(ranking.getAll().get(4).getScore(), is(closeTo(2.0, 0.0001)));
        assertThat(ranking.getAll().get(3).getValue(), is("c"));
        assertThat(ranking.getAll().get(3).getScore(), is(closeTo(3.0, 0.0001)));
        assertThat(ranking.getAll().get(2).getValue(), is("b"));
        assertThat(ranking.getAll().get(2).getScore(), is(closeTo(4.0, 0.0001)));
        assertThat(ranking.getAll().get(1).getValue(), is("e"));
        assertThat(ranking.getAll().get(1).getScore(), is(closeTo(5.0, 0.0001)));
        assertThat(ranking.getAll().get(0).getValue(), is("f"));
        assertThat(ranking.getAll().get(0).getScore(), is(closeTo(6.0, 0.0001)));
    }

    @Test
    public void testInformationGainFeatureExtraction() throws Exception {
        FeatureRanker featureSelector = new InformationGainFeatureSelector();

        FeatureRanking ranking = featureSelector.rankFeatures(fixture);
        System.out.println(ranking);

        assertThat(ranking.getAll().get(5).getValue(), is("d"));
        assertThat(ranking.getAll().get(5).getScore(), is(closeTo(-0.013155014372715268, 0.0001)));
        assertThat(ranking.getAll().get(4).getValue(), is("e"));
        assertThat(ranking.getAll().get(4).getScore(), is(closeTo(0.2995107095169547, 0.0001)));
        assertThat(ranking.getAll().get(3).getValue(), is("f"));
        assertThat(ranking.getAll().get(3).getScore(), is(closeTo(0.2995107095169547, 0.0001)));
        assertThat(ranking.getAll().get(2).getValue(), is("a"));
        assertThat(ranking.getAll().get(2).getScore(), is(closeTo(0.2995107095169547, 0.0001)));
        assertThat(ranking.getAll().get(1).getValue(), is("c"));
        assertThat(ranking.getAll().get(1).getScore(), is(closeTo(0.2995107095169547, 0.0001)));
        assertThat(ranking.getAll().get(0).getValue(), is("b"));
        assertThat(ranking.getAll().get(0).getScore(), is(closeTo(0.2995107095169547, 0.0001)));
    }

    @Test
    public void testNumericFeatureWithInformationGain() throws Exception {
        List<Instance> dataset = new ArrayList<Instance>();
        FeatureVector fV1 = new FeatureVector();
        fV1.add(new NumericFeature("numeric", 1.0d));
        Instance instance1 = new Instance("a", fV1);
        dataset.add(instance1);
        FeatureVector fV2 = new FeatureVector();
        fV2.add(new NumericFeature("numeric", 2.0d));
        Instance instance2 = new Instance("b", fV2);
        dataset.add(instance2);
        FeatureVector fV3 = new FeatureVector();
        fV3.add(new NumericFeature("numeric", 3.0d));
        Instance instance3 = new Instance("a", fV3);
        dataset.add(instance3);

        FeatureRanker featureSelector = new InformationGainFeatureSelector();

        FeatureRanking ranking = featureSelector.rankFeatures(dataset);
        System.out.println(ranking);

        Assert.assertThat(ranking.getAll().get(0).getValue(), Matchers.is("numeric"));
        Assert.assertThat(ranking.getAll().get(0).getIdentifier(), Matchers.is("feature"));
        Assert.assertThat(ranking.getAll().get(0).getScore(), Matchers.is(Matchers.closeTo(0.35255381922216517, 0.0001)));
    }
}
