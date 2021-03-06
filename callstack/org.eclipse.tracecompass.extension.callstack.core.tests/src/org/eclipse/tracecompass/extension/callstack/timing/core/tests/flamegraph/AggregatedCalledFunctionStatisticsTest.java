package org.eclipse.tracecompass.extension.callstack.timing.core.tests.flamegraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.extension.callstack.timing.core.tests.stubs.CallGraphAnalysisStub;
import org.eclipse.tracecompass.extension.internal.callstack.timing.core.callgraph.AggregatedCalledFunction;
import org.eclipse.tracecompass.extension.internal.callstack.timing.core.callgraph.AggregatedCalledFunctionStatistics;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemFactory;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.junit.Test;

/**
 * Test the statistics of each node in the aggregation tree. This creates a
 * virtual state system in each test then tests the statistics of the
 * aggregation tree returned by CallGraphAnalysis.
 *
 * @author Sonia Farrah
 *
 */
public class AggregatedCalledFunctionStatisticsTest {

    private static final String QUARK_0 = "0";
    private static final String QUARK_1 = "1";
    private static final String QUARK_2 = "2";
    private static final String QUARK_3 = "3";
    private static final double ERROR = 0.000001;

    private static @NonNull ITmfStateSystemBuilder createFixture() {
        IStateHistoryBackend backend;
        backend = StateHistoryBackendFactory.createInMemoryBackend("Test", 0L);
        ITmfStateSystemBuilder fixture = StateSystemFactory.newStateSystem(backend);
        return fixture;
    }

    /**
     * The call stack's structure used in this test is shown below:
     *
     * <pre>
     *                 Aggregated tree
     *  ___ main___      ___ main___
     *   _1_    _1_  =>      _1_
     *   _1_                 _1_
     * </pre>
     */
    @Test
    public void TreeStatisticsTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        // Build the state system
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH, CallGraphAnalysisStub.CALLSTACK_PATH);
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        TmfStateValue statev = TmfStateValue.newValueLong(0);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(100, TmfStateValue.nullValue(), quark);

        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        statev = TmfStateValue.newValueLong(1);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(50, TmfStateValue.nullValue(), quark);
        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(90, TmfStateValue.nullValue(), quark);

        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_2);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(30, TmfStateValue.nullValue(), quark);
        fixture.closeHistory(102);

        // Execute the CallGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        assertTrue(cga.iterate());
        @NonNull
        List<AggregatedCalledFunction> threads = cga.getGroupNodes();
        // Test the threads generated by the analysis
        assertNotNull(threads);
        Object[] children = threads.get(0).getChildren().toArray();
        AggregatedCalledFunction firstFunction = (AggregatedCalledFunction) children[0];
        Object[] firstFunctionChildren = firstFunction.getChildren().toArray();
        AggregatedCalledFunction SecondFunction = (AggregatedCalledFunction) firstFunctionChildren[0];
        Object[] secondFunctionChildren = SecondFunction.getChildren().toArray();
        AggregatedCalledFunction ThirdFunction = (AggregatedCalledFunction) secondFunctionChildren[0];
        // Test the main statistics
        @NonNull
        AggregatedCalledFunctionStatistics mainStatistics1 = firstFunction.getFunctionStatistics();
        assertEquals("Test main's maximum duration", 100, mainStatistics1.getDurationStatistics().getMax());
        assertEquals("Test main's minimum duration", 100, mainStatistics1.getDurationStatistics().getMin());
        assertEquals("Test main's maximum self time", 20, mainStatistics1.getSelfTimeStatistics().getMax());
        assertEquals("Test main's minimum self time", 20, mainStatistics1.getSelfTimeStatistics().getMin());
        assertEquals("Test main's number of calls", 1, mainStatistics1.getDurationStatistics().getNbElements());
        assertEquals("Test main's average duration", 100, mainStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test main's standard deviation", 20, mainStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test main's standard deviation", Double.NaN, mainStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test main's standard deviation", Double.NaN, mainStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the first function statistics
        @NonNull
        AggregatedCalledFunctionStatistics functionStatistics1 = SecondFunction.getFunctionStatistics();
        assertEquals("Test first function's maximum duration", 50, functionStatistics1.getDurationStatistics().getMax());
        assertEquals("Test first function's minimum duration", 30, functionStatistics1.getDurationStatistics().getMin());
        assertEquals("Test first function's maximum self time", 30, functionStatistics1.getSelfTimeStatistics().getMax());
        assertEquals("Test first function's mininmum self time", 20, functionStatistics1.getSelfTimeStatistics().getMin());
        assertEquals("Test first function's number of calls", 2, functionStatistics1.getDurationStatistics().getNbElements());
        assertEquals("Test first function's average duration", 40, functionStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test first function's average self time", 25, functionStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test first function's standard deviation", Double.NaN, functionStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test first function's self time standard deviation", Double.NaN, functionStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the third function statistics
        @NonNull
        AggregatedCalledFunctionStatistics functionStatistics2 = ThirdFunction.getFunctionStatistics();
        assertEquals("Test second function's maximum duration", 30, functionStatistics2.getDurationStatistics().getMax());
        assertEquals("Test second function's minimum duration", 30, functionStatistics2.getDurationStatistics().getMin());
        assertEquals("Test second function's maximum self time", 30, functionStatistics2.getSelfTimeStatistics().getMax());
        assertEquals("Test second function's minimum self time", 30, functionStatistics2.getSelfTimeStatistics().getMin());
        assertEquals("Test second function's number of calls", 1, functionStatistics2.getDurationStatistics().getNbElements());
        assertEquals("Test second function's average duration", 30, functionStatistics2.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test second function's average self time", 30, functionStatistics2.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test second function's standard deviation", Double.NaN, functionStatistics2.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test second function's self time standard deviation", Double.NaN, functionStatistics2.getSelfTimeStatistics().getStdDev(), ERROR);
        cga.dispose();
    }

    /**
     * The call stack's structure used in this test is shown below:
     *
     * <pre>
     *                    Aggregated tree
     *  ___ main___        ___ main___
     *   _1_    _1_ =>         _1_
     *   _2_    _3_          _2_ _3_
     * </pre>
     */
    @Test
    public void MergeFirstLevelCalleesStatisticsTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        // Build the state system
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH, CallGraphAnalysisStub.CALLSTACK_PATH);
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        TmfStateValue statev = TmfStateValue.newValueLong(0);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(100, TmfStateValue.nullValue(), quark);

        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        statev = TmfStateValue.newValueLong(1);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(50, TmfStateValue.nullValue(), quark);
        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(90, TmfStateValue.nullValue(), quark);

        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_2);
        statev = TmfStateValue.newValueLong(2);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(30, TmfStateValue.nullValue(), quark);
        statev = TmfStateValue.newValueLong(3);
        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(80, TmfStateValue.nullValue(), quark);
        fixture.closeHistory(102);

        // Execute the CallGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        assertTrue(cga.iterate());
        List<AggregatedCalledFunction> threads = cga.getGroupNodes();
        assertNotNull(threads);
        Object[] children = threads.get(0).getChildren().toArray();
        AggregatedCalledFunction firstFunction = (AggregatedCalledFunction) children[0];
        Object[] firstFunctionChildren = firstFunction.getChildren().toArray();
        AggregatedCalledFunction secondFunction = (AggregatedCalledFunction) firstFunctionChildren[0];
        Object[] secondFunctionChildren = secondFunction.getChildren().toArray();
        AggregatedCalledFunction leaf1 = (AggregatedCalledFunction) secondFunctionChildren[0];
        AggregatedCalledFunction leaf2 = (AggregatedCalledFunction) secondFunctionChildren[1];
        // Test the first function statistics
        @NonNull
        AggregatedCalledFunctionStatistics functionStatistics1 = firstFunction.getFunctionStatistics();
        assertEquals("Test first function's maximum duration", 100, functionStatistics1.getDurationStatistics().getMax());
        assertEquals("Test first function's minimum duration", 100, functionStatistics1.getDurationStatistics().getMin());
        assertEquals("Test first function's maximum self time", 20, functionStatistics1.getSelfTimeStatistics().getMax());
        assertEquals("Test first function's minimum self time", 20, functionStatistics1.getSelfTimeStatistics().getMin());
        assertEquals("Test first function's number of segments", 1, functionStatistics1.getDurationStatistics().getNbElements());
        assertEquals("Test first function's average duration", 100, functionStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test first function's average self time", 20, functionStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test first function's standard deviation", Double.NaN, functionStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test first function's standard deviation", Double.NaN, functionStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the first function statistics
        @NonNull
        AggregatedCalledFunctionStatistics functionStatistics2 = secondFunction.getFunctionStatistics();
        assertEquals("Test second function's maximum duration", 50, functionStatistics2.getDurationStatistics().getMax());
        assertEquals("Test second function's minimum duration", 30, functionStatistics2.getDurationStatistics().getMin());
        assertEquals("Test second function's maximum self time", 20, functionStatistics2.getSelfTimeStatistics().getMax());
        assertEquals("Test second function's minimum self time", 10, functionStatistics2.getSelfTimeStatistics().getMin());
        assertEquals("Test second function's number of calls", 2, functionStatistics2.getDurationStatistics().getNbElements());
        assertEquals("Test second function's average duration", 40, functionStatistics2.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test second function's average self time", 15, functionStatistics2.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test second function's standard deviation", Double.NaN, functionStatistics2.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test second function's standard deviation", Double.NaN, functionStatistics2.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the first leaf statistics
        @NonNull
        AggregatedCalledFunctionStatistics leafStatistics1 = leaf1.getFunctionStatistics();
        assertEquals("Test first leaf's maximum duration", 30, leafStatistics1.getDurationStatistics().getMax());
        assertEquals("Test first leaf's minimum duration", 30, leafStatistics1.getDurationStatistics().getMin());
        assertEquals("Test first leaf's maximum self time", 30, leafStatistics1.getSelfTimeStatistics().getMax());
        assertEquals("Test first leaf's minimum self time", 30, leafStatistics1.getSelfTimeStatistics().getMin());
        assertEquals("Test first leaf's number of calls", 1, leafStatistics1.getDurationStatistics().getNbElements());
        assertEquals("Test first leaf's minimum duration", 30, leafStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test first leaf's average self time", 30, leafStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test first leaf's standard deviation", Double.NaN, leafStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test first leaf's self time standard deviation", Double.NaN, leafStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the second leaf statistics
        @NonNull
        AggregatedCalledFunctionStatistics leafStatistics2 = leaf2.getFunctionStatistics();
        assertEquals("Test second leaf's maximum duration", 20, leafStatistics2.getDurationStatistics().getMax());
        assertEquals("Test second leaf's minimum duration", 20, leafStatistics2.getDurationStatistics().getMin());
        assertEquals("Test second leaf's maximum self time", 20, leafStatistics2.getSelfTimeStatistics().getMax());
        assertEquals("Test second leaf's minimum self time", 20, leafStatistics2.getSelfTimeStatistics().getMin());
        assertEquals("Test second leaf's number of calls", 1, leafStatistics2.getDurationStatistics().getNbElements());
        assertEquals("Test second leaf's average duration", 20, leafStatistics2.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test second leaf's average self time", 20, leafStatistics2.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test second leaf's standard deviation", Double.NaN, leafStatistics2.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test second leaf's self time standard deviation", Double.NaN, leafStatistics2.getSelfTimeStatistics().getStdDev(), ERROR);
        cga.dispose();
    }

    /**
     * The call stack's structure used in this test is shown below:
     *
     * <pre>
     *              Aggregated tree
     * _1_  _1_  =>    _1_
     * _2_  _3_      _2_ _3_
     * </pre>
     */
    @Test
    public void multiFunctionRootsTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH, CallGraphAnalysisStub.CALLSTACK_PATH);
        // Create the first root function
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        TmfStateValue statev = TmfStateValue.newValueLong(1);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(20, TmfStateValue.nullValue(), quark);
        // Create the second root function
        fixture.modifyAttribute(30, statev, quark);
        fixture.modifyAttribute(80, TmfStateValue.nullValue(), quark);
        // Create the first root function's callee
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        statev = TmfStateValue.newValueLong(2);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(10, TmfStateValue.nullValue(), quark);
        // Create the second root function's callee
        statev = TmfStateValue.newValueLong(3);
        fixture.modifyAttribute(30, statev, quark);
        fixture.modifyAttribute(40, TmfStateValue.nullValue(), quark);
        fixture.closeHistory(81);

        // Execute the callGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        assertTrue(cga.iterate());
        List<AggregatedCalledFunction> threads = cga.getGroupNodes();
        // Test the threads generated by the analysis
        assertNotNull(threads);
        Object[] children = threads.get(0).getChildren().toArray();
        AggregatedCalledFunction firstFunction = (AggregatedCalledFunction) children[0];
        Object[] firstFunctionChildren = firstFunction.getChildren().toArray();
        AggregatedCalledFunction function2 = (AggregatedCalledFunction) firstFunctionChildren[0];
        AggregatedCalledFunction function3 = (AggregatedCalledFunction) firstFunctionChildren[1];
        // Test the first function statistics
        @NonNull
        AggregatedCalledFunctionStatistics functionStatistics1 = firstFunction.getFunctionStatistics();
        assertEquals("Test first function's maximum duration", 50, functionStatistics1.getDurationStatistics().getMax());
        assertEquals("Test first function's minimum duration", 20, functionStatistics1.getDurationStatistics().getMin());
        assertEquals("Test first function's maximum self time", 40, functionStatistics1.getSelfTimeStatistics().getMax());
        assertEquals("Test first function's minimum self time", 10, functionStatistics1.getSelfTimeStatistics().getMin());
        assertEquals("Test first function's number of segments", 2, functionStatistics1.getDurationStatistics().getNbElements());
        assertEquals("Test first function's average duration", 35, functionStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test first function's average self time", 25, functionStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test first function's standard deviation", Double.NaN, functionStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test first function's self time standard deviation", Double.NaN, functionStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the second function statistics
        @NonNull
        AggregatedCalledFunctionStatistics functionStatistics2 = function2.getFunctionStatistics();
        assertEquals("Test second function's maximum duration", 10, functionStatistics2.getDurationStatistics().getMax());
        assertEquals("Test second function's minimum duration", 10, functionStatistics2.getDurationStatistics().getMin());
        assertEquals("Test second function's maximum self time", 10, functionStatistics2.getSelfTimeStatistics().getMax());
        assertEquals("Test second function's minimum self time", 10, functionStatistics2.getSelfTimeStatistics().getMin());
        assertEquals("Test second function's number of calls", 1, functionStatistics2.getDurationStatistics().getNbElements());
        assertEquals("Test second function's average duration", 10, functionStatistics2.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test second function's average self time", 10, functionStatistics2.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test second function's standard deviation", Double.NaN, functionStatistics2.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test second function's self time standard deviation", Double.NaN, functionStatistics2.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the third function statistics
        @NonNull
        AggregatedCalledFunctionStatistics functionStatistics3 = function3.getFunctionStatistics();
        assertEquals("Test third function's maximum duration", 10, functionStatistics3.getDurationStatistics().getMax());
        assertEquals("Test third function's minimum duration", 10, functionStatistics3.getDurationStatistics().getMin());
        assertEquals("Test third function's maximum selftime", 10, functionStatistics3.getSelfTimeStatistics().getMax());
        assertEquals("Test third function's minimum self time", 10, functionStatistics3.getSelfTimeStatistics().getMin());
        assertEquals("Test third function's number of calls", 1, functionStatistics3.getDurationStatistics().getNbElements());
        assertEquals("Test third function's average duration", 10, functionStatistics3.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test third function's average self time", 10, functionStatistics3.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test third function's standard deviation", Double.NaN, functionStatistics3.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test third function's self time standard deviation", Double.NaN, functionStatistics3.getSelfTimeStatistics().getStdDev(), ERROR);
        cga.dispose();
    }

    /**
     * Build a call stack example.This call stack's structure is shown below :
     *
     * <pre>
     *      ___ main____
     *  ___1___    _1_  _1_
     *  _2_ _3_    _2_
     *  _4_        _4_
     * </pre>
     */
    private static void buildCallStack(ITmfStateSystemBuilder fixture) {
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH, CallGraphAnalysisStub.CALLSTACK_PATH);
        // Create the first function
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        TmfStateValue statev = TmfStateValue.newValueLong(0);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(150, TmfStateValue.nullValue(), quark);
        // Create the first level functions
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        statev = TmfStateValue.newValueLong(1);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(50, TmfStateValue.nullValue(), quark);
        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(100, TmfStateValue.nullValue(), quark);
        fixture.modifyAttribute(130, statev, quark);
        fixture.modifyAttribute(150, TmfStateValue.nullValue(), quark);
        // Create the third function
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_2);
        statev = TmfStateValue.newValueLong(2);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(10, TmfStateValue.nullValue(), quark);

        statev = TmfStateValue.newValueLong(3);
        fixture.modifyAttribute(20, statev, quark);
        fixture.modifyAttribute(30, TmfStateValue.nullValue(), quark);

        statev = TmfStateValue.newValueLong(2);
        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(90, TmfStateValue.nullValue(), quark);

        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_3);
        statev = TmfStateValue.newValueLong(4);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(10, TmfStateValue.nullValue(), quark);

        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(80, TmfStateValue.nullValue(), quark);
        fixture.closeHistory(151);
    }

    /**
     * The call stack's structure used in this test is shown below:
     *
     * <pre>
     *                          Aggregated tree
     *     ___ main____          ____ main____
     *  ___1___    _1_ _1_            _1_
     *  _2_ _3_    _2_      =>      _2_ _3_
     *  _4_        _4_              _4_
     * </pre>
     */
    @Test
    public void MergeSecondLevelCalleesTest() {
        ITmfStateSystemBuilder fixture = createFixture();

        buildCallStack(fixture);
        // Execute the CallGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        assertTrue(cga.iterate());
        List<AggregatedCalledFunction> threads = cga.getGroupNodes();
        // Test the threads generated by the analysis
        assertNotNull(threads);
        // Test the threads generated by the analysis
        assertNotNull(threads);
        Object[] children = threads.get(0).getChildren().toArray();
        AggregatedCalledFunction main = (AggregatedCalledFunction) children[0];
        Object[] mainChildren = main.getChildren().toArray();
        AggregatedCalledFunction function1 = (AggregatedCalledFunction) mainChildren[0];
        Object[] firstFunctionChildren = function1.getChildren().toArray();
        AggregatedCalledFunction function2 = (AggregatedCalledFunction) firstFunctionChildren[0];
        AggregatedCalledFunction function3 = (AggregatedCalledFunction) firstFunctionChildren[1];
        Object[] firstChildCallee = function2.getChildren().toArray();
        AggregatedCalledFunction function4 = (AggregatedCalledFunction) firstChildCallee[0];
        // Test the main function statistics
        AggregatedCalledFunctionStatistics mainStatistics1 = main.getFunctionStatistics();
        assertEquals("Test main's maximum duration", 150, mainStatistics1.getDurationStatistics().getMax());
        assertEquals("Test main's minimum duration", 150, mainStatistics1.getDurationStatistics().getMin());
        assertEquals("Test main's maximum self time", 40, mainStatistics1.getSelfTimeStatistics().getMax());
        assertEquals("Test main's minimum self time", 40, mainStatistics1.getSelfTimeStatistics().getMin());
        assertEquals("Test main's number of calls", 1, mainStatistics1.getDurationStatistics().getNbElements());
        assertEquals("Test main's average duration", 150, mainStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test main's average self time", 40, mainStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test main's standard deviation", Double.NaN, mainStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test main's self time standard deviation", Double.NaN, mainStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the first function statistics
        AggregatedCalledFunctionStatistics firstFunctionStatistics = function1.getFunctionStatistics();
        assertEquals("Test first function's maximum duration", 50, firstFunctionStatistics.getDurationStatistics().getMax());
        assertEquals("Test first function's minimum duration", 20, firstFunctionStatistics.getDurationStatistics().getMin());
        assertEquals("Test first function's maximum self time", 30, firstFunctionStatistics.getSelfTimeStatistics().getMax());
        assertEquals("Test first function's minimum self time", 10, firstFunctionStatistics.getSelfTimeStatistics().getMin());
        assertEquals("Test first function's number of segments", 3, firstFunctionStatistics.getDurationStatistics().getNbElements());
        assertEquals("Test first function's average duration", 36.666666667, firstFunctionStatistics.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test first function's average self time", 20, firstFunctionStatistics.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test first function's standard deviation", 15.275252316, firstFunctionStatistics.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test first function's self time standard deviation", 10, firstFunctionStatistics.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the second function statistics
        AggregatedCalledFunctionStatistics secondFunctionStatistics2 = function2.getFunctionStatistics();
        assertEquals("Test second function's maximum duration", 30, secondFunctionStatistics2.getDurationStatistics().getMax());
        assertEquals("Test second function's minimum duration", 10, secondFunctionStatistics2.getDurationStatistics().getMin());
        assertEquals("Test second function's maximum self time", 10, secondFunctionStatistics2.getSelfTimeStatistics().getMax());
        assertEquals("Test second function's minimum self time", 0, secondFunctionStatistics2.getSelfTimeStatistics().getMin());
        assertEquals("Test second function's number of segments", 2, secondFunctionStatistics2.getDurationStatistics().getNbElements());
        assertEquals("Test second function's average duration", 20, secondFunctionStatistics2.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test second function's average self time", 5, secondFunctionStatistics2.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test second function's standard deviation", Double.NaN, secondFunctionStatistics2.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test second function's self time standard deviation", Double.NaN, secondFunctionStatistics2.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the third function statistics
        AggregatedCalledFunctionStatistics thirdFunctionStatistics3 = function3.getFunctionStatistics();
        assertEquals("Test third function's maximum duration", 10, thirdFunctionStatistics3.getDurationStatistics().getMax());
        assertEquals("Test third function's minimum duration", 10, thirdFunctionStatistics3.getDurationStatistics().getMin());
        assertEquals("Test third function's maximum self time", 10, thirdFunctionStatistics3.getSelfTimeStatistics().getMax());
        assertEquals("Test third function's minimum self time", 10, thirdFunctionStatistics3.getSelfTimeStatistics().getMin());
        assertEquals("Test third function's number of segments", 1, thirdFunctionStatistics3.getDurationStatistics().getNbElements());
        assertEquals("Test third function's average duration", 10, thirdFunctionStatistics3.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test third function's average self time", 10, thirdFunctionStatistics3.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test third function's self time deviation", Double.NaN, thirdFunctionStatistics3.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test third function's self time standard deviation", Double.NaN, thirdFunctionStatistics3.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the fourth function statistics
        AggregatedCalledFunctionStatistics fourthFunctionStatistics4 = function4.getFunctionStatistics();
        assertEquals("Test fourth function's maximum duration", 20, fourthFunctionStatistics4.getDurationStatistics().getMax());
        assertEquals("Test fourth function's minimum duration", 10, fourthFunctionStatistics4.getDurationStatistics().getMin());
        assertEquals("Test fourth function's maximum self time", 20, fourthFunctionStatistics4.getSelfTimeStatistics().getMax());
        assertEquals("Test fourth function's maximum self time", 10, fourthFunctionStatistics4.getSelfTimeStatistics().getMin());
        assertEquals("Test fourth function's number of segments", 2, fourthFunctionStatistics4.getDurationStatistics().getNbElements());
        assertEquals("Test fourth function's average duration", 15, fourthFunctionStatistics4.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test fourth function's average duration", 15, fourthFunctionStatistics4.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test fourth function's standard deviation", Double.NaN, fourthFunctionStatistics4.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test fourth function's self time deviation", Double.NaN, fourthFunctionStatistics4.getSelfTimeStatistics().getStdDev(), ERROR);
        cga.dispose();
    }

}
