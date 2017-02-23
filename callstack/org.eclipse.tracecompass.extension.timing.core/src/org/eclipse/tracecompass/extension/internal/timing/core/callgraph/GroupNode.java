/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.timing.core.callgraph;

import org.eclipse.tracecompass.extension.internal.provisional.timing.core.callstack.ICallStackLeafElement;

/**
 * This class represents one thread. It's used as a root node for the aggregated
 * tree created in the CallGraphAnalysis.
 *
 * @author Sonia Farrah
 */
public class GroupNode extends AggregatedCalledFunction {

    private final String fId;
    private final ICallStackLeafElement fElement;

    /**
     * @param calledFunction
     *            the called function
     * @param maxDepth
     *            The maximum depth
     * @param id
     *            The thread id
     */
    public GroupNode(AbstractCalledFunction calledFunction, ICallStackLeafElement element, int maxDepth, String id) {
        super(calledFunction, maxDepth);
        fId = id;
        fElement = element;
    }

    /**
     * The thread id
     *
     * @return The thread id
     */
    public String getId() {
        return fId;
    }

    public ICallStackLeafElement getElement() {
        return fElement;
    }

}
