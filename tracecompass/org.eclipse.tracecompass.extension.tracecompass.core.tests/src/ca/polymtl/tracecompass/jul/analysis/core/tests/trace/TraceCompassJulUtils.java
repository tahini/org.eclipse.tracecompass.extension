/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package ca.polymtl.tracecompass.jul.analysis.core.tests.trace;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.ILexicalScope;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.StringDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDefinition;
import org.eclipse.tracecompass.ctf.core.trace.CTFStreamInputReader;
import org.eclipse.tracecompass.ctf.core.trace.ICTFPacketDescriptor;
import org.eclipse.tracecompass.ctf.core.trace.ICTFStream;

/**
 * Contains utility methods and objects to test Trace Compass JUL traces and
 * events
 *
 * @author Geneviève Bastien
 */
public final class TraceCompassJulUtils {

    private static class JulEventDefinition implements IEventDefinition {


        private final IEventDeclaration fDeclaration;
        private final ICompositeDefinition fFields;

        public JulEventDefinition(IEventDeclaration decl, ICompositeDefinition fields, ICTFPacketDescriptor currentPacket) {
            fDeclaration = decl;
            fFields = fields;
        }

        @Override
        public IEventDeclaration getDeclaration() {
            return fDeclaration;
        }

        @Override
        public ICompositeDefinition getEventHeader() {
            return null;
        }

        @Override
        public ICompositeDefinition getFields() {
            return fFields;
        }

        @Override
        public ICompositeDefinition getEventContext() {
            return null;
        }

        @Override
        public ICompositeDefinition getContext() {
            return null;
        }

        @Override
        public ICompositeDefinition getPacketContext() {
            return null;
        }

        @Override
        public int getCPU() {
            return 0;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }

        @Override
        public @NonNull Map<@NonNull String, @NonNull Object> getPacketAttributes() {
            return NonNullUtils.checkNotNull(Collections.EMPTY_MAP);
        }

    }

    private static final @NonNull IEventDeclaration JUL_DECLARATION = new IEventDeclaration() {

        private final StructDeclaration fFields = createJulDeclaration();
        private final StructDeclaration fContext = new StructDeclaration(0);

        @Override
        public IEventDefinition createDefinition(CTFStreamInputReader streamInputReader, @NonNull BitBuffer input, long timestamp) throws CTFException {
            StructDefinition eventPayload = fFields != null ? fFields.createDefinition(null, ILexicalScope.FIELDS, input) : null;
            return new JulEventDefinition(this,
                    eventPayload,
                    null);
        }

        @Override
        public String getName() {
            return "lttng_jul:event";
        }

        @Override
        public StructDeclaration getFields() {
            return fFields;
        }

        @Override
        public StructDeclaration getContext() {
            return fContext;
        }

        @Override
        public Long getId() {
            return null;
        }

        @Override
        public ICTFStream getStream() {
            return null;
        }

        @Override
        public long getLogLevel() {
            return 0;
        }

        @Override
        public @NonNull Set<@NonNull String> getCustomAttributes() {
            return NonNullUtils.checkNotNull(Collections.EMPTY_SET);
        }

        @Override
        public String getCustomAttribute(String key) {
            return null;
        }

    };

    private TraceCompassJulUtils() {

    }

    private static StructDeclaration createJulDeclaration() {
        StructDeclaration structDecl = new StructDeclaration(0);
        structDecl.addField("msg", StringDeclaration.getStringDeclaration());

        return structDecl;
    }

    /**
     * Get the event declaration for a basic JUL event
     *
     * This declaraction contains the following fields: msg
     *
     * @return The JUL event declaration
     */
    public static @NonNull IEventDeclaration getJulEventDeclaration() {
        return JUL_DECLARATION;
    }

}
