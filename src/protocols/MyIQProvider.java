package protocols;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class MyIQProvider implements IQProvider {
	public IQ parseIQ(XmlPullParser parser) throws Exception {
		String name;
		MyIQ pkiMessage = null;
		
		while (parser.next() == XmlPullParser.START_TAG) {
			name = parser.getName();
			if(parser.getEventType() == XmlPullParser.START_TAG)
				pkiMessage = new MyIQ(name, parser.nextText());
		}
		
		return pkiMessage;
	}
}