package unlimited.core.util.bitwise;

import java.util.BitSet;

public abstract class BitWise {
	public static boolean testBit(long bits, int index) {
		/*
		 * if(index>64) { return false; } long mask = 1l<<index; return (bits&mask)!=0l;
		 */
		long[] longs = {bits};
		return BitSet.valueOf(longs).get(index);
	}
}
