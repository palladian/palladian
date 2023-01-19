package ws.palladian.classification.featureselection;

public interface RankingSource {

    /**
     * @return Ranked features based on their occurrence statistics within
     * contained trees.
     */
    FeatureRanking getFeatureRanking();

}