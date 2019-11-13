package unlimited.core.io.data;

import java.io.IOException;

public interface DataProducer<DataType> {
	public  SourceData<DataType> produce() throws IOException;
}
