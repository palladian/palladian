package ws.palladian.classification.xgboost;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ws.palladian.classification.featureselection.FeatureRanking;
import ws.palladian.classification.featureselection.RankedFeature;
import ws.palladian.classification.featureselection.RankingSource;
import ws.palladian.core.Model;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

public class XGBoostModel implements Model, RankingSource {

	private static final long serialVersionUID = 1L;

	private final Booster booster;

	private final List<String> labelIndices;

	private final Map<String, Integer> featureIndices;

	/** Temporary file which stores the feature map. Created when necessary. */
	private File featureMapFile;

	XGBoostModel(Booster booster, List<String> labelIndices, Map<String, Integer> featureIndices) {
		this.booster = booster;
		this.labelIndices = new ArrayList<>(labelIndices);
		this.featureIndices = new HashMap<>(featureIndices);
	}

	Booster getBooster() {
		return booster;
	}

	Map<String, Integer> getFeatureIndices() {
		return Collections.unmodifiableMap(this.featureIndices);
	}

	String getLabel(int index) {
		return labelIndices.get(index);
	}

	@Override
	public Set<String> getCategories() {
		return new HashSet<>(labelIndices);
	}

	/**
	 * Create a "feature map", which can be used to export human-readable model
	 * dumps which contain the actual feature names instead of running indices.
	 * 
	 * @return The feature map as string.
	 */
	private String buildFeatureMap() {
		// https://github.com/dmlc/xgboost/blob/master/demo/data/featmap.txt
		// https://github.com/dmlc/xgboost/tree/master/demo/binary_classification
		Map<String, Integer> sortedByIndex = CollectionHelper.sortByValue(featureIndices);
		StringBuilder featureMap = new StringBuilder();
		for (Entry<String, Integer> featureIndex : sortedByIndex.entrySet()) {
			featureMap.append(featureIndex.getValue()).append('\t');
			String name = featureIndex.getKey();
			// TODO spaces are not allowed, that's why we replace them;
			// however, the names should be replaced back later, in
			// #getFeatureScore()
			String fixedName = name.replaceAll("\\s", "_");
			featureMap.append(fixedName).append("\tq\n");
		}
		return featureMap.toString();
	}

	private synchronized void conditionallyWriteFeatureMap() {
		if (featureMapFile == null) {
			featureMapFile = FileHelper.getTempFile();
			try {
				Files.write(featureMapFile.toPath(), buildFeatureMap().getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ws.palladian.kaggle.redhat.classifier.xgboost.RankingSource#getFeatureRanking
	 * ()
	 */
	@Override
	public FeatureRanking getFeatureRanking() {
		return new FeatureRanking(getFeatureScore());
	}

	/** @deprecated Use {@link #getFeatureRanking()} instead. */
	@Deprecated
	public Map<String, Integer> getFeatureScore() {
		conditionallyWriteFeatureMap();
		try {
			return booster.getFeatureScore(featureMapFile.getAbsolutePath());
		} catch (XGBoostError e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String toString() {
		conditionallyWriteFeatureMap();
		try {
			return Arrays.toString(booster.getModelDump(featureMapFile.getAbsolutePath(), true));
		} catch (XGBoostError e) {
			throw new IllegalStateException(e);
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new IllegalArgumentException("First argument must be path to the model");
		}
		File modelFile = new File(args[0]);
		if (!modelFile.isFile()) {
			throw new IllegalArgumentException(modelFile + " is not a file");
		}
		XGBoostModel model = FileHelper.deserialize(args[0]);
		List<RankedFeature> featureRanking = model.getFeatureRanking().getAll();
		CollectionHelper.print(featureRanking);
	}

}
