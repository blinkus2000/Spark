package unlimited.core.io.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Stopwatch;

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
	private final int threadLimit = 5;
	private Semaphore threadLimiter;
	boolean lastNeeedsTruncate;
	@Before
	public void setUp() throws Exception {
		threadLimiter = new Semaphore(threadLimit);
		expectedList = new LinkedList<>();
		values = 0l;
		receivedLongs = new ConcurrentHashMap<>();
		receiveOut = new ReceiveConsumer();
		receivers = new ConcurrentHashMap<>();
		senders = new ConcurrentHashMap<>();
		sendPool = Executors.newFixedThreadPool(threadLimit);
		receivePool = Executors.newFixedThreadPool(threadLimit);
		resultsSenders = new Vector<>();
		resultsReceivers = new Vector<>();
		gen = new Random();
		blockSize = 1000 + gen.nextInt(1000);
		limit = 10000000l;
		lastNeeedsTruncate = limit%blockSize!=0;
		System.out.println("Running test with "+limit+" items and a block size of "+blockSize+" lastValue will be truncated "+lastNeeedsTruncate);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
		Stream<SourceData<Long>> incomingDataStream = Stream.generate(()->values++)
				.peek(expectedList::add)
				.limit(limit)
				.map(this::buildSourceData);
		Stopwatch sw = Stopwatch.createStarted();
		StreamUtil.collapseWithFunction(incomingDataStream, this::addSourceToSender, this::mapToSender)
				.forEach(this::runSender);
		//.forEach(System.out::println);
		
		resultsSenders.forEach(future->assertNull(getException(future)));
		resultsReceivers.forEach(future->assertNull(getException(future)));
		long packetsPerSec = (long) (limit/(((double)sw.elapsed(TimeUnit.MILLISECONDS))/1000));
		System.out.println("Executed at a rate of "+packetsPerSec+" items per second with a block size of "+blockSize);
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
	boolean addSourceToSender(SourceData<Long> data, Sender sender ) {
		sender.putData(data.getIndex(), data);
		return !sender.isFull();
	}
	Sender mapToSender(SourceData<Long> data) {
		Sender sender = new Sender(new BlockBuilder(blockSize,data.indexOfBlock), new SendConsumer());
		senders.put(data.indexOfBlock, sender);
		return sender;
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
			System.out.println("Truncating "+sourceData);
		}
		
		return sourceData;
	}
	void runSender(Sender sender) {
		try {
			System.out.println("Attempting to run sender "+sender);
			threadLimiter.acquire();
			System.out.println("Submitting sender"+sender);
			resultsSenders.add(sendPool.submit(sender));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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

		@Override
		public Results call() throws Exception {
			try {
				System.out.println("Executing: "+this);
				return super.call();
			} finally {
				System.out.println("Releasing: "+this);
				threadLimiter.release();
			}
		}

		@Override
		public String toString() {
			return "Sender [blockIndex=" + blockIndex + ", dataCount=" + dataCount + "]";
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
