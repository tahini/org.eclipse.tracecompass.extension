/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.module.FusedVirtualMachineStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * @author Cédric Biancheri
 */
public class ProcessFreeHandler extends VMKernelEventHandler {

    public ProcessFreeHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
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
        if (host != null && host.isGuest()) {
            Integer physicalCPU = sp.getPhysicalCPU(host, cpu);
            if (physicalCPU != null) {
                cpu = physicalCPU;
            } else {
                return;
            }
        }
        Integer tid = ((Long) event.getContent().getField(getLayout().fieldTid()).getValue()).intValue();
        String machineName = event.getTrace().getName();

        String threadAttributeName = FusedVMEventHandlerUtils.buildThreadAttributeName(tid, cpu);
        if (threadAttributeName == null) {
            return;
        }

        /*
         * Remove the process and all its sub-attributes from the current state
         */
        int quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getNodeThreads(ss), machineName, tid.toString());
        ss.removeAttribute(FusedVMEventHandlerUtils.getTimestamp(event), quark);
    }
}
