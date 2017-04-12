/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.callstack.core.callgraph.profiling;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.extension.internal.callstack.timing.core.callstack.CallStackElement;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.extension.internal.provisional.callstack.timing.core.callstack.ICallStackGroupDescriptor;

public class ProfilingGroupDescriptor implements ICallStackGroupDescriptor {

    private final String fName;
    private final @Nullable ICallStackGroupDescriptor fNextGroup;

    public ProfilingGroupDescriptor(String name, @Nullable ICallStackGroupDescriptor nextGroup) {
        fName = name;
        fNextGroup = nextGroup;
    }

    @Override
    public @Nullable ICallStackGroupDescriptor getNextGroup() {
        return fNextGroup;
    }

    @Override
    public List<ICallStackElement> getElements(@Nullable CallStackElement parent, int baseQuark, @Nullable ICallStackElement symbolKeyElement, @Nullable IThreadIdResolver threadIdProvider) {
        return Collections.emptyList();
    }

    @Override
    public boolean isSymbolKeyGroup() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        return fName;
    }

}
