/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.lttng.callstack.context.core.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

public class ContextCallStackStateProvider extends AbstractTmfStateProvider {

    private static final String ID = "fdas";

    public ContextCallStackStateProvider(@NonNull ITmfTrace trace) {
        super(trace, ID);

    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new ContextCallStackStateProvider(getTrace());
    }

    /**
     * Get CPU
     *
     * @param event
     *            The event containing the cpu
     *
     * @return the CPU number (null for not set)
     */
    public static @Nullable Integer getCpu(ITmfEvent event) {
        Integer cpuObj = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        if (cpuObj == null) {
            /* We couldn't find any CPU information, ignore this event */
            return null;
        }
        return cpuObj;
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        Integer cpu = getCpu(event);
        if (cpu == null) {
            return;
        }
        ITmfEventField content = event.getContent();
        ITmfEventField field = content.getField("context._callstack_kernel");
        if (field != null) {
            saveKernelCallstack(cpu, event.getTimestamp().toNanos(), "kernel", field);
        }
        field = content.getField("context._callstack_user");
        if (field != null) {
            saveKernelCallstack(cpu, event.getTimestamp().toNanos(), "user", field);
        }
    }

    private void saveKernelCallstack(Integer cpu, long ts, String domain, ITmfEventField field) {
        Object value = field.getValue();
        if (!(value instanceof long[])) {
            return;
        }
        final ITmfStateSystemBuilder ss = NonNullUtils.checkNotNull(getStateSystemBuilder());
        long[] callstack = (long[]) value;
        int csAttrib = ss.getQuarkAbsoluteAndAdd("Domain", domain, "cpu", String.valueOf(cpu), "callstack");
        ITmfStateValue sizeValue = ss.queryOngoingState(csAttrib);
        int oldSize = 0;
        if (!sizeValue.isNull()) {
            oldSize = sizeValue.unboxInt();
        }
        for (int i = 0; i < callstack.length; i++) {
            int noAttrib = ss.getQuarkRelativeAndAdd(csAttrib, String.valueOf(i));
            ss.modifyAttribute(ts, TmfStateValue.newValueLong(callstack[i]), noAttrib);
        }
        for (int i = callstack.length; i < oldSize; i++) {
            int noAttrib = ss.getQuarkRelativeAndAdd(csAttrib, String.valueOf(i));
            ss.removeAttribute(ts, noAttrib);
        }

    }

}
