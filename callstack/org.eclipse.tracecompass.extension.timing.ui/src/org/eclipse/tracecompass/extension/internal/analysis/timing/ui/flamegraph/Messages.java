/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.extension.internal.analysis.timing.ui.flamegraph;

import org.eclipse.osgi.util.NLS;

/**
 * Message bundle for the flame graph view
 *
 * @author Sonia Farrah
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$
    public static String FlameGraph_CpuTimes;
    public static String FlameGraph_CpuTime;
    public static String FlameGraph_AverageCpuTime;
    public static String FlameGraph_MinCpuTime;
    public static String FlameGraph_MaxCpuTime;
    public static String FlameGraph_CpuTimeDeviation;
    /**
     * The duration of a function
     */
    public static String FlameGraph_Duration;
    /**
     * The time percentage of a function
     */
    public static String FlameGraph_Percentage;
    /**
     * The number of calls of a function
     */
    public static String FlameGraph_NbCalls;
    /**
     * The self time of a function
     */
    public static String FlameGraph_SelfTime;
    /**
     * The depth of a function
     */
    public static String FlameGraph_Depth;
    /**
     * The maximum duration of a function
     */
    public static String FlameGraph_MaxDuration;
    /**
     * The minimum duration of a function
     */
    public static String FlameGraph_MinDuration;
    /**
     * The average duration of a function
     */
    public static String FlameGraph_AverageDuration;
    /**
     * The average self time of a function
     */
    public static String FlameGraph_AverageSelfTime;
    /**
     * The maximum self time of a function
     */
    public static String FlameGraph_MaxSelfTime;
    /**
     * The minimum self time of a function
     */
    public static String FlameGraph_MinSelfTime;
    /**
     * The standard deviation of a function
     */
    public static String FlameGraph_Deviation;
    /**
     * The standard deviation of a function's self time
     */
    public static String FlameGraph_SelfTimeDeviation;
    /**
     * The function's durations
     */
    public static String FlameGraph_Durations;
    /**
     * The function's self times
     */
    public static String FlameGraph_SelfTimes;
    /**
     * Go to maximum duration
     */
    public static String FlameGraphView_GotoMaxDuration;
    /**
     * Go to minimum duration
     */
    public static String FlameGraphView_GotoMinDuration;
    /**
     * The action name for sorting by thread name
     */
    public static String FlameGraph_SortByThreadName;
    /**
     * The action name for sorting by thread name
     */
    public static String FlameGraph_SortByThreadId;
    /**
     * Execution of the callGraph Analysis
     */
    public static String CallGraphAnalysis_Execution;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}