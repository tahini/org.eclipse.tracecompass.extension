/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.callstack.ui.views.stacktable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.tracecompass.extension.internal.callstack.core.context.ContextCallStackAnalysis;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.signal.TmfEventSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

public class CallStackTableView extends TmfView {

    private @Nullable TreeViewer fTreeViewer;

    public CallStackTableView() {
        super("Call Stack Table");
    }

    /* The elements of the tree viewer are of type ITmfTreeViewerEntry */
    private class TreeContentProvider implements ITreeContentProvider {

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(@Nullable Viewer viewer, @Nullable Object oldInput, @Nullable Object newInput) {
        }

        @Override
        public Object[] getElements(@Nullable Object inputElement) {
            if (inputElement instanceof StackTableEntry) {
                return ((StackTableEntry) inputElement).getChildren();
            }
            return new StackTableEntry[0];
        }

        @Override
        public Object[] getChildren(@Nullable Object parentElement) {
            if (parentElement == null) {
                return new StackTableEntry[0];
            }
            StackTableEntry entry = (StackTableEntry) parentElement;
            return entry.getChildren();
        }

        @Override
        public @Nullable Object getParent(@Nullable Object element) {
            if (element == null) {
                return null;
            }
            StackTableEntry entry = (StackTableEntry) element;
            return entry.getParent();
        }

        @Override
        public boolean hasChildren(@Nullable Object element) {
            if (element == null) {
                return false;
            }
            StackTableEntry entry = (StackTableEntry) element;
            return entry.hasChildren();
        }

    }

    /**
     * Base class to provide the labels for the tree viewer. Views extending
     * this class typically need to override the getColumnText method if they
     * have more than one column to display. It also allows to change the font
     * and colors of the cells.
     */
    protected static class TreeLabelProvider implements ITableLabelProvider, ITableFontProvider, ITableColorProvider {

        @Override
        public void addListener(@Nullable ILabelProviderListener listener) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isLabelProperty(@Nullable Object element, @Nullable String property) {
            return false;
        }

        @Override
        public void removeListener(@Nullable ILabelProviderListener listener) {
        }

        @Override
        public @Nullable Image getColumnImage(@Nullable Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(@Nullable Object element, int columnIndex) {
            return String.valueOf(element);
        }

        @Override
        public Color getForeground(@Nullable Object element, int columnIndex) {
            return Display.getCurrent().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        }

        @Override
        public Color getBackground(@Nullable Object element, int columnIndex) {
            return Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        }

        @Override
        public @Nullable Font getFont(@Nullable Object element, int columnIndex) {
            return null;
        }

    }


    // ------------------------------------------------------------------------
    // ViewPart
    // ------------------------------------------------------------------------

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);
        SashForm sf = new SashForm(parent, SWT.NONE);
        /* Build the tree viewer part of the view */
        TreeViewer treeViewer = new TreeViewer(sf, SWT.FULL_SELECTION | SWT.H_SCROLL);
        fTreeViewer = treeViewer;
        treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
        final Tree tree = treeViewer.getTree();
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);
        TreeViewerColumn column = new TreeViewerColumn(treeViewer, SWT.LEFT);
        column.getColumn().setText("CallSite");
        column.getColumn().setWidth(50);
        treeViewer.setContentProvider(new TreeContentProvider());
        treeViewer.setLabelProvider(new TreeLabelProvider());
    }

    @Override
    public void setFocus() {

    }

    @TmfSignalHandler
    public void eventSelected(TmfEventSelectedSignal signal) {
        ITmfEvent event = signal.getEvent();
        IAnalysisModule analysisModule = event.getTrace().getAnalysisModule(ContextCallStackAnalysis.ID);
        TreeViewer treeViewer = fTreeViewer;
        if (treeViewer == null) {
            return;
        }
        if (!(analysisModule instanceof ContextCallStackAnalysis)) {
            treeViewer.setInput(null);
        }
        Map<String, List<Long>> stack = ContextCallStackAnalysis.eventHandle(event);
        treeViewer.setInput(convertStack(stack));
    }

    private class StackTableEntry {

        private @Nullable StackTableEntry fParent;
        public StackTableEntry(@Nullable StackTableEntry parent) {
            fParent = parent;
        }

        public StackTableEntry[] getChildren() {
            return new StackTableEntry[0];
        }

        public boolean hasChildren() {
            return getChildren().length != 0;
        }

        public @Nullable Object getParent() {
            return fParent;
        }

    }

    private class StackTableRootEntry extends StackTableEntry {
        List<StackTableEntry> fEntries = new ArrayList<>();
        public StackTableRootEntry() {
            super(null);
        }
        public void addEntry(StackTableEntry entry) {
            fEntries.add(entry);
        }

        @Override
        public StackTableEntry[] getChildren() {
            return fEntries.toArray(new StackTableEntry[fEntries.size()]);
        }
    }

    private class StackTableStringEntry extends StackTableEntry {
        private String fName;
        private List<StackTableEntry> fChildren;

        StackTableStringEntry(StackTableRootEntry entry, String name, List<Long> list) {
            super(entry);
            entry.addEntry(this);
            fName = name;
            fChildren = list.stream().map(l -> new StackTableLongEntry(this, l)).collect(Collectors.toList());
        }

        @Override
        public StackTableEntry[] getChildren() {
            return fChildren.toArray(new StackTableEntry[fChildren.size()]);
        }

        @Override
        public String toString() {
            return fName;
        }
    }

    private class StackTableLongEntry extends StackTableEntry {
        private long fCallsite;

        public StackTableLongEntry(StackTableStringEntry entry, long callsite) {
            super(entry);
            fCallsite = callsite;
        }

        @Override
        public String toString() {
            return Long.toHexString(fCallsite);
        }
    }

    private Object convertStack(Map<@NonNull String, @NonNull List<@NonNull Long>> stack) {
        StackTableRootEntry rootEntry = new StackTableRootEntry();
        for (Entry<String, List<Long>> entry: stack.entrySet()) {
            new StackTableStringEntry(rootEntry, entry.getKey(), entry.getValue());
        }
        return rootEntry;
    }

}
