package ws.palladian.utils;

import ws.palladian.helper.functional.Filter;

/**
 * This filter simply accepts a certain fraction of series of data, which is
 * defined by a modulo value and a remainder.
 * 
 * @author pk
 */
public final class ModuloFilter implements Filter<Object> {

	private final int mod;
	private final int remainder;

	private int count = 0;

	public ModuloFilter(int mod, int remainder) {
		this.mod = mod;
		this.remainder = remainder;
	}

	public ModuloFilter(boolean oddOrEven) {
		this(2, oddOrEven ? 1 : 0);
	}

	@Override
	public boolean accept(Object item) {
		return ++count % mod == remainder;
	}

}
