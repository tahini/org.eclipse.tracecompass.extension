/*******************************************************************************
 * Copyright (c) 2013, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.statesystem;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.CallStackSeries;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

/**
 * The base classes for analyses who want to populate the CallStack state
 * system.
 *
 * @author Matthew Khouzam
 * @author Geneviève Bastien
 */
public abstract class CallStackAnalysis extends TmfStateSystemAnalysisModule implements ICallStackProvider {

    private static final String[] DEFAULT_PROCESSES_PATTERN = new String[] { CallStackStateProvider.PROCESSES, "*" }; //$NON-NLS-1$
    private static final String[] DEFAULT_THREADS_PATTERN = new String[] { "*" }; //$NON-NLS-1$
    private static final String[] DEFAULT_CALL_STACK_PATH = new String[] { CallStackStateProvider.CALL_STACK };

    private static final List<String[]> PATTERNS = ImmutableList.of(DEFAULT_PROCESSES_PATTERN, DEFAULT_THREADS_PATTERN, DEFAULT_CALL_STACK_PATH);

    private @Nullable Collection<@NonNull CallStackSeries> fCallStacks;

    /**
     * Abstract constructor (should only be called via the sub-classes'
     * constructors.
     */
    protected CallStackAnalysis() {
        super();
    }

    @Override
    public Collection<CallStackSeries> getCallStackSeries() {
        Collection<CallStackSeries> callstacks = fCallStacks;
        if (callstacks == null) {
            ITmfStateSystem ss = getStateSystem();
            if (ss == null) {
                return Collections.EMPTY_SET;
            }
            callstacks = Collections.singleton(new CallStackSeries(ss, PATTERNS, 0, "", getHostId(), new CallStackSeries.AttributeValueThreadResolver(1))); //$NON-NLS-1$
            fCallStacks = callstacks;
        }
        return callstacks;
    }

    /**
     * Get the patterns for the process, threads and callstack levels in the
     * state system
     *
     * @return The patterns for the different levels in the state system
     */
    @VisibleForTesting
    protected List<String[]> getPatterns() {
        return PATTERNS;
    }

    @Override
    public @NonNull String getHostId() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return ""; //$NON-NLS-1$
        }
        return trace.getHostId();
    }

}