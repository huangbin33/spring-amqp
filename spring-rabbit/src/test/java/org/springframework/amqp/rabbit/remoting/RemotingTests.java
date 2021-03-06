/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.amqp.rabbit.remoting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.BrokerRunning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 1.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RemotingTests {

	@Rule
	public BrokerRunning brokerRunning = BrokerRunning.isRunning();

	@Autowired
	private ServiceInterface client;

	@Autowired
	private RabbitTemplate template;

	private static CountDownLatch latch;

	private static String receivedMessage;

	@Test
	public void testEcho() throws Exception {
		String reply = client.echo("foo");
		assertEquals("echo:foo", reply);
	}

	@Test
	public void testNoAnswer() throws Exception {
		latch = new CountDownLatch(1);
		client.noAnswer("foo");
		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertEquals("received:foo", receivedMessage);
	}

	@Test
	public void testTimeout() {
		try {
			client.suspend();
			fail("Exception expected");
		}
		catch (RemoteProxyFailureException e) {
			assertTrue("No reply received - perhaps a timeout in the template?".equals(e.getMessage()));
		}
	}

	public interface ServiceInterface {

		String echo(String message);

		void noAnswer(String message);

		void suspend();

	}

	public static class ServiceImpl implements ServiceInterface {

		@Override
		public String echo(String message) {
			return "echo:" + message;
		}

		@Override
		public void noAnswer(String message) {
			receivedMessage = "received:" + message;
			latch.countDown();
		}

		@Override
		public void suspend() {
			try {
				Thread.sleep(3000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}


	}
}
