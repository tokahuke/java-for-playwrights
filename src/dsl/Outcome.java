package dsl;

import communications.RxException;
import communications.TxException;


public enum Outcome {
	TRUE, FALSE, OK, NO_SEND, NO_RECEIVE;
	
	public static Boolean effectOf(Outcome outcome, TxException noSend,
			RxException noReceive) {
		switch(outcome) {
			case FALSE:
				return false;
			case NO_RECEIVE:
				throw noReceive;
			case NO_SEND:
				throw noSend;
			case OK:
				return null;
			case TRUE:
				return true;
		}
		
		return null;
	}
}