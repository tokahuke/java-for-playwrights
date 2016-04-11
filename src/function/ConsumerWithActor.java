package function;

import dsl.Actor;

@FunctionalInterface public interface ConsumerWithActor<T, P extends Actor<T>> {
	public void accept(P performer, T t);
}
