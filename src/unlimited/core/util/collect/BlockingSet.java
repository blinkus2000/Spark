package unlimited.core.util.collect;

import java.util.HashSet;

public class BlockingSet<T> {
	final HashSet<T> innerSet = new HashSet<>();
	public synchronized void offer(T t) {
		innerSet.add(t);
		notifyAll();
	}

	public T take() throws InterruptedException {
		T returnVal = null;
		synchronized(this) {
			while(innerSet.isEmpty()) {
				wait();
			}
			final T next = innerSet.iterator().next();
			innerSet.remove(next);
			returnVal = next;
			notifyAll();
		}
		return returnVal;
	}

}
