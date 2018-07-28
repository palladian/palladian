package ws.palladian.kaggle.restaurants.rules.apriori;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.helper.collection.CollectionHelper.newHashSet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class AprioriTest {
	private static final double DELTA = 0.01;

	@Test
	public void testKMinusOneSubsets() {
		Set<Integer> s = newHashSet(1, 2, 3, 4);
		Set<Set<Integer>> subsets = Apriori.kMinusOneSubsets(s);
		assertEquals(4, subsets.size());
		assertTrue(subsets.contains(newHashSet(2, 3, 4)));
		assertTrue(subsets.contains(newHashSet(1, 3, 4)));
		assertTrue(subsets.contains(newHashSet(1, 2, 4)));
		assertTrue(subsets.contains(newHashSet(2, 3, 4)));
	}

	@Test
	public void testAprioriGen() {
		Set<Integer> s1 = newHashSet(1, 2, 3);
		Set<Integer> s2 = newHashSet(1, 2, 4);
		Set<Integer> s3 = newHashSet(1, 2, 5);
		Set<Integer> s4 = newHashSet(1, 3, 4);
		Set<Integer> s5 = newHashSet(1, 3, 5);
		Set<Integer> s6 = newHashSet(2, 3, 4);
		Set<Set<Integer>> l_3 = newHashSet(s1, s2, s3, s4, s5, s6);
		Set<Set<Integer>> c_4 = Apriori.aprioriGen(l_3);
		assertEquals(1, c_4.size());
		assertTrue(c_4.contains(newHashSet(1, 2, 3, 4)));
	}

	@Test
	public void testExtractItemsets() {
		// second example from
		// http://www2.cs.uregina.ca/~dbd/cs831/notes/itemsets/itemset_apriori.html
		Set<String> s1 = newHashSet("a", "b", "c");
		Set<String> s2 = newHashSet("a", "b", "c", "d", "e");
		Set<String> s3 = newHashSet("a", "c", "d");
		Set<String> s4 = newHashSet("a", "c", "d", "e");
		Set<String> s5 = newHashSet("a", "b", "c", "d");
		Set<Set<String>> data = newHashSet(s1, s2, s3, s4, s5);
		Map<Set<String>, Double> result = Apriori.extractItemsets(data, 0.4);
		assertEquals(23, result.size());
		assertEquals(1., result.get(newHashSet("a")), DELTA);
		assertEquals(.6, result.get(newHashSet("b")), DELTA);
		assertEquals(1., result.get(newHashSet("c")), DELTA);
		assertEquals(.8, result.get(newHashSet("d")), DELTA);
		assertEquals(.4, result.get(newHashSet("e")), DELTA);
		assertEquals(.6, result.get(newHashSet("a", "b")), DELTA);
		assertEquals(1., result.get(newHashSet("a", "c")), DELTA);
		assertEquals(.8, result.get(newHashSet("a", "d")), DELTA);
		assertEquals(.4, result.get(newHashSet("a", "e")), DELTA);
		assertEquals(.6, result.get(newHashSet("b", "c")), DELTA);
		assertEquals(.4, result.get(newHashSet("b", "d")), DELTA);
		assertEquals(.8, result.get(newHashSet("c", "d")), DELTA);
		assertEquals(.4, result.get(newHashSet("c", "e")), DELTA);
		assertEquals(.4, result.get(newHashSet("d", "e")), DELTA);
		assertEquals(.6, result.get(newHashSet("a", "b", "c")), DELTA);
		assertEquals(.4, result.get(newHashSet("a", "b", "d")), DELTA);
		assertEquals(.8, result.get(newHashSet("a", "c", "d")), DELTA);
		assertEquals(.4, result.get(newHashSet("a", "c", "e")), DELTA);
		assertEquals(.4, result.get(newHashSet("a", "d", "e")), DELTA);
		assertEquals(.4, result.get(newHashSet("b", "c", "d")), DELTA);
		assertEquals(.4, result.get(newHashSet("c", "d", "e")), DELTA);
		assertEquals(.4, result.get(newHashSet("a", "b", "c", "d")), DELTA);
		assertEquals(.4, result.get(newHashSet("a", "c", "d", "e")), DELTA);
	}

	@Test
	public void testGenerateAssociationRules() {
		// example data taken from
		// http://cis.poly.edu/~mleung/FRE7851/f07/AssociationRules3.pdf
		Set<Integer> s1 = newHashSet(1, 2, 5);
		Set<Integer> s2 = newHashSet(2, 4);
		Set<Integer> s3 = newHashSet(2, 3, 6);
		Set<Integer> s4 = newHashSet(1, 2, 4);
		Set<Integer> s5 = newHashSet(1, 3);
		Set<Integer> s6 = newHashSet(2, 3);
		Set<Integer> s7 = newHashSet(1, 3);
		Set<Integer> s8 = newHashSet(1, 2, 3, 5);
		Set<Integer> s9 = newHashSet(1, 2, 3);
		List<Set<Integer>> data = Arrays.asList(s1, s2, s3, s4, s5, s6, s7, s8, s9);
		Ruleset<Integer> rules = Apriori.buildRules(data, 0.1, 0.1);
		assertEquals(.5, rules.getRule(newHashSet(1, 2), newHashSet(5)).getConfidence(), DELTA);
		assertEquals(1., rules.getRule(newHashSet(1, 5), newHashSet(2)).getConfidence(), DELTA);
		assertEquals(1., rules.getRule(newHashSet(2, 5), newHashSet(1)).getConfidence(), DELTA);
		assertEquals(.33, rules.getRule(newHashSet(1), newHashSet(2, 5)).getConfidence(), DELTA);
		assertEquals(.29, rules.getRule(newHashSet(2), newHashSet(1, 5)).getConfidence(), DELTA);
		assertEquals(1., rules.getRule(newHashSet(5), newHashSet(1, 2)).getConfidence(), DELTA);
	}

}
