/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.callstack.timing.core.tests.callstack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.extension.callstack.timing.core.tests.stubs.CallStackAnalysisStub;
import org.eclipse.tracecompass.extension.internal.callstack.timing.core.callgraph.CalledFunctionFactory;
import org.eclipse.tracecompass.extension.internal.callstack.timing.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.extension.internal.callstack.timing.core.callstack.CallStackLeafElement;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.CallStack;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.CallStackSeries;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackLeafElement;
import org.junit.Test;

/**
 * Test the callstack data structure and traversal
 *
 * @author Geneviève Bastien
 */
public class CallStackTest extends CallStackTestBase {

    private static final @NonNull IProgressMonitor MONITOR = new NullProgressMonitor();
    private static final long START_TIME = 1L;
    private static final long END_TIME = 20L;

    /**
     * Test the callstack data using the callstack object
     */
    @Test
    public void testCallStackTraversal() {
        CallStackAnalysisStub module = getModule();
        assertNotNull(module);

        Collection<CallStackSeries> callStacks = module.getCallStackSeries();
        assertEquals(1, callStacks.size());
        CallStackSeries callstack = callStacks.iterator().next();
        assertNotNull(callstack);

        List<ICallStackElement> processes = callstack.getRootElements();
        assertEquals(2, processes.size());

        for (ICallStackElement element : processes) {
            assertNull(element.getParentElement());
            // Make sure the element does not return any call list
            switch (element.getName()) {
            case "1":
                // Make sure the symbol key is correctly resolved
                assertEquals(1, element.getSymbolKeyAt(START_TIME));
                assertEquals(1, element.getSymbolKeyAt(END_TIME));
                verifyProcess1(element);
                break;
            case "5":
                // Make sure the symbol key is correctly resolved
                assertEquals(5, element.getSymbolKeyAt(START_TIME));
                assertEquals(5, element.getSymbolKeyAt(END_TIME));
                verifyProcess5(element);
                break;
            default:
                fail("Unknown process in callstack");
            }
        }
    }

    private static void verifyProcess1(ICallStackElement element) {
        Collection<ICallStackElement> children = element.getChildren();
        IHostModel model = ModelManager.getModelFor("");
        for (ICallStackElement thread : children) {
            assertEquals(element, thread.getParentElement());
            // Make sure the element does not return any call list
            switch (thread.getName()) {
            case "2": {
                // Make sure the symbol key is correctly resolved
                assertEquals(1, thread.getSymbolKeyAt(START_TIME));
                assertEquals(1, thread.getSymbolKeyAt(END_TIME));

                Collection<ICallStackElement> stackElements = thread.getChildren();
                assertEquals(1, stackElements.size());
                ICallStackElement stackElement = stackElements.iterator().next();
                assertEquals(thread, stackElement.getParentElement());
                assertNull(stackElement.getNextGroup());
                assertTrue(stackElement instanceof CallStackLeafElement);
                CallStack callStack = ((CallStackLeafElement) stackElement).getCallStack();

                assertEquals(1, callStack.getSymbolKeyAt(START_TIME));
                assertEquals(1, callStack.getSymbolKeyAt(END_TIME));

                /* Check the first level */
                List<ICalledFunction> callList = callStack.getCallListAtDepth(1, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(2, callList.size());
                assertEquals(CalledFunctionFactory.create(1L, 10L, 1, "op1", 1, 2, null, model), callList.get(0));
                assertEquals(CalledFunctionFactory.create(12L, 20L, 1, "op4", 1, 2, null, model), callList.get(1));

                /* Check the second level */
                callList = callStack.getCallListAtDepth(2, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(3L, 7L, 2, "op2", 1, 2, null, model), callList.get(0));

                /* Check the third level */
                callList = callStack.getCallListAtDepth(3, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(4L, 5L, 3, "op3", 1, 2, null, model), callList.get(0));
            }
                break;
            case "3": {
                // Make sure the symbol key is correctly resolved
                assertEquals(1, element.getSymbolKeyAt(START_TIME));
                assertEquals(1, element.getSymbolKeyAt(END_TIME));

                Collection<ICallStackElement> stackElements = thread.getChildren();
                assertEquals(1, stackElements.size());
                ICallStackElement stackElement = stackElements.iterator().next();
                assertEquals(thread, stackElement.getParentElement());
                assertNull(stackElement.getNextGroup());
                assertTrue(stackElement instanceof CallStackLeafElement);
                CallStack callStack = ((CallStackLeafElement) stackElement).getCallStack();

                assertEquals(1, callStack.getSymbolKeyAt(START_TIME));
                assertEquals(1, callStack.getSymbolKeyAt(END_TIME));

                /* Check the first level */
                List<ICalledFunction> callList = callStack.getCallListAtDepth(1, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(3L, 20L, 1, "op2", 1, 3, null, model), callList.get(0));

                /* Check the second level */
                callList = callStack.getCallListAtDepth(2, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(2, callList.size());
                assertEquals(CalledFunctionFactory.create(5L, 6L, 2, "op3", 1, 3, null, model), callList.get(0));
                assertEquals(CalledFunctionFactory.create(7L, 13L, 2, "op2", 1, 3, null, model), callList.get(1));
            }
                break;
            default:
                fail("Unknown thread child of process 5");
            }
        }
    }

    private static void verifyProcess5(ICallStackElement element) {
        Collection<ICallStackElement> children = element.getChildren();
        IHostModel model = ModelManager.getModelFor("");
        for (ICallStackElement thread : children) {
            // Make sure the element does not return any call list
            switch (thread.getName()) {
            case "6": {
                // Make sure the symbol key is correctly resolved
                assertEquals(5, thread.getSymbolKeyAt(START_TIME));
                assertEquals(5, thread.getSymbolKeyAt(END_TIME));

                Collection<ICallStackElement> stackElements = thread.getChildren();
                assertEquals(1, stackElements.size());
                ICallStackElement stackElement = stackElements.iterator().next();
                assertEquals(thread, stackElement.getParentElement());
                assertNull(stackElement.getNextGroup());
                assertTrue(stackElement instanceof CallStackLeafElement);
                CallStack callStack = ((CallStackLeafElement) stackElement).getCallStack();

                assertEquals(5, callStack.getSymbolKeyAt(START_TIME));
                assertEquals(5, callStack.getSymbolKeyAt(END_TIME));

                /* Check the first level */
                List<ICalledFunction> callList = callStack.getCallListAtDepth(1, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(1L, 20L, 1, "op1", 1, 6, null, model), callList.get(0));

                /* Check the second level */
                callList = callStack.getCallListAtDepth(2, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(2, callList.size());
                assertEquals(CalledFunctionFactory.create(2L, 7L, 2, "op3", 1, 6, null, model), callList.get(0));
                assertEquals(CalledFunctionFactory.create(12L, 20L, 2, "op4", 1, 6, null, model), callList.get(1));

                /* Check the third level */
                callList = callStack.getCallListAtDepth(3, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(4L, 6L, 3, "op1", 1, 6, null, model), callList.get(0));
            }
                break;
            case "7": {
                // Make sure the symbol key is correctly resolved
                assertEquals(5, thread.getSymbolKeyAt(START_TIME));
                assertEquals(5, thread.getSymbolKeyAt(END_TIME));

                Collection<ICallStackElement> stackElements = thread.getChildren();
                assertEquals(1, stackElements.size());
                ICallStackElement stackElement = stackElements.iterator().next();
                assertEquals(thread, stackElement.getParentElement());
                assertNull(stackElement.getNextGroup());
                assertTrue(stackElement instanceof CallStackLeafElement);
                CallStack callStack = ((CallStackLeafElement) stackElement).getCallStack();

                assertEquals(5, callStack.getSymbolKeyAt(START_TIME));
                assertEquals(5, callStack.getSymbolKeyAt(END_TIME));

                /* Check the first level */
                List<ICalledFunction> callList = callStack.getCallListAtDepth(1, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(1L, 20L, 1, "op5", 1, 6, null, model), callList.get(0));

                /* Check the second level */
                callList = callStack.getCallListAtDepth(2, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(3, callList.size());
                assertEquals(CalledFunctionFactory.create(2L, 6L, 2, "op2", 1, 6, null, model), callList.get(0));
                assertEquals(CalledFunctionFactory.create(9L, 13L, 2, "op2", 1, 6, null, model), callList.get(1));
                assertEquals(CalledFunctionFactory.create(15L, 19L, 2, "op2", 1, 6, null, model), callList.get(2));

                /* Check the third level */
                callList = callStack.getCallListAtDepth(3, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(10L, 11L, 3, "op3", 1, 6, null, model), callList.get(0));

            }
                break;
            default:
                fail("Unknown thread child of process 5");
            }
        }
    }

    private CallStack getElementToTest() {
        // Return the second callstack level of process 5 / thread 7
        CallStackAnalysisStub module = getModule();
        assertNotNull(module);

        Collection<CallStackSeries> callStacks = module.getCallStackSeries();
        assertEquals(1, callStacks.size());
        CallStackSeries callstack = callStacks.iterator().next();
        assertNotNull(callstack);

        List<ICallStackElement> processes = callstack.getRootElements();
        assertEquals(2, processes.size());

        ICallStackElement process = processes.get(1);
        assertEquals("5", process.getName());
        Collection<ICallStackElement> threads = process.getChildren();
        assertEquals(2, threads.size());

        Iterator<ICallStackElement> iterator = threads.iterator();
        iterator.next();
        ICallStackElement thread = iterator.next();
        assertEquals("7", thread.getName());
        Collection<ICallStackElement> callstacks = thread.getChildren();
        assertEquals(1, callstacks.size());
        return ((ICallStackLeafElement) callstacks.iterator().next()).getCallStack();
    }

    /**
     * Test getting the function calls with different ranges and resolutions
     */
    @Test
    public void testCallStackRanges() {
        CallStack element = getElementToTest();

        /**
         * <pre>Function calls for this element:
         * (2, 6), (9, 13), (15, 19)
         * </pre>
         */

        /* Following test with a resolution of 1 */
        int resolution = 1;
        // Test a range before the first element
        List<ICalledFunction> callList = element.getCallListAtDepth(2, START_TIME, START_TIME, resolution, MONITOR);
        assertEquals(0, callList.size());

        // Test a range including the start of a function call
        callList = element.getCallListAtDepth(2, START_TIME, 2L, resolution, MONITOR);
        assertEquals(1, callList.size());

        // Test a range in the middle of one function call
        callList = element.getCallListAtDepth(2, START_TIME, 4L, resolution, MONITOR);
        assertEquals(1, callList.size());

        // Test a range including not fully including 2 function calls
        callList = element.getCallListAtDepth(2, 4L, 10L, resolution, MONITOR);
        assertEquals(2, callList.size());

        // Test a range outside the trace range
        callList = element.getCallListAtDepth(2, END_TIME + 1, END_TIME + 3, resolution, MONITOR);
        assertEquals(0, callList.size());

        // Test the full range of the trace
        callList = element.getCallListAtDepth(2, START_TIME, END_TIME, resolution, MONITOR);
        assertEquals(3, callList.size());

        // Test a range after the first call with a resolution that should skip one call
    }

    /**
     * Test getting the {@link CallStack#getNextFunction(long, int)} method
     */
    @Test
    public void testCallStackNext() {
        CallStack element = getElementToTest();
        IHostModel model = ModelManager.getModelFor("");

        /**
         * <pre>Function calls for this element:
         * (2, 6), (9, 13), (15, 19)
         * </pre>
         */

        ICalledFunction function = element.getNextFunction(START_TIME, 2);
        assertNotNull(function);
        assertEquals(CalledFunctionFactory.create(2L, 6L, 2, "op2", 1, 6, null, model), function);

        function = element.getNextFunction(function.getEnd(), 2);
        assertNotNull(function);
        assertEquals(CalledFunctionFactory.create(9L, 13L, 2, "op2", 1, 6, null, model), function);

        function = element.getNextFunction(function.getEnd(), 2);
        assertNotNull(function);
        assertEquals(CalledFunctionFactory.create(15L, 19L, 2, "op2", 1, 6, null, model), function);

        function = element.getNextFunction(function.getEnd(), 2);
        assertNull(function);

    }

}
