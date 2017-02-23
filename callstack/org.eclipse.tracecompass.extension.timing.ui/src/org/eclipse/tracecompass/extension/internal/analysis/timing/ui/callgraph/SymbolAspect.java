/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.analysis.timing.ui.callgraph;

import java.util.Comparator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.extension.internal.timing.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderManager;

/**
 * An aspect used to get the function name of a call stack event or to compare
 * the duration of two events
 *
 * @author Sonia Farrah
 */
public final class SymbolAspect implements ISegmentAspect {
    /**
     * A symbol aspect
     */
    public static final @NonNull ISegmentAspect SYMBOL_ASPECT = new SymbolAspect();

    /**
     * Constructor
     */
    public SymbolAspect() {
    }

    @Override
    public @NonNull String getName() {
        return NonNullUtils.nullToEmptyString(Messages.CallStack_FunctionName);
    }

    @Override
    public @NonNull String getHelpText() {
        return NonNullUtils.nullToEmptyString(Messages.CallStack_FunctionName);
    }

    @Override
    public @Nullable Comparator<?> getComparator() {
        return new Comparator<ISegment>() {
            @Override
            public int compare(@Nullable ISegment o1, @Nullable ISegment o2) {
                if (o1 == null || o2 == null) {
                    throw new IllegalArgumentException();
                }
                return Long.compare(o1.getLength(), o2.getLength());
            }
        };
    }

    @Override
    public @Nullable Object resolve(@NonNull ISegment segment) {
        if (segment instanceof ICalledFunction) {
            ICalledFunction calledFunction = (ICalledFunction) segment;
            ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
            if (trace != null) {
                String symbolText;
                Object symbol = calledFunction.getSymbol();
                if (symbol instanceof Long) {
                    Long longAddress = (Long) symbol;
                    ISymbolProvider provider = SymbolProviderManager.getInstance().getSymbolProvider(trace);
                    symbolText = provider.getSymbolText(longAddress);
                    if (symbolText == null) {
                        return "0x" + Long.toHexString(longAddress); //$NON-NLS-1$
                    }
                    // take the start time in the query for the symbol name
                    long time = segment.getStart();
                    int pid = calledFunction.getProcessId();
                    if (pid > 0) {
                        String text = provider.getSymbolText(pid, time, longAddress);
                        if (text != null) {
                            return text;
                        }
                    }
                    return symbolText;
                }
                return String.valueOf(symbol);
            }
        }
        return null;
    }
}