package unlimited.core.io.data;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import unlimited.core.io.data.BlockMapper.BlockBuilder;
import unlimited.core.io.data.receive.BlockReceive;
import unlimited.core.io.data.send.BlockSend;
import unlimited.core.util.stream.StreamUtil;


public class BlockTest {
	
	//R = BlockIndex, C = dataIndex, V = longVal
	ConcurrentHashMap<Long,ConcurrentHashMap<Integer,Long>> receivedLongs;
	
	ReceiveConsumer receiveOut;
	ConcurrentHashMap<Long,Receiver> receivers;
	ConcurrentHashMap<Long,Sender> senders;
	private ExecutorService sendPool,receivePool;
	
	private Vector<Future<Results>> resultsSenders,resultsReceivers;
	private LinkedList<Long> expectedList;
	private long values = 0l;
	private Random gen;
	private int blockSize;
	private long limit;
	@Before
	public void setUp() throws Exception {
		expectedList = new LinkedList<>();
		values = 0l;
		receivedLongs = new ConcurrentHashMap<>();
		receiveOut = new ReceiveConsumer();
		receivers = new ConcurrentHashMap<>();
		senders = new ConcurrentHashMap<>();
		sendPool = Executors.newFixedThreadPool(5);
		receivePool = Executors.newFixedThreadPool(5);
		resultsSenders = new Vector<>();
		resultsReceivers = new Vector<>();
		gen = new Random();
		blockSize = 1000 + gen.nextInt(4000);
		limit = 1000000l;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
		final Stream<LinkedList<Long>> orderedLongLists = StreamUtil.collapse(
				Stream.generate(()->values++).peek(expectedList::add),(val,list)->{
			list.add(val);
			return gen.nextInt(100)>80;
		},LinkedList::new);
		
		
		StreamUtil.expand(orderedLongLists.peek(Collections::shuffle), LinkedList::stream)
		.limit(limit)
		.map(this::buildSourceData)
		.forEach(this::putData);
		resultsSenders.forEach(future->assertNull(getException(future)));
		resultsReceivers.forEach(future->assertNull(getException(future)));
		final LinkedList<Long> resultList = getResultList();
		assertEquals(expectedList,resultList);
	}
	LinkedList<Long> getResultList(){
		LinkedList<Long> returnVal = new LinkedList<>();
		final int blockCount = receivedLongs.size();
		for(long i = 0; i < blockCount ; i ++) {
			final ConcurrentHashMap<Integer, Long> blockData = receivedLongs.get(i);
			final int blockSize = blockData.size();
			for(int j = 0 ; j < blockSize ; j++ ) {
				returnVal.add(blockData.get(j));
			}
		}
		return returnVal;
	}
	Exception getException(Future<Results> task) {
		try {
			
			final Exception error = task.get().error;
			if(error!=null) {
				error.printStackTrace();
			}
			return error;
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	SourceData<Long> buildSourceData(Long val){
		final SourceData<Long> sourceData = new SourceData<Long>(blockSize,val,val);
		if(val == limit-1l) {
			sourceData.truncateHere();
		}
		return sourceData;
	}
	void putData(SourceData<Long> data) {
		senders.compute(data.indexOfBlock, (blockIndex,sender)->{
			final Sender s = runSenderIfNeeeded(blockIndex,sender);
			s.putData(data.getIndex(), data);
			return s;
		});
	}
	Sender runSenderIfNeeeded(Long blockIndex,Sender sender) {
		if(sender==null) {
			sender = new Sender(new BlockBuilder(blockSize,blockIndex), new SendConsumer());
			resultsSenders.add(sendPool.submit(sender));
		}
		return sender;
	}
	public class SendConsumer implements DataConsumer<SourceData<Long>>{
		LinkedBlockingQueue<SourceData<Long>> pipe = new LinkedBlockingQueue<>();
		@Override
		public void consume(int index, SourceData<Long> data) throws IOException {
			pipe.offer(data);
		}
		SourceData<Long> safeGet(){
			try {
				return pipe.take();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	public class SendStatusSupplier implements Supplier<BlockStatus>{
		final BlockBuilder builder;
		private SendConsumer sendOut;
		public SendStatusSupplier(BlockBuilder builder, SendConsumer sendOut) {
			super();
			this.builder = builder;
			this.sendOut = sendOut;
		}
		@Override
		public BlockStatus get() {
			return receivers.compute(builder.blockIndex, (key,rec)->{
				if(rec==null) {
					rec = new Receiver(builder,sendOut::safeGet);
					resultsReceivers.add(receivePool.submit(rec));
				}
				return rec;
			}).getStatus();
			
		}
		
	}
	public class Sender extends BlockSend<Long, SendConsumer>{

		public Sender(BlockBuilder builder,SendConsumer sendOut) {
			super(builder, sendOut, new SendStatusSupplier(builder,sendOut));
			
		}
	}
	public class ReceiveConsumer implements DataConsumer<SourceData<Long>>{
		@Override
		public void consume(int index, SourceData<Long> data) throws IOException {
			receivedLongs.compute(data.indexOfBlock, (k,v)->{
				if(v==null) {
					v = new ConcurrentHashMap<>();
				}
				v.put(index, data.getData());
				return v;
			});
			
		}
		
	}
	
	public class Receiver extends BlockReceive<Long, ReceiveConsumer>{

		public Receiver(BlockBuilder builder,DataProducer<Long> in) {
			super(builder,in, receiveOut);
		}
		
	}
}
