package ws.palladian.helper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ws.palladian.helper.date.DateHelper;

@RunWith(PowerMockRunner.class)
public class StopWatchTest {

  @PrepareForTest({System.class, StopWatch.class})
  @Test
  public void testGetStartTime() {
    PowerMockito.mockStatic(System.class);
    final StopWatch stopWatch = new StopWatch();
    PowerMockito.when(System.currentTimeMillis()).thenReturn(0L);

    Assert.assertEquals(0L, stopWatch.getStartTime());
  }

  @PrepareForTest({System.class, StopWatch.class})
  @Test
  public void testGetTotalElapsedTime() {
    PowerMockito.mockStatic(System.class);
    final StopWatch stopWatch = new StopWatch();
    stopWatch.setCountDown(0L);
    PowerMockito.when(System.currentTimeMillis()).thenReturn(0L).thenReturn(6_231L).thenReturn(95_648L);

    Assert.assertEquals(0L, stopWatch.getTotalElapsedTime());
    Assert.assertEquals(6_231L, stopWatch.getTotalElapsedTime());
    Assert.assertEquals(95_648L, stopWatch.getTotalElapsedTime());
  }

  @Test
  public void testGetCountDown() {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.setCountDown(0L);

    Assert.assertEquals(0L, stopWatch.getCountDown());

    stopWatch.setCountDown(5_432L);

    Assert.assertEquals(5_432L, stopWatch.getCountDown());
  }

  @PrepareForTest({System.class, StopWatch.class})
  @Test
  public void testGetElapsedTime() {
    PowerMockito.mockStatic(System.class);
    final StopWatch stopWatch = new StopWatch();
    PowerMockito.when(System.currentTimeMillis()).thenReturn(0L).thenReturn(0L).thenReturn(0L).thenReturn(4_758L);
    stopWatch.start();
    stopWatch.stop();

    Assert.assertEquals(0L, stopWatch.getElapsedTime(false));
    Assert.assertEquals(0L, stopWatch.getElapsedTime(true));

    stopWatch.start();
    stopWatch.stop();

    Assert.assertEquals(4_758L, stopWatch.getElapsedTime(false));
    Assert.assertEquals(4L, stopWatch.getElapsedTime(true));
  }

  @PrepareForTest({DateHelper.class, StopWatch.class, System.class})
  @Test
  public void testGetElapsedTimeStringAndIncrement() {
    PowerMockito.mockStatic(System.class);
    final StopWatch stopWatch = new StopWatch();
    PowerMockito.when(System.currentTimeMillis()).thenReturn(0L);

    Assert.assertEquals("0ms (+0ms)", stopWatch.getElapsedTimeStringAndIncrement());
  }

  @PrepareForTest({StopWatch.class, System.class})
  @Test
  public void testGetTotalElapsedTimeString() {
    PowerMockito.mockStatic(System.class);
    final StopWatch stopWatch = new StopWatch();
    PowerMockito.when(System.currentTimeMillis()).thenReturn(0L);
    stopWatch.stop();

    Assert.assertEquals("0ms", stopWatch.toString());
    Assert.assertEquals("0ms", stopWatch.getTotalElapsedTimeString(true));

    stopWatch.setOutputDetail(null);

    Assert.assertEquals("0ms", stopWatch.getTotalElapsedTimeString());
  }

  @PrepareForTest({DateHelper.class, StopWatch.class, System.class})
  @Test
  public void testGetTotalElapsedTimeString2() {
    PowerMockito.mockStatic(System.class);
    final StopWatch stopWatch = new StopWatch();
    PowerMockito.when(System.currentTimeMillis()).thenReturn(0L);

    Assert.assertEquals("0ms", stopWatch.toString());
    Assert.assertEquals("0ms", stopWatch.getTotalElapsedTimeString(true));
  }

  @PrepareForTest({StopWatch.class, System.class})
  @Test
  public void testTimeIsUpOutputFalse() {
    PowerMockito.mockStatic(System.class);
    final StopWatch stopWatch = new StopWatch();

    Assert.assertEquals(false, stopWatch.timeIsUp());

    stopWatch.setCountDown(-2L);
    PowerMockito.when(System.currentTimeMillis()).thenReturn(-765_937L);

    Assert.assertEquals(false, stopWatch.timeIsUp());
  }

  @Test
  public void testTimeIsUpOutputTrue() {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.setCountDown(-2L);

    Assert.assertEquals(true, stopWatch.timeIsUp());
  }
}
