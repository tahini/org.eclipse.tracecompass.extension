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
import org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.LeafGroupNode;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackGroupDescriptor;

/**
 * @author Geneviève Bastien
 */
public class ProfilingGroup extends LeafGroupNode {

    public ProfilingGroup(@NonNull String name, ICallStackGroupDescriptor descriptor) {
        super(name, descriptor);
    }

    /**
     * @param stackTrace
     */
    public void addStackTrace(Object[] stackTrace) {

    }

    /**
     * @param stackTrace
     */
    public void addStackTrace(long[] stackTrace) {
        if (stackTrace.length == 0) {
            return;
        }
        // Create the callsite for this stack trace
        AggregatedCallSite prevCallsite = new AggregatedStackTraces(stackTrace[stackTrace.length - 1]);
        for (int i = stackTrace.length - 2; i >= 0; i--) {
            AggregatedCallSite callsite = new AggregatedStackTraces(stackTrace[i]);
            callsite.addChild(prevCallsite);
            prevCallsite = callsite;
        }
        addAggregatedData(prevCallsite);
    }

}
