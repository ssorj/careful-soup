//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package net.example;

import java.net.URI;
import java.util.Hashtable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.CompletionListener;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jaegertracing.Configuration;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

@Singleton
@Path("/api")
public class Frontend {
    private static Logger log = LoggerFactory.getLogger(Frontend.class);
    private static ConnectionFactory connectionFactory = null;

    public static void main(String[] args) {
        try {
            // Register the global tracer

            Tracer tracer = Configuration.fromEnv("frontend").getTracer();
            GlobalTracer.registerIfAbsent(tracer);

            // AMQP

            String amqpHost = System.getenv("MESSAGING_SERVICE_HOST");
            String amqpPort = System.getenv("MESSAGING_SERVICE_PORT");
            String user = System.getenv("MESSAGING_SERVICE_USER");
            String password = System.getenv("MESSAGING_SERVICE_PASSWORD");

            if (amqpHost == null) amqpHost = "localhost";
            if (amqpPort == null) amqpPort = "5672";
            if (user == null)     user = "example";
            if (password == null) password = "example";

            String url = String.format("failover:(amqp://%s:%s)?jms.tracing=opentracing", amqpHost, amqpPort);

            Hashtable<Object, Object> env = new Hashtable<Object, Object>();
            env.put("connectionfactory.factory1", url);

            InitialContext context = new InitialContext(env);
            ConnectionFactory factory = (ConnectionFactory) context.lookup("factory1");

            Frontend.connectionFactory = factory;

            // HTTP

            String httpHost = System.getenv("HTTP_HOST");
            String httpPort = System.getenv("HTTP_PORT");

            if (httpHost == null) httpHost = "0.0.0.0";
            if (httpPort == null) httpPort = "8080";

            URI uri = URI.create(String.format("http://%s:%s/", httpHost, httpPort));
            ResourceConfig rc = new ResourceConfig(Frontend.class);

            NettyHttpContainerProvider.createHttp2Server(uri, rc, null);
        } catch (Exception e) {
            log.error("Startup failed", e);
            System.exit(1);
        }
    }

    private JMSContext jmsContext = Frontend.connectionFactory.createContext();

    @POST
    @Path("/send-request")
    @Consumes("text/plain")
    @Produces("text/plain")
    public String sendRequest(String requestText) {
        Span span = GlobalTracer.get().buildSpan("sendRequest").start();

        try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
            synchronized (jmsContext) {
                Queue requestQueue = jmsContext.createQueue("careful-soup/requests");
                Queue responseQueue = jmsContext.createTemporaryQueue();
                JMSProducer producer = jmsContext.createProducer();
                JMSConsumer consumer = jmsContext.createConsumer(responseQueue);

                producer.setAsync(new CompletionListener() {
                        @Override
                        public void onCompletion(Message message) {
                            try {
                                log.info("FRONTEND: Receiver acknowledged '{}'", message.getBody(String.class));
                            } catch (JMSException e) {
                                log.error("Message access error", e);
                            }
                        }

                        @Override
                        public void onException(Message message, Exception e) {
                            log.info("FRONTEND: Send failed: {}", e.toString());
                        }
                    });

                Message request = jmsContext.createTextMessage(requestText);
                String responseText = null;

                try {
                    request.setJMSReplyTo(responseQueue);
                } catch (JMSException e) {
                    log.error("Message access error", e);
                }

                producer.send(requestQueue, request);

                log.info("FRONTEND: Sent request '{}'", requestText);

                try {
                    responseText = consumer.receive().getBody(String.class);
                } catch (JMSException e) {
                    log.error("Message receive error", e);
                }

                log.info("FRONTEND: Received response '{}'", responseText);

                return "OK -> \"" + responseText + "\"\n";
            }
        } finally {
            span.finish();
        }
    }

    @GET
    @Path("/ready")
    @Produces("text/plain")
    public String ready() {
        log.info("FRONTEND: I am ready!");

        return "OK\n";
    }
}
