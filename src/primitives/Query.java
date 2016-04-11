 package primitives;

import java.util.function.UnaryOperator;

import communications.CommunicationResource;
import dsl.Actor;
import dsl.Play;

public class Query {

	private static class QueryPlay<PayloadT> extends Play<PayloadT> {
		public static final String CLIENT = "client";
		public static final String SERVER = "server";

		private final String queryName;
		private UnaryOperator<PayloadT> process;
		private StatelessCharacter client, server;

		@Override public void dramatisPersonae() {
			client = new StatelessCharacter(CLIENT);
			server = new StatelessCharacter(SERVER);
		}

		@Override public void scene() {
			client.query(server, (me, msg) -> process.apply(msg), queryName);
		}

		public QueryPlay(String queryName, UnaryOperator<PayloadT> process) {
			super();
			this.queryName = queryName;
			this.process = process;
		}

		public QueryPlay(String queryName) {
			this(queryName, null);
		}
	}

	public static class Client<PayloadT> {
		private Actor<PayloadT> client;
		private final String queryName;

		public Client(CommunicationResource<PayloadT> communicationsResource,
				String queryName, String serverAddress) {
			this.queryName = queryName;
			client = new Actor<PayloadT>(new QueryPlay<PayloadT>(
					queryName).interpretAs(QueryPlay.CLIENT),
					communicationsResource);
			
			client.setInitialAddress(QueryPlay.SERVER, serverAddress);
		}

		public PayloadT query(PayloadT query) {
			client.setMessage(queryName + "Query", query);
			client.perform();
			return client.getMessage(queryName + "Response");
		}
	}

	public static class Server<PayloadT> {
		private dsl.Server<PayloadT> server;

		public Server(CommunicationResource<PayloadT> communicationsResource,
				String queryName, UnaryOperator<PayloadT> process) {
			server = new dsl.Server<PayloadT>(new QueryPlay<PayloadT>(
					queryName, process).interpretAs(QueryPlay.SERVER),
					communicationsResource, Actor::new, 4);

			server.start();
		}
	}
}
