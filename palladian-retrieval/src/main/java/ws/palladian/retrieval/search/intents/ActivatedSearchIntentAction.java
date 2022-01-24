package ws.palladian.retrieval.search.intents;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.beanutils.ConvertUtilsBean;

public class ActivatedSearchIntentAction extends SearchIntentAction<ActivatedSearchIntentFilter> {
    /** Actions might modify the query, e.g. remove parts so we need to get the updated query. */
    private String modifiedQuery = null;

    /** who triggered me? */
    private SearchIntentTrigger intentTrigger;

    public ActivatedSearchIntentAction(SearchIntentAction<SearchIntentFilter> ia, String modifiedQuery) {
        try {
            ConvertUtilsBean convertUtilsBean = BeanUtilsBean2.getInstance().getConvertUtils();
            convertUtilsBean.register(false, true, -1);
            BeanUtils.copyProperties(this, ia);
            List<ActivatedSearchIntentFilter> activatedFilters = new ArrayList<>();
            for (SearchIntentFilter filter : ia.getFilters()) {
                activatedFilters.add(new ActivatedSearchIntentFilter(filter));
            }
            setFilters(activatedFilters);
            setModifiedQuery(modifiedQuery);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public String getModifiedQuery() {
        return modifiedQuery;
    }

    public void setModifiedQuery(String modifiedQuery) {
        this.modifiedQuery = modifiedQuery.trim();
    }

    @Override
    public String toString() {
        return "ActivatedSearchIntentAction{" +
                "modifiedQuery='" + modifiedQuery + '\'' +
                '}';
    }

    public void setIntentTrigger(SearchIntentTrigger intentTrigger) {
        this.intentTrigger=intentTrigger;
    }
    public SearchIntentTrigger getIntentTrigger() {
        return intentTrigger;
    }
}
