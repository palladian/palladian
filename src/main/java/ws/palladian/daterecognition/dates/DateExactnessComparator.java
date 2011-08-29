package ws.palladian.daterecognition.dates;

import java.util.Comparator;

public class DateExactnessComparator implements Comparator<ExtractedDate> {

	/**
	 * <p>Highest exactness first.</p>
	 * 
	 * @param ed1 An extracted date.
	 * @param ed2 An extracted date.
	 * @return 0 or 1 depending on the exactness.
	 */
	@Override
	public int compare(ExtractedDate ed1, ExtractedDate ed2) {
		return (int) (ed2.getExactness() - ed1.getExactness());
	}
}