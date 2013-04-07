package ws.palladian.retrieval.search.events;

import java.util.Date;
import java.util.List;

import ws.palladian.retrieval.search.SearcherException;

public abstract class EventSearcher {

    public abstract List<Event> search(String keywords, String location, Integer radius, Date startDate, Date endDate,
            EventType eventType) throws SearcherException;

    public abstract String getName();

}
