package segmentation;

import java.util.BitSet;

import communications.RxException;

import dsl.Play;

public class SimpleTransfer extends Play<byte[]>{
	private Character<Buffered> sender;
	private Character<Buffered> receiver;
	
	public SimpleTransfer() {
		super.protocolName = "muST";
	}
	
	@Override public void dramatisPersonae() {
		sender = new Character<Buffered>(Buffered.class, "sender");
		receiver = new Character<Buffered>(Buffered.class, "receiver");
	}

	@Override public void scene() {
		sender.send(receiver, snd -> Util.inBytes(snd.getBulk().length), (rec,
				size) -> rec.setBulk(new byte[Util.toInt(size)]), "size");
		
		while (sender.test(snd -> !snd.done())) {
			sender.run(snd -> snd.stackMissing());

			try {
				while (sender.test(snd -> !snd.oneToGo())) {
					sender.send(receiver, snd -> snd.nextChunk(),
							(rec, chk) -> rec.cat(chk), "chunk", 50);
				}

				sender.send(receiver, snd -> snd.nextChunk(),
						(rec, chk) -> rec.cat(chk), "last", 50);
			} catch (RxException e) {}

			if (receiver.test(rec -> rec.isMissing())) {
				receiver.send(sender, rec -> rec.arrived.toByteArray(), (snd,
						arr) -> {
					snd.arrived = BitSet.valueOf(arr);
				}, "nack", 50);
			} else {
				receiver.send(sender, "ack", 50);
				
				sender.run(snd -> snd.nextSegment());
				receiver.run(rec -> rec.nextSegment());
			}
		}
		
		sender.send(receiver, "end");
	}
}
  