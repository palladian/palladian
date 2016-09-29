package ws.palladian.classification.utils;

/**
 * Provides access to available {@link Normalizer} implementations.
 * 
 * @author pk
 */
public final class Normalizers {
	private Normalizers() {
		// no instances
	}

	public static Normalizer none() {
		return new NoNormalizer();
	}

	public static Normalizer minMax() {
		return new MinMaxNormalizer();
	}

	public static Normalizer zScore() {
		return new ZScoreNormalizer();
	}
}
