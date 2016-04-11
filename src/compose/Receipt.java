package compose;

class Receipt<V> {	
	private V value;
	private Throwable outcome;
	private boolean outcomeSet = false;
	
	public Receipt(V value) {
		this.setValue(value);
	}
	
	synchronized void setValue(V value) {
		this.value = value;
	}
	
	synchronized V getValue() throws InterruptedException {
		return value;
	}
	
	synchronized void setOutcome(Throwable outcome) {
		this.outcome = outcome;
		outcomeSet = true;
		notify();
	}
	
	synchronized Throwable getOutcome() throws InterruptedException {
		while (!outcomeSet) {
			wait();
		}
		
		return outcome;
	}
}