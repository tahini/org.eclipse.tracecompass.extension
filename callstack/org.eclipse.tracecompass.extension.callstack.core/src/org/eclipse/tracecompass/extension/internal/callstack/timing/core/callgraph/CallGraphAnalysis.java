/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.callstack.timing.core.callgraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.common.core.StreamUtils;
import org.eclipse.tracecompass.extension.internal.callstack.timing.core.callstack.CallStackAllGroupDescriptor;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.CallStack;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.CallStackSeries;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackLeafElement;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackProvider;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentStoreFactory;
import org.eclipse.tracecompass.segmentstore.core.SegmentStoreFactory.SegmentStoreType;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

/**
 * Call stack analysis used to create a segment for each call function from an
 * entry/exit event. It builds a segment tree from the state system. An example
 * taken from the Fibonacci trace's callStack shows the structure of the segment
 * tree given by this analysis:
 *
 * <pre>
 * (Caller)  main
 *            ↓↑
 * (Callee) Fibonacci
 *           ↓↑    ↓↑
 *      Fibonacci Fibonacci
 *         ↓↑         ↓↑
 *         ...        ...
 * </pre>
 *
 * @author Sonia Farrah
 */
public class CallGraphAnalysis extends TmfAbstractAnalysisModule implements ISegmentStoreProvider {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * Segment store
     */
    private final ISegmentStore<@NonNull ISegment> fStore;

    /**
     * Listeners
     */
    private final ListenerList fListeners = new ListenerList(ListenerList.IDENTITY);

    /**
     * The Trace's root functions list
     */
    private final List<ICalledFunction> fRootFunctions = new ArrayList<>();

    /**
     * The List of thread nodes. Each thread has a virtual node having the root
     * function as children
     */
    private List<GroupNode> fThreadNodes = new ArrayList<>();

    private @Nullable ICallStackGroupDescriptor fGroupBy = null;

    /**
     * Default constructor
     */
    public CallGraphAnalysis() {
        super();
        fStore = SegmentStoreFactory.createSegmentStore(SegmentStoreType.Fast);
    }

    @Override
    public @NonNull String getHelpText() {
        String msg = Messages.CallGraphAnalysis_Description;
        return (msg != null) ? msg : super.getHelpText();
    }

    @Override
    public @NonNull String getHelpText(@NonNull ITmfTrace trace) {
        return getHelpText();
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        /*
         * FIXME: change to !Iterables.isEmpty(getDependentAnalyses()) when
         * analysis dependencies work better
         */
        return true;
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        return TmfTraceManager.getTraceSet(getTrace()).stream()
                .flatMap(trace -> StreamUtils.getStream(TmfTraceUtils.getAnalysisModulesOfClass(trace, ICallStackProvider.class)))
                .distinct().collect(Collectors.toList());
    }

    @Override
    protected boolean executeAnalysis(@Nullable IProgressMonitor monitor) {
        ITmfTrace trace = getTrace();
        if (monitor == null || trace == null) {
            return false;
        }
        Iterable<IAnalysisModule> dependentAnalyses = getDependentAnalyses();
        for (IAnalysisModule module : dependentAnalyses) {
            if (!(module instanceof ICallStackProvider)) {
                return false;
            }
            module.schedule();
        }
        // TODO:Look at updates while the state system's being built
        dependentAnalyses.forEach((t) -> t.waitForCompletion(monitor));
        for (IAnalysisModule module : dependentAnalyses) {
            ICallStackProvider callstackModule = (ICallStackProvider) module;
            IHostModel model = ModelManager.getModelFor(callstackModule.getHostId());

            for (CallStackSeries callstack : callstackModule.getCallStackSeries()) {
                if (!iterateOverCallstackSerie(callstack, model, monitor)) {
                    return false;
                }
            }
        }
        monitor.worked(1);
        monitor.done();
        return true;

    }

    /**
     * Iterate over a callstack series. It will do a depth-first search to
     * create teh callgraph
     *
     * @param callstackSerie
     *            The series to iterate over
     * @param model
     *            The model of the host on which this callstack was running
     * @param monitor
     *            A progress monitor
     * @return Whether the series was successfully iterated over
     */
    @VisibleForTesting
    protected boolean iterateOverCallstackSerie(CallStackSeries callstackSerie, IHostModel model, IProgressMonitor monitor) {
        List<ICallStackLeafElement> finalElements = callstackSerie.getLeafElements();
        for (ICallStackLeafElement element : finalElements) {
            if (monitor.isCanceled()) {
                return false;
            }
            CallStack callStack = element.getCallStack();

            // Get the symbol key element for this callstack element
            int symbolKey = callStack.getSymbolKeyAt(callStack.getStartTime());
            int threadId = callStack.getThreadId(callStack.getStartTime());

            // Create a root segment
            ICallStackElement parentElement = element.getParentElement();
            String name = parentElement != null ? parentElement.getName() : element.getName();
            AbstractCalledFunction rootSegment = CalledFunctionFactory.create(0, 0, 0, name, symbolKey, threadId, null, model);
            GroupNode parentNode = new GroupNode(rootSegment, element, callStack.getMaxDepth(), name);
            fThreadNodes.add(parentNode);

            AbstractCalledFunction nextFunction = (AbstractCalledFunction) callStack.getNextFunction(callStack.getStartTime(), 1, null, model);
            while (nextFunction != null) {
                AggregatedCalledFunction aggregatedChild = new AggregatedCalledFunction(nextFunction, parentNode);
                iterateOverCallstack(callStack, nextFunction, 2, aggregatedChild, model, monitor);
                fRootFunctions.add(nextFunction);
                parentNode.addChild(nextFunction, aggregatedChild);
                nextFunction = (AbstractCalledFunction) callStack.getNextFunction(nextFunction.getEnd(), 1, null, model);
            }
        }
        return true;
    }

    private void iterateOverCallstack(CallStack callstack, ICalledFunction function, int nextLevel, AggregatedCalledFunction aggregatedCall, IHostModel model, IProgressMonitor monitor) {
        fStore.add(function);
        if (nextLevel > callstack.getMaxDepth()) {
            return;
        }

        AbstractCalledFunction nextFunction = (AbstractCalledFunction) callstack.getNextFunction(function.getStart(), nextLevel, function, model);
        while (nextFunction != null) {
            AggregatedCalledFunction aggregatedChild = new AggregatedCalledFunction(nextFunction, aggregatedCall);
            iterateOverCallstack(callstack, nextFunction, nextLevel + 1, aggregatedChild, model, monitor);
            aggregatedCall.addChild(nextFunction, aggregatedChild);
            nextFunction = (AbstractCalledFunction) callstack.getNextFunction(nextFunction.getEnd(), nextLevel, function, model);
        }

    }

    /**
     * Get the callstack series of the providers of this analysis
     *
     * @return The collection of callstack series
     */
    public Collection<CallStackSeries> getSeries() {
        List<CallStackSeries> series = new ArrayList<>();
        for (IAnalysisModule dependent : getDependentAnalyses()) {
            if (!(dependent instanceof ICallStackProvider)) {
                continue;
            }
            ICallStackProvider csProvider = (ICallStackProvider) dependent;
            series.addAll(csProvider.getCallStackSeries());
        }
        return series;
    }

    @Override
    public void addListener(@NonNull IAnalysisProgressListener listener) {
        fListeners.add(listener);
    }

    @Override
    public void removeListener(@NonNull IAnalysisProgressListener listener) {
        fListeners.remove(listener);
    }

    @Override
    protected void canceling() {
        // Do nothing
    }

    @Override
    public @Nullable ISegmentStore<@NonNull ISegment> getSegmentStore() {
        return fStore;
    }

    /**
     * Update listeners
     *
     * @param store
     *            The segment store
     */
    protected void sendUpdate(final ISegmentStore<@NonNull ISegment> store) {
        getListeners().forEach(listener -> listener.onComplete(this, store));
    }

    /**
     * Get Listeners
     *
     * @return The listeners
     */
    protected Iterable<IAnalysisProgressListener> getListeners() {
        return Arrays.stream(fListeners.getListeners())
                .filter(listener -> listener instanceof IAnalysisProgressListener)
                .map(listener -> (IAnalysisProgressListener) listener)
                .collect(Collectors.toList());
    }

    /**
     * The functions of the first level
     *
     * @return Functions of the first level
     */
    public List<ICalledFunction> getRootFunctions() {
        return ImmutableList.copyOf(fRootFunctions);
    }

    /**
     * List of thread nodes. Each thread has a virtual node having the root
     * functions called as children.
     *
     * @return The thread nodes
     */
    public List<AggregatedCalledFunction> getGroupNodes() {
        ICallStackGroupDescriptor groupBy = fGroupBy;
        List<GroupNode> threadNodes = fThreadNodes;
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return Collections.emptyList();
        }
        IHostModel model = ModelManager.getModelFor(trace.getHostId());
        if (groupBy instanceof CallStackAllGroupDescriptor) {
            AbstractCalledFunction initSegment = CalledFunctionFactory.create(0, 0, 0, "", 0, 0, null, model); //$NON-NLS-1$
            AggregatedCalledFunction init = new AggregatedCalledFunction(initSegment, 3);
            threadNodes.forEach(
                    tn -> tn.getChildren().forEach(
                            child -> init.addChild(initSegment, child)));
            return Collections.singletonList(init);
        }
        if (groupBy == null) {
            return ImmutableList.copyOf(threadNodes);
        }
        ICallStackGroupDescriptor nextGroup = groupBy.getNextGroup();
        // Leaf group, return with the thread nodes
        if (nextGroup == null) {
            return ImmutableList.copyOf(threadNodes);
        }
        Map<ICallStackElement, GroupNode> map = new HashMap<>();
        // Group the leaf nodes by the requested group descriptor
        AbstractCalledFunction initSegment = CalledFunctionFactory.create(0, 0, 0, "", 0, 0, null, model); //$NON-NLS-1$
        threadNodes.forEach(tn -> {
            ICallStackElement element = tn.getElement(nextGroup);
            GroupNode acf = map.get(element);
            if (acf == null) {
                acf = new GroupNode(initSegment, element, tn.getMaxDepth(), element.getName());
                map.put(element, acf);
            }
            // FIXME The aggregate modifies the child
            final AggregatedCalledFunction aggregate = acf;
            tn.getChildren().forEach(
                    child -> aggregate.addChild(initSegment, child));
        });
        return ImmutableList.copyOf(map.values());
    }

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        return Collections.EMPTY_LIST;
    }

    /**
     * Set the group descriptor by which to group the callgraph data
     *
     * @param descriptor
     *            The descriptor by which to group the callgraph elements, or
     *            <code>null</code> will group them all together
     */
    public void setGroupBy(@Nullable ICallStackGroupDescriptor descriptor) {
        fGroupBy = descriptor;
    }

}