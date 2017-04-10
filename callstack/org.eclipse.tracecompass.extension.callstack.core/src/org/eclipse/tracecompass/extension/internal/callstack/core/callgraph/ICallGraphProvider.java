/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.callstack.core.callgraph;

import java.util.Collection;

/**
 * Interface that analyses who provide callgraph
 *
 * @author Geneviève Bastien
 */
public interface ICallGraphProvider {

    /**
     * Get the groups
     *
     * @return
     */
    Collection<GroupNode> getGroups();

}
