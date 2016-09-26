package ws.palladian.helper.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Priority queue with a fixed size. Useful for retrieving the top N elements
 * from a larger collection.
 * 
 * @author <a href="http://stackoverflow.com/a/19060708/388827">Murat Derya
 *         Özen</a>
 *
 * @param <E>
 */
public class FixedSizePriorityQueue<E> {
	private final PriorityQueue<E> priorityQueue; /* backing data structure */
	private final Comparator<? super E> comparator;
	private final int maxSize;

	/**
	 * Constructs a {@link FixedSizePriorityQueue} with the specified
	 * {@code maxSize} and {@code comparator}.
	 *
	 * @param maxSize
	 *            The maximum size the queue can reach, must be a positive
	 *            integer.
	 * @param comparator
	 *            The comparator to be used to compare the elements in the
	 *            queue, must be non-null.
	 */
	public FixedSizePriorityQueue(int maxSize, Comparator<? super E> comparator) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("maxSize = " + maxSize + "; expected a positive integer.");
		}
		Objects.requireNonNull(comparator, "comparator was null");
		this.priorityQueue = new PriorityQueue<E>(maxSize, comparator);
		this.comparator = priorityQueue.comparator();
		this.maxSize = maxSize;
	}

	/**
	 * Adds an element to the queue. If the queue contains {@code maxSize}
	 * elements, {@code e} will be compared to the greatest element in the queue
	 * using {@code comparator}. If {@code e} is less than or equal to the
	 * greatest element, that element will be removed and {@code e} will be
	 * added instead. Otherwise, the queue will not be modified and {@code e}
	 * will not be added.
	 *
	 * @param e
	 *            Element to be added, must be non-null.
	 */
	public void add(E e) {
		Objects.requireNonNull(e, "e was null");
		if (maxSize <= priorityQueue.size()) {
			E firstElm = priorityQueue.peek();
			if (comparator.compare(e, firstElm) < 1) {
				return;
			} else {
				priorityQueue.poll();
			}
		}
		priorityQueue.add(e);
	}

	/**
	 * @return Returns a sorted view of the queue as a
	 *         {@link Collections#unmodifiableList(java.util.List)}
	 *         unmodifiableList.
	 */
	public List<E> asList() {
		return Collections.unmodifiableList(new ArrayList<E>(priorityQueue));
	}
}
