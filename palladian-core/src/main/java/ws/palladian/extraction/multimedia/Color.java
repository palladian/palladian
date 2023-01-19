package ws.palladian.extraction.multimedia;

import java.util.Objects;

/**
 * <p>Created by David Urbansky on 07.10.2015.</p>
 *
 * @author David Urbansky
 */
public class Color {
    private String hexCode;
    private String mainColorName;
    private String fineColorName;

    public Color(String hexCode, String fineColorName, String mainColorName) {
        this.hexCode = hexCode;
        if (!this.hexCode.startsWith("#")) {
            this.hexCode = "#" + this.hexCode;
        }
        this.fineColorName = fineColorName;
        this.mainColorName = mainColorName;
    }

    public String getHexCode() {
        return this.hexCode;
    }

    public String getSpecificColorName() {
        return this.fineColorName;
    }

    public String getMainColorName() {
        return this.mainColorName;
    }

    @Override
    public String toString() {
        return "Color{" + "hexCode='" + hexCode + '\'' + ", mainColorName='" + mainColorName + '\'' + ", fineColorName='" + fineColorName + '\'' + '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(fineColorName, hexCode, mainColorName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Color other = (Color) obj;
        if (!Objects.equals(fineColorName, other.fineColorName)) {
            return false;
        }
        if (!Objects.equals(hexCode, other.hexCode)) {
            return false;
        }
        if (!Objects.equals(mainColorName, other.mainColorName)) {
            return false;
        }
        return true;
    }
}
