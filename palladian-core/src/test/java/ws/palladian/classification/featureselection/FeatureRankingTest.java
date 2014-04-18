package ws.palladian.classification.featureselection;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Tests whether the {@link FeatureRanker} works correctly or not.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class FeatureRankingTest {
    private Collection<Instance> fixture;

    @Before
    public void setUp() {
        fixture = CollectionHelper.newArrayList();
        fixture.add(new InstanceBuilder().set("a", 2).set("b", 1).set("c", 1).set("d", 1).create("c1"));
        fixture.add(new InstanceBuilder().set("a", 1).set("b", 1).set("c", 1).create("c1"));
        fixture.add(new InstanceBuilder().set("d", 1).set("e", 1).set("f", 1).create("c2"));
        fixture.add(new InstanceBuilder().set("d", 1).set("f", 1).set("f", 1).create("c2"));
        fixture.add(new InstanceBuilder().set("a", 1).set("c", 1).create("c1"));
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

        assertThat(ranking.getAll().get(5).getName(), is("e"));
        assertThat(ranking.getAll().get(5).getScore(), is(closeTo(1.875, 0.0001)));
        assertThat(ranking.getAll().get(4).getName(), is("b"));
        assertThat(ranking.getAll().get(4).getScore(), is(closeTo(2.22222, 0.0001)));
        assertThat(ranking.getAll().get(3).getName(), is("d"));
        assertThat(ranking.getAll().get(3).getScore(), is(closeTo(2.22222, 0.0001)));
        assertThat(ranking.getAll().get(2).getName(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(2).getScore(), is(closeTo(5.0, 0.0001)));
        assertThat(ranking.getAll().get(1).getName(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(1).getScore(), is(closeTo(5.0, 0.0001)));
        assertThat(ranking.getAll().get(0).getName(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(0).getScore(), is(closeTo(5.0, 0.0001)));
    }

    @Test
    public void testChiSquaredRoundRobinMerge() throws Exception {
        FeatureRanker featureSelector = new ChiSquaredFeatureRanker(new RoundRobinMergingStrategy());

        FeatureRanking ranking = featureSelector.rankFeatures(fixture);
        // System.out.println(ranking);

        assertThat(ranking.getAll().get(5).getName(), is("e"));
        assertThat(ranking.getAll().get(5).getScore(), is(closeTo(1.0, 0.0001)));
        assertThat(ranking.getAll().get(4).getName(), is("b"));
        assertThat(ranking.getAll().get(4).getScore(), is(closeTo(2.0, 0.0001)));
        assertThat(ranking.getAll().get(3).getName(), is("d"));
        assertThat(ranking.getAll().get(3).getScore(), is(closeTo(3.0, 0.0001)));
        assertThat(ranking.getAll().get(2).getName(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(2).getScore(), is(closeTo(4.0, 0.0001)));
        assertThat(ranking.getAll().get(1).getName(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(1).getScore(), is(closeTo(5.0, 0.0001)));
        assertThat(ranking.getAll().get(0).getName(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(0).getScore(), is(closeTo(6.0, 0.0001)));
    }

    @Test
    public void testInformationGain() throws Exception {
        FeatureRanker featureSelector = new InformationGainFeatureRanker();

        FeatureRanking ranking = featureSelector.rankFeatures(fixture);
        // System.out.println(ranking);

        assertThat(ranking.getAll().get(5).getName(), is("e"));
        assertThat(ranking.getAll().get(5).getScore(), is(notNullValue()));
        assertThat(ranking.getAll().get(4).getName(), isOneOf("b", "d"));
        assertThat(ranking.getAll().get(4).getScore(), is(notNullValue()));
        assertThat(ranking.getAll().get(3).getName(), isOneOf("b", "d"));
        assertThat(ranking.getAll().get(3).getScore(), is(notNullValue()));
        assertThat(ranking.getAll().get(2).getName(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(2).getScore(), is(notNullValue()));
        assertThat(ranking.getAll().get(1).getName(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(1).getScore(), is(notNullValue()));
        assertThat(ranking.getAll().get(0).getName(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(0).getScore(), is(notNullValue()));
    }

    @Test
    public void testNumericFeatureWithInformationGain() throws Exception {
        List<Instance> dataset = CollectionHelper.newArrayList();
        dataset.add(new InstanceBuilder().set("numeric", 1.0d).create("a"));
        dataset.add(new InstanceBuilder().set("numeric", 2.0d).create("b"));
        dataset.add(new InstanceBuilder().set("numeric", 3.0d).create("a"));
        FeatureRanker featureSelector = new InformationGainFeatureRanker();

        FeatureRanking ranking = featureSelector.rankFeatures(dataset);
        // System.out.println(ranking);

        assertThat(ranking.getAll().get(0).getName(), is("numeric"));
        assertThat(ranking.getAll().get(0).getScore(), is(notNullValue()));
    }
}
