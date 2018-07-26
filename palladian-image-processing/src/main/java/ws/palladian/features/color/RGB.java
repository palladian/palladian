package ws.palladian.features.color;

import java.awt.*;

public enum RGB implements ColorExtractor {
    RED, GREEN, BLUE;

    public int extractValue(Color color) {
        switch (this) {
            case RED:
                return color.getRed();
            case GREEN:
                return color.getGreen();
            case BLUE:
                return color.getBlue();
        }
        throw new IllegalStateException();
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}