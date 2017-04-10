/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.profiling;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.AggregatedCallSite;

/**
 * @author Geneviève Bastien
 */
public class AggregatedStackTraces extends AggregatedCallSite {

    public AggregatedStackTraces(Object symbol) {
        super(symbol);
    }

    private int fCount = 1;

    @Override
    public long getLength() {
        return fCount;
    }

    @Override
    protected void merge(@NonNull AggregatedCallSite child) {
        if (!child.getSymbol().equals(getSymbol())) {
            throw new IllegalArgumentException("AggregatedStackTraces: trying to merge stack traces of different symbols"); //$NON-NLS-1$
        }
        fCount += child.getLength();
        mergeChildren(child);
    }

}
