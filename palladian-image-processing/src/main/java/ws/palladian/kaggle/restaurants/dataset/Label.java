package ws.palladian.kaggle.restaurants.dataset;

/**
 * Label names.
 */
public enum Label {
    GOOD_FOR_LUNCH(0),
    GOOD_FOR_DINNER(1),
    TAKES_RESERVATIONS(2),
    OUTDOOR_SEATING(3),
    RESTAURANT_IS_EXPENSIVE(4),
    HAS_ALCOHOL(5),
    HAS_TABLE_SERVICE(6),
    AMBIENCE_IS_CLASSY(7),
    GOOD_FOR_KIDS(8);

    private final int labelId;

    Label(int labelId) {
        this.labelId = labelId;
    }

    public int getLabelId() {
        return labelId;
    }

    public static Label getById(int labelId) {
        for (Label l : Label.values()) {
            if (l.getLabelId() == labelId) {
                return l;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
