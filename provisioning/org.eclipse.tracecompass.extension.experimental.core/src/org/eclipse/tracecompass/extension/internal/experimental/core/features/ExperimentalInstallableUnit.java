/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.experimental.core.features;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * @author Geneviève Bastien
 * @since 2.3
 */
public class ExperimentalInstallableUnit {


    private final IInstallableUnit fIu;

    public ExperimentalInstallableUnit(IInstallableUnit iu) {
        fIu = iu;
    }

    public IInstallableUnit getInstallableUnit() {
        return fIu;
    }

    public @Nullable ExperimentalInstallableUnit getParent() {
        return null;
    }

    public List<ExperimentalInstallableUnit> getChildren() {
        return Collections.emptyList();
    }

    public String getName() {
        return NonNullUtils.nullToEmptyString(fIu.getProperty(IInstallableUnit.PROP_NAME, null));
    }

    public String getDescription() {
        return NonNullUtils.nullToEmptyString(fIu.getProperty(IInstallableUnit.PROP_DESCRIPTION, null));
    }

    public String getAction() {
        return StringUtils.EMPTY;
    }

    @Override
    public String toString() {
        return getClass().toString() + " IU: " + fIu.getId();
    }
}
