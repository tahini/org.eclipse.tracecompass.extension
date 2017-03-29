/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package ca.polymtl.tracecompass.internal.jul.analysis.core.formatter;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Formatter for the TraceCompass custom text parser for tracecompass' own
 * traces
 *
 * <pre>
 * Format is [<timestamp in nanos>] [TID=<current TID>] [<logger name>] [<payload>]
 * </pre>
 *
 * @author Geneviève Bastien
 * @since 2.0
 */
public class TraceCompassLogFormatter extends Formatter {

    private static final String DOUBLE_FORMAT = "%1$09d"; //$NON-NLS-1$
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    @Override
    public String format(@Nullable LogRecord record) {

        if (record == null) {
            return EMPTY_STRING;
        }

        // Set the current timestamp
        long currentTime = System.nanoTime();
        StringBuilder message = new StringBuilder("["); //$NON-NLS-1$
        message.append(currentTime / 1000000000);
        message.append("."); //$NON-NLS-1$
        message.append(String.format(DOUBLE_FORMAT, currentTime % 1000000000));
        message.append("] "); //$NON-NLS-1$

        // Set the thread id
        message.append("[TID="); //$NON-NLS-1$
        message.append(Thread.currentThread().getId());
        message.append("] "); //$NON-NLS-1$

        // The the logger name
        message.append("["); //$NON-NLS-1$
        message.append(record.getLoggerName());
        message.append("] "); //$NON-NLS-1$

        // Set the payload
        message.append(record.getMessage());
        message.append("\n"); //$NON-NLS-1$

        return message.toString();
    }

}
