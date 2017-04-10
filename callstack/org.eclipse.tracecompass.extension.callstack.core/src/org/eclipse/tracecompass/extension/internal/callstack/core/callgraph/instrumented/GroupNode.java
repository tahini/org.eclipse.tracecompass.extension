/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.instrumented;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackGroupDescriptor;

/**
 * This class represents one thread. It's used as a root node for the aggregated
 * tree created in the CallGraphAnalysis.
 *
 * @author Sonia Farrah
 */
public class GroupNode extends AggregatedCalledFunction {

    private final String fId;
    private final ICallStackElement fElement;

    /**
     * @param calledFunction
     *            the called function
     * @param element
     *            The leaf element under which the callstack is found
     * @param maxDepth
     *            The maximum depth
     * @param id
     *            The thread id
     */
    public GroupNode(AbstractCalledFunction calledFunction, ICallStackElement element, int maxDepth, String id) {
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

    /**
     * Get the callstakc leaf element associated with this group
     *
     * @return The callstack leaf element
     */
    public ICallStackElement getElement() {
        return fElement;
    }

    /**
     * Get the callstack element of this group for a given group descriptor
     *
     * @param descriptor The descriptor to match
     * @return The callstack element
     */
    public ICallStackElement getElement(@Nullable ICallStackGroupDescriptor descriptor) {
        ICallStackElement element = fElement;
        while (element != null && element.getNextGroup() != descriptor) {
            element = element.getParentElement();
        }
        return (element == null ? fElement : element);
    }

}
