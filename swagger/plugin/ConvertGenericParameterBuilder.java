package bap.jp.smartfashion.support.swagger.plugin;

import bap.jp.smartfashion.support.httpdefault.DefaultHttpService;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.ReadResponseClassDTO;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import springfox.documentation.RequestHandler;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.contexts.ModelContext;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.spring.web.DescriptionResolver;

import static springfox.documentation.schema.ResolvedTypes.modelRefFactory;
import static springfox.documentation.spi.schema.contexts.ModelContext.inputParam;

/**
 * Parameter Builder Plugin for Generic Controller.
 *
 * @author hungp
 */
@Slf4j
@Component
@Order
public class ConvertGenericParameterBuilder extends GenericSwaggerPlugin implements ParameterBuilderPlugin {

    private final TypeNameExtractor nameExtractor;
    private final DescriptionResolver descriptions;

    public ConvertGenericParameterBuilder(TypeNameExtractor nameExtractor, TypeResolver resolver, DescriptionResolver descriptions) {
        super(resolver);
        this.nameExtractor = nameExtractor;
        this.descriptions = descriptions;
    }

    @Override
    public void apply(ParameterContext context) {
        RequestHandler handler = getRequestHandler(context);
        if (haveConfigurationEntityClass(handler)) {
            ResolvedType requestResolvedType = buildResolveTypeForRequestObject(context);

            if (null != requestResolvedType && shouldConvert(context.resolvedMethodParameter())) {
                ModelContext modelContext = inputParam(
                        context.getGroupName(),
                        requestResolvedType,
                        context.getDocumentationType(),
                        context.getAlternateTypeProvider(),
                        context.getGenericNamingStrategy(),
                        context.getIgnorableParameterTypes());

                context.parameterBuilder()
                        .type(requestResolvedType)
                        .modelRef(modelRefFactory(modelContext, nameExtractor).apply(requestResolvedType));
            } else if (null != context.resolvedMethodParameter()) {
                overrideQueryParamForGetMethod(context, context.resolvedMethodParameter());
            }
        }
    }

    /**
     * Only convert for param has RequestBody annotation.
     *
     * @param resolvedMethodParameter Resolve Method Parameter
     * @return true if param has RequestBody annotation
     */
    private boolean shouldConvert(ResolvedMethodParameter resolvedMethodParameter) {
        if (null != resolvedMethodParameter) {
            return resolvedMethodParameter.hasParameterAnnotation(RequestBody.class);
        }
        return false;
    }

    /**
     * Override type and description of param for Get and Get All method.
     *
     * @param context                 Parameter Context
     * @param resolvedMethodParameter Resolved Method Parameter
     */
    private void overrideQueryParamForGetMethod(ParameterContext context, ResolvedMethodParameter resolvedMethodParameter) {
        if (null != resolvedMethodParameter && resolvedMethodParameter.hasParameterAnnotation(ReadResponseClassDTO.class)) {
            context.parameterBuilder().parameterType("query");
            ApiModelProperty apiModelProperty = resolvedMethodParameter.findAnnotation(ApiModelProperty.class).orNull();
            if (null != apiModelProperty) {
                context.parameterBuilder().description(apiModelProperty.value());
            } else if (DefaultHttpService.PAGE.equals(context.resolvedMethodParameter().defaultName().orNull())) {
                context.parameterBuilder().description("Page");
            } else if (DefaultHttpService.LIMIT.equals(context.resolvedMethodParameter().defaultName().orNull())) {
                context.parameterBuilder().description("Limit");
            } else if (DefaultHttpService.FILTER.equals(context.resolvedMethodParameter().defaultName().orNull())) {
                context.parameterBuilder().description("Filter by properties of DTO");
            }
        }
    }

    @Override
    public boolean supports(DocumentationType documentationType) {
        return true;
    }
}
