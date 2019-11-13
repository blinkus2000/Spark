package unlimited.core.io.data;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SourceDataTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		SourceData<Boolean> data = new SourceData<>(100, 1000l, true);
		assertEquals(0,data.getIndex());
		assertEquals(10l,data.indexOfBlock);
		assertEquals(0,data.getTruncate());
		data.truncateHere();
		assertEquals(100,data.getTruncate());
	}

}
