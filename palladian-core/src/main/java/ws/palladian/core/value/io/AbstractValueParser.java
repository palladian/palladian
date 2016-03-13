package ws.palladian.core.value.io;

/**
 * Default implementation for a {@link ValueParser}.
 * 
 * @author pk
 */
public abstract class AbstractValueParser implements ValueParser {

	@Override
	public boolean canParse(String input) {
		try {
			parse(input);
			return true;
		} catch (ValueParserException e) {
			return false;
		}
	}

}
