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
import org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.data.Attributes;
import org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.module.FusedVirtualMachineStateProvider;
import org.eclipse.tracecompass.extension.internal.virtual.machine.analysis.core.module.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * @author Cédric Biancheri
 */
public class SysEntryHandler extends VMKernelEventHandler {

    public SysEntryHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
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
        VirtualCPU cpuObject = VirtualCPU.getVirtualCPU(host, cpu.longValue());
        if (host != null && host.isGuest()) {
            Integer physicalCPU = sp.getPhysicalCPU(host, cpu);
            if (physicalCPU != null) {
                cpu = physicalCPU;
            } else {
                return;
            }
        }
        /* Assign the new system call to the process */
        int currentThreadNode = FusedVMEventHandlerUtils.getCurrentThreadNode(cpu, ss);
        int quark = ss.getQuarkRelativeAndAdd(currentThreadNode, Attributes.SYSTEM_CALL);
        ITmfStateValue value = TmfStateValue.newValueString(event.getName());
        long timestamp = FusedVMEventHandlerUtils.getTimestamp(event);
        ss.modifyAttribute(timestamp, value, quark);

        /* Put the process in system call mode */
        quark = ss.getQuarkRelativeAndAdd(currentThreadNode, Attributes.STATUS);
        value = StateValues.PROCESS_STATUS_RUN_SYSCALL_VALUE;
        ss.modifyAttribute(timestamp, value, quark);

        /* Put the CPU in system call (kernel) mode */
        int currentCPUNode = FusedVMEventHandlerUtils.getCurrentCPUNode(cpu, ss);

        /*
         * If the trace that generates the event doesn't match the currently
         * running machine on this pcpu then we do not modify the state system.
         */
        boolean modify = true;
        if (host != null) {
            int machineNameQuark = ss.getQuarkRelativeAndAdd(currentCPUNode, Attributes.MACHINE_NAME);
            try {
                modify = ss.querySingleState(timestamp, machineNameQuark).getStateValue().unboxStr().equals(host.getTraceName());
            } catch (StateSystemDisposedException e) {
                e.printStackTrace();
            }
        }

        quark = ss.getQuarkRelativeAndAdd(currentCPUNode, Attributes.STATUS);
        value = StateValues.CPU_STATUS_RUN_SYSCALL_VALUE;
        if (modify) {
            ss.modifyAttribute(timestamp, value, quark);
        }
        cpuObject.setCurrentState(value);
    }

}
