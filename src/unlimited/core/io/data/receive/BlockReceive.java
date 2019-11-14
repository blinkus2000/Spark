package unlimited.core.io.data.receive;


import java.io.IOException;

import unlimited.core.io.data.Block;
import unlimited.core.io.data.BlockMapper;
import unlimited.core.io.data.BlockStatus;
import unlimited.core.io.data.DataConsumer;
import unlimited.core.io.data.DataProducer;
import unlimited.core.io.data.Results;
import unlimited.core.io.data.SourceData;

public class BlockReceive<DataType,Out extends DataConsumer<SourceData<DataType>>> extends Block<DataType,Out> {
	final BlockStatus status;
	final DataProducer<DataType> in;
	public BlockReceive(BlockMapper.BlockBuilder builder, DataProducer<DataType> in, Out out) {
		super(builder, out);
		status = new BlockStatus(dataCount);
		this.in = in;
	}

	@Override
	public Results call() throws Exception {
		Thread.currentThread().setName("RECEIVER ID: "+this.blockIndex);
		while(!status.isComplete()) {
			try {
				receive();
			} catch (Exception e) {
				return new Results(e);
			}
		}
		return new Results(null);
	}
	public void receive() throws IOException {
		SourceData<DataType> incoming = in.produce();
		synchronized (status) {
			int index = incoming.getIndex();
			if(!status.hasIndex(index)) {
				final int truncate = incoming.getTruncate();
				if(truncate>0) {
					status.truncate(truncate);
				}
				this.out.consume(index, incoming);
				status.addIndex(index);
			}
		}		
	}

	public  BlockStatus getStatus() {
		synchronized (status) {
			return status.clone();
		}
	}

	@Override
	public boolean isFull() {
		return this.getStatus().isComplete();
	}
	
}
