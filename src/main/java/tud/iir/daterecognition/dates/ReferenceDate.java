package tud.iir.daterecognition.dates;

public class ReferenceDate extends ExtractedDate {

    public static final int RATE = 101;
    private int rate = -1;

    public ReferenceDate() {
        // TODO Auto-generated constructor stub
    }

    public ReferenceDate(String dateString) {
        super(dateString);
        // TODO Auto-generated constructor stub
    }

    public ReferenceDate(String dateString, String format) {
        super(dateString, format);
        // TODO Auto-generated constructor stub
    }

    @Override
    public int get(int field) {
        int value;
        switch (field) {
            case RATE:
                value = rate;
                break;
            default:
                value = super.get(field);
        }
        return value;
    }

    @Override
    public void set(int field, int value) {
        switch (field) {
            case RATE:
                rate = value;
                break;
            default:
                super.set(field, value);
        }
    }

    @Override
    public int getType() {
        return TECH_REFERENCE;
    }

}
