package ws.palladian.classification.evaluation.roc;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class RocCurvesTest {

	@Test
	public void testRocCurves() throws Exception {
		List<RocCurves.ResultEntry> results = new ArrayList<>();
		results.add(new RocCurves.ResultEntry(true, 1));
		results.add(new RocCurves.ResultEntry(false, 0.895));
		results.add(new RocCurves.ResultEntry(false, 0.894));
		results.add(new RocCurves.ResultEntry(true, 0.856));
		results.add(new RocCurves.ResultEntry(true, 0.833));
		results.add(new RocCurves.ResultEntry(true, 0.723));
		results.add(new RocCurves.ResultEntry(true, 0.703));
		results.add(new RocCurves.ResultEntry(false, 0.674));
		results.add(new RocCurves.ResultEntry(true, 0.651));
		results.add(new RocCurves.ResultEntry(true, 0.589));
		results.add(new RocCurves.ResultEntry(true, 0.548));
		results.add(new RocCurves.ResultEntry(false, 0.37));
		results.add(new RocCurves.ResultEntry(false, 0.363));
		results.add(new RocCurves.ResultEntry(false, 0.338));
		results.add(new RocCurves.ResultEntry(true, 0));
		RocCurves rocCurves = new RocCurves(results);
		// rocCurves.saveCurves(new File("/Users/pk/Desktop/curves.png"));
		// System.out.println(rocCurves.getAreaUnderCurve());
		assertEquals(0.5741, rocCurves.getAreaUnderCurve(), 0.0001);
		// CollectionHelper.print(rocCurves);
		// rocCurves.writeEntries(System.out, ';');
	}

}
