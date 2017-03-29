/******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package ca.polymtl.tracecompass.internal.jul.analysis.ui.latency.views;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density.AbstractSegmentStoreDensityViewer;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import ca.polymtl.tracecompass.internal.jul.analysis.core.latency.LttngJulTimeAnalysis;

/**
 * System Call density viewer
 *
 * @author Matthew Khouzam
 * @author Marc-Andre Laperle
 */
public class JulEventDensityViewer extends AbstractSegmentStoreDensityViewer {

    /**
     * Constructs a new density viewer.
     *
     * @param parent
     *            the parent of the viewer
     */
    public JulEventDensityViewer(Composite parent) {
        super(parent);
    }

    @Override
    protected @Nullable ISegmentStoreProvider getSegmentStoreProvider(ITmfTrace trace) {
        return TmfTraceUtils.getAnalysisModuleOfClass(trace, LttngJulTimeAnalysis.class, LttngJulTimeAnalysis.ID);
    }
}
