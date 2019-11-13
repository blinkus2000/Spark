package unlimited.core.io.data;

import static org.junit.Assert.*;

import org.junit.Test;

import unlimited.core.util.bitwise.BitWise;

public class BlockStatusTest {

	@Test
	public void testGetNextEmptyEntry() {
		BlockStatus underTest = new BlockStatus(10);
		for(int i = 0 ; i < 10 ; i ++) {
			assertEquals(i,underTest.getNextEmptyEntry());
			assertFalse(underTest.isComplete());
			underTest.addIndex(i);
			assertEquals(i+1,underTest.getNextEmptyEntry());
		}
		assertTrue(underTest.isComplete());
		underTest = new BlockStatus(10);
		for(int i = 9 ; i >= 0 ; i --) {
			assertEquals(0,underTest.getNextEmptyEntry());
			assertFalse(underTest.isComplete());
			underTest.addIndex(i);
		}
		assertTrue(underTest.isComplete());
		assertEquals(10,underTest.getNextEmptyEntry());
	}

	@Test
	public void testGetLongMap() {
		BlockStatus underTest = new BlockStatus(10);
		for(int i = 0 ; i < 10 ; i ++) {
			long[] longMap = underTest.getLongMap();
			
			for(int j = 0 ; j < i ; j++) {
				assertFalse(BitWise.testBit(longMap, j));
			}
			assertFalse(underTest.isComplete());
			underTest.addIndex(i);
		}
		assertTrue(underTest.isComplete());
		underTest = new BlockStatus(10);
		int expectedCount = 0;
		for(int i = 9 ; i >= 0 ; i --) {
			long[] next64 = underTest.getLongMap();
			int actualCount = 0;
			for(int j = 0 ; j < 10 ; j ++) {
				if(BitWise.testBit(next64, j)) {
					actualCount++;
				}
			}
			assertEquals(expectedCount,actualCount);
			assertFalse(underTest.isComplete());
			underTest.addIndex(i);
			expectedCount++;
		}
		assertTrue(underTest.isComplete());
	}

}