package ws.palladian.extraction.multimedia;

/**
 * <p>Created by David Urbansky on 07.10.2015.</p>
 *
 * @author David Urbansky
 */
public class Color {

    String hexCode;
    String mainColorName;
    String fineColorName;

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
        return "Color{" +
                "hexCode='" + hexCode + '\'' +
                ", mainColorName='" + mainColorName + '\'' +
                ", fineColorName='" + fineColorName + '\'' +
                '}';
    }
}
