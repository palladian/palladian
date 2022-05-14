// package ws.palladian.classification.quickml;

// import java.text.DecimalFormat;
// import java.text.DecimalFormatSymbols;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.Locale;
// import java.util.Map;

// import quickml.supervised.tree.decisionTree.DecisionTree;
// import quickml.supervised.tree.decisionTree.nodes.DTCatBranch;
// import quickml.supervised.tree.decisionTree.nodes.DTLeaf;
// import quickml.supervised.tree.decisionTree.nodes.DTNumBranch;
// import ws.palladian.classification.featureselection.FeatureRanking;
// import ws.palladian.classification.featureselection.RankingSource;
// import ws.palladian.helper.collection.Bag;
// import ws.palladian.helper.math.FatStats;
// import ws.palladian.helper.math.Stats;

// public final class ModelStats implements TreeVisitor, RankingSource {
// 	private class AttributeDepthStats implements Comparable<AttributeDepthStats> {
// 		final String attribute;
// 		final Bag<Integer> depths = new Bag<>();
// 		public AttributeDepthStats(String attribute) {
// 			this.attribute = attribute;
// 		}
// 		public void addDepth(int depth) {
// 			depths.add(depth);
// 		}
// 		public double getDepthScore() {
// 			double score = 0;
// 			for (Integer depth : depths) {
// 				score += 1./FastMath.pow(2, depth);
// 			}
// 			return score;
// 		}
// 		public int getCount() {
// 			return depths.size();
// 		}
// 		@Override
// 		public int compareTo(AttributeDepthStats o) {
// 			return Double.compare(o.getDepthScore(), this.getDepthScore());
// 		}
// 	}
	
// 	int numTrees = 0;
// 	int numCategoricalBranches = 0;
// 	int numNumericalBranches = 0;
// 	int numLeaves = 0;
// 	Stats leafDepthStats = new FatStats();
// 	Stats instanceStats = new FatStats(); 
// 	Map<String, AttributeDepthStats> attributesDepthStats = new HashMap<>();
// 	@Override
// 	public void tree(DecisionTree tree) {
// 		numTrees++;
// 	}
// 	@Override
// 	public void categoricalBranch(DTCatBranch branch, boolean trueChild) {
// 		numCategoricalBranches++;
// 		addAttribute(branch.attribute, branch.getDepth());
// 	}
// 	@Override
// 	public void numericalBranch(DTNumBranch branch, boolean trueChild) {
// 		numNumericalBranches++;
// 		addAttribute(branch.attribute, branch.getDepth());
// 	}
// 	private void addAttribute(String attribute, int depth) {
// 		AttributeDepthStats depthStats = attributesDepthStats.get(attribute);
// 		if (depthStats == null) {
// 			depthStats = new AttributeDepthStats(attribute);
// 			attributesDepthStats.put(attribute, depthStats);
// 		}
// 		depthStats.addDepth(depth);
// 	}
// 	@Override
// 	public void leaf(DTLeaf leaf, boolean trueChild) {
// 		numLeaves++;
// 		instanceStats.add(leaf.exampleCount);
// 		leafDepthStats.add(leaf.getDepth());
// 	}
// 	@Override
// 	public String toString() {
// 		StringBuilder builder = new StringBuilder();
// 		builder.append("numTrees = ").append(numTrees).append('\n');
// 		builder.append("numCategoricalBranches = ").append(numCategoricalBranches).append('\n');
// 		builder.append("numNumericalBranches = ").append(numNumericalBranches).append('\n');
// 		builder.append("numLeaves = ").append(numLeaves).append('\n');
// 		builder.append("minDepth = ").append((int) leafDepthStats.getMin()).append('\n');
// 		builder.append("maxDepth = ").append((int) leafDepthStats.getMax()).append('\n');
// 		builder.append("meanDepth = ").append(format(leafDepthStats.getMean())).append('\n');
// 		builder.append("medianDepth = ").append((int) leafDepthStats.getMedian()).append('\n');
// 		builder.append("minNumExamples = ").append((int) instanceStats.getMin()).append('\n');
// 		builder.append("maxNumExamples = ").append((int) instanceStats.getMax()).append('\n');
// 		builder.append("meanNumExamples = ").append(format(instanceStats.getMean())).append('\n');
// 		builder.append("medianNumExamples = ").append((int) instanceStats.getMedian()).append('\n');
// 		builder.append("numExamples = ").append((int) instanceStats.getSum()).append('\n');
// 		builder.append('\n');
// 		builder.append("attributes:\n");
// 		ArrayList<AttributeDepthStats> depthStats = new ArrayList<>(attributesDepthStats.values());
// 		Collections.sort(depthStats);
// 		for (AttributeDepthStats attribute : depthStats) {
// 			builder.append(attribute.attribute + "\tcount=" + attribute.getCount() + ", score="
// 					+ format(attribute.getDepthScore())).append('\n');
// 		}
// 		return builder.toString();
// 	}
// 	private static final String format(double v) {
// 		return new DecimalFormat("#.####", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(v);
// 	}
// 	@Override
// 	public FeatureRanking getFeatureRanking() {
// 		Map<String, Double> ranking = new HashMap<>();
// 		for (AttributeDepthStats attribute : attributesDepthStats.values()) {
// 			ranking.put(attribute.attribute, attribute.getDepthScore());
// 		}
// 		return new FeatureRanking(ranking);
// 	}

// }