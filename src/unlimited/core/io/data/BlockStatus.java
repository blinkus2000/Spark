package unlimited.core.io.data;

import java.util.BitSet;

public class BlockStatus {
	final int blockSize;
	BitSet received;
	int currentFull = 0;
	public BlockStatus(int blockSize) {
		this.blockSize = blockSize;
		this.received = new BitSet(blockSize);
	}
	public void addIndex(int index) {
		received.set(index);
	}
	public boolean hasIndex(int index) {
		return this.received.get(index);
	}
	public int getNextEmptyEntry() {
		int nextClearBit = this.received.nextClearBit(currentFull);
		if(nextClearBit>currentFull) {
			currentFull = nextClearBit-1;
		}
		return nextClearBit;
	}
	public long[] getLongMap() {
		int firstEmpty = getNextEmptyEntry();
		int bitsToCheck = Math.min(64, (blockSize-firstEmpty));
		BitSet bitSet = this.received.get(firstEmpty, firstEmpty+bitsToCheck);
		return bitSet.toLongArray();
	}
}
