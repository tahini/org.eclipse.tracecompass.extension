/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.callstack.timing.core.callgraph;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * Factory to create {@link ICalledFunction}s.
 *
 * @author Matthew Khouzam
 */
public final class CalledFunctionFactory {

    private static final String SEPARATOR = ": "; //$NON-NLS-1$
    private static final String ERROR_MSG = "Cannot create a called function of type : "; //$NON-NLS-1$

    private CalledFunctionFactory() {
        // do nothing
    }

    /**
     * Factory Method for a state value mapped called function
     *
     * @param start
     *            the start time
     * @param end
     *            the end time
     * @param depth
     *            the depth
     * @param stateValue
     *            the symbol
     * @param processId
     *            The process ID of the traced application
     * @param threadId
     *            The thread ID of the called function or
     *            {@link IHostModel#UNKNOWN_TID} if not available
     * @param parent
     *            the parent node
     * @param model
     *            The operating system model this function is a part of
     * @return an ICalledFunction with the specified properties
     */
    public static AbstractCalledFunction create(long start, long end, int depth, ITmfStateValue stateValue, int processId, int threadId, @Nullable ICalledFunction parent, IHostModel model) {
        switch (stateValue.getType()) {
        case INTEGER:
            return create(start, end, depth, stateValue.unboxInt(), processId, threadId, parent, model);
        case LONG:
            return create(start, end, depth, stateValue.unboxLong(), processId, threadId, parent, model);
        case STRING:
            return create(start, end, depth, stateValue.unboxStr(), processId, threadId, parent, model);
        case CUSTOM:
            // Fall through
        case DOUBLE:
            // Fall through
        case NULL:
            // Fall through
        default:
            throw new IllegalArgumentException(ERROR_MSG + stateValue.getType() + SEPARATOR + stateValue.toString());
        }
    }

    /**
     * Factory method to create a called function with a symbol that is a long
     * integer
     *
     * @param start
     *            the start time
     * @param end
     *            the end time
     * @param depth
     *            the depth
     * @param value
     *            the symbol
     * @param processId
     *            The process ID of the traced application
     * @param parent
     *            the parent node
     * @return an ICalledFunction with the specified propertiess
     */
    private static CalledFunction create(long start, long end, int depth, long value, int processId, int threadId, @Nullable ICalledFunction parent, IHostModel model) {
        if (start > end) {
            throw new IllegalArgumentException(Messages.TimeError + '[' + start + ',' + end + ']');
        }
        return new CalledFunction(start, end, value, depth, processId, threadId, parent, model);
    }

    /**
     * Factory method to create a called function with a symbol that is a
     * {@link String}
     *
     * @param start
     *            the start time
     * @param end
     *            the end time
     * @param depth
     *            the depth
     * @param value
     *            the symbol
     * @param processId
     *            The process ID of the traced application
     * @param threadId
     *            The thread ID of the called function or
     *            {@link IHostModel#UNKNOWN_TID} if not available
     * @param parent
     *            the parent node
     * @param model
     *            The operating system model this function is a part of
     * @return an ICalledFunction with the specified properties
     */
    public static CalledStringFunction create(long start, long end, int depth, String value, int processId, int threadId, @Nullable ICalledFunction parent, IHostModel model) {
        if (start > end) {
            throw new IllegalArgumentException(Messages.TimeError + '[' + start + ',' + end + ']');
        }
        return new CalledStringFunction(start, end, value, depth, processId, threadId, parent, model);
    }

}
