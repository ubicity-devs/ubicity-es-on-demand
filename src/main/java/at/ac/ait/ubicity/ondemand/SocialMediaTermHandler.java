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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.StringRestResponse;

import at.ac.ait.ubicity.commons.protocol.Answer;
import at.ac.ait.ubicity.commons.protocol.Command;
import at.ac.ait.ubicity.commons.protocol.Control;
import at.ac.ait.ubicity.commons.protocol.Media;
import at.ac.ait.ubicity.commons.protocol.Term;
import at.ac.ait.ubicity.commons.protocol.Terms;

/**
 *
 * @author jan van oort
 */
public class SocialMediaTermHandler implements RestHandler {

	private static final Logger logger = Logger
			.getLogger(SocialMediaTermHandler.class);

	private static String HOST;
	private static int PORT;
	private static int TIMEOUT;

	static {
		try {
			// set necessary stuff for us to ueberhaupt be able to work
			Configuration config = new PropertiesConfiguration(
					SocialMediaTermHandler.class.getResource("/ondemand.cfg"));
			HOST = config.getString("plugins.ondemand.reverse_cac_host");
			PORT = config.getInt("plugins.ondemand.reverse_cac_port");
			TIMEOUT = config.getInt("plugins.ondemand.reverse_cac_timeout");

		} catch (ConfigurationException noConfig) {
			logger.fatal("Configuration not found! " + noConfig.toString());
		}
	}

	@Inject
	public SocialMediaTermHandler(RestController restController) {
		restController.registerHandler(GET, "/_jitindex", this);
	}

	@Override
	public void handleRequest(RestRequest rr, RestChannel rc) {
		Command _command = buildCommandFrom(rr);

		logger.info("q=" + rr.param("q") + " &m=" + rr.param("m") + " &c="
				+ rr.param("c"));

		Answer _a = dispatch(_command);
		StringBuilder sb = new StringBuilder();
		sb.append("for your request : ").append("\n")
				.append(_command.toRESTString() + "\n")
				.append("the server returned the following answer\n")
				.append("http ");

		int _code = _a.getCode();

		switch (_code) {
		case Answer.HTTP_ACCEPTED:
			sb.append(Answer.HTTP_ACCEPTED);
			rc.sendResponse(new StringRestResponse(RestStatus.ACCEPTED, sb
					.toString()));
		case Answer.HTTP_OK:
			sb.append(Answer.HTTP_OK);
			rc.sendResponse(new StringRestResponse(RestStatus.OK, sb.toString()));
		case Answer.HTTP_BAD_REQUEST:
			sb.append(Answer.HTTP_BAD_REQUEST);
			rc.sendResponse(new StringRestResponse(RestStatus.BAD_REQUEST, sb
					.toString()));
		case Answer.HTTP_UNAVAILABLE:
			sb.append(Answer.HTTP_UNAVAILABLE);
			rc.sendResponse(new StringRestResponse(
					RestStatus.SERVICE_UNAVAILABLE, sb.toString()));
		case Answer.HTTP_SERVERSIDE_ERROR:
			sb.append(Answer.HTTP_SERVERSIDE_ERROR);
			rc.sendResponse(new StringRestResponse(
					RestStatus.INTERNAL_SERVER_ERROR, sb.toString()));
		case Answer.HTTP_UNSPECIFIED_ERROR:
			sb.append(Answer.HTTP_UNSPECIFIED_ERROR);
			sb.append("\nContact your server administrator, you are the probable victim of a server-side bug");
			sb.append("\nalternatively or jointly, you may file a bug report at https://github.com/ubicity-principal/ubicity-core/issues");
			rc.sendResponse(new StringRestResponse(
					RestStatus.INTERNAL_SERVER_ERROR, sb.toString()));
		}

	}

	public static Command buildCommandFrom(RestRequest rr) {
		logger.info("building command from " + rr.toString());
		Terms _terms = null;
		Media _media = null;
		Control _control = null;

		String __termString = rr.param("q");
		if (!(null == __termString || __termString.equals(""))) {
			_terms = new Terms();
			__termString = __termString.replace("(", "").replace(")", "")
					.toLowerCase();
			String[] __termStrings = __termString.split(" ");
			for (String ___term : __termStrings) {
				Term t = new Term(___term);
				_terms.get().add(t);
			}

		}

		String __mediaString = rr.param("m");
		if (!(null == __mediaString || __mediaString.equals(""))) {
			_media = new Media();
			__mediaString = __mediaString.replace("(", "").replace(")", "")
					.toLowerCase();
			String[] __mediaStrings = __mediaString.split(" ");
			for (String __medium : __mediaStrings) {
				_media.get().add(Media.knownMedia.get(__medium.toLowerCase()));
			}
		}
		String _controlString = rr.param("c");
		if (!(null == _controlString)) {
			_control = Control.knownControls.get(_controlString.toLowerCase());
		}

		return new Command(_terms, _media, _control);
	}

	private Answer dispatch(Command _command) {
		try {
			Socket _s = new Socket(HOST, PORT);
			ObjectInputStream in = null;
			ObjectOutputStream out = new ObjectOutputStream(
					_s.getOutputStream());
			out.writeObject(_command);
			out.flush();
			logger.info("Wrote " + _command.toRESTString() + " to "
					+ _s.getInetAddress());

			long _startTime = System.currentTimeMillis();

			while ((System.currentTimeMillis() - _startTime) < TIMEOUT) {
				try {
					in = new ObjectInputStream(_s.getInputStream());
					Object _a = in.readObject();
					Answer a = (Answer) _a;

					logger.info("Got an Answer for Command "
							+ _command.toRESTString() + " :: " + a.getCode());
					return a;
				} catch (IOException | ClassNotFoundException ioex) {
					logger.fatal(
							"IOException while trying to read an answer for dispatch : ",
							ioex);
				} finally {
					try {
						in.close();
						out.close();
						_s.close();
					} catch (Throwable t) {
						;
					} finally {
						in = null;
						out = null;
						_s = null;
					}
				}
			}

		} catch (IOException ex) {
			logger.fatal("IOException while trying to dispatch : ", ex);
		}
		// this should never happen:
		return Answer.NIL;
	}
}
