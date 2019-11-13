package unlimited.core.io.data.send;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import unlimited.core.io.data.BlockMapper.BlockBuilder;
import unlimited.core.io.data.BlockMapper;
import unlimited.core.io.data.BlockStatus;
import unlimited.core.io.data.DataConsumer;
import unlimited.core.io.data.SourceData;

public class BlockSendTest {
	long values = 0l;

	@Before
	public void setUp() throws Exception {
		values = 0l;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCall() throws Exception {
		final List<SourceData<Long>> shuffled = Stream.generate(()->values++).map(val->new SourceData<Long>(1000,val,val)).limit(1000).collect(Collectors.toList());
		Collections.shuffle(shuffled);
		assertEquals(1000,shuffled.size());
		BlockMapper mapper = new BlockMapper(1000);
		BlockStatus status = new BlockStatus(1000);
		Output out = new Output(status);
		TestSender underTest = new TestSender(mapper.map(0), out);
		shuffled.forEach(data->underTest.putData(data.getIndex(), data));
		underTest.call();
		shuffled.stream().map(SourceData::getIndex).forEach(i->assertTrue(status.hasIndex(i)));
	}
	public class Output implements DataConsumer<SourceData<Long>>{
		final BlockStatus status;
		
		public Output(BlockStatus status) {
			super();
			this.status = status;
		}

		@Override
		public void consume(int index, SourceData<Long> d) throws IOException {
			status.addIndex(index);
		}
		
	}
	public class TestSender extends BlockSend <Long,Output>{

		public TestSender(BlockBuilder builder, Output out) {
			super(builder, out, out.status::clone);
		}
		
	}
}
