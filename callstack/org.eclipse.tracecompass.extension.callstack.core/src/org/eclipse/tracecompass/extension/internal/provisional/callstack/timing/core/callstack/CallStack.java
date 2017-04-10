/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.instrumented.CalledFunctionFactory;
import org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.instrumented.ICalledFunction;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.CallStackSeries.IThreadIdProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * Represents the actual callstack for one element. The callstack is a stack of
 * calls, whether function calls, executions, sub-routines that have a certain
 * depth and where durations at each depth is in the form of a reverse pyramid,
 * ie, a call at level n+1 will have start_n+1 >= start_n and end_n+1 <= end_n.
 *
 * TODO: Is that true? the reverse pyramid?
 *
 * @author Geneviève Bastien
 */
public class CallStack {

    private final @Nullable ICallStackElement fSymbolKeyElement;
    private final @Nullable IThreadIdProvider fThreadIdProvider;
    private final ITmfStateSystem fStateSystem;
    private final List<Integer> fQuarks;
    private final String fHostId;

    /**
     * Constructor
     *
     * @param ss
     *            The state system containing the callstack
     * @param quarks
     *            The quarks corresponding to each of the depth levels
     * @param symbolKeyElement
     *            The element containing the symbol key for this callstack
     * @param hostId
     *            The ID of the host this callstack is from
     * @param threadIdProvider
     *            The provider of the thread ID for this callstack
     */
    public CallStack(ITmfStateSystem ss, List<Integer> quarks, @Nullable ICallStackElement symbolKeyElement, String hostId, @Nullable IThreadIdProvider threadIdProvider) {
        fSymbolKeyElement = symbolKeyElement;
        fThreadIdProvider = threadIdProvider;
        fStateSystem = ss;
        fQuarks = quarks;
        fHostId = hostId;
    }

    /**
     * Get the maximum depth of this callstack
     *
     * @return The maximum depth of the callstack
     */
    public int getMaxDepth() {
        return fQuarks.size();
    }

    /**
     * Get the list of calls at a given depth
     *
     * @param depth
     *            The requested depth
     * @param startTime
     *            The start of the period for which to get the call list
     * @param endTime
     *            The end of the period for which to get the call list
     * @param resolution
     *            The resolution of the calls. TODO: what is a resolution?
     * @param monitor
     *            The progress monitor to follow the progress of this query
     * @return The list of called functions at this depth
     */
    public List<ICalledFunction> getCallListAtDepth(int depth, long startTime, long endTime, long resolution, IProgressMonitor monitor) {
        if (depth > getMaxDepth()) {
            throw new ArrayIndexOutOfBoundsException("CallStack depth " + depth + " is too large"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        try {
            Integer quark = fQuarks.get(depth - 1);
            long start = Math.max(fStateSystem.getStartTime(), startTime);
            long end = Math.min(fStateSystem.getCurrentEndTime(), endTime);
            if (start > end) {
                return Collections.EMPTY_LIST;
            }
            List<ITmfStateInterval> stackIntervals = StateSystemUtils.queryHistoryRange(fStateSystem, quark, start, end, resolution, monitor);
            List<ICalledFunction> callList = new ArrayList<>(stackIntervals.size());

            for (ITmfStateInterval callInterval : stackIntervals) {
                if (monitor.isCanceled()) {
                    return Collections.EMPTY_LIST;
                }
                if (!callInterval.getStateValue().isNull()) {
                    callList.add(CalledFunctionFactory.create(callInterval.getStartTime(), callInterval.getEndTime() + 1, depth, callInterval.getStateValue(), getSymbolKeyAt(callInterval.getStartTime()), getThreadId(callInterval.getStartTime()),
                            null, ModelManager.getModelFor(fHostId)));
                }
            }
            return callList;
        } catch (AttributeNotFoundException | StateSystemDisposedException | TimeRangeException e) {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Get the function call with closest beginning or end from time, either
     * forward or backward.
     *
     * @param time
     *            The time of query
     * @param forward
     *            Set to <code>true</code> if the beginning or end is forward in
     *            time, <code>false</code> to go backwards
     * @return The next function
     */
    public @Nullable ICalledFunction getNextFunction(long time, boolean forward) {
        // From the bottom of the stack, query at time t to find the last level
        // with an active call
        try {
            for (int i = fQuarks.size() - 1; i >= 0; i--) {
                ITmfStateInterval interval;

                interval = fStateSystem.querySingleState(time, fQuarks.get(i));
                if (!interval.getStateValue().isNull()) {

                }
            }
        } catch (StateSystemDisposedException e) {

        }
        return null;

    }

    /**
     * Get the function call at a given depth that either begins or ends after
     * the requested time.
     *
     * @param time
     *            The time to query
     * @param depth
     *            The depth of the requested function
     * @return The next function call at this level
     */
    public @Nullable ICalledFunction getNextFunction(long time, int depth) {
        if (depth > getMaxDepth()) {
            throw new ArrayIndexOutOfBoundsException("CallStack depth " + depth + " is too large"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (time > fStateSystem.getCurrentEndTime()) {
            return null;
        }
        try {
            ITmfStateInterval interval = fStateSystem.querySingleState(time, fQuarks.get(depth - 1));
            while ((interval.getStateValue().isNull() || (interval.getStartTime() < time)) && interval.getEndTime() + 1 < fStateSystem.getCurrentEndTime()) {
                interval = fStateSystem.querySingleState(interval.getEndTime() + 1, fQuarks.get(depth - 1));
            }
            if (!interval.getStateValue().isNull() && interval.getStartTime() >= time) {
                return CalledFunctionFactory.create(interval.getStartTime(), interval.getEndTime() + 1, depth, interval.getStateValue(), getSymbolKeyAt(interval.getStartTime()), getThreadId(interval.getStartTime()), null,
                        ModelManager.getModelFor(fHostId));
            }
        } catch (StateSystemDisposedException e) {

        }
        return null;
    }

    /**
     * Get the next function call
     *
     * @param time
     *            The time of the request
     * @param depth
     *            The depth FIXME: with the parent, do we need depth?
     * @param parent
     *            The parent function call
     * @param model
     *            The operating system model to retrieve extra information.
     *            FIXME: Since we have the host ID, the model may not be
     *            necessary
     * @return The next function call
     */
    public @Nullable ICalledFunction getNextFunction(long time, int depth, @Nullable ICalledFunction parent, IHostModel model) {
        if (depth > getMaxDepth()) {
            throw new ArrayIndexOutOfBoundsException("CallStack depth " + depth + " is too large"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        long endTime = (parent == null ? fStateSystem.getCurrentEndTime() : parent.getEnd() - 1);
        if (time > endTime) {
            return null;
        }
        try {
            ITmfStateInterval interval = fStateSystem.querySingleState(time, fQuarks.get(depth - 1));
            while ((interval.getStateValue().isNull() || (interval.getStartTime() < time)) && interval.getEndTime() + 1 < endTime) {
                interval = fStateSystem.querySingleState(interval.getEndTime() + 1, fQuarks.get(depth - 1));
            }
            if (!interval.getStateValue().isNull() && interval.getStartTime() >= time) {
                return CalledFunctionFactory.create(interval.getStartTime(), interval.getEndTime() + 1, depth, interval.getStateValue(), getSymbolKeyAt(interval.getStartTime()), getThreadId(interval.getStartTime()), parent, model);
            }
        } catch (StateSystemDisposedException e) {

        }
        return null;
    }

    /**
     * Iterate over the callstack in a depth-first manner
     *
     * @param startTime
     *            The start time of the iteration
     * @param endTime
     *            The end time of the iteration
     * @param consumer
     *            The consumer to consume the function calls
     */
    public void iterateOverCallStack(long startTime, long endTime, Consumer<ICalledFunction> consumer) {
        // TODO Do we need this? If so, implement me!
    }

    /**
     * Get the symbol key for this callstack at a given time
     *
     * @param time
     *            The time of query
     * @return The symbol key or {@link ICallStackElement#DEFAULT_SYMBOL_KEY} if
     *         not available
     */
    public int getSymbolKeyAt(long time) {
        if (fSymbolKeyElement != null) {
            return fSymbolKeyElement.getSymbolKeyAt(time);
        }
        return ICallStackElement.DEFAULT_SYMBOL_KEY;
    }

    /**
     * Get the ID of the thread running this callstack at time t. This method is
     * used in conjonction with other trace data to get the time spent on the
     * CPU for this call.
     *
     * @param time
     *            The time of query
     * @return The thread ID or <code>-1</code> if not available.
     */
    public int getThreadId(long time) {
        if (fThreadIdProvider != null) {
            return fThreadIdProvider.getTheadId(time);
        }
        return -1;
    }

    /**
     * Get the start time of this callstack
     *
     * @return The start time of the callstack
     */
    public long getStartTime() {
        return fStateSystem.getStartTime();
    }

    /**
     * Get the end time of this callstack
     *
     * @return The end time of the callstack
     */
    public long getEndTime() {
        return fStateSystem.getCurrentEndTime();
    }

    /**
     * Callstacks may save extra attributes in the state system. By convention,
     * these attributes will be located in the parent attribute of the
     * callstack, under their given name and its value will be queried by this
     * method at a given time.
     *
     * Extra attributes can be used for instance for some callstacks to save the
     * CPU on which they were running at the beginning of a function call. This
     * CPU can then be cross-referenced with other trace data to retrieve the
     * thread ID.
     *
     * @param name
     *            The name of the extra attribute to get
     * @param time
     *            The time at which to query
     * @return The value of attribute at the requested time
     */
    public @Nullable Object getExtraAttribute(String name, long time) {
        if (time < getStartTime() || time > getEndTime()) {
            return null;
        }

        int parentQuark = fStateSystem.getParentAttributeQuark(fQuarks.get(0));
        if (parentQuark < 0) {
            return null;
        }
        parentQuark = fStateSystem.getParentAttributeQuark(parentQuark);
        if (parentQuark < 0) {
            return null;
        }
        try {
            int quark = fStateSystem.optQuarkRelative(parentQuark, name);
            if (quark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                ITmfStateInterval state = fStateSystem.querySingleState(time, quark);
                switch (state.getStateValue().getType()) {

                case INTEGER:
                    return state.getStateValue().unboxInt();
                case LONG:
                    return state.getStateValue().unboxLong();
                case STRING:
                    return state.getStateValue().unboxStr();
                case CUSTOM:
                case DOUBLE:
                case NULL:
                default:
                    break;

                }
            }

        } catch (StateSystemDisposedException e) {

        }
        return null;
    }

}
