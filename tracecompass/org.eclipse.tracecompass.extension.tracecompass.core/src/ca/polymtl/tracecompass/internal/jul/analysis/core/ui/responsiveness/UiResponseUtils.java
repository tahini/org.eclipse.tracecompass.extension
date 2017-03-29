/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package ca.polymtl.tracecompass.internal.jul.analysis.core.ui.responsiveness;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.segment.TmfXmlPatternSegment;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 *
 *
 * @author Geneviève Bastien
 */
public final class UiResponseUtils {

    private UiResponseUtils() {

    }

    public static @Nullable String getSegmentContentValue(TmfXmlPatternSegment segment, String contentStr) {
        @Nullable ITmfStateValue value = segment.getContent().get(contentStr);
        if (value == null) {
            return null;
        }
        return value.unboxStr();
    }

    public static @Nullable Integer getIntSegmentContentValue(TmfXmlPatternSegment segment, String contentStr) {
        @Nullable ITmfStateValue value = segment.getContent().get(contentStr);
        if (value == null) {
            return null;
        }
        return value.unboxInt();
    }

}
