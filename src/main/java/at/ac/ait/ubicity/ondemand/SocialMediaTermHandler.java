/**
    Copyright (C) 2014  AIT / Austrian Institute of Technology
    http://www.ait.ac.at

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see http://www.gnu.org/licenses/agpl-3.0.html
 */
package at.ac.ait.ubicity.ondemand;

import static org.elasticsearch.rest.RestRequest.Method.GET;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

import at.ac.ait.ubicity.commons.jit.Action;
import at.ac.ait.ubicity.commons.jit.Answer;
import at.ac.ait.ubicity.commons.jit.Answer.Status;
import at.ac.ait.ubicity.commons.util.PropertyLoader;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 *
 * @author jan van oort
 */
public class SocialMediaTermHandler implements RestHandler {

	private static final Logger logger = Logger.getLogger(SocialMediaTermHandler.class);

	private static String HOST;
	private static int PORT;
	Client client = Client.create();

	static {
		PropertyLoader config = new PropertyLoader(SocialMediaTermHandler.class.getResource("/ondemand.cfg"));

		HOST = config.getString("plugins.ondemand.http_host");
		PORT = config.getInt("env.http.endpoint_port");
	}

	@Inject
	public SocialMediaTermHandler(RestController restController) {
		restController.registerHandler(GET, "/_jitindex", this);
	}

	@Override
	public void handleRequest(RestRequest rr, RestChannel rc) {
		Action action = buildCommandFrom(rr);

		Answer answer = dispatch(action);
		rc.sendResponse(new BytesRestResponse(RestStatus.ACCEPTED, answer.toString()));
	}

	public Action buildCommandFrom(RestRequest rr) {
		String receiver = rr.param("m");
		String command = rr.param("cmd");
		String data = rr.param("q");

		logger.info("Data received: receiver: " + receiver + ", command: " + command + ", data: " + data);

		if (receiver != null && command != null && data != null) {
			data = data.replace("(", "").replace(")", "").toLowerCase();
			return new Action(receiver, command, data);
		}

		return null;
	}

	/**
	 * Send action to Ubicity REST Plugin.
	 * 
	 * @param action
	 * @return
	 */
	private Answer dispatch(Action action) {

		String endpoint = action.getReceiver() + "?cmd=" + action.getCommand() + "&data=" + action.getData();
		WebResource webResource = client.resource(HOST + ":" + PORT + "/command/" + endpoint);

		ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		return new Answer(action, Status.PROCESSED, response.getEntity(String.class));
	}
}
