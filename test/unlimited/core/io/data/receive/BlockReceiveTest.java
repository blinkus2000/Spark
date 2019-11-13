package unlimited.core.io.data.receive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import unlimited.core.io.data.BlockMapper;
import unlimited.core.io.data.BlockMapper.BlockBuilder;
import unlimited.core.io.data.DataConsumer;
import unlimited.core.io.data.DataProducer;
import unlimited.core.io.data.Results;
import unlimited.core.io.data.SourceData;

public class BlockReceiveTest {

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
		final Iterator<SourceData<Long>> iterator = shuffled.iterator();
		Output out = new Output();
		Input  in = new Input(iterator);
		BlockMapper mapper = new BlockMapper(1000);
		TestReceive underTest = new TestReceive(mapper.map(0),  in,  out);
		final Results call = underTest.call();
		assertNull(call.getError());
		for(int i = 0 ; i < 1000 ; i ++) {
			assertEquals(Long.valueOf(i),out.data.get(i));
		}
		
	}
	public class Output implements DataConsumer<SourceData<Long>>{
		HashMap<Integer,Long> data = new HashMap<>();
		@Override
		public void consume(int index, SourceData<Long> d) throws IOException {
			data.put(d.getIndex(), d.getData());
		}
		
	}
	public class Input implements DataProducer<Long>{
		final Iterator<SourceData<Long>> iterator;
		
		public Input(Iterator<SourceData<Long>> iterator) {
			super();
			this.iterator = iterator;
		}

		@Override
		public SourceData<Long> produce() throws IOException {
			return this.iterator.next();
		}
		
	}
	public class TestReceive extends BlockReceive<Long,Output>{

		public TestReceive(BlockBuilder builder, Input in, Output out) {
			super(builder, in, out);
		}
		
	}

}
