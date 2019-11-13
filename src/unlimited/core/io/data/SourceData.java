package unlimited.core.io.data;

public class SourceData<DataType> {
	final long sourceIndex;
	final long indexOfBlock;
	final int indexWithinBlock;
	final DataType data;
	final int dataCountPerBlock;
	int truncate = 0;
	public SourceData(int dataCountPerBlock, long sourceIndex, DataType data) {
		super();
		this.sourceIndex = sourceIndex;
		this.data = data;
		this.dataCountPerBlock = dataCountPerBlock;
		indexOfBlock = sourceIndex / this.dataCountPerBlock;
		indexWithinBlock = (int) (sourceIndex % this.dataCountPerBlock);
	}

	public int getIndex() {
		return indexWithinBlock;
	}

	public DataType getData() {
		return data;
	}

	public int getTruncate() {
		return truncate;
	}

	public void setTruncate(int truncate) {
		this.truncate = truncate;
	}
	

}
