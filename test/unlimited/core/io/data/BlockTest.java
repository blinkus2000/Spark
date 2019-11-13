package unlimited.core.io.data;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import unlimited.core.io.data.BlockMapper.BlockBuilder;
import unlimited.core.io.data.receive.BlockReceive;
import unlimited.core.io.data.send.BlockSend;


public class BlockTest {
	LinkedBlockingQueue<SourceData<Long>> pipe;
	//R = BlockIndex, C = dataIndex, V = longVal
	ConcurrentHashMap<Long,HashMap<Integer,Long>> receivedLongs;
	SendConsumer sendOut;
	ReceiveConsumer receiveOut;
	ConcurrentHashMap<Long,Receiver> receivers;
	private ExecutorService pool;
	private Vector<Future<Results>> results;
	@Before
	public void setUp() throws Exception {
		pipe = new LinkedBlockingQueue<>();
		receivedLongs = new ConcurrentHashMap<>();
		sendOut = new SendConsumer();
		receiveOut = new ReceiveConsumer();
		receivers = new ConcurrentHashMap<>();
		pool = Executors.newCachedThreadPool();
		results = new Vector<>();
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		fail("Not yet implemented");
	}
	public class SendConsumer implements DataConsumer<SourceData<Long>>{
		@Override
		public void consume(int index, SourceData<Long> data) throws IOException {
			pipe.offer(data);
		}
	}
	public class SendStatusSupplier implements Supplier<BlockStatus>{
		final BlockBuilder builder;
		BlockStatus status;
		public SendStatusSupplier(BlockBuilder builder) {
			super();
			this.builder = builder;
			this.status = new BlockStatus(builder.dataCount);
		}
		@Override
		public BlockStatus get() {
			receivers.compute(builder.blockIndex, (key,rec)->{
				if(rec==null) {
					rec = new Receiver(builder);
					results.add(pool.submit(rec));
				}
				return rec;
			});
			return status;
		}
		
	}
	public class Sender extends BlockSend<Long, SendConsumer>{
		public Sender(BlockBuilder builder, Supplier<BlockStatus> status) {
			super(builder, sendOut, status);
		}
	}
	public class ReceiveConsumer implements DataConsumer<SourceData<Long>>{
		@Override
		public void consume(int index, SourceData<Long> data) throws IOException {
			receivedLongs.compute(data.indexOfBlock, (k,v)->{
				if(v==null) {
					v = new HashMap<>();
				}
				v.put(index, data.getData());
				return v;
			});
			
		}
		
	}
	public class Receiver extends BlockReceive<Long, ReceiveConsumer>{

		public Receiver(BlockBuilder builder) {
			super(builder, pipe::poll, receiveOut);
		}
		
	}
}
