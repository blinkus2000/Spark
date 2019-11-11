package unlimited.core.io.data;

import java.io.IOException;

public interface DataConsumer<DataType> {
	void consume(DataType d) throws IOException ;
}
