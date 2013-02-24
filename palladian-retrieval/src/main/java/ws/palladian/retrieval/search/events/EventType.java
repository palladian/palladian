package ws.palladian.retrieval.search.events;

import ws.palladian.helper.nlp.StringHelper;

public enum EventType {

    THEATRE("theatre show", "theater show", "theatre shows", "theater shows"), COMEDY("comedy"), EXHIBITION(
            "exhibition", "exhibitions"), CONCERT("concert", "concerts", "live music", "tour dates"), FESTIVAL(
            "festival", "festivals"), CONFERENCE("conferences", "conference"), EVENT("event", "events");

    private String[] eventTypeNames;

    EventType(String... eventTypeNames) {
        this.eventTypeNames = eventTypeNames;
    }

    public static EventType find(String string) {

        string = string.toLowerCase().trim();

        for (EventType eventType : values()) {
            for (String eventTypeName : eventType.getEventTypeNames()) {
                if (StringHelper.containsWord(eventTypeName, string)) {
                    return eventType;
                }
            }
        }

        return EVENT;

    }

    public String[] getEventTypeNames() {
        return eventTypeNames;
    }

}
