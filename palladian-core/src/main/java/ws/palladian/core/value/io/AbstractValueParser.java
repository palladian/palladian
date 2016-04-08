package ws.palladian.core.value.io;

import ws.palladian.core.value.Value;

/**
 * Default implementation for a {@link ValueParser}.
 * 
 * @author pk
 */
public abstract class AbstractValueParser implements ValueParser {

	private final Class<? extends Value> type;

	protected AbstractValueParser(Class<? extends Value> type) {
		this.type = type;
	}

	@Override
	public boolean canParse(String input) {
		try {
			parse(input);
			return true;
		} catch (ValueParserException e) {
			return false;
		}
	}
	
	@Override
	public Class<? extends Value> getType() {
		return type;
	}

}
