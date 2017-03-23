/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.experimental.core.tests.features;

import static org.junit.Assert.assertFalse;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalFeatureManager;
import org.junit.Test;

/**
 * @author gbastien
 *
 */
public class ExperimentalFeaturesManagerTest {

    @Test
    public void testGetInstalled() {
//        IMetadataRepository repo = new org.eclipse.equinox.p2.tests.TestMetadataRepository();
        IQueryResult<IInstallableUnit> installedFeatures = ExperimentalFeatureManager.getInstalledFeatures();
        assertFalse(installedFeatures.isEmpty());
    }

}
