/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package ca.polymtl.tracecompass.internal.jul.analysis.core.latency;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisEventBasedModule;
import org.eclipse.tracecompass.segmentstore.core.BasicSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

/**
 * @author Geneviève Bastien
 */
public class LttngJulTimeAnalysis extends AbstractSegmentStoreAnalysisEventBasedModule {

    /**
     * The ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.examples.tracecompass.timing"; //$NON-NLS-1$
    private long fMaxDiff = 0;
    private long fNbEvents = 0;
    private long fNbSameMilli = 0;
    private long fNbWithin15 = 0;
    private long fNbWithin2 = 0;
    private long fNbOutliers = 0;

    @Override
    protected void canceling() {

    }

    private class TimingEventRequest extends AbstractSegmentStoreAnalysisRequest {

        @Override
        public void handleSuccess() {
            super.handleSuccess();
            System.out.println("Percentage in same milli: " + (fNbSameMilli * 100.0f / fNbEvents) );
            System.out.println("Percentage within 1.5 milliseconds: " + (fNbWithin15 * 100.0f / fNbEvents));
            System.out.println("Percentage within 2 milliseconds: " + (fNbWithin2 * 100.0f / fNbEvents));
            System.out.println("Percentage outliers: " + (fNbOutliers * 100.0f / fNbEvents));
        }

        public TimingEventRequest(ISegmentStore<@NonNull ISegment> segmentStore) {
            super(segmentStore);
        }

        @Override
        public void handleData(@NonNull ITmfEvent event) {
            super.handleData(event);

            long lttngTs = event.getTimestamp().getValue();
            ITmfEventField field = event.getContent().getField("long_millis");
            if (field == null) {
                return;
            }
            fNbEvents++;
            Long value = (Long) field.getValue();
            ITmfTimestamp fromMillis = TmfTimestamp.fromMillis(value);
            long javaTs = fromMillis.toNanos();

            long abs = Math.abs(lttngTs - javaTs);

            if (abs < 1000000) {
                fNbSameMilli++;
            } else if (abs <= 1500000) {
                fNbWithin15++;
            } else if (abs <= 2000000) {
                fNbWithin2++;
            } else {
                fNbOutliers++;
                System.out.println("difference of " + abs + " at " + event.getTimestamp());
            }

            fMaxDiff = Math.max(abs, fMaxDiff);
            getSegmentStore().add(new BasicSegment(Math.min(lttngTs, javaTs), Math.max(javaTs, lttngTs)));
        }
    }

    @Override
    protected Object @NonNull [] readObject(@NonNull ObjectInputStream ois) throws ClassNotFoundException, IOException {
        return checkNotNull((Object[]) ois.readObject());
    }

    @Override
    protected @NonNull AbstractSegmentStoreAnalysisRequest createAnalysisRequest(@NonNull ISegmentStore<@NonNull ISegment> segmentStore) {
        return new TimingEventRequest(segmentStore);
    }

}
