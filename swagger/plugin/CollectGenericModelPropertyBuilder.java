package bap.jp.smartfashion.support.swagger.plugin;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import springfox.documentation.RequestHandler;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationModelsProviderPlugin;
import springfox.documentation.spi.service.contexts.RequestMappingContext;

import java.util.List;

import static springfox.documentation.schema.ResolvedTypes.resolvedTypeSignature;

/**
 * Model Property Builder Plugin for Generic Controller.
 *
 * @author hungp
 */
@Slf4j
@Order
@Component
public class CollectGenericModelPropertyBuilder extends GenericSwaggerPlugin implements OperationModelsProviderPlugin {

    public CollectGenericModelPropertyBuilder(TypeResolver resolver) {
        super(resolver);
    }

    @Override
    public void apply(RequestMappingContext context) {
        RequestHandler handler = getRequestHandler(context);

        if (haveConfigurationEntityClass(handler)) {
            collectFromReturnType(context);
            collectParameters(context);
            collectGlobalModels(context);
        }
    }

    /**
     * Collect Global Models.
     *
     * @param context Request context
     */
    private void collectGlobalModels(RequestMappingContext context) {
        for (ResolvedType each : context.getAdditionalModels()) {
            context.operationModelsBuilder().addInputParam(each);
            context.operationModelsBuilder().addReturn(each);
        }
    }

    /**
     * Collect Response Model.
     *
     * @param context Request context
     */
    private void collectFromReturnType(RequestMappingContext context) {
        ResolvedType responseModelType = buildResolveTypeForResponseObject(context);
        if (null == responseModelType) {
            responseModelType = context.getReturnType();
        }
        responseModelType = context.alternateFor(responseModelType);
        log.debug("Adding return parameter of type {}", resolvedTypeSignature(responseModelType).or("<null>"));
        context.operationModelsBuilder().addReturn(responseModelType);
    }

    /**
     * Collect Parameters.
     *
     * @param context Request context
     */
    private void collectParameters(RequestMappingContext context) {
        log.debug("Reading parameters models for handlerMethod |{}|", context.getName());
        List<ResolvedMethodParameter> parameterTypes = context.getParameters();
        for (ResolvedMethodParameter parameterType : parameterTypes) {
            if (parameterType.hasParameterAnnotation(RequestBody.class)
                    || parameterType.hasParameterAnnotation(RequestPart.class)) {
                ResolvedType modelType = buildResolveTypeForRequestObject(context);
                if (null != modelType) {
                    modelType = context.alternateFor(modelType);
                } else {
                    modelType = context.alternateFor(parameterType.getParameterType());
                }
                log.debug("Adding input parameter of type {}", resolvedTypeSignature(modelType).or("<null>"));
                context.operationModelsBuilder().addInputParam(modelType);
            }
        }
        log.debug("Finished reading parameters models for handlerMethod |{}|", context.getName());
    }

    @Override
    public boolean supports(DocumentationType documentationType) {
        return true;
    }
}
