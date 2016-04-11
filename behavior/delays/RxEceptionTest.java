package delays;

import communications.RxException;
import dsl.Play;

public class RxEceptionTest extends Play<String> {

	StatelessCharacter ch1, ch2;
	
	@Override public void dramatisPersonae() {
		ch1 = new StatelessCharacter("ch1");
		ch2 = new StatelessCharacter("ch2");
	}

	@Override public void scene() {
		try {
			ch1.send(ch2, "msg");
		} catch (RxException e) {
			ch2.send(ch1, "error");
			ch1.send(ch2, "msg2");
		}
		
		ch2.send(ch1, "ok");
	}

	public static void main(String[] args) {
		new RxEceptionTest().interpretAs("ch1");
		System.out.println("========");
		new RxEceptionTest().interpretAs("ch2");
	}

}
