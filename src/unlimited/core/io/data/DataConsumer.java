package unlimited.core.io.data;

import java.io.IOException;

public interface DataConsumer<DataType> {
	void consume(int index, DataType d) throws IOException ;
}
