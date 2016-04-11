package delays;

import communications.TxException;
import dsl.Play;

public class DelayTest extends Play<String> {

	StatelessCharacter ch1, ch2, ch3, ch4;
	
	@Override public void dramatisPersonae() {
		ch1 = new StatelessCharacter("ch1");
		ch2 = new StatelessCharacter("ch2");
		ch3 = new StatelessCharacter("ch3");
		ch4 = new StatelessCharacter("ch4");
	}

	@Override public void scene() {
		ch1.send(ch2, "m12", 100);
		ch2.send(ch3, "m23", 150);
		
		ch2.send(ch1, "m21", 200);
		
		try {
			ch3.send(ch4, "m31_t", 200);
		} catch (TxException e) {
			ch3.send(ch4, "m31_f", 100);
		}
		
		if (ch4.test(c4 -> true)) {
			ch4.send(ch2, "m42",  10);
			ch2.send(ch3, "m24", 20);
			ch3.send(ch4, "m34", 10);
		}
		
		ch4.send(ch1, "m41", 100);
	}
	
	public static void main(String[] args) {
		new DelayTest().interpretAs("ch1");
		System.out.println("End!");
	}

}
