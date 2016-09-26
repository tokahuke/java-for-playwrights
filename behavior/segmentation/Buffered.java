package segmentation;

import java.util.BitSet;

import dsl.Actor;


class Buffered extends Actor<byte[]> {
	public static final int chunkSize = 180 * 8;
	public static final int segmentSize = 32;
	
	private byte[] bulk;
	private byte[] stack = new byte[segmentSize];
	private short ptr;
	private int pos;
	private short segmentId = 0;
	public BitSet arrived = new BitSet(segmentSize);
	private boolean done = false;
	
	public byte[] getBulk() {
		return bulk;
	}
	
	public void setBulk(byte[] bulk) {
		ptr = 0;
		this.bulk = bulk;
		
		for (int i = 0; i < segmentSize; i++) {
			arrived.set(i);
		}
	}
	
	/* package-private */byte[] nextChunk() {
		byte chunkId = stack[--ptr];
		byte[] chunk = new byte[chunkSize + 1];
		
		pos = (chunkId + (segmentId * segmentSize)) * chunkSize;
		
		if (pos + chunkSize < bulk.length) {
			System.arraycopy(bulk, pos, chunk, 1, chunkSize);
			chunk[0] = chunkId;
		} else {
			done = true;
			System.arraycopy(bulk, pos, chunk, 1, bulk.length - pos);
		}
		
		return chunk;
	}
	
	/* package-private */void cat(byte[] chunk) {
		if (chunk != null && arrived.get(chunk[0])) {
			byte chunkId = chunk[0];
			
			arrived.clear(chunkId);
			
			try {
				System.arraycopy(chunk, 1, bulk,
						(chunkId + (segmentId * segmentSize)) * chunkSize,
						chunkSize);
			} catch (ArrayIndexOutOfBoundsException e) {}
		}
	}
	
	public void stackMissing() {
		int maxId = (bulk.length / chunkSize) % segmentSize;
		int maxSeg = bulk.length / chunkSize / segmentSize;
		
		for (int i = (segmentId == maxSeg ? maxId : segmentSize - 1); i > -1; i--) {
			if (arrived.get(i)) {
				stack[ptr++] = (byte) i;
			}
		}
	}
	
	public boolean oneToGo() {
		return ptr == 1;
	}
	
	/* package-private */boolean done() {
		return done;
	}
	
	public boolean isMissing() {
		return !arrived.isEmpty();
	}
	
	public void nextSegment() {
		segmentId++;
		
		for (int i = 0; i < segmentSize; i++) {
			arrived.set(i);
		}
	}
}

class Util {
	static public byte[] inBytes(int i) {
		byte[] arr = new byte[4];
		
		arr[3] = (byte) (i >> 24);
		arr[2] = (byte) (i >> 16);
		arr[1] = (byte) (i >> 8);
		arr[0] = (byte) (i /* >> 0 */);
		
		return arr;
	}
	
	static public int toInt(byte[] arr) {
		int res = ((int) arr[0]) + (((int) arr[1]) << 8)
				+ (((int) arr[2]) << 16) + (((int) arr[3]) << 24);
		return res;
	}
}