package bap.jp.smartfashion.support.swagger.plugin;

import bap.jp.smartfashion.common.base.BaseModel;
import bap.jp.smartfashion.common.vo.PageInfo;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.CreateRequestClassDTO;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.CreateResponseClassDTO;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.ReadResponseClassDTO;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.UpdateRequestClassDTO;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.UpdateResponseClassDTO;
import bap.jp.smartfashion.support.swagger.*;
import bap.jp.smartfashion.util.ObjectUtils;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import springfox.documentation.RequestHandler;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.spi.service.contexts.RequestMappingContext;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * Swagger Plugin.
 *
 * @author hungp
 */
@Slf4j
public class GenericSwaggerPlugin {

    protected final TypeResolver resolver;

    public GenericSwaggerPlugin(TypeResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Get request handler from Request Context.
     *
     * @param context Request Context
     * @return request handler
     */
    protected RequestHandler getRequestHandler(Object context) {
        try {
            if (null != context) {
                if (ParameterContext.class.isAssignableFrom(context.getClass())) {
                    context = ObjectUtils.getValueOfField(context, "operationContext", OperationContext.class);
                    return getRequestHandler(context);
                } else if (OperationContext.class.isAssignableFrom(context.getClass())) {
                    context = ObjectUtils.getValueOfField(context, "requestContext", RequestMappingContext.class);
                    return getRequestHandler(context);
                } else if (RequestMappingContext.class.isAssignableFrom(context.getClass())) {
                    return ObjectUtils.getValueOfField(context, "handler", RequestHandler.class);
                }
            }
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Get Entity Class.
     *
     * @param handler Request Handler.
     * @return Entity class
     */
    protected Class<? extends BaseModel> getEntityClass(RequestHandler handler) {
        SwaggerEntity swaggerEntity = handler.findControllerAnnotation(SwaggerEntity.class).orNull();
        if (null != swaggerEntity) {
            return swaggerEntity.value();
        }
        return null;
    }

    /**
     * Check configuration generic controller.
     *
     * @param handler request handler
     * @return true if request handler have SwaggerEntity annotation, otherwise return false
     */
    protected boolean haveConfigurationEntityClass(RequestHandler handler) {
        return null != handler && handler.findControllerAnnotation(SwaggerEntity.class).isPresent();
    }

    /**
     * Check Request Handler have any annotations.
     *
     * @param handler     Request Handler
     * @param annotations list annotation class
     * @return true if Request Handler have any annotation
     */
    @SafeVarargs
    protected final boolean hasAnyAnnotation(RequestHandler handler, Class<? extends Annotation>... annotations) {
        return Arrays.stream(annotations).anyMatch(annotation -> handler.findAnnotation(annotation).isPresent());
    }

    /**
     * Build Request Resolve Type.
     *
     * @param context Request Context
     * @return Request Resolved Type
     */
    protected ResolvedType buildResolveTypeForRequestObject(Object context) {
        RequestHandler handler = getRequestHandler(context);
        Class<? extends BaseModel> entityClass = getEntityClass(handler);

        Class<?> dtoClass = null;
        if (null != entityClass) {
            SwaggerGenericCreateMethod genericCreateMethod = handler.findAnnotation(SwaggerGenericCreateMethod.class).orNull();
            SwaggerGenericUpdateMethod genericUpdateMethod = handler.findAnnotation(SwaggerGenericUpdateMethod.class).orNull();
            if (null != genericCreateMethod) {
                CreateRequestClassDTO requestClassDTO = AnnotatedElementUtils.findMergedAnnotation(entityClass, CreateRequestClassDTO.class);
                dtoClass = null != requestClassDTO ? requestClassDTO.value() : null;
            } else if (null != genericUpdateMethod) {
                UpdateRequestClassDTO requestClassDTO = AnnotatedElementUtils.findMergedAnnotation(entityClass, UpdateRequestClassDTO.class);
                dtoClass = null != requestClassDTO ? requestClassDTO.value() : null;
            }
        }

        return null != dtoClass ? resolver.resolve(dtoClass) : null;
    }

    /**
     * Build Response Resolve Type.
     *
     * @param context Request Context
     * @return Response Resolve Type
     */
    protected ResolvedType buildResolveTypeForResponseObject(Object context) {
        RequestHandler handler = getRequestHandler(context);
        Class<? extends BaseModel> entityClass = getEntityClass(handler);

        Class<?> dtoClass = null;
        Class<?> wrapper = null;

        if (null != entityClass) {
            SwaggerGenericCreateMethod genericCreateMethod = handler.findAnnotation(SwaggerGenericCreateMethod.class).orNull();
            SwaggerGenericUpdateMethod genericUpdateMethod = handler.findAnnotation(SwaggerGenericUpdateMethod.class).orNull();
            SwaggerGenericReadMethod genericReadMethod = handler.findAnnotation(SwaggerGenericReadMethod.class).orNull();
            SwaggerGenericReadAllMethod genericReadAllMethod = handler.findAnnotation(SwaggerGenericReadAllMethod.class).orNull();
            if (null != genericCreateMethod) {
                CreateResponseClassDTO responseClassDTO = AnnotatedElementUtils.findMergedAnnotation(entityClass, CreateResponseClassDTO.class);
                dtoClass = null != responseClassDTO ? responseClassDTO.value() : null;
            } else if (null != genericUpdateMethod) {
                UpdateResponseClassDTO responseClassDTO = AnnotatedElementUtils.findMergedAnnotation(entityClass, UpdateResponseClassDTO.class);
                dtoClass = null != responseClassDTO ? responseClassDTO.value() : null;
            } else if (null != genericReadMethod) {
                ReadResponseClassDTO responseClassDTO = AnnotatedElementUtils.findMergedAnnotation(entityClass, ReadResponseClassDTO.class);
                dtoClass = null != responseClassDTO ? responseClassDTO.value() : null;
            } else if (null != genericReadAllMethod) {
                ReadResponseClassDTO responseClassDTO = AnnotatedElementUtils.findMergedAnnotation(entityClass, ReadResponseClassDTO.class);
                dtoClass = null != responseClassDTO ? responseClassDTO.value() : null;
                wrapper = PageInfo.class;
            }
        }

        return null != dtoClass ? (null == wrapper ? resolver.resolve(dtoClass) : resolver.resolve(wrapper, dtoClass)) : null;
    }
}
