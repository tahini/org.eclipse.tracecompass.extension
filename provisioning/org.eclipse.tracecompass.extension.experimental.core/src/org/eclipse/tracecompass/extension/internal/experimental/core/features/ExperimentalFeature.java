/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.experimental.core.features;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Geneviève Bastien
 * @since 2.3
 */
public class ExperimentalFeature extends ExperimentalInstallableUnit {
    private @Nullable ExperimentalInstallableUnit fParent = null;

    public ExperimentalFeature(IInstallableUnit iu) {
        super(iu);
    }

    @Override
    public @Nullable ExperimentalInstallableUnit getParent() {
        return fParent;
    }

    public void setParent(ExperimentalInstallableUnit parent) {
        fParent = parent;
    }

    @Override
    public String getAction() {
//        @NonNull IInstallableUnit iu = getInstallableUnit();
//        Version version = iu.getVersion();
        return StringUtils.EMPTY;
    }

    public boolean isInstalled() {
        return false;
    }

    public @Nullable Version getInstalledVersion() {
        @NonNull IInstallableUnit iu = getInstallableUnit();
        return iu.getVersion();
    }

}
