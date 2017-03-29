/*******************************************************************************
 * Copyright (c) 2015, 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bernd Hufmann - Initial API and implementation
 *******************************************************************************/
package ca.polymtl.tracecompass.internal.jul.analysis.ui.responsiveness.views;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsView;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsViewer;
import org.eclipse.tracecompass.common.core.NonNullUtils;

import ca.polymtl.tracecompass.internal.jul.analysis.ui.responsiveness.views.UiResponseStatisticsViewer.DisplayMode;

/**
 * Abstract view to to be extended to display segment store statistics.
 *
 * @author Bernd Hufmann
 *
 */
public class UiResponseStatisticsView extends AbstractSegmentStoreStatisticsView {

//    @Nullable private UiResponseStatisticsViewer fStatsViewer = null;

    /**
     * Constructor
     */
    public UiResponseStatisticsView() {
        super();


    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);
        /* Add a menu for adding charts */
        Action cpuTimes = new Action() {

            @Override
            public void run() {
                UiResponseStatisticsViewer viewer = getViewer();
                if (viewer != null) {
                    viewer.setMode(DisplayMode.CPU);
                }
            }

        };
        cpuTimes.setText("Show CPU Times"); //$NON-NLS-1$

        IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
        menuMgr.add(cpuTimes);

        /* Add a menu for adding charts */
        Action selfTimes = new Action() {

            @Override
            public void run() {
                UiResponseStatisticsViewer viewer = getViewer();
                if (viewer != null) {
                    viewer.setMode(DisplayMode.SELF);
                }
            }

        };
        selfTimes.setText("Show Self Times"); //$NON-NLS-1$
        menuMgr.add(selfTimes);

        /* Add a menu for adding charts */
        Action totalTimes = new Action() {

            @Override
            public void run() {
                UiResponseStatisticsViewer viewer = getViewer();
                if (viewer != null) {
                    viewer.setMode(DisplayMode.DURATION);
                }
            }

        };
        totalTimes.setText("Show Durations"); //$NON-NLS-1$

        menuMgr.add(totalTimes);
    }
//
//    @Override
//    public void setFocus() {
//        UiResponseStatisticsViewer statsViewer = fStatsViewer;
//        if (statsViewer != null) {
//            statsViewer.getControl().setFocus();
//        }
//    }
//
//    @Override
//    public void dispose() {
//        super.dispose();
//        UiResponseStatisticsViewer statsViewer = fStatsViewer;
//        if (statsViewer != null) {
//            statsViewer.dispose();
//        }
//    }

    @Override
    protected @NonNull AbstractSegmentStoreStatisticsViewer createSegmentStoreStatisticsViewer(@NonNull Composite parent) {
        return new UiResponseStatisticsViewer(NonNullUtils.checkNotNull(parent));
    }

    /**
     * @since 1.2
     */
    @Override
    protected @Nullable UiResponseStatisticsViewer getViewer() {
        return (UiResponseStatisticsViewer) super.getViewer();
    }

}
