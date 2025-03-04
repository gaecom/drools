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
package org.kie.pmml.evaluator.core.utils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.efesto.common.api.model.GeneratedExecutableResource;
import org.kie.efesto.runtimemanager.api.exceptions.KieRuntimeServiceException;
import org.kie.efesto.runtimemanager.api.model.EfestoInput;
import org.kie.pmml.api.enums.PMML_MODEL;
import org.kie.pmml.api.enums.PMML_STEP;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.api.models.PMMLModel;
import org.kie.pmml.api.models.PMMLStep;
import org.kie.pmml.api.runtime.PMMLRuntimeContext;
import org.kie.pmml.commons.model.KiePMMLModel;
import org.kie.pmml.commons.model.KiePMMLModelFactory;
import org.kie.pmml.commons.model.ProcessingDTO;
import org.kie.pmml.evaluator.core.executor.PMMLModelEvaluator;
import org.kie.pmml.evaluator.core.executor.PMMLModelEvaluatorFinder;
import org.kie.pmml.evaluator.core.executor.PMMLModelEvaluatorFinderImpl;
import org.kie.pmml.evaluator.core.implementations.PMMLRuntimeStep;
import org.kie.pmml.evaluator.core.model.EfestoInputPMML;
import org.kie.pmml.evaluator.core.model.EfestoOutputPMML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.efesto.runtimemanager.api.utils.GeneratedResourceUtils.getAllGeneratedExecutableResources;
import static org.kie.efesto.runtimemanager.api.utils.GeneratedResourceUtils.isPresentExecutableOrRedirect;
import static org.kie.pmml.api.enums.PMML_STEP.END;
import static org.kie.pmml.api.enums.PMML_STEP.POST_EVALUATION;
import static org.kie.pmml.api.enums.PMML_STEP.PRE_EVALUATION;
import static org.kie.pmml.api.enums.PMML_STEP.START;
import static org.kie.pmml.commons.Constants.PMML_STRING;
import static org.kie.pmml.commons.Constants.PMML_SUFFIX;
import static org.kie.pmml.commons.utils.PMMLLoaderUtils.loadKiePMMLModelFactory;
import static org.kie.pmml.evaluator.core.utils.PMMLListenerUtils.stepExecuted;
import static org.kie.pmml.evaluator.core.utils.PostProcess.postProcess;
import static org.kie.pmml.evaluator.core.utils.PreProcess.preProcess;

public class PMMLRuntimeHelper {

    private static final Logger logger = LoggerFactory.getLogger(PMMLRuntimeHelper.class.getName());
    private static final PMMLModelEvaluatorFinder pmmlModelExecutorFinder = new PMMLModelEvaluatorFinderImpl();


    private PMMLRuntimeHelper() {
    }


    public static boolean canManage(EfestoInput toEvaluate) {
        return (toEvaluate instanceof EfestoInputPMML) && isPresentExecutableOrRedirect(toEvaluate.getFRI(), PMML_STRING);
    }

    public static Optional<EfestoOutputPMML> execute(EfestoInputPMML toEvaluate, PMMLRuntimeContext pmmlContext) {
        KiePMMLModelFactory kiePMMLModelFactory;
        try {
            kiePMMLModelFactory = loadKiePMMLModelFactory(toEvaluate.getFRI(), pmmlContext);
        } catch (Exception e) {
            logger.warn("{} can not execute {}",
                        PMMLRuntimeHelper.class.getName(),
                        toEvaluate.getFRI());
            return Optional.empty();
        }
        try {
            return Optional.of(getEfestoOutput(kiePMMLModelFactory, toEvaluate));
        } catch (KiePMMLException e) {
            throw e;
        } catch (Exception e) {
            throw new KieRuntimeServiceException(String.format("%s failed to execute %s",
                    PMMLRuntimeHelper.class.getName(),
                    toEvaluate.getFRI()), e);
        }
    }

    public static List<PMMLModel> getPMMLModels(PMMLRuntimeContext pmmlContext) {
        logger.debug("getPMMLModels {}", pmmlContext);
        Collection<GeneratedExecutableResource> finalResources = getAllGeneratedExecutableResources(PMML_STRING);
        logger.debug("finalResources {}", finalResources);
        return finalResources.stream()
                .map(finalResource -> loadKiePMMLModelFactory(finalResource, pmmlContext))
                .flatMap(factory -> factory.getKiePMMLModels().stream())
                .collect(Collectors.toList());
    }

    public static Optional<PMMLModel> getPMMLModel(String fileName, String modelName, PMMLRuntimeContext pmmlContext) {
        logger.trace("getPMMLModel {} {}", fileName, modelName);
        String fileNameToUse = !fileName.endsWith(PMML_SUFFIX) ? fileName + PMML_SUFFIX : fileName;
        return getPMMLModels(pmmlContext)
                .stream()
                .filter(model -> Objects.equals(fileNameToUse, model.getFileName()) && Objects.equals(modelName,
                                                                                                      model.getName()))
                .findFirst();
    }

    public static PMML4Result evaluate(final KiePMMLModel model, final PMMLRuntimeContext context) {
        if (logger.isDebugEnabled()) {
            logger.debug("evaluate {} {}", model, context);
        }
        addStep(() -> getStep(START, model, context.getRequestData()), context);
        final ProcessingDTO processingDTO = preProcess(model, context);
        addStep(() -> getStep(PRE_EVALUATION, model, context.getRequestData()), context);
        PMMLModelEvaluator executor = getFromPMMLModelType(model.getPmmlMODEL())
                .orElseThrow(() -> new KiePMMLException(String.format("PMMLModelEvaluator not found for model %s",
                                                                      model.getPmmlMODEL())));
        PMML4Result toReturn = executor.evaluate(model, context);
        addStep(() -> getStep(POST_EVALUATION, model, context.getRequestData()), context);
        postProcess(toReturn, model, context, processingDTO);
        addStep(() -> getStep(END, model, context.getRequestData()), context);
        return toReturn;
    }

    static EfestoOutputPMML getEfestoOutput(KiePMMLModelFactory kiePMMLModelFactory, EfestoInputPMML darInputPMML) {
        List<KiePMMLModel> kiePMMLModels = kiePMMLModelFactory.getKiePMMLModels();
        PMML4Result result = evaluate(kiePMMLModels, darInputPMML.getInputData());
        return new EfestoOutputPMML(darInputPMML.getFRI(), result);
    }

    static PMML4Result evaluate(final List<KiePMMLModel> kiePMMLModels, final PMMLRuntimeContext pmmlContext) {
        if (logger.isDebugEnabled()) {
            logger.debug("evaluate {}", pmmlContext);
        }
        String modelName = pmmlContext.getRequestData().getModelName();
        KiePMMLModel toEvaluate = getPMMLModel(kiePMMLModels, pmmlContext.getFileName(), modelName).orElseThrow(() -> new KiePMMLException("Failed to retrieve model with name " + modelName));
        return evaluate(toEvaluate, pmmlContext);
    }

    static Optional<KiePMMLModel> getPMMLModel(final List<KiePMMLModel> kiePMMLModels, String fileName, String modelName) {
        logger.trace("getPMMLModel {} {}", kiePMMLModels, modelName);
        String fileNameToUse =  ! fileName.endsWith(PMML_SUFFIX) ? fileName + PMML_SUFFIX: fileName;
        return kiePMMLModels
                .stream()
                .filter(model -> Objects.equals(fileNameToUse, model.getFileName()) &&  Objects.equals(modelName, model.getName()))
                .findFirst();
    }

    /**
     * Returns an <code>Optional&lt;PMMLModelExecutor&gt;</code> to allow
     * incremental development of different model-specific executors
     *
     * @param pmmlMODEL
     * @return
     */
    private static Optional<PMMLModelEvaluator> getFromPMMLModelType(final PMML_MODEL pmmlMODEL) {
        logger.trace("getFromPMMLModelType {}", pmmlMODEL);
        return pmmlModelExecutorFinder.getImplementations(false)
                .stream()
                .filter(implementation -> pmmlMODEL.equals(implementation.getPMMLModelType()))
                .findFirst();
    }

    /**
     * Send the given <code>PMMLStep</code>
     * to the <code>PMMLRuntimeContext</code>
     * @param stepSupplier
     * @param pmmlContext
     */
    private static void addStep(final Supplier<PMMLStep> stepSupplier, final PMMLRuntimeContext pmmlContext) {
        stepExecuted(stepSupplier, pmmlContext);
    }

    static PMMLStep getStep(final PMML_STEP pmmlStep, final KiePMMLModel model, final PMMLRequestData requestData) {
        final PMMLStep toReturn = new PMMLRuntimeStep(pmmlStep);
        toReturn.addInfo("MODEL", model.getName());
        toReturn.addInfo("CORRELATION ID", requestData.getCorrelationId());
        toReturn.addInfo("REQUEST MODEL", requestData.getModelName());
        requestData.getRequestParams()
                .forEach(requestParam -> toReturn.addInfo(requestParam.getName(), requestParam.getValue()));
        return toReturn;
    }
}
