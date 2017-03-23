/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.tmf.ui.experimental.wizard;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.OperationFactory;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalFeature;
import org.eclipse.tracecompass.extension.internal.experimental.ui.Activator;

/**
 *
 *
 * @author Geneviève Bastien
 */
public class ManageExperimentalWizard extends Wizard {

    private @Nullable ManageExperimentalFeaturesWizardPage fPage;

    /**
     * Constructor
     */
    public ManageExperimentalWizard() {
        setWindowTitle("Manage Experimental Features");
    }

    @Override
    public void addPages() {
        super.addPages();
        fPage = new ManageExperimentalFeaturesWizardPage();
        addPage(fPage);
    }

    @Override
    public boolean performFinish() {
        ManageExperimentalFeaturesWizardPage page = fPage;
        if (page == null) {
            return true;
        }
        List<@NonNull ExperimentalFeature> selected = page.getSelected();
        if (selected.isEmpty()) {
            return true;
        }
        Job doExperimentalProvisioning = new Job("Experimental provisioning") {

            @Override
            protected IStatus run(@Nullable IProgressMonitor monitor) {
                try {
                    Set<IInstallableUnit> collect = selected.stream().map(e -> e.getInstallableUnit()).collect(Collectors.toSet());
                    System.out.println(selected);
                    OperationFactory factory = new OperationFactory();

                    InstallOperation installOp = factory.createInstallOperation(collect, Collections.singleton(new URI(ManageExperimentalFeaturesWizardPage.EXTENSION_UPDATE_URI)), monitor);

//                    UninstallOperation uninstallOp = factory.createUninstallOperation(collect, Collections.singleton(new URI(ManageExperimentalFeaturesWizardPage.EXTENSION_UPDATE_URI)), monitor);

                    LoadMetadataRepositoryJob job = new LoadMetadataRepositoryJob(ProvisioningUI.getDefaultUI());
                    Display.getDefault().asyncExec(() -> ProvisioningUI.getDefaultUI().openInstallWizard(collect, installOp, job));

//                    Display.getDefault().asyncExec(() -> ProvisioningUI.getDefaultUI().openUninstallWizard(collect, uninstallOp, job));

                    return Status.OK_STATUS;
                } catch (ProvisionException | URISyntaxException e1) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e1.getMessage());
                }
            }

        };
        doExperimentalProvisioning.schedule();
        return true;
    }

}
