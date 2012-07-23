package ws.palladian.extraction.date.weka;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;

import weka.core.Instance;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.helper.DateWekaInstanceFactory;
import ws.palladian.helper.date.dates.ContentDate;

public class DateWekaInstanceFactoryTest {

    @Test
    public void testGetDateInstanceByArff() {
        DateWekaInstanceFactory dwif = new DateWekaInstanceFactory(PageDateType.publish);
        Instance instance = dwif.getDateInstanceByArffTemplate(getTestDate());
        assertThat(isNull(instance), is(false));
    }

    @Test
    public void testDatefeaturesToString() {
        ContentDate date = getTestDate();
        String dateString = null;
        DateWekaInstanceFactory dwif = new DateWekaInstanceFactory(PageDateType.publish);
        Class<? extends DateWekaInstanceFactory> dwifClass = dwif.getClass();
        Class<?>[] param = new Class[1];
        param[0] = ContentDate.class;
        Method method;
        try {
            method = dwifClass.getDeclaredMethod("datefeaturesToString", param);
            method.setAccessible(true);
            dateString = (String) method.invoke(dwif, date);

        } catch (SecurityException e1) {
            e1.printStackTrace();
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        assertThat(
                dateString,
                is("0.0,0.0,0.0,0.375,0.75,0.5,1,0,0.8,0.0,0.0,P,0.0,0.0,0.0,0.25,0.01,4840,6042,-1,14113,'MMMM DD, YYYY',create,3,0.0,1.0,1.0,0.0,0.0"));
    }

    private boolean isNull(Object obj) {
        return obj == null;
    }

    private ContentDate getTestDate() {
        ContentDate cDate = new ContentDate("April 01, 2011", "MMMM DD, YYYY");

        cDate.setRelDocPos(0.375);
        cDate.setOrdDocPos(0.75);
        cDate.setOrdAgePos(0.5);

        cDate.setKeyClass("1");
        cDate.setKeyLoc("0");
        cDate.setKeyDiff(0.8);

        cDate.setSimpleTag("0");
        cDate.sethTag("0");
        cDate.setTag("P");

        cDate.setHasStrucutreDate(false);
        cDate.setInMetaDates(false);
        cDate.setInUrl(false);

        cDate.setRelCntSame(0.25);
        cDate.setRelSize(0.01);

        // out of bound in distposbefore
        cDate.setDistPosBefore(4840);
        cDate.setDistPosAfter(6042);
        cDate.setDistAgeAfter(14113);

        cDate.setKeyLoc201("0");
        cDate.setKeyLoc202("1");

        cDate.setIsKeyClass1("1");
        cDate.setIsKeyClass2("0");
        cDate.setIsKeyClass3("0");
        cDate.setKeyword("create");

        return cDate;
    }

}
