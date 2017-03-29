/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package ca.polymtl.tracecompass.jul.analysis.core.tests.trace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.junit.Test;

import ca.polymtl.tracecompass.internal.jul.analysis.core.trace.LttngTraceCompassJulTrace;
import ca.polymtl.tracecompass.internal.jul.analysis.core.trace.TraceCompassJulEvent;

/**
 * Test the {@link TraceCompassJulEvent} class
 *
 * @author Geneviève Bastien
 */
public class TraceCompassJulEventTest {

    private static final @NonNull CtfTmfTrace DUMMY_TRACE = new LttngTraceCompassJulTrace();
    private static final @NonNull IEventDeclaration DUMMY_DECL = TraceCompassJulUtils.getJulEventDeclaration();
    private static final @NonNull ITmfTimestamp DUMMY_TIMESTAMP = TmfTimestamp.fromNanos(0L);

    private static class TraceCompassJulEventStub extends TraceCompassJulEvent {

        public TraceCompassJulEventStub(@NonNull IEventDefinition def) {
            super(DUMMY_TRACE, 0L, DUMMY_TIMESTAMP, "chan0", 0, DUMMY_DECL, def);

        }

    }

    private static @NonNull BitBuffer createBitBuffer(String msg) {
        ByteBuffer buff = ByteBuffer.allocate(1024);
        assertNotNull(buff);
        BitBuffer bb = new BitBuffer(buff);
        buff.mark();
        buff.put(msg.getBytes());
        buff.put((byte) 0);
        buff.reset();
        return bb;
    }

    private static ITmfEvent createEvent(String msgString) {
        try {
            IEventDefinition def = DUMMY_DECL.createDefinition(null, createBitBuffer(msgString), 0L);
            assertNotNull(def);
            TraceCompassJulEventStub event = new TraceCompassJulEventStub(def);
            return event;
        } catch (CTFException e) {
            fail(e.getMessage());
        }
        return null;
    }

    private static void testEventName(String msgString, String expected) {
        ITmfEvent event = createEvent(msgString);
        assertEquals(expected, event.getName());
    }

    /**
     * Test the JUL event for event name
     */
    @Test
    public void testEventName() {
        testEventName("[Test:MyTest]", "Test:MyTest");
        testEventName("[Bla bla]", "Bla bla");
        testEventName("[Bla bla] something something", "Bla bla");
        testEventName("something [Bla bla] something something", DUMMY_DECL.getName());
        testEventName("Not the right format", DUMMY_DECL.getName());
    }

    /**
     * Test the JUL event for event fields
     */
    @Test
    public void testEventFields() {
        ITmfEvent event = createEvent("[Test:MyTest]");
        assertNotNull(event);
        assertNull(event.getContent().getField("fieldName"));
        assertNotNull(event.getContent().getField("msg"));

        event = createEvent("[Test:MyTest] field1=val1, field2=val2");
        assertNotNull(event);
        assertNotNull(event.getContent().getField("field1"));
        assertEquals("val1", event.getContent().getField("field1").getValue());
        assertNotNull(event.getContent().getField("field2"));
        assertEquals("val2", event.getContent().getField("field2").getValue());
        assertNotNull(event.getContent().getField("msg"));

        event = createEvent("[Test:MyTest] field1=val1 extra, field2=val2");
        assertNotNull(event);
        assertNotNull(event.getContent().getField("field1"));
        assertEquals("val1 extra", event.getContent().getField("field1").getValue());
        assertNotNull(event.getContent().getField("field2"));
        assertEquals("val2", event.getContent().getField("field2").getValue());
        assertNotNull(event.getContent().getField("msg"));
    }
}
