/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.instrumented;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model.IHostModel;

/**
 * This class represents a function call in a certain level in the call stack.
 * It's used to build an aggregation segment tree (aggregated by depth and
 * callers). Per example,the two calls to the function A() in the call graph
 * below will be combined into one node in the generated tree:
 *
 * <pre>
 *   (Depth=0)      main              main
 *               ↓↑  ↓↑   ↓↑    =>   ↓↑   ↓↑
 *   (Depth=1)  A()  B()  A()       A()   B()
 * </pre>
 *
 * @author Sonia Farrah
 *
 */
public class AggregatedCalledFunction extends AggregatedCallSite {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final AggregatedCalledFunctionStatistics fStatistics;
    private long fDuration;
    private long fSelfTime;
    private long fCpuTime;
    private final int fProcessId;

    /**
     * Constructor, parent is not null
     *
     * @param function
     *            called function
     * @param parent
     *            the parent entry
     */
    public AggregatedCalledFunction(Object symbol) {
        super(symbol);
        fStatistics = new AggregatedCalledFunctionStatistics();
        fProcessId = -1;
    }

    /**
     * Constructor, parent is not null
     *
     * @param function
     *            called function
     * @param parent
     *            the parent entry
     */
    public AggregatedCalledFunction(ICalledFunction function) {
        super(function.getSymbol());
        fDuration = function.getLength();
        fSelfTime = function.getLength();
        fProcessId = function.getProcessId();
        fCpuTime = function.getCpuTime();
        fStatistics = new AggregatedCalledFunctionStatistics();
    }

    /**
     * Root constructor, parent is null
     *
     * @param calledFunction
     *            the called function
     * @param maxDepth
     *            the maximum depth
     */
    public AggregatedCalledFunction(AbstractCalledFunction calledFunction, int maxDepth) {
        super(calledFunction.getSymbol());
        fDuration = calledFunction.getLength();
        fSelfTime = calledFunction.getLength();
        fProcessId = calledFunction.getProcessId();
        fCpuTime = calledFunction.getCpuTime();
        fStatistics = new AggregatedCalledFunctionStatistics();
    }

    @Override
    public long getLength() {
        return fDuration;
    }

    @Override
    protected void mergeData(@NonNull AggregatedCallSite other) {
        if (!(other instanceof AggregatedCalledFunction)) {
            return;
        }
        AggregatedCalledFunction otherFct = (AggregatedCalledFunction) other;

        addToDuration(otherFct.getDuration());
        addToSelfTime(otherFct.getSelfTime());
        addToCpuTime(otherFct.getCpuTime());
        getFunctionStatistics().merge(otherFct.getFunctionStatistics(), true);
    }

    /**
     * Add a new callee into the Callees list. If the function exists in the
     * callees list, the new callee's duration will be added to its duration and
     * it'll combine their callees.
     *
     * @param child
     *            The callee to add to this function
     * @param aggregatedChild
     *            The aggregated data of the callee
     */
    public synchronized void addChild(AbstractCalledFunction child, AggregatedCalledFunction aggregatedChild) {
        // Update the child's statistics with itself
        fSelfTime -= aggregatedChild.getDuration();
        aggregatedChild.addFunctionCall(child);
        super.addChild(aggregatedChild);
    }

    public synchronized void addFunctionCall(AbstractCalledFunction call) {
        getFunctionStatistics().update(call);
    }

    /**
     * Modify the function's duration
     *
     * @param duration
     *            The amount to increment the duration by
     */
    private void addToDuration(long duration) {
        fDuration += duration;
    }

//    /**
//     * Merge the callees of two functions.
//     *
//     * @param firstNode
//     *            The first parent secondNode The second parent
//     */
//    private static void mergeChildren(AggregatedCalledFunction firstNode, AggregatedCalledFunction secondNode) {
//        for (Map.Entry<Object, AggregatedCalledFunction> FunctionEntry : secondNode.fChildren.entrySet()) {
//            Object childSymbol = NonNullUtils.checkNotNull(FunctionEntry.getKey());
//            AggregatedCalledFunction secondNodeChild = NonNullUtils.checkNotNull(FunctionEntry.getValue());
//            AggregatedCalledFunction aggregatedCalledFunction = firstNode.fChildren.get(childSymbol);
//            if (aggregatedCalledFunction == null) {
//                firstNode.fChildren.put(secondNodeChild.getSymbol(), secondNodeChild);
//            } else {
//                // combine children
//                AggregatedCalledFunction firstNodeChild = aggregatedCalledFunction;
//                merge(firstNodeChild, secondNodeChild, true);
//                firstNode.fChildren.replace(firstNodeChild.getSymbol(), firstNodeChild);
//            }
//        }
//    }
//
//    /**
//     * Merge two functions, add durations, self times, increment the calls,
//     * update statistics and merge children.
//     *
//     * @param destination
//     *            the node to merge to
//     * @param source
//     *            the node to merge
//     */
//    private static void merge(AggregatedCalledFunction destination, AggregatedCalledFunction source, boolean isGroup) {
//        long sourceDuration = source.getDuration();
//        long sourceSelfTime = source.getSelfTime();
//        destination.addToDuration(sourceDuration);
//        destination.addToSelfTime(sourceSelfTime);
//        destination.addToCpuTime(source.getCpuTime());
//        destination.getFunctionStatistics().merge(source.getFunctionStatistics(), isGroup);
//        // merge the children callees.
//        mergeChildren(destination, source);
//    }

    private void addToCpuTime(long cpuTime) {
        if (cpuTime != IHostModel.TIME_UNKNOWN) {
            fCpuTime += cpuTime;
        }
    }

    /**
     * The function's duration
     *
     * @return The duration of the function
     */
    public long getDuration() {
        return fDuration;
    }

    /**
     * The number of calls of a function
     *
     * @return The number of calls of a function
     */
    public long getNbCalls() {
        return fStatistics.getDurationStatistics().getNbElements();
    }

    /**
     * The self time of an aggregated function
     *
     * @return The self time
     */
    public long getSelfTime() {
        return fSelfTime;
    }

    /**
     * Add to the self time of an aggregated function
     *
     * @param selfTime
     *            The amount of self time to add
     */
    private void addToSelfTime(long selfTime) {
        fSelfTime += selfTime;
    }

    /**
     * The self time of an aggregated function
     *
     * @return The self time
     */
    public long getCpuTime() {
        return fCpuTime;
    }

    /**
     * The process ID of the trace application.
     * @return The process Id
     */
    public int getProcessId() {
        return fProcessId;
    }

    /**
     * The function's statistics
     *
     * @return The function's statistics
     */
    public AggregatedCalledFunctionStatistics getFunctionStatistics() {
        return fStatistics;
    }

    @Override
    public String toString() {
        return "Aggregate Function: " + getSymbol() + ", Duration: " + getDuration() + ", Self Time: " + fSelfTime + " on " + getNbCalls() + " calls"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

}
