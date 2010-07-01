package tud.irr.daterecognition;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import tud.iir.helper.DateHelper;

public class DateGetterHelperTest {
	
	private String url1;
	private String url2;
	private String url3;
	private String url4;
	private String url5;
	private String url6;
	private String url7;
	private String url8;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		url1= "http://www.example.com/2010-06-30/example.html";
		url2= "http://www.zeit.de/sport/2010-06/example";
		url3= "http://www.nytimes.com/2010/06/30/business/economy/30leonhardt.html?hp";
		url4= "http://www.example.com/2010/06/example.html";
		url5= "http://www.example.com/2010_06_30/example.html";
		url6= "http://www.example.com/2010_06/example.html";
		url7= "http://www.example.com/2010.06.30/example.html";
		url8= "http://www.example.com/2010.06/example.html";
	}

	@After
	public void tearDown() throws Exception {
	}

	
	@Test
	public void testGetURLDate() {
		
		//Cases with given day
		long time = DateHelper.getTimestamp("2010-06-30");
		assertEquals(time, DateHelper.getTimestamp(DateGetterHelper.getURLDate(url1)[0]));
		assertEquals(time, DateHelper.getTimestamp(DateGetterHelper.getURLDate(url3)[0]));
		assertEquals(time, DateHelper.getTimestamp(DateGetterHelper.getURLDate(url5)[0]));
		assertEquals(time, DateHelper.getTimestamp(DateGetterHelper.getURLDate(url7)[0]));
		
		//Cases without given day, so day will be set to 1st
		time = DateHelper.getTimestamp("2010-06-01");
		assertEquals(time, DateHelper.getTimestamp(DateGetterHelper.getURLDate(url2)[0]));
		assertEquals(time, DateHelper.getTimestamp(DateGetterHelper.getURLDate(url4)[0]));
		assertEquals(time, DateHelper.getTimestamp(DateGetterHelper.getURLDate(url6)[0]));
		assertEquals(time, DateHelper.getTimestamp(DateGetterHelper.getURLDate(url8)[0]));
	}
	
	@Test
	public void testGetSeparator(){
		String date1 = "2010-05-06";
		String date2 = "2010_05_06";
		String date3 = "2010.05.06";
		String date4 = "2010/05/06";
		
		assertEquals("-", DateGetterHelper.getSeparator(date1));
		assertEquals("_", DateGetterHelper.getSeparator(date2));
		assertEquals("\\.", DateGetterHelper.getSeparator(date3));
		assertEquals("/", DateGetterHelper.getSeparator(date4));
		
	}
	
	@Test
	public void testGetDateparts(){
		String[] referenz1 = {"2010", "06", "30"};
		String[] referenz2 = {"93", "06", "14"};
		String[] referenz3 = {"10", "06", "30"};
		
		
		String[] date1 = {"2010", "06", "30"};
		String[] date2 = {"30", "2010", "06"};
		String[] date3 = {"06", "2010", "30"};
		String[] date4 = {"06", "30", "2010"};
	}
}
