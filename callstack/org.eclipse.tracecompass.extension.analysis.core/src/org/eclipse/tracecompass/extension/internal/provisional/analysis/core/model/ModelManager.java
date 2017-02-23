/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.provisional.analysis.core.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.extension.internal.analysis.core.model.CompositeHostModel;

/**
 * Utility class to manage the models for the hosts
 *
 * @author Geneviève Bastien
 */
public final class ModelManager {

    private static final Map<String, @Nullable IHostModel> MODELS_FOR_HOST = new HashMap<>();

    private ModelManager() {

    }

    /**
     * Get the model for a given host ID.
     *
     * @param hostId
     *            The ID of the host for which to retrieve the model
     * @return The model for the host
     */
    public static IHostModel getModelFor(String hostId) {
        IHostModel model = MODELS_FOR_HOST.get(hostId);
        if (model == null) {
            model = new CompositeHostModel();
            MODELS_FOR_HOST.put(hostId, model);
        }
        return model;
    }

}
