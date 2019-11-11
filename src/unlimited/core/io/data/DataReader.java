package unlimited.core.io.data;

import java.util.concurrent.ConcurrentHashMap;

public abstract class DataReader<DataType> {
	final ConcurrentHashMap<Long,DataType> dataMap = new ConcurrentHashMap<>();
	public DataType get(Long index) {
		return dataMap.get(index);
	}
	public void put(Long index, DataType data) {
		dataMap.put(index,data);
	}
	public void markSent(Long index) {
		dataMap.remove(index);
	}
}
