package function;

import java.util.function.Function;

public class Conversion<X, Y> {
	
	static public <X> Conversion<X, X> getIdentity() {
		return new Conversion<X, X>() {
			@Override public X convert(X x) {
				return x;
			}
			
			@Override public X revert(X x) {
				return x;
			}
		};
	}
	
	private Function<X, Y> x2y;
	private Function<Y, X> y2x;
	
	public Conversion(Function<X, Y> x2y, Function<Y, X> y2x) {
		this.x2y = x2y;
		this.y2x = y2x;
	}
	
	protected Conversion() {}
	
	public Y convert(X x) {
		return x2y.apply(x);
	}
	
	public X revert(Y y) {
		return y2x.apply(y);
	}
	
	public Conversion<Y, X> getInverse() {
		return new Conversion<Y, X>(y2x, x2y);
	}
}
