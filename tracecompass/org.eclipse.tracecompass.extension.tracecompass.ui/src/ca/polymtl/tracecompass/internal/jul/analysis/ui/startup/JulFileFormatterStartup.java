/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package ca.polymtl.tracecompass.internal.jul.analysis.ui.startup;

import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.ui.IStartup;

import ca.polymtl.tracecompass.internal.jul.analysis.core.formatter.TraceCompassLogFormatter;

/**
 * Startup class that will set the JUL file handler formatter to TraceCompass's
 * own formatter.
 *
 * @author Geneviève Bastien
 */
public class JulFileFormatterStartup implements IStartup {

    @Override
    public void earlyStartup() {
        Logger logger = TraceCompassLog.getLogger(""); //$NON-NLS-1$
        // Add the tracecompass formatter to the FileHandler handler
        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof FileHandler) {
                Formatter formatter = new TraceCompassLogFormatter();
                handler.setFormatter(formatter);
            }
        }
    }



}
