package compose;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import communications.MessageLimboException;
import communications.TxException;
import dsl.Actor;


public class SendingActor<M, P> extends Actor<P> {
	
	private BlockingQueue<Receipt<M>> receiptQueue =
			new LinkedBlockingQueue<Receipt<M>>();
	private Receipt<M> currentReceipt = null;
	
	/* package-private */ void put(M msg) throws InterruptedException {
		Receipt<M> receipt;
		Throwable outcome;
		
		receiptQueue.put(receipt = new Receipt<M>(msg));
		
		outcome = receipt.getOutcome();

		if (outcome != null) {
			throw new TxException(outcome);
		}
	}
	
	public M take() {
		try {
			if (currentReceipt != null) {
				currentReceipt.setOutcome(null);
			}
			
			currentReceipt = receiptQueue.take();
			
			currentReceipt.getValue();
			return currentReceipt.getValue();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			currentReceipt.setOutcome(e);
			return null;
		}
	}
	
	public void setException(Throwable throwable) {
		if (currentReceipt != null) {
			currentReceipt.setOutcome(throwable);
		}
	}
	
	/*package-private*/ void setFinalException(Throwable throwable) {
		if (currentReceipt != null) {
			currentReceipt.setOutcome(throwable);
			
			if (throwable == null) {
				for (Receipt<M> funny : receiptQueue) {
					funny.setOutcome(new MessageLimboException());
				}
			} else {
				for (Receipt<M> funny : receiptQueue) {
					funny.setOutcome(throwable);
				}
			}
		}
	}
}