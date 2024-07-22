package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE })
public @interface DefaultFor {

    TypeUseLocation[] value() default {};

    TypeKind[] typeKinds() default {};

    Class<?>[] types() default {};

    String[] names() default {};

    String[] namesExceptions() default {};
}
