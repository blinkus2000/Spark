package unlimited.core.io.data;

import java.util.concurrent.Callable;

import unlimited.core.io.data.BlockMapper;

public abstract class Block<DataType,Out extends DataConsumer<SourceData<DataType>> > implements Callable<Results> {
	protected final int dataCount;
	protected final long blockIndex;
	protected final Out out;
	public Block(BlockMapper.BlockBuilder builder,  Out out) {
		super();
		this.dataCount = builder.dataCount;
		this.blockIndex = builder.blockIndex;
		this.out = out;
	}

	public boolean containsDataIndex(long dataIndex) {
		long indexOfData = dataIndex / dataCount;
		return indexOfData == blockIndex;
	}
}
