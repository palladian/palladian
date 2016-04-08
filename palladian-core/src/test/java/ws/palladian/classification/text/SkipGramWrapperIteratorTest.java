package ws.palladian.classification.text;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.core.ImmutableToken;
import ws.palladian.core.Token;
import ws.palladian.helper.collection.CollectionHelper;

public class SkipGramWrapperIteratorTest {
	@Test
	public void testSkipGramWrapperIterator_singleTokenOrDoubleTokens() {
		Token t1 = new ImmutableToken(0, "the");
		SkipGramWrapperIterator iterator = new SkipGramWrapperIterator(asList(t1).iterator());
		assertEquals(1, CollectionHelper.count(iterator));
	}

	@Test
	public void testSkipGramWrapperIterator_doubleToken() {
		Token t2 = new ImmutableToken(0, "the quick");
		SkipGramWrapperIterator iterator = new SkipGramWrapperIterator(asList(t2).iterator());
		assertEquals(1, CollectionHelper.count(iterator));
	}

	@Test
	public void testSkipGramWrapperIterator_threeTokens() {
		Token t3 = new ImmutableToken(0, "the quick brown");
		SkipGramWrapperIterator iterator = new SkipGramWrapperIterator(asList(t3).iterator());
		List<Token> tokens = CollectionHelper.newArrayList(iterator);
		assertEquals(2, tokens.size());
		assertEquals("the quick brown", tokens.get(0).getValue());
		assertEquals("the brown", tokens.get(1).getValue());
	}

}
