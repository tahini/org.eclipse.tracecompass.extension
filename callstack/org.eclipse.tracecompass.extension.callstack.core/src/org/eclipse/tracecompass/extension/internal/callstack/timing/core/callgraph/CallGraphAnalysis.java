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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.common.core.StreamUtils;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.CallStack;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.CallStackSeries;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackElement;
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

    // /**
    // * Iterate over the process of the state system,then iterate over the
    // * different threads of each process.
    // *
    // * @param ss
    // * The state system
    // * @param threadsPattern
    // * The threads pattern
    // * @param processesPattern
    // * The processes pattern
    // * @param callStackPath
    // * The call stack path
    // * @param monitor
    // * The monitor
    // * @return Boolean
    // */
    // @VisibleForTesting
    // protected boolean iterateOverProcesses(@Nullable ITmfStateSystem ss,
    // String[] threadsPattern, String[] processesPattern, String[]
    // callStackPath, IProgressMonitor monitor) {
    // if (ss == null) {
    // return false;
    // }
    // List<Integer> processQuarks = ss.getQuarks(processesPattern);
    // for (int processQuark : processQuarks) {
    // int processId = getProcessId(ss, processQuark, ss.getCurrentEndTime());
    // for (int threadQuark : ss.getQuarks(processQuark, threadsPattern)) {
    // if (!iterateOverThread(ss, processId, threadQuark, callStackPath,
    // monitor)) {
    // return false;
    // }
    // }
    // }
    // sendUpdate(fStore);
    // return true;
    // }
    //
    // /**
    // * Iterate over functions with the same quark,search for their callees
    // then
    // * add them to the segment store
    // *
    // * @param stateSystem
    // * The state system
    // * @param processId
    // * The process ID of the traced application
    // * @param threadQuark
    // * The thread quark
    // * @param subAttributePath
    // * sub-Attributes path
    // * @param monitor
    // * The monitor
    // * @return Boolean
    // */
    // private boolean iterateOverThread(ITmfStateSystem stateSystem, int
    // processId, int threadQuark, String[] subAttributePath, IProgressMonitor
    // monitor) {
    //
    // }
    //
    // /**
    // * Find the functions called by a parent function in a call stack then add
    // * segments for each child, updating the self times of each node
    // * accordingly.
    // *
    // * @param functionCall
    // * The segment of the stack call event(the parent) callStackQuark
    // * @param depth
    // * The depth of the parent function
    // * @param ss
    // * The quark of the segment parent ss The actual state system
    // * @param callStackQuarks
    // * The last quark in the state system
    // * @param aggregatedCall
    // * A node in the aggregation tree
    // * @param processId
    // * The process ID of the traced application
    // * @param monitor
    // * The progress monitor The progress monitor TODO: if stack size
    // * is an issue, convert to a stack instead of recursive function
    // */
    // private boolean iterateOnStackRecursive(AbstractCalledFunction
    // functionCall, int depth, ITmfStateSystem ss, List<Integer>
    // callStackQuarks, AggregatedCalledFunction aggregatedCall, int processId,
    // IProgressMonitor monitor) {
    // fStore.add(functionCall);
    //
    // // Quick return if we reached the end of the stack
    // if (callStackQuarks.size() <= depth + 1) {
    // return true;
    // }
    // long curTime = functionCall.getStart();
    // long limit = functionCall.getEnd();
    // ITmfStateInterval interval = null;
    // while (curTime < limit) {
    // if (monitor.isCanceled()) {
    // return false;
    // }
    // try {
    // interval = ss.querySingleState(curTime, callStackQuarks.get(depth + 1));
    // } catch (StateSystemDisposedException e) {
    // Activator.getInstance().logError(Messages.QueringStateSystemError, e);
    // return false;
    // }
    // ITmfStateValue stateValue = interval.getStateValue();
    // if (!stateValue.isNull()) {
    // long intervalStart = interval.getStartTime();
    // long intervalEnd = interval.getEndTime();
    // if (intervalStart < functionCall.getStart() || intervalEnd > limit) {
    // return true;
    // }
    // AbstractCalledFunction childCall =
    // CalledFunctionFactory.create(intervalStart, intervalEnd + 1,
    // functionCall.getDepth() + 1, stateValue, processId, functionCall);
    // AggregatedCalledFunction childAggregated = new
    // AggregatedCalledFunction(childCall, aggregatedCall);
    // // Search for the children with the next quark.
    // iterateOnStackRecursive(childCall, depth + 1, ss, callStackQuarks,
    // childAggregated, processId, monitor);
    // aggregatedCall.addChild(childAggregated);
    // functionCall.addChild(childCall);
    // }
    // curTime = interval.getEndTime() + 1;
    // }
    // return true;
    // }

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
    public List<GroupNode> getGroupNodes() {
        return ImmutableList.copyOf(fThreadNodes);
    }

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        return Collections.EMPTY_LIST;
    }

}