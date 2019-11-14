package unlimited.core.io.data.send;



import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import unlimited.core.io.data.Block;
import unlimited.core.io.data.BlockMapper;
import unlimited.core.io.data.BlockStatus;
import unlimited.core.io.data.DataConsumer;
import unlimited.core.io.data.Results;
import unlimited.core.io.data.SourceData;

public class BlockSend <DataType,Out extends DataConsumer<SourceData<DataType>>> extends Block<DataType,Out> {
	final ConcurrentHashMap<Integer,SourceData<DataType>> allData = new ConcurrentHashMap<>();
	private final Supplier<BlockStatus> status;
	private int truncate = 0;
	private int sendCount = 0;
	public BlockSend(BlockMapper.BlockBuilder builder, Out out, Supplier<BlockStatus> status) {
		super(builder, out);
		this.status = status;
	}

	@Override
	public Results call() throws Exception {
		Thread.currentThread().setName("SENDER ID: "+this.blockIndex);
		try {
			while(sending());
		} catch (Exception e) {
			return new Results(e);
		}
		System.out.println("SEND COUNT "+sendCount+" for "+this);
		return new Results(null);
	}

	public boolean sending() {
		try {
			BlockStatus currentStatus = status.get();
			if (currentStatus.isComplete()) {
				return false;
			}
			for (int i = currentStatus.getNextEmptyEntry(); i < this.dataCount; i++) {
				if (!currentStatus.hasIndex(i)) {
					allData.computeIfPresent(i, this::send);
				}
			}
			sendCount++;
			return true;
		} finally {
			try {
				Thread.sleep(0l);
			} catch (InterruptedException e) {}
		}
	}
	private SourceData<DataType> send(int index, SourceData<DataType> data) {
		try {
			this.out.consume(index, data);
			return data;
		} catch (IOException e) {
			return data;
		}
	}
	public void putData(int index, SourceData<DataType> data) {
		if(data.getTruncate()>0) {
			this.truncate  = data.getTruncate();
		}
		allData.put(index, data);
	}

	@Override
	public boolean isFull() {
		return allData.size()==(this.dataCount-this.truncate);
	}

}
