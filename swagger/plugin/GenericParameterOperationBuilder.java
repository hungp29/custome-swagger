package bap.jp.smartfashion.support.swagger.plugin;

import bap.jp.smartfashion.common.base.BaseModel;
import bap.jp.smartfashion.support.httpdefault.ConvertUtils;
import bap.jp.smartfashion.support.httpdefault.DefaultHttpService;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.ReadResponseClassDTO;
import bap.jp.smartfashion.support.swagger.SwaggerGenericReadAllMethod;
import bap.jp.smartfashion.support.swagger.SwaggerGenericReadMethod;
import bap.jp.smartfashion.util.ObjectUtils;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.service.Parameter;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.EnumTypeDeterminer;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.spring.web.plugins.DocumentationPluginsManager;
import springfox.documentation.spring.web.readers.parameter.ExpansionContext;
import springfox.documentation.spring.web.readers.parameter.ModelAttributeParameterExpander;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Lists.newArrayList;
import static springfox.documentation.schema.Collections.isContainerType;
import static springfox.documentation.schema.Maps.isMapType;
import static springfox.documentation.schema.Types.isBaseType;
import static springfox.documentation.schema.Types.typeNameFor;

/**
 * Request Parameters Operation Builder Plugin for Get method of Generic Controller.
 *
 * @author hungp
 */
@Slf4j
@Component
@Order
public class GenericParameterOperationBuilder extends GenericSwaggerPlugin implements OperationBuilderPlugin {

    private final ModelAttributeParameterExpander expander;
    private final EnumTypeDeterminer enumTypeDeterminer;
    private final DocumentationPluginsManager pluginsManager;

    public GenericParameterOperationBuilder(TypeResolver resolver,
                                            ModelAttributeParameterExpander expander,
                                            EnumTypeDeterminer enumTypeDeterminer,
                                            DocumentationPluginsManager pluginsManager) {
        super(resolver);
        this.expander = expander;
        this.enumTypeDeterminer = enumTypeDeterminer;
        this.pluginsManager = pluginsManager;
    }

    @Override
    public void apply(OperationContext context) {
        RequestHandler handler = getRequestHandler(context);

        if (haveConfigurationEntityClass(handler) &&
                hasAnyAnnotation(handler, SwaggerGenericReadMethod.class, SwaggerGenericReadAllMethod.class)) {
            List<ResolvedMethodParameter> resolvedMethodParameters = buildResolvedMethodParameter(handler);

            context.operationBuilder().parameters(context.getGlobalOperationParameters());
            context.operationBuilder().parameters(readParameters(context, resolvedMethodParameters));
        }
    }

    /**
     * Build Resolved Method Parameter.
     *
     * @param handler Request handler
     * @return List Resolved Method parameter
     */
    private List<ResolvedMethodParameter> buildResolvedMethodParameter(RequestHandler handler) {
        int index = 0;
        List<ResolvedMethodParameter> resolvedMethodParameters = new ArrayList<>();

        ReadResponseClassDTO responseClassDTO = AnnotatedElementUtils.findMergedAnnotation(getEntityClass(handler), ReadResponseClassDTO.class);

        if (hasAnyAnnotation(handler, SwaggerGenericReadAllMethod.class) && null != responseClassDTO) {
            Class<? extends BaseModel> entityClass = getEntityClass(handler);
            List<Field> entityFields = ObjectUtils.getFields(entityClass);
            // Add param page
            ResolvedMethodParameter page = new ResolvedMethodParameter(index++, DefaultHttpService.PAGE,
                    ObjectUtils.getAnnotationOfField(null), resolver.resolve(int.class));
            resolvedMethodParameters.add(page.annotate(responseClassDTO));
            // Add param limit
            ResolvedMethodParameter limit = new ResolvedMethodParameter(index++, DefaultHttpService.LIMIT,
                    ObjectUtils.getAnnotationOfField(null), resolver.resolve(int.class));
            resolvedMethodParameters.add(limit.annotate(responseClassDTO));

            List<Field> fields = ObjectUtils.getFields(responseClassDTO.value());
            for (Field field : fields) {
                // Find entity Field
                Field entityField = ConvertUtils.findFieldByMapFieldFrom(entityFields, field.getName());

                // Build Resolve Type
                ResolvedType queryParamType;
                if (null != entityField && (ConvertUtils.isPKField(entityField) || ConvertUtils.isFKField(entityField))) {
                    queryParamType = resolver.resolve(List.class, field.getType());
                } else if (ConvertUtils.isNumber(field)) {
                    queryParamType = resolver.resolve(String.class);
                } else {
                    queryParamType = resolver.resolve(field.getType());
                }

                ResolvedMethodParameter resolvedMethodParameter = new ResolvedMethodParameter(index++, field.getName(),
                        ObjectUtils.getAnnotationOfField(field), queryParamType);

                resolvedMethodParameters.add(resolvedMethodParameter.annotate(responseClassDTO));
            }

            // Add param orderBy
            ResolvedMethodParameter orderBy = new ResolvedMethodParameter(index++, DefaultHttpService.ORDER_BY,
                    ObjectUtils.getAnnotationOfField(null), resolver.resolve(String.class));
            resolvedMethodParameters.add(orderBy.annotate(responseClassDTO));
        }
        // Add param filter
        ResolvedMethodParameter filter = new ResolvedMethodParameter(index, DefaultHttpService.FILTER,
                ObjectUtils.getAnnotationOfField(null), resolver.resolve(String[].class));
        resolvedMethodParameters.add(filter.annotate(responseClassDTO));

        return resolvedMethodParameters;
    }

    /**
     * Read parameters.
     *
     * @param context          Operation Context
     * @param methodParameters List Resolved Method Parameter.
     * @return List parameter
     */
    private List<Parameter> readParameters(final OperationContext context, List<ResolvedMethodParameter> methodParameters) {
        methodParameters.addAll(context.getParameters());
        List<Parameter> parameters = newArrayList();

        for (ResolvedMethodParameter methodParameter : methodParameters) {
            ResolvedType alternate = context.alternateFor(methodParameter.getParameterType());
            if (!shouldIgnore(methodParameter, alternate, context.getIgnorableParameterTypes())) {
                ParameterContext parameterContext = new ParameterContext(methodParameter,
                        new ParameterBuilder(),
                        context.getDocumentationContext(),
                        context.getGenericsNamingStrategy(),
                        context);

                if (shouldExpand(methodParameter, alternate)) {
                    parameters.addAll(
                            expander.expand(
                                    new ExpansionContext("", alternate, context.getDocumentationContext())));
                } else {
                    parameters.add(pluginsManager.parameter(parameterContext));
                }
            }
        }
        return FluentIterable.from(parameters).filter(not(hiddenParams())).toList();
    }

    /**
     * Check param is hidden.
     */
    private Predicate<Parameter> hiddenParams() {
        return Parameter::isHidden;
    }

    /**
     * Check param should ignore.
     */
    private boolean shouldIgnore(
            final ResolvedMethodParameter parameter,
            ResolvedType resolvedParameterType,
            final Set<Class> ignorableParamTypes) {

        if (ignorableParamTypes.contains(resolvedParameterType.getErasedType())) {
            return true;
        }
        return FluentIterable.from(ignorableParamTypes)
                .filter(isAnnotation())
                .filter(parameterIsAnnotatedWithIt(parameter)).size() > 0;

    }

    /**
     * Check param has annotation.
     */
    private Predicate<Class> parameterIsAnnotatedWithIt(final ResolvedMethodParameter parameter) {
        return parameter::hasParameterAnnotation;
    }

    /**
     * Check param is annotation.
     */
    private Predicate<Class> isAnnotation() {
        return Annotation.class::isAssignableFrom;
    }

    /**
     * Check should expand param.
     */
    private boolean shouldExpand(final ResolvedMethodParameter parameter, ResolvedType resolvedParamType) {
        return !parameter.hasParameterAnnotation(RequestBody.class)
                && !parameter.hasParameterAnnotation(RequestPart.class)
                && !parameter.hasParameterAnnotation(RequestParam.class)
                && !parameter.hasParameterAnnotation(PathVariable.class)
                && !isBaseType(typeNameFor(resolvedParamType.getErasedType()))
                && !enumTypeDeterminer.isEnum(resolvedParamType.getErasedType())
                && !isContainerType(resolvedParamType)
                && !isMapType(resolvedParamType);

    }

    @Override
    public boolean supports(DocumentationType documentationType) {
        return true;
    }
}
