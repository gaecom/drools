/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.drl.engine.compilation.service;

import java.util.Collections;
import java.util.List;

import org.drools.drl.ast.descr.PackageDescr;
import org.kie.drl.engine.compilation.model.DrlCompilationContext;
import org.kie.drl.engine.compilation.model.DrlCompilationContextImpl;
import org.kie.drl.engine.compilation.model.DrlCompilationContext;
import org.kie.drl.engine.compilation.model.DrlPackageDescrSetResource;
import org.kie.efesto.compilationmanager.api.exceptions.KieCompilerServiceException;
import org.kie.efesto.compilationmanager.api.model.EfestoCompilationContext;
import org.kie.efesto.compilationmanager.api.model.EfestoCompilationContextImpl;
import org.kie.efesto.compilationmanager.api.model.EfestoCompilationContext;
import org.kie.efesto.compilationmanager.api.model.EfestoCompilationOutput;
import org.kie.efesto.compilationmanager.api.model.EfestoResource;
import org.kie.efesto.compilationmanager.api.model.EfestoSetResource;
import org.kie.efesto.compilationmanager.api.service.KieCompilerService;
import org.kie.memorycompiler.KieMemoryCompiler;

import static org.kie.drl.engine.compilation.utils.DrlCompilerHelper.pkgDescrToExecModel;

public class KieCompilerServicePackDesc implements KieCompilerService<EfestoCompilationOutput, EfestoCompilationContext> {

    @Override
    public boolean canManageResource(EfestoResource toProcess) {
        return toProcess instanceof DrlPackageDescrSetResource || (toProcess instanceof EfestoSetResource  && ((EfestoSetResource)toProcess).getContent().iterator().next() instanceof PackageDescr);
    }

    @Override
    public List<EfestoCompilationOutput> processResource(EfestoResource toProcess, EfestoCompilationContext context) {
        if (!canManageResource(toProcess)) {
            throw new KieCompilerServiceException(String.format("%s can not process %s",
                    this.getClass().getName(),
                    toProcess.getClass().getName()));
        }
        if (!(context instanceof DrlCompilationContext)) {
            context =  getDrlCompilationContext(context);
        }
        return Collections.singletonList( pkgDescrToExecModel((EfestoSetResource<PackageDescr>) toProcess, (DrlCompilationContext) context) );
    }


    private DrlCompilationContext getDrlCompilationContext(EfestoCompilationContext context) {
        if (!(context instanceof EfestoCompilationContextImpl)) {
            throw new KieCompilerServiceException("Expected an EfestoCompilationContextImpl, but got " + context.getClass().getCanonicalName());
        }
        return DrlCompilationContext.buildWithEfestoCompilationContext((EfestoCompilationContextImpl) context);
    }

}
