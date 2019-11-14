package unlimited.core.util.stream;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class StreamUtilTest {
	int values = 0;
	LinkedList<Integer> indexListWasCreated;
	@Before
	public void setUp() throws Exception {
		values = 0;
		indexListWasCreated = new LinkedList<>();
	}

	@Test
	public void test() {
		StreamUtil.collapseWithFunction(
				Stream.generate(()->values++).limit(5),this::addToList,this::mapToList).forEach(System.out::println);
		indexListWasCreated.forEach(System.out::println);
	}
	boolean addToList(Integer val, LinkedList<Integer> list) {
		list.add(val);
		return list.size()<2;
	}
	LinkedList<Integer> mapToList(Integer index){
		indexListWasCreated.add(index);
		return new LinkedList<>();
	}
}
