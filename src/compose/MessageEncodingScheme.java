package compose;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.Base64;
import java.util.function.Function;

import communications.ShortMessage;


public interface MessageEncodingScheme<P, M> {
	public M encode(ShortMessage<P> msg) throws ParseException;
	public ShortMessage<P> decode(M msg) throws ParseException;
	
	public static <P> MessageEncodingScheme<P, ShortMessage<P>> getTrivialScheme() {
		return new MessageEncodingScheme<P, ShortMessage<P>>() {
			
			@Override public ShortMessage<P> encode(ShortMessage<P> msg)
					throws ParseException {
				return msg;
			}
			
			@Override public ShortMessage<P> decode(ShortMessage<P> msg)
					throws ParseException {
				return msg;
			}
		};
	}
	
	public static <P, M> MessageEncodingScheme<P, M> forLambda(
			Function<ShortMessage<P>, M> encode,
			Function<M, ShortMessage<P>> decode) {
		return new MessageEncodingScheme<P, M>() {
			
			@Override public ShortMessage<P> decode(M msg) {
				return decode.apply(msg);
			}	
			
			@Override public M encode(ShortMessage<P> msg) {
				return encode.apply(msg);
			}
		};
	}
	
	public static <P> MessageEncodingScheme<P, byte[]> getSerializationScheme() {
		return new MessageEncodingScheme<P, byte[]>() {
			
			@Override public byte[] encode(ShortMessage<P> msg)
					throws ParseException {
				try (ByteArrayOutputStream boss = new ByteArrayOutputStream();
						ObjectOutputStream out = new ObjectOutputStream(boss)) {
					out.writeObject(msg);
					return boss.toByteArray();
				} catch (IOException e) {
					throw new ParseException(String.format("IO exception: %s",
							e.getMessage()), 0);
				}
			}
			
			@SuppressWarnings("unchecked")
			@Override public ShortMessage<P> decode(byte[] msg)
					throws ParseException {
				try (ByteArrayInputStream bis = new ByteArrayInputStream(msg);
						ObjectInputStream in = new ObjectInputStream(bis)) {
					return (ShortMessage<P>) in.readObject();
				} catch (IOException e) {
					throw new ParseException(String.format("IO exception: %s",
							e.getMessage()), 0);
				} catch (ClassCastException e) {
					throw new ParseException(
							"Cast error: classes do not match.", 0);
				} catch (ClassNotFoundException e) {
					throw new ParseException("Cast error: class not found.", 0);
				}
			}
		};
	}
	
	public static <P> MessageEncodingScheme<P, String> getBase64Scheme() {
		return new MessageEncodingScheme<P, String>() {
			@Override public String encode(ShortMessage<P> msg)
					throws ParseException {
				try (ByteArrayOutputStream boss = new ByteArrayOutputStream();
						ObjectOutputStream out = new ObjectOutputStream(boss)) {
					out.writeObject(msg);
					return Base64.getUrlEncoder().encodeToString(
							boss.toByteArray());
				} catch (IOException e) {
					throw new ParseException(String.format("IO exception: %s",
							e.getMessage()), 0);
				}
			}
			
			@SuppressWarnings("unchecked")
			@Override public ShortMessage<P> decode(String msg)
					throws ParseException {
				try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64
						.getUrlDecoder().decode(msg));
						ObjectInputStream in = new ObjectInputStream(bis)) {
					return (ShortMessage<P>) in.readObject();
				} catch (IOException e) {
					throw new ParseException(String.format("IO exception: %s",
							e.getMessage()), 0);
				} catch(IllegalArgumentException e) {
					throw new ParseException("Invalid base 64 scheme.", 0);
				} catch (ClassCastException e) {
					throw new ParseException(
							"Cast error: classes do not match.", 0);
				} catch (ClassNotFoundException e) {
					throw new ParseException("Cast error: class not found.", 0);
				}
			}
		};
	}
}
