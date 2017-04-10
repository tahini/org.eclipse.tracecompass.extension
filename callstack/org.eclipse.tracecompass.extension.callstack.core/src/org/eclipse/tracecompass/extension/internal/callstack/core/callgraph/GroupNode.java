/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.extension.internal.callstack.core.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Geneviève Bastien
 */
public class GroupNode {

    private final String fName;
    private final List<GroupNode> fChildren = new ArrayList<>();

    public GroupNode(String name) {
        fName = name;
    }

    public String getName() {
        return fName;
    }

    public Collection<GroupNode> getChildren() {
        return fChildren;
    }

    public void addChild(GroupNode node) {
        fChildren.add(node);
    }

    public @Nullable Collection<AggregatedCallSite> getAggregatedData() {
        return null;
    }


}
