/*******************************************************************************
 * Copyright (c) 2015, 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bernd Hufmann - Initial API and implementation
 *******************************************************************************/
package ca.polymtl.tracecompass.internal.jul.analysis.ui.responsiveness.views;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.SegmentStoreStatistics;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsViewer;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.AggregatedCalledFunctionStatistics;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;

import com.google.common.collect.Table;

import ca.polymtl.tracecompass.internal.jul.analysis.core.ui.responsiveness.UiResponseAnalysis;
import ca.polymtl.tracecompass.internal.jul.analysis.core.ui.responsiveness.UiResponseAnalysis.PerViewStatistics;
import ca.polymtl.tracecompass.internal.jul.analysis.core.ui.responsiveness.UiResponseStatistics;

/**
 * An abstract tree viewer implementation for displaying segment store
 * statistics
 *
 * @author Bernd Hufmann
 *
 */
public class UiResponseStatisticsViewer extends AbstractSegmentStoreStatisticsViewer {

    private static final Format FORMATTER =  new DecimalFormat("###");

    /** Provides label for the Segment Store tree viewer cells */
    protected class UiResponse extends SegmentStoreStatisticsLabelProvider {

        @Override
        public String getColumnText(@Nullable Object element, int columnIndex) {

            String value = ""; //$NON-NLS-1$
            if (element instanceof HiddenTreeViewerEntry) {
                if (columnIndex == 0) {
                    value = ((HiddenTreeViewerEntry) element).getName();
                }
            } else if (element instanceof SegmentStoreStatisticsEntry) {
                SegmentStoreStatisticsEntry entry = (SegmentStoreStatisticsEntry) element;
                if (columnIndex == 0) {
                    return beautifyString(String.valueOf(entry.getName()));
                }
                if (entry.getEntry().getNbSegments() > 0) {
                    SegmentStoreStatistics entry2 = entry.getEntry();
                    if (entry2 instanceof AggregatedCalledFunctionStatistics) {
                        AggregatedCalledFunctionStatistics stats = (AggregatedCalledFunctionStatistics) entry2;
                        DisplayModeData data = fMode.getData();
                        if (columnIndex == 1) {
                            value = toFormattedString(data.getMin(stats));
                        } else if (columnIndex == 2) {
                            value = String.valueOf(toFormattedString(data.getMax(stats)));
                        } else if (columnIndex == 3) {
                            value = String.valueOf(toFormattedString(data.getAverage(stats)));
                        } else if (columnIndex == 4) {
                            value = String.valueOf(toFormattedString(data.getStdDev(stats)));
                        } else if (columnIndex == 5) {
                            value = String.valueOf(data.getNb(stats));
                        } else if (columnIndex == 6) {
                            value = String.valueOf(toFormattedString(data.getTotal(stats)));
                        } else if (stats.wasMerged() && columnIndex == 7) {
                            value = FORMATTER.format(stats.getPerCallCalls());
                        } else if (stats.wasMerged() && columnIndex == 8) {
                            value = toFormattedString(data.getPerCall(stats));
                        } else if (columnIndex == 9) {
                            if (stats instanceof UiResponseStatistics) {
                                value = String.valueOf(((UiResponseStatistics) stats).getCacheHit());
                            }
                        } else if (columnIndex == 10) {
                            if (stats instanceof UiResponseStatistics) {
                                value = String.valueOf(((UiResponseStatistics) stats).getCacheMiss());
                            }
                        }
                    }

                }
            }
            return checkNotNull(value);
        }
    }

    /**
     * Constructor
     *
     * @param parent
     *            the parent composite
     */
    public UiResponseStatisticsViewer(Composite parent) {
        super(parent);
        setLabelProvider(new UiResponse());
    }

    /**
     * Gets the statistics analysis module
     *
     * @return the statistics analysis module
     */
    @Override
    protected @Nullable TmfAbstractAnalysisModule createStatisticsAnalysiModule() {
        return new UiResponseAnalysis();
    }

    @Override
    protected @Nullable ITmfTreeViewerEntry updateElements(long start, long end, boolean isSelection) {
        if (isSelection || (start == end)) {
            return null;
        }

        TmfAbstractAnalysisModule analysisModule = getStatisticsAnalysisModule();

        if (getTrace() == null || !(analysisModule instanceof UiResponseAnalysis)) {
            return null;
        }
        UiResponseAnalysis module = (UiResponseAnalysis) analysisModule;
        module.waitForCompletion();

        TmfTreeViewerEntry root = new TmfTreeViewerEntry(""); //$NON-NLS-1$
        Table<String, String, @Nullable PerViewStatistics> stats = module.getStats();

        List<ITmfTreeViewerEntry> entryList = root.getChildren();
        for (String trace : stats.rowKeySet()) {
            TmfTreeViewerEntry child = new HiddenTreeViewerEntry(trace);
            entryList.add(child);
            for (String component : stats.columnKeySet()) {
                PerViewStatistics perViewStats = stats.get(trace, component);
                if (perViewStats != null) {
                    TmfTreeViewerEntry componentChild = new HiddenTreeViewerEntry(component.substring(component.lastIndexOf(".") + 1));
                    child.addChild(componentChild);
                    for (Entry<String, @Nullable UiResponseStatistics> entry : perViewStats.getMap().entrySet()) {
                        UiResponseStatistics value = NonNullUtils.checkNotNull(entry.getValue());
                        TmfTreeViewerEntry statChild = new SegmentStoreStatisticsEntry(beautifyString(entry.getKey()), value);
                        componentChild.addChild(statChild);
                        Map<String, SegmentStoreStatistics> children = value.getChildren();
                        children.entrySet().stream().forEach(e -> {
                            TmfTreeViewerEntry otherChild = new SegmentStoreStatisticsEntry(beautifyString(e.getKey()), e.getValue());
                            statChild.addChild(otherChild);
                        });

                    }
                }
            }
        }

        return root;
    }

    private static String beautifyString(String string) {
        switch (string) {
        case "TimeGraphView:BuildThreadStart":
            return "Build thread";
        case "TimeGraphView:ZoomThreadStart":
            return "Zoom thread";
        case "TimeGraphView:RedrawStart":
            return "Redraw";
        case "TimeGraphView:RefreshStart":
            return "refresh";
        case "StateSystem:SingleQueryStart":
            return "Single query";
        case "StateSystem:FullQueryStart":
            return "Full query";
        default:
            return string;
        }
    }

    @Override
    protected Collection<? extends @Nullable TmfTreeColumnData> getExtraColumnData() {
        List<TmfTreeColumnData> columns = new ArrayList<>();
        TmfTreeColumnData column = new TmfTreeColumnData("Avg Calls per call");
        column.setAlignment(SWT.RIGHT);
        columns.add(column);

        column = new TmfTreeColumnData("Per call time");
        column.setAlignment(SWT.RIGHT);
        columns.add(column);

        column = new TmfTreeColumnData("Cache hit");
        column.setAlignment(SWT.RIGHT);
        columns.add(column);

        column = new TmfTreeColumnData("Cache miss");
        column.setAlignment(SWT.RIGHT);
        columns.add(column);

        return columns;

    }

    public enum DisplayMode {
        CPU(CPU_DISPLAY),
        SELF(SELF_DISPLAY),
        DURATION(DURATION_DISPLAY);

        private final DisplayModeData fData;
        private DisplayMode(DisplayModeData data) {
            fData = data;
        }

        public DisplayModeData getData() {
            return fData;
        }
    }

    private static interface DisplayModeData {
        long getMin(AggregatedCalledFunctionStatistics stats);
        long getMax(AggregatedCalledFunctionStatistics stats);
        double getAverage(AggregatedCalledFunctionStatistics stats);
        double getStdDev(AggregatedCalledFunctionStatistics stats);
        default long getNb(AggregatedCalledFunctionStatistics stats) {
            return stats.getNbSegments();
        }
        double getTotal(AggregatedCalledFunctionStatistics stats);
        double getPerCall(AggregatedCalledFunctionStatistics stats);
    }

    private static final DisplayModeData CPU_DISPLAY = new DisplayModeData() {

        @Override
        public long getMin(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getMinCpuTime();
        }

        @Override
        public long getMax(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getMaxCpuTime();
        }

        @Override
        public double getAverage(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getAverageCpuTime();
        }

        @Override
        public double getStdDev(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getStdDevCpuTime();
        }

        @Override
        public double getTotal(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getTotalCpuTime();
        }

        @Override
        public double getPerCall(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getPerCallCpuTime();
        }

    };

    private static final DisplayModeData SELF_DISPLAY = new DisplayModeData() {

        @Override
        public long getMin(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getMinSelfTime();
        }

        @Override
        public long getMax(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getMaxSelfTime();
        }

        @Override
        public double getAverage(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getAverageSelfTime();
        }

        @Override
        public double getStdDev(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getStdDevSelfTime();
        }

        @Override
        public double getTotal(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getTotalSelfTime();
        }

        @Override
        public double getPerCall(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getPerCallSelfTime();
        }

    };

    private static final DisplayModeData DURATION_DISPLAY  = new DisplayModeData() {

        @Override
        public long getMin(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getMin();
        }

        @Override
        public long getMax(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getMax();
        }

        @Override
        public double getAverage(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getAverage();
        }

        @Override
        public double getStdDev(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getStdDev();
        }

        @Override
        public double getTotal(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getTotal();
        }

        @Override
        public double getPerCall(@NonNull AggregatedCalledFunctionStatistics stats) {
            return stats.getPerCallCalls();
        }

    };

    private DisplayMode fMode = DisplayMode.DURATION;

    public void setMode(DisplayMode mode) {
        fMode = mode;
        refresh();
    }
}
