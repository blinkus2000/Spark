package unlimited.core.util.bitwise;

import java.util.BitSet;

public abstract class BitWise {
	public static boolean testBit(long[] bits, int index) {
		return BitSet.valueOf(bits).get(index);
	}
}
