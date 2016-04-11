package function;

import dsl.Actor;


@FunctionalInterface public interface TestableWithActor<P extends Actor<?>> {
	public boolean test(P performer);
}
