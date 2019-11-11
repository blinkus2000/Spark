package unlimited.core.io.data;

public class PacketWrapper {
	protected final long packetIndex;
	protected final long blockIndex;
	public PacketWrapper(long packetIndex, long blockIndex) {
		super();
		this.packetIndex = packetIndex;
		this.blockIndex = blockIndex;
	}
	
}
