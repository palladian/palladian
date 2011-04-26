package ws.palladian.daterecognition.weka;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;

import static org.hamcrest.core.IsNot.*;
import org.junit.Test;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.helper.date.DateWekaInstanceFactory;

public class DateWekaInstanceFactoryTest {

	private String[] numericAttributes = { "relDocPos", "ordDocPos",
			"ordAgePos", "keyDiff", "relCntSame", "relSize", "distPosBefore",
			"distPosAfter", "distAgeBefore", };
	private String[] nominalAttributes = { "hour", "minute", "second",
			"keyClass", "keyLoc", "simpleTag", "hTag", "tagName",
			"hasStructureDate", "inMetaDates", "inUrl", "format", "keyword",
			"excatness", "keyLoc201", "keyLoc202", "isKeyClass1",
			"isKeyClass2", "isKeyClass3" };

	@Test
	public void testCreateAttribute() {
		DateWekaInstanceFactory dwif = new DateWekaInstanceFactory(
				PageDateType.publish);
		Class<? extends DateWekaInstanceFactory> dwifClass = dwif.getClass();
		Class[] param = new Class[1];
		param[0] = String.class;
		Attribute attribute = null;
		Method method;
		try {
			method = dwifClass.getDeclaredMethod("getVector", param);
			method.setAccessible(true);
			FastVector vectorResult;
			vectorResult = (FastVector) method.invoke(dwif, "0;1");
			attribute = new Attribute("hour", vectorResult);
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

		assertThat(attribute.name(), is("hour"));

	}

	@Test
	public void testDatefeaturesToString() {
		ContentDate date = getTestDate();
		String dateString = null;
		DateWekaInstanceFactory dwif = new DateWekaInstanceFactory(
				PageDateType.publish);
		Class<? extends DateWekaInstanceFactory> dwifClass = dwif.getClass();
		Class[] param = new Class[1];
		param[0] = ContentDate.class;
		Attribute attribute = null;
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
				is("0,0,0,0.375,0.75,0.5,1,0,0.8,0,0,P,0,0,0,0.25,0.01,4840,6042,14113,'MMMM DD, YYYY',create,3,0,1,1,0,0"));
	}

	@Test
	public void testGetDateInstanceByArffTemplate(){
		DateWekaInstanceFactory dwif = new DateWekaInstanceFactory(PageDateType.publish);
		Instance instance = dwif.getDateInstanceByArffTemplate(getTestDate()).firstInstance();
		assertThat(isNull(instance), is(false));
	}
	
	private boolean isNull(Object obj){
		return obj == null;
	}
	
	@Test
	public void testGetVector() {
		String[] vector01 = { "0", "1" };
		testGetVector(vector01);
		String[] vector0231 = { "0", "2", "3", "1" };
		testGetVector(vector0231);
		String[] vector021 = { "0", "2", "1" };
		testGetVector(vector021);
		String[] vector3564 = { "3", "5", "6", "4" };
		testGetVector(vector3564);
	}

	private void testGetVector(String[] getVectorParameter) {
		DateWekaInstanceFactory dwif = new DateWekaInstanceFactory(
				PageDateType.publish);
		Class<? extends DateWekaInstanceFactory> dwifClass = dwif.getClass();
		Class[] param = new Class[1];
		param[0] = String.class;
		Method method;
		FastVector vectorResult = new FastVector();
		try {
			method = dwifClass.getDeclaredMethod("getVector", param);
			method.setAccessible(true);
			String getVectorParameterString = "";
			for (int i = 0; i < getVectorParameter.length; i++) {
				if (i == 0) {
					getVectorParameterString = getVectorParameter[i];
				} else {
					getVectorParameterString += ";" + getVectorParameter[i];
				}
			}
			vectorResult = (FastVector) method.invoke(dwif,
					getVectorParameterString);
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
		for (int i = 0; i < vectorResult.capacity(); i++) {
			String value = (String) vectorResult.elementAt(i);
			assertThat(value, is(getVectorParameter[i]));
		}
	}

	@Test
	public void testAttributes() {
		DateWekaInstanceFactory dwif = new DateWekaInstanceFactory(
				PageDateType.publish);
		Class<? extends DateWekaInstanceFactory> dwifClass = dwif.getClass();

		HashSet<String> attributes = getAttributeNames();

		for (String name : attributes) {
			Field field = null;
			try {
				field = dwifClass.getDeclaredField(name);
				field.setAccessible(true);
				Attribute attribute = getAttribute(field, dwif);
				testAttributeName(attribute, name);
				testAttributeType(attribute, name);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			testFieldType(field);
		}
	}

	private void testAttributeType(Attribute attribute, String name) {
		if (getNominalNames().contains(name)) {
			assertThat(attribute.isNominal(), is(true));
		}
		if (getNumericNames().contains(name)) {
			assertThat(attribute.isNumeric(), is(true));
		}
	}

	private void testAttributeName(Attribute attribute, String name) {
		assertThat(attribute.name(), is(name));
	}

	private void testFieldType(Field field) {
		assertThat(field.getGenericType(), is(Attribute.class.getClass()));
	}

	private HashSet<String> getAttributeNames() {
		HashSet<String> result = new HashSet<String>();
		result.addAll(getNumericNames());
		result.addAll(getNominalNames());
		return result;
	}

	private HashSet<String> getNumericNames() {
		HashSet<String> result = new HashSet<String>();
		for (int i = 0; i < this.numericAttributes.length; i++) {
			result.add(numericAttributes[i]);
		}
		return result;
	}

	private HashSet<String> getNominalNames() {
		HashSet<String> result = new HashSet<String>();
		for (int i = 0; i < this.nominalAttributes.length; i++) {
			result.add(nominalAttributes[i]);
		}
		return result;
	}

	private Attribute getAttribute(Field field, Object obj) {
		try {
			return ((Attribute) field.get(obj));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void testInstantiate() {

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
