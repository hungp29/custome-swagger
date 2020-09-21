package bap.jp.smartfashion.support.swagger;

import bap.jp.smartfashion.common.base.BaseModel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defined entity for Generic Controller.
 *
 * @author hungp
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SwaggerEntity {

    Class<? extends BaseModel> value();
}
