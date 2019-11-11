package unlimited.core.io.data.receive;


import unlimited.core.io.data.Block;
import unlimited.core.io.data.DataConsumer;

public class BlockReceive<DataType,In extends DataConsumer<DataType>,Out extends DataConsumer<DataType>> extends Block<DataType,In,Out,BlockReceiveResults> {

	public BlockReceive(long numOfPackets, long blockIndex, In in, Out out) {
		super(numOfPackets, blockIndex, in, out);
	}

	@Override
	public BlockReceiveResults call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
