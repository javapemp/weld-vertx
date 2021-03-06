/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.vertx.extension;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.vertx.Timeouts;
import org.jboss.weld.vertx.VertxExtension;
import org.junit.Test;

import io.vertx.core.Vertx;

/**
 *
 * @author Martin Kouba
 */
public class RegisterConsumersAfterBootstrapTest {

    static final BlockingQueue<Object> SYNCHRONIZER = new LinkedBlockingQueue<>();

    @Test
    public void testConsumers() throws InterruptedException {
        try (WeldContainer weld = new Weld().disableDiscovery().addExtension(new VertxExtension()).addPackage(false, RegisterConsumersAfterBootstrapTest.class)
                .initialize()) {
            Vertx vertx = Vertx.vertx();
            try {
                weld.select(VertxExtension.class).get().registerConsumers(vertx, weld.event());
                vertx.eventBus().send(HelloObserver.HELLO_ADDRESS, "hello");
                assertEquals("hello", SYNCHRONIZER.poll(Timeouts.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS));
            } finally {
                vertx.close();
            }
        }
    }

}
