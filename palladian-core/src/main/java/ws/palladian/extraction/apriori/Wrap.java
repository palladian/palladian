package ws.palladian.extraction.apriori;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Helper class which allows negation. This is useful when learning association
 * rules and one wants to learn rules based on negative conditions as well
 * (e.g. "restaurant_has_no_alcohol" => "restaurant_is_good_for_kids").
 * 
 * @author pk
 */
public final class Wrap<T> {
	private final T value;
	private final boolean negated;
	public static <T> Wrap<T> not(T value) {
		return new Wrap<T>(value, true);
	}
	public static <T> Wrap<T> of(T value) {
		return new Wrap<T>(value, false);
	}
	private Wrap(T value, boolean negated) {
		this.value = value;
		this.negated = negated;
	}
	@Override
	public String toString() {
		return (negated ? "￢" : "") + value;
	}
	@Override
	public int hashCode() {
		return Boolean.hashCode(negated) ^ value.hashCode();
	}
	public T getValue() {
		return value;
	}
	public boolean isNegated() {
		return negated;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Wrap<?> other = (Wrap<?>) obj;
		if (negated != other.negated) {
			return false;
		}
		return value.equals(other.value);
	}
	public static <E extends Enum<E>> Set<Wrap<E>> createNegated(E[] allValues, Set<E> presentValues) {
		Objects.requireNonNull(allValues, "allValues must not be null");
		Objects.requireNonNull(presentValues, "presentValues must not be null");
		Set<Wrap<E>> negatedLabels = new HashSet<>();
		for (E value : allValues) {
			negatedLabels.add(presentValues.contains(value) ? Wrap.of(value) : Wrap.not(value));
		}
		return negatedLabels;
	}
}