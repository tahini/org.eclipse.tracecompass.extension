/*******************************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.analysis.timing.ui.views.callstack;

import org.eclipse.tracecompass.extension.internal.timing.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * Time Event implementation specific to the Call Stack View
 *
 * @author Patrick Tasse
 */
public class CallStackEvent extends TimeEvent {

    private final ICalledFunction fFunction;

    /**
     * Standard constructor
     *
     * @param entry
     *            The entry that this event affects
     * @param time
     *            The start time of the event
     * @param duration
     *            The duration of the event
     * @param function
     *            The ID of the process, used to resolve the symbols
     * @param value
     *            The event value (1-256)
     */
    public CallStackEvent(CallStackEntry entry, long time, long duration, ICalledFunction function, int value) {
        super(entry, time, duration, value);
        fFunction = function;
    }

    /**
     * @since 2.1
     */
    @Override
    public CallStackEntry getEntry() {
        /* Type enforced at constructor */
        return (CallStackEntry) fEntry;
    }

    /**
     * Get the name of the function to display for this event. If there is a
     * symbol mapper, this method will return the mapped value.
     *
     * @return The name of the function call to show
     */
    public String getFunctionName() {
        return getEntry().resolveFunctionName(fFunction, getTime());
    }

}
