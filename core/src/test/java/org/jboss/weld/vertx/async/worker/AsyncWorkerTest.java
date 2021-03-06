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
package org.jboss.weld.vertx.async.worker;

import java.util.concurrent.ExecutionException;

import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.vertx.AsyncWorker;
import org.jboss.weld.vertx.Timeouts;
import org.jboss.weld.vertx.WeldVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 *
 * @author Martin Kouba
 */
@RunWith(VertxUnitRunner.class)
public class AsyncWorkerTest {

    private WeldContainer weld;

    private Vertx vertx;

    @Rule
    public Timeout globalTimeout = Timeout.millis(Timeouts.GLOBAL_TIMEOUT);

    @Before
    public void init(TestContext context) throws InterruptedException {
        final WeldVerticle weldVerticle = new WeldVerticle();
        Async async = context.async();
        vertx = Vertx.vertx();
        vertx.deployVerticle(weldVerticle, r -> {
            if (r.succeeded()) {
                weld = weldVerticle.container();
                async.complete();
            } else {
                context.fail(r.cause());
            }
        });
    }

    @After
    public void close(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testWorker(TestContext context) throws InterruptedException, ExecutionException {
        BlockingFoo.reset();
        Async async = context.async();

        BlockingFoo foo = weld.select(BlockingFoo.class).get();

        AsyncWorker.from(vertx).performBlocking(foo::getMessage).thenAccept(m -> {
            context.assertEquals("ping", m);
            async.complete();
        });
        context.assertEquals(1, async.count());
        BlockingFoo.complete("ping");
    }

    @Test
    public void testWorkerInject(TestContext context) throws InterruptedException, ExecutionException {
        BlockingFoo.reset();
        Async async = context.async();

        Hello hello = weld.select(Hello.class).get();

        hello.hello().thenAccept((m) -> {
            context.assertEquals("Hello ping!", m);
            async.complete();
        });
        context.assertEquals(1, async.count());
        BlockingFoo.complete("ping");
    }

    @Test
    public void testAsyncReferenceWorkerCombo(TestContext context) throws InterruptedException, ExecutionException {
        BlockingAlpha.reset();
        Async async = context.async();

        HelloCombo hello = weld.select(HelloCombo.class).get();

        hello.hello().thenAccept((m) -> {
            context.assertEquals("Hello ping!", m);
            async.complete();
        });
        BlockingAlpha.completeInit();
        context.assertEquals(1, async.count());
        BlockingAlpha.completeOperation("ping");
    }

    @Test
    public void testWorkerNonBlocking(TestContext context) throws InterruptedException, ExecutionException {
        BlockingFoo.reset();
        Async async = context.async();

        BlockingFoo foo = weld.select(BlockingFoo.class).get();

        AsyncWorker.from(vertx).perform(foo::getMessageNonBlocking).thenAccept(m -> {
            context.assertEquals(BlockingFoo.class.getName(), m);
            async.complete();
        });
    }
}
