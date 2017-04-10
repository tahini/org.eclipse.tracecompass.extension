/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.perf.profiling.core.callgraph;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.GroupNode;
import org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.profiling.ProfilingGroup;
import org.eclipse.tracecompass.extension.internal.perf.profiling.core.Activator;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

public class PerfCallchainAnalysisModule extends TmfAbstractAnalysisModule implements ICallGraphProvider {

    private ITmfEventRequest fRequest;
    private final ProfilingGroup fGroupNode = new ProfilingGroup("Data");

    @Override
    protected boolean executeAnalysis(@NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new IllegalStateException("Trace has not been set, yet the analysis is being run!");
        }
        /* Cancel any previous request */
        ITmfEventRequest request = fRequest;
        if ((request != null) && (!request.isCompleted())) {
            request.cancel();
        }

        try {
            request = new PerfCallchainEventRequest(trace);
            fRequest = request;
            trace.sendRequest(request);

            request.waitForCompletion();
        } catch (InterruptedException e) {
            Activator.getInstance().logError("Request interrupted", e); //$NON-NLS-1$
        }
        return request.isCompleted();
    }

    @Override
    protected void canceling() {
        ITmfEventRequest req = fRequest;
        if ((req != null) && (!req.isCompleted())) {
            req.cancel();
        }
    }

    private class PerfCallchainEventRequest extends TmfEventRequest {
        private final ITmfTrace fTrace;

        /**
         * Constructor
         * @param trace The trace
         */
        public PerfCallchainEventRequest(ITmfTrace trace) {
            super(TmfEvent.class,
                    TmfTimeRange.ETERNITY,
                    0,
                    ITmfEventRequest.ALL_DATA,
                    ITmfEventRequest.ExecutionType.BACKGROUND);
            fTrace = trace;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            if (event.getTrace() == fTrace) {
                processEvent(event);
            } else if (fTrace instanceof TmfExperiment) {
                /*
                 * If the request is for an experiment, check if the event is
                 * from one of the child trace
                 */
                for (ITmfTrace childTrace : ((TmfExperiment) fTrace).getTraces()) {
                    if (childTrace == event.getTrace()) {
                        processEvent(event);
                    }
                }
            }
        }

        private void processEvent(ITmfEvent event) {
            if (!event.getName().equals("cycles")) {
                return;
            }
            // Get the callchain is available
            ITmfEventField field = event.getContent().getField("perf_callchain");
            if (field == null) {
                return;
            }
            long[] value = (long[]) field.getValue();
            int size = value.length;
            long tmp;
            for (int i = 0, mid = size >> 1, j = size - 1; i < mid; i++, j--) {
                tmp = value[i];
                value[i] = value[j];
                value[j] = tmp;
            }
            ProfilingGroup lgn = getLeafGroup(event);
            lgn.addStackTrace(value);
        }

        /**
         * @param event
         */
        private ProfilingGroup getLeafGroup(ITmfEvent event) {
            // TODO: see if we can add a group
            return fGroupNode;
        }
    }

    @Override
    public Collection<GroupNode> getGroups() {
        return Collections.singleton(fGroupNode);
    }
}
