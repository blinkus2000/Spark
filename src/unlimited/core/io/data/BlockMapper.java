package unlimited.core.io.data;

public class BlockMapper {
	protected final int dataCountPerBlock;

	public BlockMapper(int dataCountPerBlock) {
		super();
		this.dataCountPerBlock = dataCountPerBlock;
	}
	public static class BlockBuilder{
		public final int dataCount;
		public final long blockIndex;
		private BlockBuilder(int dataCount, long blockIndex) {
			super();
			this.dataCount = dataCount;
			this.blockIndex = blockIndex;
		}
		
	}
	public BlockBuilder map(long dataIndex) {
		long indexOfBlock = dataIndex/this.dataCountPerBlock;
		return new BlockBuilder(this.dataCountPerBlock, indexOfBlock);
	}
}
