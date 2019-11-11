package unlimited.core.io.data.send;



import unlimited.core.io.data.Block;
import unlimited.core.io.data.DataConsumer;

public class BlockSend <DataType,In extends DataConsumer<DataType>,Out extends DataConsumer<DataType>> extends Block<DataType,In,Out,BlockSendResults> {
	public BlockSend(long numOfPackets, long blockIndex, In in, Out out) {
		super(numOfPackets, blockIndex, in, out);
	}

	@Override
	public BlockSendResults call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
