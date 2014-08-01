package ws.palladian.helper.functional;

public final class Filters {

	private Filters() {
		// no instances
	}

	/** A filter which removes <code>null</code> elements. */
	public static final Filter<Object> NULL_FILTER = new Filter<Object>() {
		@Override
		public boolean accept(Object item) {
			return item != null;
		}
	};

	/** A filter which accepts all elements. */
	public static final Filter<Object> ACCEPT = new Filter<Object>() {
		@Override
		public boolean accept(Object item) {
			return true;
		}
	};

	/** A filter which rejects all elements. */
	public static final Filter<Object> REJECT = new Filter<Object>() {
		@Override
		public boolean accept(Object item) {
			return false;
		}
	};

}
