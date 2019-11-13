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
	public BlockSend(BlockMapper.BlockBuilder builder, Out out, Supplier<BlockStatus> status) {
		super(builder, out);
		this.status = status;
	}

	@Override
	public Results call() throws Exception {
		try {
			while(send());
		} catch (Exception e) {
			return new Results(e);
		}
		return new Results(null);
	}
	public boolean send() {
		BlockStatus currentStatus = status.get();
		if(currentStatus.isComplete()) {
			return true;
		}
		for(int i = currentStatus.getNextEmptyEntry(); i < this.dataCount ; i ++) {
			allData.computeIfPresent(i, this::send);
		}
		return false;
	}
	private SourceData<DataType> send(int index, SourceData<DataType> data) {
		try {
			this.out.consume(index, data);
			return null;
		} catch (IOException e) {
			return data;
		}
	}
	public void putData(int index, SourceData<DataType> data) {
		allData.put(index, data);
	}

}
