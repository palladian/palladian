package ws.palladian.features;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;

import java.awt.image.BufferedImage;

public enum BoundsFeatureExtractor implements FeatureExtractor {
	BOUNDS;

	@Override
	public FeatureVector extract(BufferedImage image) {
		InstanceBuilder instanceBuilder = new InstanceBuilder();
		instanceBuilder.set("width", image.getWidth());
		instanceBuilder.set("height", image.getHeight());
		instanceBuilder.set("ratio", (double) image.getHeight() / image.getWidth());
		return instanceBuilder.create();
	}

}
