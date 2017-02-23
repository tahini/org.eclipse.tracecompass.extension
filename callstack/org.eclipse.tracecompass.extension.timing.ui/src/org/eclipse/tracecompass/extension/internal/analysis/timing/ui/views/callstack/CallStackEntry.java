/*******************************************************************************
 * Copyright (c) 2013, 2016 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.analysis.timing.ui.views.callstack;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.extension.internal.provisional.timing.core.callstack.CallStack;
import org.eclipse.tracecompass.extension.internal.timing.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An entry, or row, in the Call Stack view
 *
 * @author Patrick Tasse
 * @author Geneviève Bastien
 */
public class CallStackEntry extends TimeGraphEntry {

    private final int fStackLevel;
    private String fFunctionName;
    private long fFunctionEntryTime;
    private long fFunctionExitTime;
    private final CallStack fCallStack;
    private final @Nullable ISymbolProvider fSymbolProvider;

    /**
     * Standard constructor
     *
     * @param symbolProvider
     *            The symbol provider for this entry
     * @param stackLevel
     *            The stack level
     * @param element
     *            The call stack state system
     * @since 2.0
     */
    public CallStackEntry(@Nullable ISymbolProvider symbolProvider, int stackLevel,
            @NonNull CallStack element) {
        super(String.valueOf(stackLevel), 0, 0);
        fStackLevel = stackLevel;
        fFunctionName = ""; //$NON-NLS-1$
        fCallStack = element;
        fSymbolProvider = symbolProvider;
    }

    /**
     * Get the function name of the call stack entry
     *
     * @return the function name
     */
    public String getFunctionName() {
        return fFunctionName;
    }

    /**
     * Set the function name of the call stack entry
     *
     * @param functionName
     *            the function name
     */
    public void setFunctionName(String functionName) {
        fFunctionName = functionName;
    }

    /**
     * Set the selected function entry time
     *
     * @param entryTime
     *            the function entry time
     */
    public void setFunctionEntryTime(long entryTime) {
        fFunctionEntryTime = entryTime;
    }

    /**
     * Get the selected function entry time
     *
     * @return the function entry time
     */
    public long getFunctionEntryTime() {
        return fFunctionEntryTime;
    }

    /**
     * Set the selected function exit time
     *
     * @param exitTime
     *            the function exit time
     */
    public void setFunctionExitTime(long exitTime) {
        fFunctionExitTime = exitTime;
    }

    /**
     * Get the selected function exit time
     *
     * @return the function exit time
     */
    public long getFunctionExitTime() {
        return fFunctionExitTime;
    }

    /**
     * Retrieve the stack level associated with this entry.
     *
     * @return The stack level or 0
     */
    public int getStackLevel() {
        return fStackLevel;
    }

    @Override
    public boolean matches(@NonNull Pattern pattern) {
        return pattern.matcher(fFunctionName).find();
    }

    /**
     * Get the list of time events for this entry. A time event will be
     * constructed for each function in the stack
     *
     * @param startTime
     *            The start of the requested period
     * @param endTime
     *            The end time of the requested period
     * @param resolution
     *            The resolution
     * @param monitor
     *            The progress monitor to use for cancellation
     * @return The list of {@link ITimeEvent} to display in the view, or
     *         <code>null</code> if the analysis was cancelled.
     */
    public @Nullable List<ITimeEvent> getEventList(long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        List<ICalledFunction> callList = fCallStack.getCallListAtDepth(fStackLevel, startTime, endTime, resolution, monitor);

        List<ITimeEvent> events = new ArrayList<>();
        final int modulo = CallStackPresentationProvider.NUM_COLORS / 2;

        long lastEndTime = Long.MAX_VALUE;
        for (ICalledFunction function : callList) {
            if (monitor.isCanceled()) {
                return null;
            }
            long time = function.getStart();
            long duration = function.getLength();

            // Do we add an null event before the function
            if (time > lastEndTime && time > startTime) {
                long a = (lastEndTime == Long.MAX_VALUE ? startTime : lastEndTime);
                events.add(new NullTimeEvent(this, a, time - a));
            }

            // Add a call stack event for this function
            events.add(new CallStackEvent(this, time, duration, function, function.getSymbol().toString().hashCode() % modulo + modulo));
            lastEndTime = function.getEnd();

        }
        return events;
    }

    String resolveFunctionName(ICalledFunction function, long time) {
        long address = Long.MAX_VALUE;
        Object symbol = function.getSymbol();
        String name = symbol.toString();
        if (symbol instanceof Number) {
            address = (Long) symbol;
            name = "0x" + Long.toUnsignedString(address, 16); //$NON-NLS-1$
        } else if (symbol instanceof String) {
            try {
                address = Long.parseLong(name, 16);
            } catch (NumberFormatException e) {
                return name;
            }
        }

        ISymbolProvider provider = fSymbolProvider;
        if (provider != null) {
            String symbolString = provider.getSymbolText(function.getProcessId(), time, address);
            if (symbolString != null) {
                name = symbolString;
            }
        }
        return name;
    }

    /**
     * Return the called function at the requested time
     *
     * @param time
     *            The time of request
     * @return The called function at the requested time, or <code>null</code>
     *         if there is no function call at this time
     */
    public @Nullable ICalledFunction updateAt(long time) {
        List<ICalledFunction> callList = fCallStack.getCallListAtDepth(fStackLevel, time, time + 1, 1, new NullProgressMonitor());
        if (callList.isEmpty()) {
            fFunctionName = ""; //$NON-NLS-1$
            return null;
        }
        ICalledFunction function = callList.get(0);
        fFunctionName = resolveFunctionName(function, time);
        fFunctionEntryTime = function.getStart();
        fFunctionExitTime = function.getEnd();
        return function;
    }

    /**
     * Get the time of the next event, either entry or exit, for this entry
     *
     * @param time
     *            The time of the request
     * @return The time of the next event
     */
    public long getNextEventTime(long time) {
        ICalledFunction nextFunction = fCallStack.getNextFunction(time, fStackLevel);
        if (nextFunction == null) {
            return time;
        }
        return (nextFunction.getStart() <= time ? nextFunction.getEnd() : nextFunction.getStart());
    }

}
