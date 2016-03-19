package ws.palladian.features.color;

import java.awt.*;

public enum HSB implements ColorExtractor {
    HUE, SATURATION, BRIGHTNESS;

    @Override
    public int extractValue(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return (int) (hsb[ordinal()] * 255);
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}