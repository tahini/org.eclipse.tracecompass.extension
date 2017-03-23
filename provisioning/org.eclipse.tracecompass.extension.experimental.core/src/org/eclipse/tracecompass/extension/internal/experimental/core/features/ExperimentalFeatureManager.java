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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.OperationFactory;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.StreamUtils;

/**
 *
 *
 * @author Geneviève Bastien
 * @since 2.3
 */
public final class ExperimentalFeatureManager {

    public enum FeatureAction {
        INSTALL, UNINSTALL, UPDATE, NONE
    }

    /** The ID of this preference page */
    // private static final String EXTENSION_UPDATE_URI =
    // "file:///home/gbastien/Dorsal/divers/extensionUpdateSite"; //$NON-NLS-1$

    private static @Nullable List<ExperimentalInstallableUnit> sfIus = null;
    // private static IQueryResult<IInstallableUnit> INSTALLED_FEATURES;

    private ExperimentalFeatureManager() {

    }

    /**
     * @param eiu
     * @param selected
     * @return
     */
    public static FeatureAction getActionToPerform(ExperimentalInstallableUnit eiu, boolean selected) {
        IQueryResult<@NonNull IInstallableUnit> installedFeatures = getInstalledFeatures();
        IInstallableUnit iu = eiu.getInstallableUnit();

        IQueryResult<IInstallableUnit> query = installedFeatures.query(QueryUtil.createIUQuery(iu.getId()), new NullProgressMonitor());
        // Is the installable unit installed?
        if (query.isEmpty()) {
            // Not present, shall we install it?
            if (selected) {
                return FeatureAction.INSTALL;
            }
            return FeatureAction.NONE;
        }
        // It is installed. Is it the exact same version?
        query = query.query(QueryUtil.createIUQuery(iu), new NullProgressMonitor());
        if (!query.isEmpty()) {
            // Same version, shall we uninstall it
            if (!selected) {
                return FeatureAction.UNINSTALL;
            }
            return FeatureAction.NONE;
        }
        // Old version, shall we update it, uninstall it?
        if (!selected) {
            return FeatureAction.UNINSTALL;
        }
        return FeatureAction.UPDATE;
    }

    /**
     * Compute information about this IU. This computes whether or not this IU
     * is installed and / or updated.
     */
    public static IQueryResult<IInstallableUnit> getInstalledFeatures() {
        OperationFactory factory = new OperationFactory();
        IProgressMonitor monitor = new NullProgressMonitor();
        IQueryResult<IInstallableUnit> installed = factory.listInstalledElements(false, monitor);
        Iterator<@NonNull IInstallableUnit> iterator = installed.iterator();
        StreamUtils.getStream(iterator).forEach(i -> System.out.println(i.getId() + " is feature " + QueryUtil.isGroup(i)));
        installed = installed.query(QueryUtil.createIUGroupQuery(), monitor);
        return installed;
    }

    public static List<ExperimentalInstallableUnit> getExperimentalUnits(@Nullable IMetadataRepository repo) {
        List<ExperimentalInstallableUnit> ius = sfIus;
        if (ius == null) {
            ius = new ArrayList<>();
            Set<IInstallableUnit> installableUnits = getInstallableUnits(repo, QueryUtil.createIUCategoryQuery());
            for (IInstallableUnit unit : installableUnits) {
                ExperimentalCategory category = new ExperimentalCategory(unit);
                ius.add(category);
                Set<IInstallableUnit> features = getInstallableUnits(repo, QueryUtil.createCompoundQuery(QueryUtil.createIUGroupQuery(), QueryUtil.createIUCategoryMemberQuery(unit), true));
                for (IInstallableUnit featureUnit : features) {
                    category.addFeature(new ExperimentalFeature(featureUnit));
                }
            }
            sfIus = ius;
        }
        return ius;
    }

    private static Set<IInstallableUnit> getInstallableUnits(@Nullable IMetadataRepository repo, @Nullable IQuery<IInstallableUnit> query) {
        IQueryResult<IInstallableUnit> installedFeatures = ExperimentalFeatureManager.getInstalledFeatures();
        System.out.println(installedFeatures);
        if (repo == null) {
            return Collections.EMPTY_SET;
        }
        IQueryResult<IInstallableUnit> queryResult = repo.query(query, new NullProgressMonitor());
        for (IInstallableUnit unit : queryResult.toSet()) {
            System.out.println(unit);
        }
        return queryResult.toSet();
    }

}
