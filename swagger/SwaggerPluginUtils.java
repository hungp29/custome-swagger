package bap.jp.smartfashion.support.swagger;

import bap.jp.smartfashion.common.base.BaseModel;
import bap.jp.smartfashion.common.vo.PageInfo;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.CreateRequestClassDTO;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.CreateResponseClassDTO;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.ReadResponseClassDTO;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.UpdateRequestClassDTO;
import bap.jp.smartfashion.support.httpdefault.annotation.dto.UpdateResponseClassDTO;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import springfox.documentation.RequestHandler;

/**
 * Swagger Plugin Utils.
 *
 * @author hungp
 */
public class SwaggerPluginUtils {

    private SwaggerPluginUtils() {
    }

    /**
     * Build Request Resolve Type.
     *
     * @param resolver    Type Resolve
     * @param handler     Request handler
     * @param entityClass entity class
     * @return Request Resolved Type
     */
    public static ResolvedType buildRequestResolveType(TypeResolver resolver, RequestHandler handler, Class<? extends BaseModel> entityClass) {
        Class<?> dtoClass = null;
        SwaggerGenericCreateMethod genericCreateMethod = handler.findAnnotation(SwaggerGenericCreateMethod.class).orNull();
        SwaggerGenericUpdateMethod genericUpdateMethod = handler.findAnnotation(SwaggerGenericUpdateMethod.class).orNull();
        if (null != genericCreateMethod) {
            CreateRequestClassDTO requestClassDTO = AnnotatedElementUtils.findMergedAnnotation(entityClass, CreateRequestClassDTO.class);
            dtoClass = null != requestClassDTO ? requestClassDTO.value() : null;
        } else if (null != genericUpdateMethod) {
            UpdateRequestClassDTO requestClassDTO = AnnotatedElementUtils.findMergedAnnotation(entityClass, UpdateRequestClassDTO.class);
            dtoClass = null != requestClassDTO ? requestClassDTO.value() : null;
        }

        return null != dtoClass ? resolver.resolve(dtoClass) : null;
    }

    /**
     * Build Response Resolve Type.
     *
     * @param resolver    Type Resolve
     * @param handler     Request handler
     * @param entityClass entity class
     * @return Response Resolve Type
     */
    public static ResolvedType buildResponseResolveType(TypeResolver resolver, RequestHandler handler, Class<? extends BaseModel> entityClass) {
        Class<?> dtoClass = null;
        Class<?> wrapper = null;

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

        return null != dtoClass ? (null == wrapper ? resolver.resolve(dtoClass) : resolver.resolve(wrapper, dtoClass)) : null;
    }
}
