/*******************************************************************************
 * Copyright (c) 2013, 2016 Ericsson and others.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *   Bernd Hufmann - Updated signal handling
 *   Marc-Andre Laperle - Map from binary file
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.callstack.ui.views.callstack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.extension.internal.callstack.timing.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.extension.internal.callstack.ui.Activator;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.CallStack;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.CallStackSeries;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackLeafElement;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampDelta;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProviderPreferencePage;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderConfigDialog;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphTimeListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphContentProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Main implementation for the Call Stack view
 *
 * @author Patrick Tasse
 */
public class CallStackView extends AbstractTimeGraphView {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /** View ID. */
    public static final String ID = "org.eclipse.tracecompass.internal.analysis.timing.ui.views.callstack"; //$NON-NLS-1$

    private static final String[] COLUMN_NAMES = new String[] {
            Messages.CallStackView_FunctionColumn,
            Messages.CallStackView_DepthColumn,
            Messages.CallStackView_EntryTimeColumn,
            Messages.CallStackView_ExitTimeColumn,
            Messages.CallStackView_DurationColumn
    };

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.CallStackView_ThreadColumn
    };

    private static final Image SYMBOL_KEY_IMAGE = Activator.getDefault().getImageFromPath("icons/obj16/process_obj.gif"); //$NON-NLS-1$
    private static final Image GROUP_IMAGE = Activator.getDefault().getImageFromPath("icons/obj16/thread_obj.gif"); //$NON-NLS-1$
    private static final Image STACKFRAME_IMAGE = Activator.getDefault().getImageFromPath("icons/obj16/stckframe_obj.gif"); //$NON-NLS-1$

    private static final String IMPORT_BINARY_ICON_PATH = "icons/obj16/binaries_obj.gif"; //$NON-NLS-1$

    private static final ImageDescriptor SORT_BY_NAME_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_alpha.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_NAME_REV_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_alpha_rev.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_ID_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_num.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_ID_REV_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_num_rev.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_TIME_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_time.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_TIME_REV_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_time_rev.gif"); //$NON-NLS-1$
    private static final String SORT_OPTION_KEY = "sort.option"; //$NON-NLS-1$

    private enum SortOption {
        BY_NAME, BY_NAME_REV, BY_ID, BY_ID_REV, BY_TIME, BY_TIME_REV
    }

    private @NonNull SortOption fSortOption = SortOption.BY_NAME;
    private @NonNull Comparator<ITimeGraphEntry> fThreadComparator = new ThreadNameComparator(false);
    private Action fSortByNameAction;
    private Action fSortByIdAction;
    private Action fSortByTimeAction;

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    private final Map<ITmfTrace, ISymbolProvider> fSymbolProviders = new HashMap<>();

    // The next event action
    private Action fNextEventAction;

    // The previous event action
    private Action fPrevEventAction;

    // The action to import a binary file mapping */
    private Action fConfigureSymbolsAction;

    // The saved time sync. signal used when switching off the pinning of a view
    private TmfSelectionRangeUpdatedSignal fSavedTimeSyncSignal;

    // The saved window range signal used when switching off the pinning of
    // a view
    private TmfWindowRangeUpdatedSignal fSavedRangeSyncSignal;

    // When set to true, syncToTime() will select the first call stack entry
    // whose current state start time exactly matches the sync time.
    private boolean fSyncSelection = false;

    // ------------------------------------------------------------------------
    // Classes
    // ------------------------------------------------------------------------

    private static class TraceEntry extends TimeGraphEntry {
        public TraceEntry(String name, long startTime, long endTime) {
            super(name, startTime, endTime);
        }

        @Override
        public boolean hasTimeEvents() {
            return false;
        }
    }

    private static class LevelEntry extends TimeGraphEntry {

        private final boolean fIsSymbolKey;

        public LevelEntry(String name, long startTime, long endTime, boolean isSymbolKey) {
            super(name, startTime, endTime);
            fIsSymbolKey = isSymbolKey;
        }

        @Override
        public boolean hasTimeEvents() {
            return false;
        }

        public boolean isSymbolKeyGroup() {
            return fIsSymbolKey;
        }
    }

    private static class ThreadEntry extends TimeGraphEntry {
        // The thread id
        private final long fThreadId;

        public ThreadEntry(String name, long threadId, long startTime, long endTime) {
            super(name, startTime, endTime);
            fThreadId = threadId;
        }

        @Override
        public boolean hasTimeEvents() {
            return false;
        }

        public long getThreadId() {
            return fThreadId;
        }
    }

    private class CallStackComparator implements Comparator<ITimeGraphEntry> {
        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            if (o1 instanceof CallStackEntry && o2 instanceof CallStackEntry) {
                return Integer.compare(((CallStackEntry) o1).getStackLevel(), ((CallStackEntry) o2).getStackLevel());
            }
            return o1.getName().compareTo(o2.getName());
        }
    }

    private static class ThreadNameComparator implements Comparator<ITimeGraphEntry> {
        private boolean reverse;

        public ThreadNameComparator(boolean reverse) {
            this.reverse = reverse;
        }

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            return reverse ? o2.getName().compareTo(o1.getName()) :
                    o1.getName().compareTo(o2.getName());
        }
    }

    private static class ThreadIdComparator implements Comparator<ITimeGraphEntry> {
        private boolean reverse;

        public ThreadIdComparator(boolean reverse) {
            this.reverse = reverse;
        }

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            if (o1 instanceof ThreadEntry && o2 instanceof ThreadEntry) {
                ThreadEntry t1 = (ThreadEntry) o1;
                ThreadEntry t2 = (ThreadEntry) o2;
                return reverse ? Long.compare(t2.getThreadId(), t1.getThreadId()) :
                    Long.compare(t1.getThreadId(), t2.getThreadId());
            }
            return 0;
        }
    }

    private static class ThreadTimeComparator implements Comparator<ITimeGraphEntry> {
        private boolean reverse;

        public ThreadTimeComparator(boolean reverse) {
            this.reverse = reverse;
        }

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            return reverse ? Long.compare(o2.getStartTime(), o1.getStartTime()) :
                    Long.compare(o1.getStartTime(), o2.getStartTime());
        }
    }

    @Override
    protected @NonNull Iterable<ITmfTrace> getTracesToBuild(@Nullable ITmfTrace trace) {
        return Collections.singleton(trace);
    }

    private static class CallStackTreeLabelProvider extends TreeLabelProvider {

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            if (columnIndex == 0) {
                if (element instanceof LevelEntry) {
                    if (((LevelEntry) element).isSymbolKeyGroup()) {
                        return SYMBOL_KEY_IMAGE;
                    }
                    return GROUP_IMAGE;
                } else if (element instanceof CallStackEntry) {
                    CallStackEntry entry = (CallStackEntry) element;
                    if (entry.getFunctionName().length() > 0) {
                        return STACKFRAME_IMAGE;
                    }
                }
            }
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof CallStackEntry) {
                CallStackEntry entry = (CallStackEntry) element;
                if (columnIndex == 0) {
                    return entry.getFunctionName();
                } else if (columnIndex == 1 && entry.getFunctionName().length() > 0) {
                    int depth = entry.getStackLevel();
                    return Integer.toString(depth);
                } else if (columnIndex == 2 && entry.getFunctionName().length() > 0) {
                    ITmfTimestamp ts = TmfTimestamp.fromNanos(entry.getFunctionEntryTime());
                    return ts.toString();
                } else if (columnIndex == 3 && entry.getFunctionName().length() > 0) {
                    ITmfTimestamp ts = TmfTimestamp.fromNanos(entry.getFunctionExitTime());
                    return ts.toString();
                } else if (columnIndex == 4 && entry.getFunctionName().length() > 0) {
                    ITmfTimestamp ts = new TmfTimestampDelta(entry.getFunctionExitTime() - entry.getFunctionEntryTime(), ITmfTimestamp.NANOSECOND_SCALE);
                    return ts.toString();
                }
            } else if (element instanceof ITimeGraphEntry) {
                if (columnIndex == 0) {
                    return ((ITimeGraphEntry) element).getName();
                }
            }
            return ""; //$NON-NLS-1$
        }

    }

    private class CallStackFilterContentProvider extends TimeGraphContentProvider {
        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof TraceEntry) {
                return super.hasChildren(element);
            }
            return false;
        }

        @Override
        public ITimeGraphEntry[] getChildren(Object parentElement) {
            if (parentElement instanceof TraceEntry) {
                return super.getChildren(parentElement);
            }
            return new ITimeGraphEntry[0];
        }
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public CallStackView() {
        super(ID, new CallStackPresentationProvider());
        setTreeColumns(COLUMN_NAMES);
        setTreeLabelProvider(new CallStackTreeLabelProvider());
        setEntryComparator(new CallStackComparator());
        setFilterColumns(FILTER_COLUMN_NAMES);
        setFilterContentProvider(new CallStackFilterContentProvider());
        setFilterLabelProvider(new CallStackTreeLabelProvider());
    }

    // ------------------------------------------------------------------------
    // ViewPart
    // ------------------------------------------------------------------------

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        getTimeGraphViewer().addTimeListener(new ITimeGraphTimeListener() {
            @Override
            public void timeSelected(TimeGraphTimeEvent event) {
                synchingToTime(event.getBeginTime());
            }
        });

        getTimeGraphViewer().getTimeGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent event) {
                Object selection = getTimeGraphViewer().getSelection();
                if (selection instanceof CallStackEntry) {
                    CallStackEntry entry = (CallStackEntry) selection;
                    if (entry.getFunctionName().length() > 0) {
                        long entryTime = entry.getFunctionEntryTime();
                        long exitTime = entry.getFunctionExitTime();
                        TmfTimeRange range = new TmfTimeRange(TmfTimestamp.fromNanos(entryTime), TmfTimestamp.fromNanos(exitTime));
                        broadcast(new TmfWindowRangeUpdatedSignal(CallStackView.this, range));
                        getTimeGraphViewer().setStartFinishTime(entryTime, exitTime);
                        startZoomThread(entryTime, exitTime);
                    }
                }
            }
        });

        getTimeGraphViewer().getTimeGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                TimeGraphControl timeGraphControl = getTimeGraphViewer().getTimeGraphControl();
                ISelection selection = timeGraphControl.getSelection();
                if (selection instanceof IStructuredSelection) {
                    for (Object object : ((IStructuredSelection) selection).toList()) {
                        if (object instanceof CallStackEvent) {
                            CallStackEvent event = (CallStackEvent) object;
                            long startTime = event.getTime();
                            long endTime = startTime + event.getDuration();
                            TmfTimeRange range = new TmfTimeRange(TmfTimestamp.fromNanos(startTime), TmfTimestamp.fromNanos(endTime));
                            broadcast(new TmfWindowRangeUpdatedSignal(CallStackView.this, range));
                            getTimeGraphViewer().setStartFinishTime(startTime, endTime);
                            startZoomThread(startTime, endTime);
                            break;
                        }
                    }
                }
            }
        });

        contributeToActionBars();
        loadSortOption();

        IEditorPart editor = getSite().getPage().getActiveEditor();
        if (editor instanceof ITmfTraceEditor) {
            ITmfTrace trace = ((ITmfTraceEditor) editor).getTrace();
            if (trace != null) {
                traceSelected(new TmfTraceSelectedSignal(this, trace));
            }
        }
    }

    /**
     * Handler for the selection range signal.
     *
     * @param signal
     *            The incoming signal
     * @since 1.0
     */
    @Override
    @TmfSignalHandler
    public void selectionRangeUpdated(final TmfSelectionRangeUpdatedSignal signal) {

        fSavedTimeSyncSignal = isPinned() ? new TmfSelectionRangeUpdatedSignal(signal.getSource(), signal.getBeginTime(), signal.getEndTime()) : null;

        if (signal.getSource() == this || getTrace() == null || isPinned()) {
            return;
        }
        final long beginTime = signal.getBeginTime().toNanos();
        final long endTime = signal.getEndTime().toNanos();
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (getTimeGraphViewer().getControl().isDisposed()) {
                    return;
                }
                if (beginTime == endTime) {
                    getTimeGraphViewer().setSelectedTime(beginTime, true);
                } else {
                    getTimeGraphViewer().setSelectionRange(beginTime, endTime, true);
                }
                fSyncSelection = true;
                synchingToTime(beginTime);
                fSyncSelection = false;
                startZoomThread(getTimeGraphViewer().getTime0(), getTimeGraphViewer().getTime1());
            }
        });

    }

    /**
     * @since 2.0
     */
    @Override
    @TmfSignalHandler
    public void windowRangeUpdated(final TmfWindowRangeUpdatedSignal signal) {

        if (isPinned()) {
            fSavedRangeSyncSignal = new TmfWindowRangeUpdatedSignal(signal.getSource(), signal.getCurrentRange());
            fSavedTimeSyncSignal = null;
        }

        if ((signal.getSource() == this) || isPinned()) {
            return;
        }
        super.windowRangeUpdated(signal);
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    /**
     * @since 2.1
     */
    @Override
    protected CallStackPresentationProvider getPresentationProvider() {
        /* Set to this type by the constructor */
        return (CallStackPresentationProvider) super.getPresentationProvider();
    }

    @Override
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        super.traceClosed(signal);
        synchronized(fSymbolProviders){
            for(ITmfTrace trace : getTracesToBuild(signal.getTrace())){
                fSymbolProviders.remove(trace);
            }
        }
    }

    /**
     * @since 2.0
     */
    @Override
    protected void refresh() {
        super.refresh();
        updateConfigureSymbolsAction();
    }

    @Override
    protected void buildEntryList(final ITmfTrace trace, final ITmfTrace parentTrace, final IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return;
        }

        /*
         * Load the symbol provider for the current trace, even if it does not
         * provide a call stack analysis module. See
         * https://bugs.eclipse.org/bugs/show_bug.cgi?id=494212
         */
        ISymbolProvider symbolProvider = fSymbolProviders.get(trace);
        if (symbolProvider == null) {
            symbolProvider = SymbolProviderManager.getInstance().getSymbolProvider(trace);
            symbolProvider.loadConfiguration(null);
            fSymbolProviders.put(trace, symbolProvider);
        }

        /* Continue with the call stack view specific operations */
        Collection<ICallStackProvider> modules = getCallStackModules(trace);
        if (modules.isEmpty()) {
            addUnavailableEntry(trace, parentTrace);
            return;
        }

        long start = trace.getStartTime().toNanos();
        long end = trace.getEndTime().toNanos();
        setStartTime(getStartTime() == SWT.DEFAULT ? start : Math.min(getStartTime(), start));
        setEndTime(getEndTime() == SWT.DEFAULT ? end + 1 : Math.max(getEndTime(), end + 1));

        Map<ITmfTrace, TraceEntry> traceEntryMap = new HashMap<>();
        for (ICallStackProvider csProvider : modules) {
            Collection<CallStackSeries> callStacks = csProvider.getCallStackSeries();
            if (callStacks.isEmpty()) {
                continue;
            }

            TraceEntry traceEntry = traceEntryMap.get(trace);
            if (traceEntry == null) {
                traceEntry = new TraceEntry(trace.getName(), start, end + 1);
                traceEntryMap.put(trace, traceEntry);
                traceEntry.sortChildren(fThreadComparator);
                addToEntryList(parentTrace, Collections.singletonList(traceEntry));
            } else {
                traceEntry.updateEndTime(end);
            }

            for (CallStackSeries callstack : callStacks) {
                // If there is more than one callstack objects in the analys
                TimeGraphEntry callStackRootEntry = traceEntry;
                if (callStacks.size() > 1) {
                    callStackRootEntry = new TimeGraphEntry(callstack.getName(), start, end + 1);
                    traceEntry.addChild(callStackRootEntry);
                }
                for (ICallStackElement element : callstack.getRootElements()) {
                    processCallStackElement(symbolProvider, element, callStackRootEntry);
                }
            }
            final long entryStart = getStartTime();
            final long entryEnd = getEndTime();
            Consumer<TimeGraphEntry> consumer = new Consumer<TimeGraphEntry>() {
                @Override
                public void accept(TimeGraphEntry entry) {
                    if (monitor.isCanceled()) {
                        return;
                    }
                    if (entry instanceof CallStackEntry) {
                        buildStatusEvents((CallStackEntry) entry, monitor, entryStart, entryEnd);
                        return;
                    }
                    entry.getChildren().forEach(this);
                }
            };

            traceEntry.getChildren().forEach(consumer);
            refresh();
        }

    }

    private void processCallStackElement(ISymbolProvider provider, ICallStackElement element, TimeGraphEntry parentEntry) {
        // Is this an intermediate or leaf element
        if (element instanceof ICallStackLeafElement) {
            ICallStackLeafElement finalElement = (ICallStackLeafElement) element;
            CallStack callStack = finalElement.getCallStack();
            setEndTime(Math.max(getEndTime(), callStack.getEndTime()));
            for (int i = 0; i < callStack.getMaxDepth(); i++) {
                parentEntry.addChild(new CallStackEntry(provider, i + 1, callStack));
            }
            return;
        }
        TimeGraphEntry entry = new LevelEntry(element.getName(), parentEntry.getStartTime(), parentEntry.getEndTime(), element.isSymbolKeyElement());
        parentEntry.addChild(entry);
        element.getChildren().stream().forEach(e -> processCallStackElement(provider, e, entry));
    }

    private void addUnavailableEntry(ITmfTrace trace, ITmfTrace parentTrace) {
        String name = Messages.CallStackView_StackInfoNotAvailable + ' ' + '(' + trace.getName() + ')';
        TraceEntry unavailableEntry = new TraceEntry(name, 0, 0);
        addToEntryList(parentTrace, Collections.singletonList(unavailableEntry));
        if (parentTrace == getTrace()) {
            refresh();
        }
    }

    private void buildStatusEvents(@NonNull CallStackEntry entry, @NonNull IProgressMonitor monitor, long start, long end) {
        long resolution = Math.max(1, (end - start) / getDisplayWidth());
        List<ITimeEvent> eventList = getEventList(entry, start, end + 1, resolution, monitor);
        if (eventList != null) {
            entry.setEventList(eventList);
        }
    }

    /**
     * @since 1.2
     */
    @Override
    protected final List<ITimeEvent> getEventList(TimeGraphEntry tgentry, long startTime, long endTime, long resolution, IProgressMonitor monitor) {
        if (!(tgentry instanceof CallStackEntry)) {
            return null;
        }
        CallStackEntry entry = (CallStackEntry) tgentry;

        return entry.getEventList(startTime, endTime, resolution, monitor);
    }

    @Override
    protected void synchingToTime(final long time) {
        List<TimeGraphEntry> traceEntries = getEntryList(getTrace());
        if (traceEntries == null) {
            return;
        }
        Consumer<TimeGraphEntry> consumer = new Consumer<TimeGraphEntry>() {
            @Override
            public void accept(TimeGraphEntry entry) {
                if (entry instanceof CallStackEntry) {
                    CallStackEntry callStackEntry = (CallStackEntry) entry;
                    ICalledFunction currentFunction = callStackEntry.updateAt(time);

                    if (fSyncSelection && currentFunction != null) {
                        if (time == currentFunction.getStart()) {
                            fSyncSelection = false;
                            Display.getDefault().asyncExec(() -> {
                                getTimeGraphViewer().setSelection(callStackEntry, true);
                                getTimeGraphViewer().getTimeGraphControl().fireSelectionChanged();
                            });
                        }
                    }
                    return;
                }
                entry.getChildren().forEach(this);
            }

        };
        traceEntries.forEach(consumer);
        if (Display.getCurrent() != null) {
            getTimeGraphViewer().refresh();
        }
    }

    private void contributeToActionBars() {
        // Create pin action
        contributePinActionToToolBar();
        fPinAction.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (IAction.CHECKED.equals(event.getProperty()) && !isPinned()) {
                    if (fSavedRangeSyncSignal != null) {
                        windowRangeUpdated(fSavedRangeSyncSignal);
                        fSavedRangeSyncSignal = null;
                    }

                    if (fSavedTimeSyncSignal != null) {
                        selectionRangeUpdated(fSavedTimeSyncSignal);
                        fSavedTimeSyncSignal = null;
                    }
                }
            }
        });
    }

    /**
     * @since 1.2
     */
    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        manager.add(getConfigureSymbolsAction());
        manager.add(new Separator());
        manager.add(getSortByNameAction());
        manager.add(getSortByIdAction());
        manager.add(getSortByTimeAction());
        manager.add(new Separator());
        manager.add(getTimeGraphViewer().getShowFilterDialogAction());
        manager.add(new Separator());
        manager.add(getTimeGraphViewer().getResetScaleAction());
        manager.add(getPreviousEventAction());
        manager.add(getNextEventAction());
        manager.add(new Separator());
        manager.add(getTimeGraphViewer().getToggleBookmarkAction());
        manager.add(getTimeGraphViewer().getPreviousMarkerAction());
        manager.add(getTimeGraphViewer().getNextMarkerAction());
        manager.add(new Separator());
        manager.add(getTimeGraphViewer().getPreviousItemAction());
        manager.add(getTimeGraphViewer().getNextItemAction());
        manager.add(getTimeGraphViewer().getZoomInAction());
        manager.add(getTimeGraphViewer().getZoomOutAction());
    }

    /**
     * @since 2.0
     */
    @Override
    protected void fillTimeGraphEntryContextMenu(IMenuManager contextMenu) {
        contextMenu.add(new GroupMarker(IWorkbenchActionConstants.GROUP_REORGANIZE));
        contextMenu.add(getSortByNameAction());
        contextMenu.add(getSortByIdAction());
        contextMenu.add(getSortByTimeAction());
    }

    /**
     * Get the the next event action.
     *
     * @return The action object
     */
    private Action getNextEventAction() {
        if (fNextEventAction == null) {
            Action nextAction = getTimeGraphViewer().getNextEventAction();
            fNextEventAction = new Action() {
                @Override
                public void run() {
                    TimeGraphViewer viewer = getTimeGraphViewer();
                    ITimeGraphEntry entry = viewer.getSelection();
                    if (entry instanceof CallStackEntry) {
                        CallStackEntry callStackEntry = (CallStackEntry) entry;
                        long newTime = callStackEntry.getNextEventTime(viewer.getSelectionBegin());
                        viewer.setSelectedTimeNotify(newTime, true);
                        startZoomThread(viewer.getTime0(), viewer.getTime1());
                    }
                }
            };

            fNextEventAction.setText(nextAction.getText());
            fNextEventAction.setToolTipText(nextAction.getToolTipText());
            fNextEventAction.setImageDescriptor(nextAction.getImageDescriptor());
        }

        return fNextEventAction;
    }

    /**
     * Get the previous event action.
     *
     * @return The Action object
     */
    private Action getPreviousEventAction() {
        if (fPrevEventAction == null) {
            Action prevAction = getTimeGraphViewer().getPreviousEventAction();
            fPrevEventAction = new Action() {
                @Override
                public void run() {
//                    TimeGraphViewer viewer = getTimeGraphCombo().getTimeGraphViewer();
//                    ITimeGraphEntry entry = viewer.getSelection();
//                    if (entry instanceof CallStackEntry) {
//                        try {
//                            CallStackEntry callStackEntry = (CallStackEntry) entry;
//
//
//
//                            ITmfStateSystem ss = callStackEntry.getStateSystem();
//                            long time = Math.max(ss.getStartTime(), Math.min(ss.getCurrentEndTime(), viewer.getSelectionBegin()));
//                            TimeGraphEntry parentEntry = callStackEntry.getParent();
//                            int quark = ss.getParentAttributeQuark(callStackEntry.getQuark());
//                            ITmfStateInterval stackInterval = ss.querySingleState(time, quark);
//                            if (stackInterval.getStartTime() == time && time > ss.getStartTime()) {
//                                stackInterval = ss.querySingleState(time - 1, quark);
//                            }
//                            viewer.setSelectedTimeNotify(stackInterval.getStartTime(), true);
//                            int stackLevel = stackInterval.getStateValue().unboxInt();
//                            ITimeGraphEntry selectedEntry = parentEntry.getChildren().get(Math.max(0, stackLevel - 1));
//                            getTimeGraphCombo().setSelection(selectedEntry);
//                            viewer.getTimeGraphControl().fireSelectionChanged();
//                            startZoomThread(viewer.getTime0(), viewer.getTime1());
//
//                        } catch (TimeRangeException | StateSystemDisposedException | StateValueTypeException e) {
//                            Activator.getDefault().logError("Error querying state system", e); //$NON-NLS-1$
//                        }
//                    }
                }
            };

            fPrevEventAction.setText(prevAction.getText());
            fPrevEventAction.setToolTipText(prevAction.getToolTipText());
            fPrevEventAction.setImageDescriptor(prevAction.getImageDescriptor());
        }

        return fPrevEventAction;
    }

    private static Collection<ICallStackProvider> getCallStackModules(@NonNull ITmfTrace trace) {
        /*
         * Since we cannot know the exact analysis ID (in separate plugins), we
         * will search using the analysis type.
         */
        Iterable<ICallStackProvider> modules =
                TmfTraceUtils.getAnalysisModulesOfClass(trace, ICallStackProvider.class);
        StreamSupport.stream(modules.spliterator(), false).forEach(m -> m.schedule());
        StreamSupport.stream(modules.spliterator(), false).forEach(m -> m.waitForCompletion());
        return StreamSupport.stream(modules.spliterator(), false).collect(Collectors.toList());

    }

    // ------------------------------------------------------------------------
    // Methods related to function name mapping
    // ------------------------------------------------------------------------

    private Action getSortByNameAction() {
        if (fSortByNameAction == null) {
            fSortByNameAction = new Action(Messages.CallStackView_SortByThreadName, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    if (fSortOption == SortOption.BY_NAME) {
                        saveSortOption(SortOption.BY_NAME_REV);
                    } else {
                        saveSortOption(SortOption.BY_NAME);
                    }
                }
            };
            fSortByNameAction.setToolTipText(Messages.CallStackView_SortByThreadName);
            fSortByNameAction.setImageDescriptor(SORT_BY_NAME_ICON);
        }
        return fSortByNameAction;
    }

    private Action getSortByIdAction() {
        if (fSortByIdAction == null) {
            fSortByIdAction = new Action(Messages.CallStackView_SortByThreadId, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    if (fSortOption == SortOption.BY_ID) {
                        saveSortOption(SortOption.BY_ID_REV);
                    } else {
                        saveSortOption(SortOption.BY_ID);
                    }
                }
            };
            fSortByIdAction.setToolTipText(Messages.CallStackView_SortByThreadId);
            fSortByIdAction.setImageDescriptor(SORT_BY_ID_ICON);
        }
        return fSortByIdAction;
    }

    private Action getSortByTimeAction() {
        if (fSortByTimeAction == null) {
            fSortByTimeAction = new Action(Messages.CallStackView_SortByThreadTime, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    if (fSortOption == SortOption.BY_TIME) {
                        saveSortOption(SortOption.BY_TIME_REV);
                    } else {
                        saveSortOption(SortOption.BY_TIME);
                    }
                }
            };
            fSortByTimeAction.setToolTipText(Messages.CallStackView_SortByThreadTime);
            fSortByTimeAction.setImageDescriptor(SORT_BY_TIME_ICON);
        }
        return fSortByTimeAction;
    }

    private void loadSortOption() {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            return;
        }
        String sortOption = section.get(SORT_OPTION_KEY);
        if (sortOption == null) {
            return;
        }

        // reset defaults
        getSortByNameAction().setChecked(false);
        getSortByNameAction().setImageDescriptor(SORT_BY_NAME_ICON);
        getSortByIdAction().setChecked(false);
        getSortByIdAction().setImageDescriptor(SORT_BY_ID_ICON);
        getSortByTimeAction().setChecked(false);
        getSortByTimeAction().setImageDescriptor(SORT_BY_TIME_ICON);

        if (sortOption.equals(SortOption.BY_NAME.name())) {
            fSortOption = SortOption.BY_NAME;
            fThreadComparator = new ThreadNameComparator(false);
            getSortByNameAction().setChecked(true);
        } else if (sortOption.equals(SortOption.BY_NAME_REV.name())) {
            fSortOption = SortOption.BY_NAME_REV;
            fThreadComparator = new ThreadNameComparator(true);
            getSortByNameAction().setChecked(true);
            getSortByNameAction().setImageDescriptor(SORT_BY_NAME_REV_ICON);
        } else if (sortOption.equals(SortOption.BY_ID.name())) {
            fSortOption = SortOption.BY_ID;
            fThreadComparator = new ThreadIdComparator(false);
            getSortByIdAction().setChecked(true);
        } else if (sortOption.equals(SortOption.BY_ID_REV.name())) {
            fSortOption = SortOption.BY_ID_REV;
            fThreadComparator = new ThreadIdComparator(true);
            getSortByIdAction().setChecked(true);
            getSortByIdAction().setImageDescriptor(SORT_BY_ID_REV_ICON);
        } else if (sortOption.equals(SortOption.BY_TIME.name())) {
            fSortOption = SortOption.BY_TIME;
            fThreadComparator = new ThreadTimeComparator(false);
            getSortByTimeAction().setChecked(true);
        } else if (sortOption.equals(SortOption.BY_TIME_REV.name())) {
            fSortOption = SortOption.BY_TIME_REV;
            fThreadComparator = new ThreadTimeComparator(true);
            getSortByTimeAction().setChecked(true);
            getSortByTimeAction().setImageDescriptor(SORT_BY_TIME_REV_ICON);
        }
    }

    private void saveSortOption(SortOption sortOption) {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
        }
        section.put(SORT_OPTION_KEY, sortOption.name());
        loadSortOption();
        List<TimeGraphEntry> entryList = getEntryList(getTrace());
        if (entryList == null) {
            return;
        }
        for (TimeGraphEntry traceEntry : entryList) {
            traceEntry.sortChildren(fThreadComparator);
        }
        refresh();
    }

    private Action getConfigureSymbolsAction() {
        if (fConfigureSymbolsAction != null) {
            return fConfigureSymbolsAction;
        }

        fConfigureSymbolsAction = new Action(Messages.CallStackView_ConfigureSymbolProvidersText) {
            @Override
            public void run() {
                SymbolProviderConfigDialog dialog = new SymbolProviderConfigDialog(getSite().getShell(), getProviderPages());
                if (dialog.open() == IDialogConstants.OK_ID) {
                    getPresentationProvider().resetFunctionNames();
                    refresh();
                }
            }
        };

        fConfigureSymbolsAction.setToolTipText(Messages.CallStackView_ConfigureSymbolProvidersTooltip);
        fConfigureSymbolsAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(IMPORT_BINARY_ICON_PATH));

        /*
         * The updateConfigureSymbolsAction() method (called by refresh()) will
         * set the action to true if applicable after the symbol provider has
         * been properly loaded.
         */
        fConfigureSymbolsAction.setEnabled(false);

        return fConfigureSymbolsAction;
    }

    /**
     * @return an array of {@link ISymbolProviderPreferencePage} that will
     *         configure the current traces
     */
    private ISymbolProviderPreferencePage[] getProviderPages() {
        List<ISymbolProviderPreferencePage> pages = new ArrayList<>();
        ITmfTrace trace = getTrace();
        if (trace != null) {
            for (ITmfTrace subTrace : getTracesToBuild(trace)) {
                ISymbolProvider provider = fSymbolProviders.get(subTrace);
                if (provider != null) {
                    ISymbolProviderPreferencePage page = provider.createPreferencePage();
                    if (page != null) {
                        pages.add(page);
                    }
                }
            }
        }
        return pages.toArray(new ISymbolProviderPreferencePage[pages.size()]);
    }

    /**
     * Update the enable status of the configure symbols action
     */
    private void updateConfigureSymbolsAction() {
        ISymbolProviderPreferencePage[] providerPages = getProviderPages();
        getConfigureSymbolsAction().setEnabled(providerPages.length > 0);
    }

}
