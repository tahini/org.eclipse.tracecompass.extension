/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package ca.polymtl.tracecompass.internal.jul.analysis.core.ui.responsiveness;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.AggregatedCalledFunction;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.AggregatedCalledFunctionStatistics;

import com.google.common.collect.ImmutableMap;

/**
 * @author Geneviève Bastien
 */
public class UiResponseStatistics extends AggregatedCalledFunctionStatistics {

    private int fCacheHit = 0;
    private int fCacheMiss = 0;

    private final Map<String, AggregatedCalledFunctionStatistics> fSubStatMap = new HashMap<>();

    // private static class SubStatistics extends
    // AggregatedCalledFunctionStatistics {
    //
    // private long fMin = Long.MAX_VALUE;
    // private long fMax = Long.MIN_VALUE;
    // private double fAvg;
    // private double fVariance;
    // private int fAvgNb;
    // private int fNbSamples = 0;
    // private long fAvgSelfTime = 0;
    // private long fAvgCpuTime = 0;
    //
    // public SubStatistics(@NonNull AggregatedCalledFunctionStatistics
    // functionStatistics) {
    // // TODO Auto-generated constructor stub
    // }
    //
    // @Override
    // public long getMin() {
    // return fMin;
    // }
    //
    // @Override
    // public long getMax() {
    // return fMax;
    // }
    //
    // @Override
    // public long getNbSegments() {
    // return fAvgNb;
    // }
    //
    // @Override
    // public double getAverage() {
    // return fAvg;
    // }
    //
    // @Override
    // public double getStdDev() {
    // return fNbSamples > 2 ? Math.sqrt(fVariance / (fNbSamples - 1)) :
    // Double.NaN;
    // }
    //
    //
    //
    // @Override
    // public double getAverageSelfTime() {
    // return fAvgSelfTime;
    // }
    //
    // @Override
    // public double getAverageCpuTime() {
    // return fAvgCpuTime;
    // }
    //
    // public void update(long nbElements, double duration, long selfTime, long
    // cpuTime) {
    // /* Update the number of sampels */
    // fNbSamples++;
    // /*
    // * Calculate the avg number of elements
    // */
    // double delta = nbElements - fAvgNb;
    // fAvgNb += delta / fNbSamples;
    // /*
    // * Calculate the average times
    // */
    // delta = duration - fAvg;
    // fAvg += delta / fNbSamples;
    // fVariance += delta * (duration - fAvg);
    // if (duration < fMin) {
    // fMin = duration;
    // }
    // if (duration > fMax) {
    // fMax = duration;
    // }
    // delta = selfTime - fAvgSelfTime;
    // fAvgSelfTime += delta / fNbSamples;
    // delta = cpuTime - fAvgCpuTime;
    // fAvgCpuTime += delta / fNbSamples;
    // }
    //
    // @Override
    // public double getTotal() {
    // return (long) fAvg;
    // }
    //
    // }

    /**
     * Constructor
     */
    public UiResponseStatistics() {
        super();
    }

    public void addChild(AggregatedCalledFunction childFunction) {
        String fctName = childFunction.getSymbol().toString();
        AggregatedCalledFunctionStatistics subStatistics = fSubStatMap.get(fctName);
        if (subStatistics != null) {
            subStatistics.merge(childFunction.getFunctionStatistics(), true);
        } else {
            subStatistics = childFunction.getFunctionStatistics();
        }
        fSubStatMap.put(fctName, subStatistics);

        /* Update average time of first refresh */
        // Integer nbRefreshes =
        // UiResponseUtils.getIntSegmentContentValue(xmlSegment, "refresh");
        // if (nbRefreshes != null && nbRefreshes > 0) {
        // Integer threadId =
        // UiResponseUtils.getIntSegmentContentValue(xmlSegment, "threadID");
        // if (threadId != null) {
        // int refreshQuark = fSs.optQuarkRelative(fComponentQuark, "Threads",
        // String.valueOf(threadId), "refresh");
        // if (refreshQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
        // try {
        // @NonNull
        // ITmfStateInterval firstRefresh =
        // fSs.querySingleState(xmlSegment.getStart(), refreshQuark);
        // if (firstRefresh.getEndTime() <= xmlSegment.getEnd()) {
        // long duration = firstRefresh.getEndTime() - xmlSegment.getStart();
        // SubStatistics subStatistics = fSubStatMap.get("time to first
        // refresh");
        // if (subStatistics == null) {
        // subStatistics = new SubStatistics();
        // fSubStatMap.put("time to first refresh", subStatistics);
        // }
        // subStatistics.update(1, duration);
        // }
        //
        // } catch (StateSystemDisposedException e) {
        // e.printStackTrace();
        // }
        // }
        // }
        // }
        //
        // /* Get statistics for call stack of this thread */
        // switch (xmlSegment.getName()) {
        // case "seg_TimeGraphView:BuildThreadStart":
        // case "seg_TimeGraphView:ZoomThreadStart": {
        // // Get the first level of the callstack of this thread
        // Integer threadId =
        // UiResponseUtils.getIntSegmentContentValue(xmlSegment, "threadID");
        // int callStackQuark = fSs.optQuarkRelative(fComponentQuark, "Threads",
        // String.valueOf(threadId), "callstack", "2");
        // if (callStackQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
        // break;
        // }
        // try {
        // Map<String, Pair<Integer, Long>> countDurations = new HashMap<>();
        // List<ITmfStateInterval> intervals =
        // StateSystemUtils.queryHistoryRange(fSs, callStackQuark,
        // xmlSegment.getStart(), xmlSegment.getEnd());
        // for (ITmfStateInterval interval : intervals) {
        // if (!interval.getStateValue().isNull()) {
        // String fctName = interval.getStateValue().unboxStr();
        //
        // Pair<Integer, Long> countDuration = countDurations.get(fctName);
        //
        // if (countDuration == null) {
        // countDuration = new Pair<>(0,0L);
        // }
        // Integer count = countDuration.getFirst();
        // Long duration = countDuration.getSecond();
        // count++;
        // duration += (interval.getEndTime() - interval.getStartTime());
        //
        // countDurations.put(fctName, new Pair<>(count, duration));
        // }
        // }
        // countDurations.entrySet().stream().forEach(e -> {
        // SubStatistics subStatistics = fSubStatMap.get(e.getKey());
        // if (subStatistics == null) {
        // subStatistics = new SubStatistics();
        // fSubStatMap.put(e.getKey(), subStatistics);
        // }
        // subStatistics.update(e.getValue().getFirst(),
        // e.getValue().getSecond());
        // });
        //
        // } catch (AttributeNotFoundException | StateSystemDisposedException e)
        // {
        // e.printStackTrace();
        // }
        // }
        // break;
        // default:
        // break;
        // }
    }

    /**
     * @return
     */
    public Map<String, SegmentStoreStatistics> getChildren() {
        return ImmutableMap.copyOf(fSubStatMap);
    }

    public void merge(AggregatedCalledFunction function) {
        AggregatedCalledFunctionStatistics functionStatistics = function.getFunctionStatistics();
        merge(functionStatistics, false);
        for (AggregatedCalledFunction child : function.getChildren()) {
            addChild(child);
        }

    }

    public void addCacheData(int lookups, int misses) {
        fCacheHit += (lookups - misses);
        fCacheMiss += misses;
    }

    public int getCacheHit() {
        return fCacheHit;
    }

    public int getCacheMiss() {
        return fCacheMiss;
    }

}
