package ws.palladian.retrieval.search.intents;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.beanutils.ConvertUtilsBean;
import ws.palladian.helper.collection.CollectionHelper;

import java.lang.reflect.InvocationTargetException;

public class ActivatedSearchIntentFilter extends SearchIntentFilter {
    private Double min;
    private Double max;

    public ActivatedSearchIntentFilter(SearchIntentFilter intentFilter) {
        try {
            ConvertUtilsBean convertUtilsBean = BeanUtilsBean2.getInstance().getConvertUtils();
            convertUtilsBean.register(false, true, -1);
            BeanUtils.copyProperties(this, intentFilter);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    @Override
    public String toString() {
        return "{" + "key='" + getKey() + '\'' + ", values=" + CollectionHelper.joinReadable(getValues()) + ", min='" + min + '\'' + ", max='" + max + '\'' + ", boolean="
                + getBooleanValue() + '}';
    }
}
