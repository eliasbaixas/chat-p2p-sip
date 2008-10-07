/*
 * InstantMessagingUserAgent.java
 *
 * Created on July 28, 2002, 8:23 AM
 */

package gov.nist.sip.instantmessaging.presence;

import javax.sip.*;
import javax.sip.message.*;
import javax.sip.header.*;
import javax.sip.address.*;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import java.awt.event.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.text.ParseException;
import java.util.*;
import java.io.*;
import gov.nist.sip.instantmessaging.*;
import gov.nist.sip.instantmessaging.authentication.*;

/**
 * 
 * @author olivier
 * @version 1.0
 */
public class IMUserAgent implements SipListener {

	private Hashtable<String, Dialog> dialogTable;

	private SipStack sipStack;

	private MessageFactory messageFactory;

	private AddressFactory addressFactory;

	private HeaderFactory headerFactory;

	private SipProvider sipProvider;

	private InstantMessagingGUI imGUI;

	IMInviteProcessing imInviteProcessing;

	private IMRegisterProcessing imRegisterProcessing;

	private IMByeProcessing imByeProcessing;

	private IMSubscribeProcessing imSubscribeProcessing;

	private IMNotifyProcessing imNotifyProcessing;

	private IMInfoProcessing imInfoProcessing;

	IMMessageProcessing imMessageProcessing;

	private IMTimerTasks imTimerTasks;

	private Timer taskTimer;

	private ProxyAuthorizationHeader proxyAuthHeader;

	private PresenceManager presenceManager;

	private IMPublishProcessing imPublishProcessing;

	private RouteHeader routeToProxy;

	private static Logger logger = Logger.getLogger(IMUserAgent.class);

	static {
		PropertyConfigurator.configure("log4j.properties");

	}

	/** Creates new InstantMessagingUserAgent */
	public IMUserAgent(InstantMessagingGUI imGUI) {
		this.imGUI = imGUI;

		imInviteProcessing = new IMInviteProcessing(this);
		imRegisterProcessing = new IMRegisterProcessing(this);
		imByeProcessing = new IMByeProcessing(this);
		imSubscribeProcessing = new IMSubscribeProcessing(this);
		imNotifyProcessing = new IMNotifyProcessing(this);
		imInfoProcessing = new IMInfoProcessing(this);
		imMessageProcessing = new IMMessageProcessing(this);
		imPublishProcessing = new IMPublishProcessing(this);
		imTimerTasks = new IMTimerTasks(this, 0);

		this.dialogTable = new Hashtable<String, Dialog>();
		presenceManager = new PresenceManager(this);
		proxyAuthHeader = null;

	}

	// niepin
	// create a timer to execute the specified task periodically, e.g. polling
	// presences of buddies (sending SUBSCRIBE)
	// @code refers the task, @interval refers the delay of the operation (in
	// milliseconds)
	public void timerStart(int code, int interval) {

		if (taskTimer == null) {
			imTimerTasks.setTaskCode(code);
			taskTimer = new Timer(interval, imTimerTasks);
			taskTimer.start();
		} else {
			this.timerStop();
			if (imTimerTasks.getCurrentTaskCode() != code) {
				imTimerTasks.setTaskCode(code);
				taskTimer.setDelay(interval);
				taskTimer.restart();
			} else {
				taskTimer.setDelay(interval);
				taskTimer.restart();
			}
		}
		System.out.println("The task timer is started!!!");
	}

	public void timerStop() {
		if (taskTimer != null) {
			this.taskTimer.stop();
		}
		System.out.println("The task timer is stopped!!!");
	}

	public PresenceManager getPresenceManager() {
		return presenceManager;
	}

	public IMRegisterProcessing getIMRegisterProcessing() {
		return imRegisterProcessing;
	}

	public IMByeProcessing getIMByeProcessing() {
		return imByeProcessing;
	}

	public IMSubscribeProcessing getIMSubscribeProcessing() {
		return imSubscribeProcessing;
	}

	public IMNotifyProcessing getIMNotifyProcessing() {
		return imNotifyProcessing;
	}

	public IMInfoProcessing getIMInfoProcessing() {
		return imInfoProcessing;
	}

	public IMMessageProcessing getIMMessageProcessing() {
		return imMessageProcessing;
	}

	public IMPublishProcessing getIMPublishProcessing() {
		return imPublishProcessing;
	}

	public SipStack getSipStack() {
		return sipStack;
	}

	public SipProvider getSipProvider() {
		return sipProvider;
	}

	public MessageFactory getMessageFactory() {
		return messageFactory;
	}

	public HeaderFactory getHeaderFactory() {
		return headerFactory;
	}

	public AddressFactory getAddressFactory() {
		return addressFactory;
	}

	public ProxyAuthorizationHeader getProxyAuthorizationHeader() {
		return proxyAuthHeader;
	}

	// niepin
	public void subscribeBuddiesPresentity(Vector buddylist) {
		// subscribe presentity of all friends in the buddy list
		if (imGUI.getBuddyList() == null) {
			System.out
					.println("--------------Buddylist is null-----------------");
		}
		// Vector buddylist = imGUI.getBuddyList().getBuddies();
		System.out.println("--------------Buddylist is: "
				+ buddylist.toString());
		if (buddylist.size() > 0)
			imSubscribeProcessing.sendSubscribeToAllPresentities(buddylist,
					false);
	}

	public String getRouterPath() {
		ListenerInstantMessaging listenerIM = imGUI
				.getListenerInstantMessaging();
		ConfigurationFrame configurationFrame = listenerIM
				.getConfigurationFrame();
		String res = configurationFrame.getRouterPath();
		if (res == null || res.trim().equals("")) {
			return null;
		} else
			return res.trim();
	}

	public String getOutputFile() {
		ListenerInstantMessaging listenerIM = imGUI
				.getListenerInstantMessaging();
		ConfigurationFrame configurationFrame = listenerIM
				.getConfigurationFrame();
		String res = configurationFrame.getOutputFile();
		if (res == null || res.trim().equals("")) {
			return null;
		} else
			return res.trim();
	}

	public String getProxyAddress() {
		ListenerInstantMessaging listenerIM = imGUI
				.getListenerInstantMessaging();
		ConfigurationFrame configurationFrame = listenerIM
				.getConfigurationFrame();
		String res = configurationFrame.getOutboundProxyAddress();

		if (res == null || res.trim().equals("")) {

			return null;
		} else
			return res.trim();
	}

	public int getProxyPort() {
		ListenerInstantMessaging listenerIM = imGUI
				.getListenerInstantMessaging();
		ConfigurationFrame configurationFrame = listenerIM
				.getConfigurationFrame();
		String res = configurationFrame.getOutboundProxyPort();

		if (res == null || res.trim().equals("")) {
			return -1;
		} else {
			try {
				int i = Integer.valueOf(res).intValue();
				return i;
			} catch (Exception e) {
				return -1;
			}
		}
	}

	public String getRegistrarAddress() {
		ListenerInstantMessaging listenerIM = imGUI
				.getListenerInstantMessaging();
		ConfigurationFrame configurationFrame = listenerIM
				.getConfigurationFrame();
		String res = configurationFrame.getRegistrarAddress();
		if (res == null || res.trim().equals("")) {
			return null;
		} else
			return res.trim();
	}

	public int getRegistrarPort() {
		ListenerInstantMessaging listenerIM = imGUI
				.getListenerInstantMessaging();
		ConfigurationFrame configurationFrame = listenerIM
				.getConfigurationFrame();
		String res = configurationFrame.getRegistrarPort();
		if (res == null || res.trim().equals("")) {
			return -1;
		} else {
			try {
				int i = Integer.valueOf(res).intValue();
				return i;
			} catch (Exception e) {
				return -1;
			}
		}
	}

	/**
	 * Get the IM Address where the IM Client is listening.
	 * 
	 * @return IP Address where the IM Client is listening for messages inbound.
	 */
	public String getIMAddress() {
		ListenerInstantMessaging listenerIM = imGUI
				.getListenerInstantMessaging();
		ConfigurationFrame configurationFrame = listenerIM
				.getConfigurationFrame();
		String res = configurationFrame.getIMAddress();

		if (res == null || res.trim().equals("")) {
			return null;
		} else
			return res.trim();
	}

	/**
	 * Get the port where the IM Client is listening.
	 * 
	 * @return -- the port where the IM Client is listening.
	 */
	public int getIMPort() {
		ListenerInstantMessaging listenerIM = imGUI
				.getListenerInstantMessaging();
		ConfigurationFrame configurationFrame = listenerIM
				.getConfigurationFrame();
		String res = configurationFrame.getIMPort();

		if (res == null || res.trim().equals("")) {
			return -1;
		} else {
			try {
				int i = Integer.valueOf(res).intValue();
				return i;
			} catch (Exception e) {
				return -1;
			}
		}
	}

	public String getIMProtocol() {
		ListenerInstantMessaging listenerIM = imGUI
				.getListenerInstantMessaging();
		ConfigurationFrame configurationFrame = listenerIM
				.getConfigurationFrame();
		String res = configurationFrame.getIMProtocol();

		if (res == null || res.trim().equals("")) {
			return null;
		} else
			return res.trim();
	}

	public InstantMessagingGUI getInstantMessagingGUI() {
		return imGUI;
	}

	public RouteHeader getRouteToProxy() {

		return this.routeToProxy;

	}

	/** **************************************************************************** */
	/** ********** The methods for implementing the listener ************ */
	/** **************************************************************************** */

	public void processRequest(RequestEvent requestEvent) {
		try {

			Request request = requestEvent.getRequest();

			// Cloning is not needed here - we never forward the request.
			// Revisit this later.

			ServerTransaction serverTransaction = requestEvent
					.getServerTransaction();
			sipProvider = (SipProvider) requestEvent.getSource();

			logger.debug("\n\nRequest " + request.getMethod() + " received:\n");

			if (serverTransaction == null
					&& (request.getMethod().equals(Request.INVITE) || request
							.getMethod().equals(Request.SUBSCRIBE)))
				serverTransaction = sipProvider
						.getNewServerTransaction(request);

			if (request.getMethod().equals(Request.INVITE)) {
				imInviteProcessing.processInvite(requestEvent,
						serverTransaction);
			} else if (request.getMethod().equals(Request.ACK)) {
				imInviteProcessing.processAck(requestEvent, serverTransaction);
			} else if (request.getMethod().equals(Request.BYE)) {
				imByeProcessing.processBye(requestEvent, serverTransaction);
			} else if (request.getMethod().equals("MESSAGE")) {
				imMessageProcessing.processMessage(requestEvent,
						serverTransaction);
			} else if (request.getMethod().equals("INFO")) {
				imInfoProcessing.processInfo(request, serverTransaction);
			} else if (request.getMethod().equals("SUBSCRIBE")) {
				System.out
						.println("--------------SUBSCRIBE is received-----------------");
				imSubscribeProcessing.processSubscribe(request,
						serverTransaction);
			} else if (request.getMethod().equals("NOTIFY")) {
				System.out
						.println("--------------NOTIFY is received-----------------");
				imNotifyProcessing.processNotify(request, serverTransaction);
			} else {
				logger.debug("processRequest: 405 Method Not Allowed replied");

				Response response = messageFactory.createResponse(
						Response.METHOD_NOT_ALLOWED, request);
				serverTransaction.sendResponse(response);
			}
		} catch (Exception ex) {
			logger.error("Unexpected error ", ex);
		}
	}

	public ContactHeader createContactHeader() {
		ContactHeader contactHeader = null;
		try {
			SipURI sipUri = addressFactory.createSipURI(null, this
					.getIMAddress());
			sipUri.setPort(this.getIMPort());
			sipUri.setTransportParam("udp");
			Address contactAddress = this.getAddressFactory().createAddress(
					sipUri);
			contactHeader = headerFactory.createContactHeader(contactAddress);
		} catch (Exception ex) {
			logger.fatal("Unexpected exception ", ex);
			System.exit(0);
		}
		return contactHeader;

	}

	public void processResponse(ResponseEvent responseEvent) {
		Response response = responseEvent.getResponse();
		logger.debug("@@@ IMua processing response: " + response.toString());
		ClientTransaction clientTransaction = responseEvent
				.getClientTransaction();

		try {
			logger.debug("\n\nResponse " + response.getStatusCode() + " "
					+ response.getReasonPhrase() + " :\n" + response);

			Response responseCloned = (Response) response.clone();
			CSeqHeader cseqHeader = (CSeqHeader) responseCloned
					.getHeader(CSeqHeader.NAME);
			if (response.getStatusCode() == Response.OK
					|| response.getStatusCode() == Response.ACCEPTED) {
				if (cseqHeader.getMethod().equals("REGISTER")) {
					imRegisterProcessing.processOK(responseEvent);
				} else if (cseqHeader.getMethod().equals("MESSAGE")) {
					imMessageProcessing.processOK(responseCloned,
							clientTransaction);
				} else if (cseqHeader.getMethod().equals("BYE")) {
					imByeProcessing
							.processOK(responseCloned, clientTransaction);
				} else if (cseqHeader.getMethod().equals("SUBSCRIBE")) {
					imSubscribeProcessing.processOK(responseCloned,
							clientTransaction);
				} else if (cseqHeader.getMethod().equals("NOTIFY")) {
					imNotifyProcessing.processOk(responseCloned,
							clientTransaction);
				} else if (cseqHeader.getMethod().equals(Request.INVITE)) {
					imInviteProcessing.processResponse(responseEvent);
				}

			} else if (response.getStatusCode() == Response.NOT_FOUND
					|| response.getStatusCode() == Response.TEMPORARILY_UNAVAILABLE) {
				if (cseqHeader.getMethod().equals("SUBSCRIBE")) {
					new AlertInstantMessaging(
							"The presence server is not aware "
									+ "of the buddy you want to add.");
				} else {

					ListenerInstantMessaging listenerInstantMessaging = imGUI
							.getListenerInstantMessaging();
					ChatSessionManager chatSessionManager = listenerInstantMessaging
							.getChatSessionManager();
					ChatSession chatSession = null;
					String toURL = IMUtilities.getKey(response, "To");
					if (chatSessionManager.hasAlreadyChatSession(toURL)) {
						chatSession = chatSessionManager.getChatSession(toURL);
						chatSession.setExitedSession(true, "Contact not found");
					}
					/*
					 * new AlertInstantMessaging( "Your instant message could
					 * not be delivered..." + " The contact is not available!!!
					 * "+cseqHeader.getMethod().toString());
					 */
					this.imGUI.getListenerInstantMessaging().reSignIn();
				}
			} else if (response.getStatusCode() == Response.DECLINE
					|| response.getStatusCode() == Response.FORBIDDEN) {

				String fromURL = IMUtilities.getKey(response, "From");
				new AlertInstantMessaging("The contact " + fromURL
						+ " has rejected your subscription!!!");
			} else if (response.getStatusCode() == Response.SERVER_INTERNAL_ERROR) {
				// niepin
				// stop SUBSCRIBE task
				this.timerStop();
			} else {
				if (response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED
						|| response.getStatusCode() == Response.UNAUTHORIZED) {
					logger
							.debug("IMUserAgent, processResponse(), Credentials to "
									+ " provide!");
					// WE start the authentication process!!!
					// Let's get the Request related to this response:
					Request request = clientTransaction.getRequest();
					if (request == null) {
						logger
								.debug("IMUserAgent, processResponse(), the request "
										+ " that caused the 407 has not been retrieved!!! Return cancelled!");
					} else {
						Request clonedRequest = (Request) request.clone();
						// Let's increase the Cseq:
						cseqHeader = (CSeqHeader) clonedRequest
								.getHeader(CSeqHeader.NAME);
						cseqHeader.setSequenceNumber(cseqHeader
								.getSequenceNumber() + 1);

						// Let's add a Proxy-Authorization header:
						// We send the informations stored:
						AuthenticationProcess authenticationProcess = imGUI
								.getAuthenticationProcess();
						Header header = authenticationProcess
								.getHeader(response);

						if (header == null) {
							logger
									.debug("IMUserAgent, processResponse(), Proxy-Authorization "
											+ " header is null, the request is not resent");
						} else {
							clonedRequest.setHeader(header);

							ClientTransaction newClientTransaction = sipProvider
									.getNewClientTransaction(clonedRequest);

							newClientTransaction.sendRequest();
							logger
									.debug("IMUserAgent, processResponse(), REGISTER "
											+ "with credentials sent:\n"
											+ clonedRequest);

						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// niepin
	public void processTimeout(TimeoutEvent timeOutEvent) {
		int timeoutType = timeOutEvent.getTimeout().getValue();
		if (timeOutEvent.getClientTransaction() != null) {
			Request request = timeOutEvent.getClientTransaction().getRequest();
			String method = request.getMethod();
			System.out
					.println("-------------Timeout captured, client transaction, request method: "
							+ method);
			if (method.equals("SUBSCRIBE")) {
				this.timerStop();
			} else if (method.equals("REGISTER")) {

			}
		}

		// System.out.println("Timeout captured: "+timeOutEvent.toString());
	}

	/**
	 * *********************** Utilities
	 * *****************************************
	 */

	/*
	 * public static String getBuddyParsedPlusSIP(String buddy) { String
	 * result=null; try{ if (buddy.startsWith("sip:") ) { return buddy; } else
	 * return "sip:"+buddy; } catch(Exception e ) { return result; } }
	 * 
	 * public static String getBuddyParsedMinusSIP(String buddy) { String
	 * result=null; try{ if (buddy.startsWith("sip:") ) {
	 * result=buddy.substring(4); return result; } else return buddy; }
	 * catch(Exception e ) { return result; } }
	 */

	/***************************************************************************
	 * Methods for initiating, *************** * starting and stopping the User
	 * agent (Useful to integrate the User agent in other applications, like a
	 * GUI)
	 **************************************************************************/

	/**
	 * Start the proxy, this method has to be called after the init method
	 * throws Exception that which can be caught by the upper application
	 */
	public void start() throws Exception {
		sipStack = null;
		sipProvider = null;

		SipFactory sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");

		headerFactory = sipFactory.createHeaderFactory();
		addressFactory = sipFactory.createAddressFactory();
		messageFactory = sipFactory.createMessageFactory();
		try {
			SipURI uri = this.addressFactory.createSipURI(null, this
					.getProxyAddress());
			uri.setPort(this.getProxyPort());
			uri.setLrParam();
			Address address = this.addressFactory.createAddress(uri);
			this.routeToProxy = this.headerFactory.createRouteHeader(address);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error("Unexpected parse exception", e);
		}
		// Create SipStack object
		Properties properties = new Properties();

		if (getIMAddress() != null) {
			logger.debug("logger, the stack address is set to: "
					+ getIMAddress());
		} else {
			throw new Exception("ERROR, Specify the stack IP Address.");
		}

		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
				"./debug/debug_im_log.txt");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
				"./debug/server_im_log.txt");

		properties.setProperty("javax.sip.STACK_NAME", "nist-sip-im-client");

		if (getProxyAddress() != null && getProxyPort() != -1
				&& getIMProtocol() != null) {
			logger.debug("logger, the outbound proxy is set to: "
					+ getProxyAddress() + ":" + getProxyPort() + "/"
					+ getIMProtocol());
		} else
			logger.debug("WARNING, the outbound proxy is not set!");

		sipStack = sipFactory.createSipStack(properties);

		// We create the Listening points:
		if (getIMPort() == -1)
			throw new Exception("ERROR, the stack port is not set");

		if (getIMProtocol() == null)
			throw new Exception("ERROR, the stack transport is not set");

		ListeningPoint lp = sipStack.createListeningPoint(this.getIMAddress(),
				getIMPort(), getIMProtocol());
		logger.debug("logger, one listening point created: port:"
				+ lp.getPort() + ", " + " transport:" + lp.getTransport());
		sipProvider = sipStack.createSipProvider(lp);
		sipProvider.addSipListener(this);

		logger.debug("logger, Instant Messaging user agent ready to work");
	}

	/**
	 * Stop the User agent, this method has to be called after the start method
	 * throws Exception that which can be caught by the upper application
	 */
	public void stop() throws Exception {
		if (sipStack == null) {
			logger
					.debug("IM user agent has not been started, so nothing to stop!");
			return;
		}
		Iterator listeningPoints = sipStack.getListeningPoints();
		if (listeningPoints != null) {
			while (listeningPoints.hasNext()) {
				ListeningPoint lp = (ListeningPoint) listeningPoints.next();
				sipStack.deleteListeningPoint(lp);
				logger.debug("One listening point removed!");
			}
			logger.debug("IM user agent stopped");
		} else {
			logger
					.debug("IM user agent has not been started, so nothing to stop!");
		}
	}

	/** **************************************************************************** */
	/** ********** The main method: to launch the proxy ************ */
	/** **************************************************************************** */

	public static void main(String args[]) {
		try {
			// the InstantMessagingUserAgent:
			IMUserAgent instantMessagingUserAgent = new IMUserAgent(null);

			instantMessagingUserAgent.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processIOException(IOExceptionEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void processDialogTerminated(DialogTerminatedEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void setDialog(String callId, Dialog dialog) {
		this.dialogTable.put(callId, dialog);

	}

}
