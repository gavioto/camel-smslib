package net.frontlinesms.camel.smslib;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.smslib.COutgoingMessage;
import org.smslib.CService;

import serial.mock.MockSerial;
import serial.mock.NoSuchPortException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Unit test for {@link SmslibService} */
public class SmslibServiceTest {
	CService cServiceMock;
	CServiceFactory cServiceFactory;
	SmslibMessageTranslator translator;

	Map<String, Object> parameters;
	String remaining;
	String uri;
	
	SmslibService service;
	
	@Before
	public void setUp() {
		MockSerial.init();

		uri = "asdf";
		remaining = "hjkl";
		parameters = Collections.emptyMap();
		
		cServiceMock = mock(CService.class);
		
		cServiceFactory = mock(CServiceFactory.class);
		when(cServiceFactory.create(uri, remaining, parameters)).thenReturn(cServiceMock);
		
		service = new SmslibService(cServiceFactory, uri, remaining, parameters);

		translator = mock(SmslibMessageTranslator.class);
		service.setTranslator(translator);
	}
	
	@Test
	public void testCServiceCreation() throws NoSuchPortException {
		verify(cServiceFactory).create(uri, remaining, parameters);
	}
	
	@Test
	public void testCServiceStart() throws Exception {
		// given
		SmslibServiceUser mockUser = mock(SmslibServiceUser.class);
		
		// when
		service.startFor(mockUser);
		
		// then
		verify(cServiceMock).connect();
	}
	
	@Test
	public void testCServiceNoStartIfAlreadyStarted() throws Exception {
		// given
		SmslibServiceUser mockUser = mock(SmslibServiceUser.class);
		when(cServiceMock.isConnected()).thenReturn(true); // TODO this assumes that CService.isConnected() returns true while TRYING to conenct...
		
		// when
		service.startFor(mockUser);
		
		// then
		verify(cServiceMock).connect();
	}
	
	@Test
	public void testCServiceStop() throws Exception {
		// given
		SmslibServiceUser mockUser = mock(SmslibServiceUser.class);
		when(cServiceMock.isConnected()).thenReturn(true);
		
		// when
		service.stopFor(mockUser);
		
		// verify
		verify(cServiceMock).disconnect();
	}
	
	@Test
	public void testCServiceNoStopIfNotStarted() throws Exception {
		// given
		SmslibServiceUser mockUser = mock(SmslibServiceUser.class);
		
		// when
		service.stopFor(mockUser);
		
		// verify
		verify(cServiceMock, never()).disconnect();
	}
	
	@Test
	public void testCServiceNoStopIfOtherUsers() throws Exception {
		// given
		SmslibServiceUser mockUser1 = mock(SmslibServiceUser.class);
		service.startFor(mockUser1);
		SmslibServiceUser mockUser2 = mock(SmslibServiceUser.class);
		service.startFor(mockUser2);
		when(cServiceMock.isConnected()).thenReturn(true);
		
		// when
		service.stopFor(mockUser1);
		
		// verify
		verify(cServiceMock, never()).disconnect();
	}
	
	@Test
	public void testCServiceStopIfNoMoreUsers() throws Exception {
		// given
		SmslibServiceUser mockUser1 = mock(SmslibServiceUser.class);
		service.startFor(mockUser1);
		SmslibServiceUser mockUser2 = mock(SmslibServiceUser.class);
		service.startFor(mockUser2);
		when(cServiceMock.isConnected()).thenReturn(true);
		
		// when
		service.stopFor(mockUser1);
		verify(cServiceMock, never()).disconnect();
		
		// and
		service.stopFor(mockUser2);
		
		// verify
		verify(cServiceMock).disconnect();
	}
	
	@Test
	public void testSend() throws Exception {
		// given
		SmslibCamelMessage smslibCamelMessageMock = mock(SmslibCamelMessage.class);
		COutgoingMessage cOutgoingMessageMock = mock(COutgoingMessage.class);
		when(translator.translateOutgoing(smslibCamelMessageMock)).thenReturn(cOutgoingMessageMock);
		
		// when
		service.send(smslibCamelMessageMock);
		
		// then
		verify(translator).translateOutgoing(smslibCamelMessageMock);
		verify(cServiceMock).sendMessage(cOutgoingMessageMock);
	}
	
	@Test
	public void testSendFailureHandlingForMessageTranslateFailure() throws Exception {
		// given
		SmslibCamelMessage camelMessage = mock(SmslibCamelMessage.class);
		when(translator.translateOutgoing(camelMessage)).thenThrow(new TranslateException());
		
		// when then
		try {
			service.send(camelMessage);
			fail();
		} catch(TranslateException ex) {
			// expected
		}
	}
	
	@Test
	public void testSendFailureHandlingForMessageRejectedByCservice() throws Exception {
		// given
		SmslibCamelMessage camelMessage = mock(SmslibCamelMessage.class);
		COutgoingMessage smslibMessage = mock(COutgoingMessage.class);
		when(translator.translateOutgoing(camelMessage)).thenReturn(smslibMessage);
		doThrow(new MessageRejectedException()).when(cServiceMock).sendMessage(smslibMessage);
		
		// when then
		try {
			service.send(camelMessage);
			fail();
		} catch(MessageRejectedException ex) {
			// expected
		}
	}
	
	@Test
	public void testSendFailureHandlingForCserviceStopped() throws Exception {
		// given
		SmslibCamelMessage camelMessage = mock(SmslibCamelMessage.class);
		COutgoingMessage smslibMessage = mock(COutgoingMessage.class);
		when(translator.translateOutgoing(camelMessage)).thenReturn(smslibMessage);
		doThrow(new ConnectionFailedException()).when(cServiceMock).sendMessage(smslibMessage);
		
		// when then
		try {
			service.send(camelMessage);
		} catch(ConnectionFailedException e) {
			// expected
		}
	}
	
	@Test
	public void testReceive() {
		throw new RuntimeException("This test has not been written yet... looks like the consumer hasn't been either.");
	}
}
