/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.data.Attributes;
import org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.module.FusedVirtualMachineStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * @author Cédric Biancheri
 */
public class KvmNestedVmExitInjectHandler extends VMKernelEventHandler {

    public KvmNestedVmExitInjectHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
        super(layout, sp);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) {
        Integer cpu = FusedVMEventHandlerUtils.getCpu(event);
        if (cpu == null) {
            return;
        }
        FusedVirtualMachineStateProvider sp = getStateProvider();
        VirtualMachine host = sp.getCurrentMachine(event);
        if (host == null) {
            return;
        }

        /* We expect to handle this event only for the host. */
        if (host.isGuest()) {
            return;
        }

        int currentCPUNode = FusedVMEventHandlerUtils.getCurrentCPUNode(cpu, ss);
        /*
         * Shortcut for the "current thread" attribute node. It requires
         * querying the current CPU's current thread.
         */
        int quark = ss.getQuarkRelativeAndAdd(currentCPUNode, Attributes.CURRENT_THREAD);

        ITmfStateValue value = ss.queryOngoingState(quark);
        int thread = value.isNull() ? -1 : value.unboxInt();

        thread = VirtualCPU.getVirtualCPU(host, cpu.longValue()).getCurrentThread().unboxInt();
        if (thread == -1) {
            return;
        }

        HostThread ht = new HostThread(event.getTrace().getHostId(), thread);

        /*
         * This event means that this thread will not go to a higher layer
         * during the next entry. So we remove it from the list of ready
         * threads.
         */
        host.removeThreadFromReadyForNextLayerSet(ht);
    }

}
