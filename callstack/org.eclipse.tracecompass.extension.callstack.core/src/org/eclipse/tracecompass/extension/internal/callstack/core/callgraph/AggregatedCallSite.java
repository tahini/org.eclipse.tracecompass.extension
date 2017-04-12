/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.callstack.core.callgraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Geneviève Bastien
 */
public abstract class AggregatedCallSite {

    private final Object fSymbol;
    private final Map<Object, AggregatedCallSite> fChildren = new HashMap<>();
    private final @Nullable AggregatedCallSite fParent = null;
//    private final AggregatedCalledFunctionStatistics fStatistics;

    public AggregatedCallSite(Object symbol) {
        fSymbol = symbol;
    }

    public abstract long getLength();

    public Object getSymbol() {
        return fSymbol;
    }

    protected @Nullable AggregatedCallSite getParent() {
        return fParent;
    }

    public Collection<AggregatedCallSite> getChildren() {
        return fChildren.values();
    }

    public void addChild(AggregatedCallSite child) {
        AggregatedCallSite callsite = fChildren.get(child.getSymbol());
        if (callsite == null) {
            fChildren.put(child.getSymbol(), child);
            return;
        }
        if (fChildren.containsKey(child.getSymbol())) {
            callsite.merge(child);
        }
    }

    public final void merge(AggregatedCallSite other) {
        if (!other.getSymbol().equals(getSymbol())) {
            throw new IllegalArgumentException("AggregatedStackTraces: trying to merge stack traces of different symbols"); //$NON-NLS-1$
        }
        mergeData(other);
        mergeChildren(other);
    }

    protected abstract void mergeData(AggregatedCallSite other);

    protected void mergeChildren(AggregatedCallSite other) {
        for (AggregatedCallSite otherChildSite : other.fChildren.values()) {
            Object childSymbol = otherChildSite.getSymbol();
            AggregatedCallSite childSite = fChildren.get(childSymbol);
            if (childSite == null) {
                fChildren.put(childSymbol, otherChildSite);
            } else {
                // combine children
                childSite.merge(otherChildSite);
            }
        }
    }

    public int getMaxDepth() {
        int maxDepth = 0;
        for (AggregatedCallSite callsite: fChildren.values()) {
            maxDepth = Math.max(maxDepth, callsite.getMaxDepth());
        }
        return maxDepth + 1;
    }

}
