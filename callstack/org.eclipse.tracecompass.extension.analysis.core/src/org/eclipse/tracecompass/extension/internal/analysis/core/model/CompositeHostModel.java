/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.analysis.core.model;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.concepts.ICpuTimeProvider;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.concepts.IThreadOnCpuProvider;
import org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model.IHostModel;

/**
 * Operating system model based on analyses who implement certain interfaces to
 * provider the necessary information. The analyses will need to implement one
 * of the interfaces found in package
 * {@link org.eclipse.tracecompass.extension.internal.provisional.analysis.core.concepts} and the analysis
 * module should be automatically picked up at creation time.
 *
 * @author Geneviève Bastien
 */
public class CompositeHostModel implements IHostModel {

    private Set<ICpuTimeProvider> fCpuTimeProviders = NonNullUtils.checkNotNull(Collections.newSetFromMap(new WeakHashMap<ICpuTimeProvider, Boolean>()));
    private Set<IThreadOnCpuProvider> fThreadOnCpuProviders = NonNullUtils.checkNotNull(Collections.newSetFromMap(new WeakHashMap<IThreadOnCpuProvider, Boolean>()));

    @Override
    public int getThreadOnCpu(int cpu, long t) {
        for (IThreadOnCpuProvider provider : fThreadOnCpuProviders) {
            Integer tid = provider.getThreadOnCpuAtTime(cpu, t);
            if (tid != null && tid != IHostModel.UNKNOWN_TID) {
                return tid;
            }
        }
        return IHostModel.UNKNOWN_TID;
    }

    @Override
    public long getCpuTime(int tid, long start, long end) {
        for (ICpuTimeProvider provider : fCpuTimeProviders) {
            long cpuTime = provider.getCpuTime(tid, start, end);
            if (cpuTime != IHostModel.TIME_UNKNOWN) {
                return cpuTime;
            }
        }
        return IHostModel.TIME_UNKNOWN;
    }

    /**
     * Set a CPU time provider for this host model
     *
     * @param provider
     *            The CPU time provider
     */
    public void setCpuTimeProvider(ICpuTimeProvider provider) {
        fCpuTimeProviders.add(provider);
    }

    /**
     * Set a thread on CPU provider for this host model
     *
     * @param provider
     *            The thread on CPU time provider
     */
    public void setThreadOnCpuProvider(IThreadOnCpuProvider provider) {
        fThreadOnCpuProviders.add(provider);
    }

}
