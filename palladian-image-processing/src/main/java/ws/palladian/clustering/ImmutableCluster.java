package ws.palladian.clustering;

import java.util.Arrays;

public final class ImmutableCluster implements Cluster {

	private final double[] center;
	private int size;

	public ImmutableCluster(double[] center) {
		this(center, SIZE_UNKNOWN);
	}

	public ImmutableCluster(double[] center, int size) {
		this.center = center;
		this.size = size;
	}

	@Override
	public double[] center() {
		return center;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(getClass().getSimpleName());
		stringBuilder.append(" [center=").append(Arrays.toString(center));
		if (size != SIZE_UNKNOWN) {
			stringBuilder.append(", size=").append(size);
		}
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

}
