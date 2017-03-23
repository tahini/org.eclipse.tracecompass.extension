/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.experimental.core.features;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;


/**
 * @author Geneviève Bastien
 * @since 2.3
 */
public class ExperimentalCategory extends ExperimentalInstallableUnit {
    private final List<ExperimentalInstallableUnit> fFeatures = new ArrayList<>();

    public ExperimentalCategory(IInstallableUnit iu) {
        super(iu);
    }

    @Override
    public List<ExperimentalInstallableUnit> getChildren() {
        return fFeatures;
    }

    public void addFeature(ExperimentalFeature feature) {
        fFeatures.add(feature);
        feature.setParent(this);
    }
}
