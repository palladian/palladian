package ws.palladian.features.color;

import java.awt.*;

public enum Luminosity implements ColorExtractor {
    LUMINOSITY;

    public int extractValue(Color color) {
        return (int) (0.21 * color.getRed() + 0.72 * color.getGreen() + 0.07 * color.getBlue());
    }

    public String toString() {
        return "luminosity";
    }
}