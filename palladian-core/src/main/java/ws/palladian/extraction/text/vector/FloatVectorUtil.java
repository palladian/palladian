package ws.palladian.extraction.text.vector;

/**
 * Utilities for float[]-based vectors. The methods which also return a vector
 * modify the first supplied array in-place!
 * 
 * @author pk
 */
public class FloatVectorUtil {

	public static float[] add(float[] vector1, float[] vector2) {
		for (int i = 0; i < vector1.length; i++) {
			vector1[i] += vector2[i];
		}
		return vector1;
	}

	public static float[] normalize(float[] vector) {
		float magnitude = magnitude(vector);
		for (int i = 0; i < vector.length; i++) {
			vector[i] /= magnitude;
		}
		return vector;
	}

	public static float cosine(float[] vector1, float[] vector2) {
		float dot = dot(vector1, vector2);
		float magnitude1 = magnitude(vector1);
		float magnitude2 = magnitude(vector2);
		return dot / (magnitude1 * magnitude2);
	}

	public static float dot(float[] vector1, float[] vector2) {
		float dot = 0;
		for (int i = 0; i < vector1.length; i++) {
			dot += vector1[i] * vector2[i];
		}
		return dot;
	}

	public static float magnitude(float[] vector) {
		float magnitude = 0;
		for (float value : vector) {
			magnitude += value * value;
		}
		return (float) Math.sqrt(magnitude);
	}

	public static float generalizedJaccard(float[] vector1, float[] vector2) {
		// https://en.wikipedia.org/wiki/Jaccard_index#Generalized_Jaccard_similarity_and_distance
		float min = 0;
		float max = 0;
		for (int i = 0; i < vector1.length; i++) {
			min += Math.min(vector1[i], vector2[i]);
			max += Math.max(vector1[i], vector2[i]);
		}
		return min / max;
	}

	public static float[] scalar(float[] vector, float scalar) {
		float[] result = new float[vector.length];
		for (int i = 0; i < vector.length; i++) {
			result[i] = vector[i]* scalar;
		}
		return result;
	}

	private FloatVectorUtil() {
		// no instances
	}

}
