package ws.palladian.kaggle.restaurants.features.descriptors;

import java.awt.Graphics;

import ws.palladian.kaggle.restaurants.features.descriptors.DescriptorExtractor.Descriptor;

public final class ImmutableDescriptor implements Descriptor {
	private final double[] vector;
	private final int x;
	private final int y;
	private final int radius;
	private final int orientation;

	public ImmutableDescriptor(double[] vector, int x, int y, int radius, int orientation) {
		this.vector = vector;
		this.x = x;
		this.y = y;
		this.radius = radius;
		this.orientation = orientation;
	}

	@Override
	public double[] vector() {
		return vector;
	}

	@Override
	public int x() {
		return x;
	}

	@Override
	public int y() {
		return y;
	}

	@Override
	public int radius() {
		return radius;
	}

	@Override
	public int orientation() {
		return orientation;
	}

	@Override
	public void draw(Graphics g) {
		g.drawOval(x, y, radius, radius);
	}

}
