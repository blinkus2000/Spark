package unlimited.core.io.data;

import java.util.BitSet;
import java.util.function.Consumer;

public class BlockStatus implements Cloneable {
	final int dataCount;
	BitSet received;
	int currentFull = 0;
	private boolean changed = false;
	public BlockStatus(int dataCount) {
		this.dataCount = dataCount;
		this.received = new BitSet(dataCount);
	}
	private BlockStatus(BlockStatus status) {
		synchronized(status) {
			this.dataCount = status.dataCount;
			this.currentFull = status.currentFull;
			this.received = (BitSet) status.received.clone();
		}
	}
	public synchronized void addIndex(int index) {
		received.set(index);
		this.changed = true;
	}
	public synchronized boolean hasIndex(int index) {
		return this.received.get(index);
	}
	public synchronized int getNextEmptyEntry() {
		int nextClearBit = this.received.nextClearBit(currentFull);
		if(nextClearBit>currentFull) {
			currentFull = nextClearBit-1;
		}
		return nextClearBit;
	}
	public synchronized long[] getLongMap() {
		int firstEmpty = getNextEmptyEntry();
		int bitsToCheck = Math.min(64, (dataCount-firstEmpty));
		BitSet bitSet = this.received.get(firstEmpty, firstEmpty+bitsToCheck);
		return bitSet.toLongArray();
	}
	@Override
	protected BlockStatus clone(){
		return new BlockStatus(this);
	}
	public synchronized void updateAndReset(Consumer<BlockStatus> updater) {
		if(this.changed) {
			updater.accept(this.clone());
			this.changed = false;
		}		
	}
	public synchronized boolean isComplete() {
		return this.received.nextClearBit(currentFull) == dataCount;
	}
	
}
