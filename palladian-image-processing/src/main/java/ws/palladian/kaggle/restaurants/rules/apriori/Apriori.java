package ws.palladian.kaggle.restaurants.rules.apriori;

import static java.util.Collections.singleton;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Apriori algorithm for mining of frequent itemsets. For an explanation of the
 * algorithms, refer to
 * "<a href="http://rakesh.agrawal-family.com/papers/vldb94apriori_rj.pdf
 * ">Fast Algorithms for Mining Association Rules</a>"; Rakesh Agrawal,
 * Ramakrishnan Srikant; 1994.
 * 
 * @author pk
 */
public final class Apriori {
	
	public static final class Rule<T> implements Comparable<Rule<T>> {
		private final Set<T> X;
		private final Set<T> Y;
		private final double support;
		private final double confidence;
		private final double lift;
		Rule(Set<T> x, Set<T> y, double support, double confidence, double lift) {
			X = x;
			Y = y;
			this.support = support;
			this.confidence = confidence;
			this.lift = lift;
		}
		/**
		 * @return The if-part of the condition (aka. antecedent).
		 */
		public Set<T> getIf() {
			return Collections.unmodifiableSet(X);
		}
		/**
		 * @return The then-part of the condition (aka. consequent).
		 */
		public Set<T> getThen() {
			return Collections.unmodifiableSet(Y);
		}

		/**
		 * @return The rule's support; i.e. the fraction [0,1] of items in the
		 *         dataset which contain all items of the rule's if- and
		 *         then-part.
		 */
		public double getSupport() {
			return support;
		}

		/**
		 * @return The rule's confidence; i.e. the ratio of the number of items
		 *         in the dataset that contain all items of the rule's if- and
		 *         then-part to the number of items which contain the if-part.
		 */
		public double getConfidence() {
			return confidence;
		}
		/**
		 * @return The lift ratio of this rule. Compares confidence of the rule
		 *         to the assumption, that the occurrence of the antecedent is
		 *         independent of the the occurrence of the consequent. A value
		 *         greater 1 means, that this rule is "useful" in general.
		 */
		public double getLift() {
			return lift;
		}
		@Override
		public String toString() {
			NumberFormat format = new DecimalFormat("#.####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			return X + " => " + Y + " (support = " + format.format(support) + ", confidence = " + format.format(confidence) + ", lift = " + format.format(lift) + ")";
		}
		@Override
		public int compareTo(Rule<T> o) {
			int result = Integer.compare(getIf().size(), o.getIf().size());
			if (result == 0) {
				result = Integer.compare(getThen().size(), o.getThen().size());
			}
			return result;
		}
	}

	private Apriori() {
		// instantiate me not
	}

	/**
	 * Extract frequent itemsets.
	 * 
	 * @param data
	 *            The input data.
	 * @param minSupport
	 *            The minimum support threshold [0,1].
	 */
	public static <T> Map<Set<T>, Double> extractItemsets(Collection<Set<T>> data, double minSupport) {
		Map<Set<T>, Double> result = new HashMap<>();
		Set<Set<T>> L_k_minus_1 = new HashSet<>();
		Collection<Set<T>> C_k_minus_1 = getSingleCandidates(data);
		for (Set<T> c : C_k_minus_1) {
			double support = support(c, data);
			if (support >= minSupport) {
				L_k_minus_1.add(c);
				result.put(c, support);
			}
		}
		for (;;) {
			Set<Set<T>> C_k = aprioriGen(L_k_minus_1);
			Set<Set<T>> L_k = new HashSet<>();
			for (Set<T> c : C_k) {
				double support = support(c, data);
				if (support >= minSupport) {
					L_k.add(c);
					result.put(c, support);
				}
			}
			if (L_k.isEmpty()) {
				break;
			}
			L_k_minus_1 = L_k;
		}
		return result;
	}
	
	/**
	 * Build association rules.
	 * 
	 * @param data
	 *            The input data.
	 * @param minSupport
	 *            The minimum support threshold [0,1].
	 * @param minConfidence
	 *            The minimum confidence threshold [0,1].
	 * @return The extracted rules.
	 */
	public static <T> Ruleset<T> buildRules(Collection<Set<T>> data, double minSupport, double minConfidence) {
		List<Rule<T>> rules = new ArrayList<>();
		Map<Set<T>, Double> itemsets = extractItemsets(data, minSupport);
		for (Entry<Set<T>, Double> itemset : itemsets.entrySet()) {
			Set<T> l_k = itemset.getKey();
			if (l_k.size() < 2) {
				continue;
			}
			double support = itemset.getValue();
			Set<Set<T>> H_m = getSingleItemSets(l_k);
			// rules with one-item consequents (see sec. 3 in paper)
			for (Set<T> Y : H_m) {
				Set<T> X = new HashSet<>(l_k);
				X.removeAll(Y);
				double confidence = confidence(X, Y, data);
				if (confidence >= minConfidence) {
					double lift = confidence / support(Y, data);
					rules.add(new Rule<>(X, Y, support, confidence, lift));
				}
			}
			// ap-genrules (see sec. 3.1 in paper)
			for (int m = 1; m < l_k.size() - 1; m++) {
				Set<Set<T>> H_m_plus_1 = aprioriGen(H_m);
				Iterator<Set<T>> iterator = H_m_plus_1.iterator();
				while (iterator.hasNext()) {
					Set<T> Y = iterator.next();
					Set<T> X = new HashSet<>(l_k);
					X.removeAll(Y);
					double confidence = confidence(X, Y, data);
					if (confidence >= minConfidence) {
						double lift = confidence / support(Y, data);
						rules.add(new Rule<>(X, Y, support, confidence, lift));
					} else {
						iterator.remove();
					}
				}
				H_m = H_m_plus_1;
			}
		}
		Collections.sort(rules);
		return new Ruleset<>(rules);
	}

	private static <T> double support(Set<T> candidateItemset, Collection<Set<T>> data) {
		int support = 0;
		for (Set<T> item : data) {
			if (item.containsAll(candidateItemset)) {
				support++;
			}
		}
		return (double) support / data.size();
	}
	
	private static <T> double confidence(Set<T> X, Set<T> Y, Collection<Set<T>> data) {
		Set<T> X_union_Y = new HashSet<T>(X);
		X_union_Y.addAll(Y);
		return support(X_union_Y, data) / support(X, data);
	}

	private static <T> Collection<Set<T>> getSingleCandidates(Collection<Set<T>> data) {
		Set<Set<T>> candidates = new HashSet<>();
		for (Set<T> set : data) {
			candidates.addAll(getSingleItemSets(set));
		}
		return candidates;
	}
	
	private static <T> Set<Set<T>> getSingleItemSets(Set<T> set) {
		Set<Set<T>> singleItemSets = new HashSet<>();
		for (T item :  set) {
			singleItemSets.add(singleton(item));
		}
		return singleItemSets;
	}

	/**
	 * Takes a set of k-1 itemsets (L_k-1) and returns a set of k itemsets as
	 * candidates.
	 * 
	 * @param itemsets
	 *            The itemsets with length of k-1.
	 * @return The candidate itemsets with length of k.
	 */
	static <T> Set<Set<T>> aprioriGen(Set<Set<T>> itemsets) {
		Set<Set<T>> candidates = new HashSet<>();
		for (Set<T> s1 : itemsets) {
			for (Set<T> s2 : itemsets) {
				Set<T> union = new HashSet<>(s1);
				union.addAll(s2);
				if (union.size() == s1.size() + 1) {
					candidates.add(union);
				}
			}
		}
		// prune; remove those candidates, where not all k-1 subsets are
		// contained in the provided itemsets
		Iterator<Set<T>> iterator = candidates.iterator();
		while (iterator.hasNext()) {
			Set<T> candidate = iterator.next();
			for (Set<T> subset : kMinusOneSubsets(candidate)) {
				if (!itemsets.contains(subset)) {
					iterator.remove();
					break;
				}
			}
		}
		return candidates;
	}

	/**
	 * Produces k-1 subsets of the given itemset.
	 * 
	 * @param c
	 *            An itemset of length k.
	 * @return A set of all subsets with length k-1.
	 */
	static <T> Set<Set<T>> kMinusOneSubsets(Set<T> c) {
		Set<Set<T>> kMinusOneSubsets = new HashSet<>();
		for (T i : c) {
			Set<T> subset = new HashSet<T>(c);
			subset.remove(i);
			kMinusOneSubsets.add(subset);
		}
		return kMinusOneSubsets;
	}

}
