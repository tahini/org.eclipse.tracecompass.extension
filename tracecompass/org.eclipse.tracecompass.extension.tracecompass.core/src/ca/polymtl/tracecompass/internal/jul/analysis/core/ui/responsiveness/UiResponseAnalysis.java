/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package ca.polymtl.tracecompass.internal.jul.analysis.core.ui.responsiveness;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.AggregatedCalledFunction;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.GroupNode;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.internal.provisional.analysis.timing.core.callstack.CallStack;
import org.eclipse.tracecompass.internal.provisional.analysis.timing.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.internal.provisional.analysis.timing.core.callstack.ICallStackLeafElement;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.pattern.stateprovider.XmlPatternAnalysis;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

/**
 *
 *
 * @author Geneviève Bastien
 */
public class UiResponseAnalysis extends TmfAbstractAnalysisModule {

    /**
     * ID of this analysis module
     */
    public static final String ID = "ca.polymtl.tracecompass.jul.analysis.core.ui.response.analysis"; //$NON-NLS-1$
    private static final String XML_ANALYSIS_ID = "ca.polymtl.tracecompass.jul.ui.response"; //$NON-NLS-1$
    private static final String XML_ANALYSIS_ID2 = "ca.polymtl.tracecompass.jul.ui.response.lttng"; //$NON-NLS-1$
    // private static final String TRACE_TYPE_ID =
    // "custom.txt.trace:TMF:TraceCompassLog"; //$NON-NLS-1$
    // private static final String TRACE_TYPE_ID2 =
    // "ca.polymtl.tracecompass.lttng.jul.trace"; //$NON-NLS-1$

    private final Table<String, String, @Nullable PerViewStatistics> fObjectStatistics = HashBasedTable.create();

    public Table<String, String, @Nullable PerViewStatistics> getStats() {
        return fObjectStatistics;
    }

    public static class PerViewStatistics {
        Map<String, @Nullable UiResponseStatistics> fMap = new HashMap<>();

        public PerViewStatistics() {
        }

        public UiResponseStatistics getForThread(String thread) {
            @Nullable
            UiResponseStatistics stats = fMap.get(thread);
            if (stats == null) {
                stats = new UiResponseStatistics();
                fMap.put(thread, stats);
            }
            return stats;
        }

        public Map<String, @Nullable UiResponseStatistics> getMap() {
            return fMap;
        }

        @Override
        public String toString() {
            return fMap.toString();
        }
    }

    @Override
    protected @NonNull Iterable<@NonNull IAnalysisModule> getDependentAnalyses() {
        @Nullable
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new IllegalStateException("The trace should not be null at this point"); //$NON-NLS-1$
        }
        @Nullable
        IAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, IAnalysisModule.class, XML_ANALYSIS_ID);
        if (module == null) {
            module = trace.getAnalysisModule(XML_ANALYSIS_ID2);
            if (module == null) {
                return Collections.EMPTY_SET;
            }
        }
        CallGraphAnalysis cgModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, CallGraphAnalysis.class, "org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph.callgraphanalysis");
        if (cgModule == null) {
            return Collections.EMPTY_SET;
        }
        return ImmutableSet.of(module, cgModule);
    }

    @Override
    public boolean canExecute(@NonNull ITmfTrace trace) {
        // @Nullable
        // String traceTypeId = trace.getTraceTypeId();
        // if (traceTypeId == null || !(traceTypeId.equals(TRACE_TYPE_ID) ||
        // traceTypeId.equals(TRACE_TYPE_ID2))) {
        // return false;
        // }
        return super.canExecute(trace);
    }

    @Override
    protected boolean executeAnalysis(@NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        @Nullable
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new IllegalStateException("The trace should not be null at this point"); //$NON-NLS-1$
        }

        XmlPatternAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, XmlPatternAnalysis.class, XML_ANALYSIS_ID);
        if (module == null) {
            module = TmfTraceUtils.getAnalysisModuleOfClass(trace, XmlPatternAnalysis.class, XML_ANALYSIS_ID2);
            if (module == null) {
                return true;
            }
        }
        if (!module.waitForCompletion()) {
            return false;
        }

        CallGraphAnalysis callgraphModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, CallGraphAnalysis.class, "org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph.callgraphanalysis");
        if (callgraphModule == null) {
            return true;
        }
        if (!callgraphModule.waitForCompletion()) {
            return false;
        }

        ISegmentStore<ISegment> segmentStore = callgraphModule.getSegmentStore();
        if (segmentStore == null) {
            return true;
        }
        doAnalysis(monitor, callgraphModule);

        return true;
    }

    private boolean doAnalysis(IProgressMonitor monitor, CallGraphAnalysis callgraphModule) {
        for (GroupNode gn : callgraphModule.getGroupNodes()) {
            if (monitor.isCanceled()) {
                return false;
            }
            ICallStackLeafElement element = gn.getElement();

            // The view is the
            ICallStackElement threadEl = element.getParentElement();
            if (threadEl == null) {
                continue;
            }
            ICallStackElement traceEl = threadEl.getParentElement();
            if (traceEl == null) {
                continue;
            }
            String traceName = traceEl.getName();
            ICallStackElement viewEl = traceEl.getParentElement();
            if (viewEl == null) {
                continue;
            }
            String viewId = viewEl.getName();

            PerViewStatistics stats = fObjectStatistics.get(traceName, viewId);
            if (stats == null) {
                stats = new PerViewStatistics();
                fObjectStatistics.put(traceName, viewId, stats);
            }

            for (AggregatedCalledFunction child : gn.getChildren()) {
                UiResponseStatistics threadStats = stats.getForThread(child.getSymbol().toString());
                threadStats.merge(child);
            }

            // Add stats for cache hit and misses
            CallStack callStack = element.getCallStack();

            for (ICalledFunction func : callStack.getCallListAtDepth(1, callStack.getStartTime(), Long.MAX_VALUE, 1, monitor)) {
                Object cacheLookup = callStack.getExtraAttribute("cacheLookup", func.getEnd() - 1);
                Object cacheMiss = callStack.getExtraAttribute("cacheMiss", func.getEnd() - 1);
                int lookups = 0;
                int misses = 0;
                if (cacheLookup != null) {
                    lookups = (int) cacheLookup;
                }
                if (cacheMiss != null) {
                    misses = (int) cacheMiss;
                }
                UiResponseStatistics threadStats = stats.getForThread(func.getSymbol().toString());
                threadStats.addCacheData(lookups, misses);
            }

        }

        return true;
    }

    @Override
    protected void canceling() {

    }

}
