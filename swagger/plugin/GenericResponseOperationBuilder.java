package bap.jp.smartfashion.support.swagger.plugin;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.ModelReference;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.contexts.ModelContext;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

import java.util.List;

import static com.google.common.collect.Sets.newHashSet;
import static springfox.documentation.schema.ResolvedTypes.modelRefFactory;
import static springfox.documentation.schema.Types.isVoid;

/**
 * Response Operation Builder Plugin for Generic Controller.
 *
 * @author hungp
 */
@Slf4j
@Component
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER)
public class GenericResponseOperationBuilder extends GenericSwaggerPlugin implements OperationBuilderPlugin {

    private final TypeNameExtractor nameExtractor;

    public GenericResponseOperationBuilder(TypeNameExtractor nameExtractor, TypeResolver resolver) {
        super(resolver);
        this.nameExtractor = nameExtractor;
    }

    @Override
    public void apply(OperationContext context) {
        RequestHandler handler = getRequestHandler(context);
        if (haveConfigurationEntityClass(handler)) {
            ResolvedType responseResolveType = buildResolveTypeForResponseObject(context);

            if (null != responseResolveType) {
                List<ResponseMessage> responseMessages = context.getGlobalResponseMessages(context.httpMethod().toString());
                context.operationBuilder().responseMessages(newHashSet(responseMessages));
                applyReturnTypeOverride(context, responseResolveType);
            }
        }
    }

    /**
     * Override Response type.
     *
     * @param context      Operation Context
     * @param resolvedType Resolved Type
     */
    private void applyReturnTypeOverride(OperationContext context, ResolvedType resolvedType) {
        ResolvedType returnType = context.alternateFor(resolvedType);
        int httpStatusCode = httpStatusCode(context);
        String message = message(context);
        ModelReference modelRef = null;
        if (!isVoid(returnType)) {
            ModelContext modelContext = ModelContext.returnValue(
                    context.getGroupName(),
                    returnType,
                    context.getDocumentationType(),
                    context.getAlternateTypeProvider(),
                    context.getGenericsNamingStrategy(),
                    context.getIgnorableParameterTypes());
            modelRef = modelRefFactory(modelContext, nameExtractor).apply(returnType);
        }
        ResponseMessage built = new ResponseMessageBuilder()
                .code(httpStatusCode)
                .message(message)
                .responseModel(modelRef)
                .build();
        context.operationBuilder().responseMessages(newHashSet(built));
    }

    /**
     * Get Http Status Code.
     *
     * @param context Operation Conntext
     * @return http status code
     */
    public static int httpStatusCode(OperationContext context) {
        Optional<ResponseStatus> responseStatus = context.findAnnotation(ResponseStatus.class);
        int httpStatusCode = HttpStatus.OK.value();
        if (responseStatus.isPresent()) {
            httpStatusCode = responseStatus.get().value().value();
        }
        return httpStatusCode;
    }

    /**
     * Get message.
     *
     * @param context Operation Context
     * @return message
     */
    public static String message(OperationContext context) {
        Optional<ResponseStatus> responseStatus = context.findAnnotation(ResponseStatus.class);
        String reasonPhrase = HttpStatus.OK.getReasonPhrase();
        if (responseStatus.isPresent()) {
            reasonPhrase = responseStatus.get().reason();
            if (reasonPhrase.isEmpty()) {
                reasonPhrase = responseStatus.get().value().getReasonPhrase();
            }
        }
        return reasonPhrase;
    }

    @Override
    public boolean supports(DocumentationType documentationType) {
        return true;
    }
}
