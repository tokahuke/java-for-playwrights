package function;

import dsl.Actor;

@FunctionalInterface public interface SupplierWithActor<T, P extends Actor<T>> {
	T get(P performer);
}
