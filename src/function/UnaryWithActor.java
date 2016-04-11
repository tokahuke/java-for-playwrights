package function;

import dsl.Actor;

@FunctionalInterface public interface UnaryWithActor<T, P extends Actor<T>> {
	T get(P performer, T t);
}

