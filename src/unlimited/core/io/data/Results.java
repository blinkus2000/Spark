package unlimited.core.io.data;

public class Results {
	final Exception error;

	public Results(Exception error) {
		super();
		this.error = error;
	}
	public boolean isSuccessFull() {
		return error == null;
	}
	public Exception getError() {
		return error;
	}
}
