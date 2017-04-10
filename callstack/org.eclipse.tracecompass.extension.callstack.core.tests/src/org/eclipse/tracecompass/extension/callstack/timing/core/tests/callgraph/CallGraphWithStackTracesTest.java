/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.callstack.timing.core.tests.callgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.profiling.ProfilingGroup;
import org.junit.Test;

/**
 * Test profiling data where the input are stack traces from events
 *
 * @author Geneviève Bastien
 */
public class CallGraphWithStackTracesTest {

    private static final long[] CALLSITE_1 = { 1, 2, 3, 4 };
    private static final long[] CALLSITE_2 = { 1, 2, 3 };
    private static final long[] CALLSITE_3 = { 1, 2, 3, 4 };
    private static final long[] CALLSITE_4 = { 1, 3, 4 };
    private static final long[] CALLSITE_5 = { 1, 2, 5 };
    private static final long[] CALLSITE_6 = { 1, 2, 5, 4 };
    private static final long[] CALLSITE_7 = { 10, 11, 12 };
    private static final long[] CALLSITE_8 = { 10, 11 };
    private static final long[] CALLSITE_9 = { 1, 2, 3, 4 };
    private static final long[] CALLSITE_10 = { 1, 2, 4, 5 };

    /**
     * Test a full sampling for one group
     */
    @Test
    public void testStackTraces() {
        ProfilingGroup pg = new ProfilingGroup("data");
        pg.addStackTrace(CALLSITE_1);
        pg.addStackTrace(CALLSITE_2);
        pg.addStackTrace(CALLSITE_3);
        pg.addStackTrace(CALLSITE_4);
        pg.addStackTrace(CALLSITE_5);
        pg.addStackTrace(CALLSITE_6);
        pg.addStackTrace(CALLSITE_7);
        pg.addStackTrace(CALLSITE_8);
        pg.addStackTrace(CALLSITE_9);
        pg.addStackTrace(CALLSITE_10);

        Collection<AggregatedCallSite> aggregatedData = pg.getAggregatedData();
        assertNotNull(aggregatedData);
        assertEquals(2, aggregatedData.size());

        for (AggregatedCallSite callsite : aggregatedData) {
            switch (((Long) callsite.getSymbol()).intValue()) {
            case 1: {
                assertEquals(8, callsite.getLength());
                assertEquals(2, callsite.getChildren().size());
                assertEquals(1L, callsite.getSymbol());
                for (AggregatedCallSite childCallsite : callsite.getChildren().values()) {
                    switch (((Long) childCallsite.getSymbol()).intValue()) {
                    case 2:
                        assertEquals(7, childCallsite.getLength());
                        assertEquals(3, childCallsite.getChildren().size());
                        break;
                    case 3:
                        assertEquals(1, childCallsite.getLength());
                        assertEquals(1, childCallsite.getChildren().size());
                        break;
                    default:
                        throw new IllegalStateException("Unknown callsite: " + childCallsite.getSymbol());
                    }
                }
            }
                break;
            case 10: {
                assertEquals(2, callsite.getLength());
                assertEquals(1, callsite.getChildren().size());
                assertEquals(10L, callsite.getSymbol());
                AggregatedCallSite childCallsite = callsite.getChildren().values().iterator().next();
                assertEquals(2, childCallsite.getLength());
                assertEquals(1, callsite.getChildren().size());
                assertEquals(11L, childCallsite.getSymbol());
            }
                break;
            default:
                throw new IllegalStateException("Unknown callsite: " + callsite.getSymbol());
            }
        }

    }

}
