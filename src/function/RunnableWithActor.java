package function;

import dsl.Actor;


@FunctionalInterface public interface RunnableWithActor<P extends Actor<?>> {
	public void run(P inst);
}