package dsl;

class Pair<T, U> {
	private final T a;
	private final U b;
	
	public Pair(T a, U b) {
		this.a = a;
		this.b = b;
	}
	
	@Override public boolean equals(Object other) {
		if (other instanceof Pair) {
			return ((Pair<?, ?>)other).a.equals(a)
					&& ((Pair<?, ?>)other).b.equals(b);
		} else {
			return false;
		}
	}
	
	@Override public int hashCode() {
		return a.hashCode() + b.hashCode();
	}
}
