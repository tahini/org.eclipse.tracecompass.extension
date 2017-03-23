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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalCategory;
import org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalFeature;
import org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalFeatureManager;
import org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalInstallableUnit;
import org.eclipse.tracecompass.extension.internal.experimental.ui.Activator;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.dialogs.FilteredCheckboxTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * @author gbastien
 *
 */
public class ManageExperimentalFeaturesWizardPage extends WizardPage {

    private static final String PAGE_NAME = "ExportTracePackageWizardPage"; //$NON-NLS-1$

    private static final Image IMG_CATEGORY = Activator.getDefault().getImageFromPath("/icons/category_obj.gif"); //$NON-NLS-1$
    private static final Image IMG_FEATURE = Activator.getDefault().getImageFromPath("/icons/iu_obj.gif"); //$NON-NLS-1$

    /** The ID of this preference page */
    // private static final String EXTENSION_UPDATE_URI =
    // "file:///home/gbastien/Dorsal/divers/extensionUpdateSite"; //$NON-NLS-1$
    public static final String EXTENSION_UPDATE_URI = "file:///home/gbastien/Dorsal/divers/extensionUpdateSite"; //$NON-NLS-1$

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private @Nullable IMetadataRepository fRepository = null;

    private @Nullable TreeViewer fTreeViewer = null;

    /**
     * Constructor
     */
    public ManageExperimentalFeaturesWizardPage() {
        super(PAGE_NAME, "Select Experimental Features", Activator.getDefault().getImageDescripterFromPath("/icons/obj16/category_obj.gif"));
        setDescription(
                "Those features are experimental. There is no guarantee they will work as expected or even what 'expected' is.\nThey may be updated, removed at any time,\nthey may break Trace Compass, or they can get you to Trace heaven for your use case!\nBut if they break, you should be able to come back here and remove them.");
    }

    @Override
    public void createControl(@Nullable Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

        GridLayout gl = new GridLayout(2, false);
        composite.setLayout(gl);

        PatternFilter patternFilter = new PatternFilter() {
            // show all children of matching profiles or profiles with matching
            // connection node
            @Override
            protected boolean isLeafMatch(@Nullable Viewer viewer, @Nullable Object element) {
                if (element instanceof ExperimentalFeature) {
                    ExperimentalFeature feature = (ExperimentalFeature) element;

                    // Return true if either the
                    String cmp = feature.getName();
                    if (wordMatches(cmp)) {
                        return true;
                    }
                    cmp = feature.getDescription();
                    if (wordMatches(cmp)) {
                        return true;
                    }
                }
                return false;
            }
        };

        final FilteredCheckboxTree filteredTree = new FilteredCheckboxTree(composite,
                SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, patternFilter, true);

//        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
//        gd.heightHint = 0;
//        // filteredTree.setLayoutData(gd);

        final TreeViewer treeViewer = filteredTree.getViewer();
        fTreeViewer  = treeViewer;
        Tree tree = treeViewer.getTree();
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);

        TreeColumn tc = new TreeColumn(tree, SWT.LEAD, 0);
        tc.setResizable(true);
        tc.setWidth(300);
        tc.setText("Name");

        tc = new TreeColumn(tree, SWT.LEAD, 1);
        tc.setResizable(true);
        tc.setWidth(50);
        tc.setText("Action");

        treeViewer.setContentProvider(new FeaturesContentProvider());

        treeViewer.setLabelProvider(new FeaturesLabelProvider());

        treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
        treeViewer.setInput(ExperimentalFeatureManager.getExperimentalUnits(getRepository()));
        treeViewer.expandAll();

        filteredTree.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(@Nullable CheckStateChangedEvent event) {
                updateSelection();
            }
        });

        setControl(composite);
    }

    private static class FeaturesContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getElements(@Nullable Object inputElement) {
            if (inputElement instanceof Collection<?>) {
                Collection<?> coll = (Collection<?>) inputElement;
                return coll.toArray();
            }
            if (inputElement instanceof ExperimentalInstallableUnit) {
                ExperimentalInstallableUnit iu = (ExperimentalInstallableUnit) inputElement;
                return Collections.singleton(iu.getInstallableUnit().getId()).toArray();
            }
            return EMPTY_ARRAY;
        }

        @Override
        public Object[] getChildren(@Nullable Object parentElement) {
            if (parentElement instanceof ExperimentalInstallableUnit) {
                ExperimentalInstallableUnit category = (ExperimentalInstallableUnit) parentElement;
                return category.getChildren().toArray();
            }
            return EMPTY_ARRAY;
        }

        @Override
        public @Nullable Object getParent(@Nullable Object element) {
            if (element instanceof ExperimentalInstallableUnit) {
                ExperimentalInstallableUnit feature = (ExperimentalInstallableUnit) element;
                return feature.getParent();
            }
            return null;
        }

        @Override
        public boolean hasChildren(@Nullable Object element) {
            if (element instanceof ExperimentalCategory) {
                return true;
            }
            return false;
        }

    }

    private static class FeaturesLabelProvider extends ColumnLabelProvider implements ITableLabelProvider {

        @Override
        public @Nullable String getText(@Nullable Object element) {
            if (element instanceof org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalInstallableUnit) {
                org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalInstallableUnit iu = (ExperimentalInstallableUnit) element;
                return iu.getInstallableUnit().getProperty(IInstallableUnit.PROP_NAME, null);
            }
            return super.getText(element);
        }

        @Override
        public @Nullable Image getImage(@Nullable Object element) {
            if (element instanceof org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalCategory) {
                return IMG_CATEGORY;
            } else if (element instanceof org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalFeature) {
                return IMG_FEATURE;
            }
            return super.getImage(element);
        }

        @Override
        public @Nullable Image getColumnImage(@Nullable Object element, int columnIndex) {
            if (columnIndex == 0) {
                return getImage(element);
            }
            return null;
        }

        @Override
        public @Nullable String getColumnText(@Nullable Object element, int columnIndex) {
            switch (columnIndex) {
            case 0:
                if (element instanceof org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalInstallableUnit) {
                    ExperimentalInstallableUnit iu = (org.eclipse.tracecompass.extension.internal.experimental.core.features.ExperimentalInstallableUnit) element;
                    return iu.getInstallableUnit().getProperty(IInstallableUnit.PROP_NAME, null);
                }
                break;
            case 1:
                if (element instanceof ExperimentalInstallableUnit) {
                    ExperimentalInstallableUnit iu = (ExperimentalInstallableUnit) element;
                    return iu.getAction();
                }
                break;
            default:
                break;
            }
            return null;
        }
    }

    private void updateSelection() {
//        int count = availableIUGroup.getCheckedLeafIUs().length;
//        setPageComplete(count > 0);
//        if (count == 0) {
//            selectionCount.setText(""); //$NON-NLS-1$
//        } else {
//            String message = count == 1 ? ProvUIMessages.AvailableIUsPage_SingleSelectionCount : ProvUIMessages.AvailableIUsPage_MultipleSelectionCount;
//            selectionCount.setText(NLS.bind(message, Integer.toString(count)));
//        }
//        getProvisioningWizard().operationSelectionsChanged(this);
    }

    private @Nullable IMetadataRepository getRepository() {
        IMetadataRepository repo = fRepository;
        if (repo == null) {
            try {
                ProvisioningUI defaultUI = ProvisioningUI.getDefaultUI();
                URI location = new URI(EXTENSION_UPDATE_URI);
                repo = defaultUI.loadMetadataRepository(location, false, new NullProgressMonitor());
                fRepository = repo;
            } catch (URISyntaxException | ProvisionException e) {
                Activator.getDefault().logError("Error reading experimental repository"); //$NON-NLS-1$
            }
        }
        return repo;
    }

    public List<ExperimentalFeature> getSelected() {
        TreeViewer treeViewer = fTreeViewer;
        if (treeViewer == null) {
            return Collections.EMPTY_LIST;
        }
        ITreeSelection structuredSelection = treeViewer.getStructuredSelection();
        List<Object> list = structuredSelection.toList();
        List<ExperimentalFeature> ius = new ArrayList<>();
        for (Object selected : list) {
            if (selected instanceof ExperimentalFeature) {
                ius.add((ExperimentalFeature) selected);
            }
        }
        return ius;
    }

}
