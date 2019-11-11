package unlimited.core.io.data;

import java.util.concurrent.Callable;

public abstract class Block<DataType,In extends DataConsumer<DataType>,Out extends DataConsumer<DataType>,ResultType extends Results> implements Callable<ResultType> {
	protected final long numOfPackets;
	protected final long blockIndex;
	protected final In in;
	protected final Out out;
	public Block(long numOfPackets, long blockIndex, In in, Out out) {
		super();
		this.numOfPackets = numOfPackets;
		this.blockIndex = blockIndex;
		this.in = in;
		this.out = out;
	}
	
}
