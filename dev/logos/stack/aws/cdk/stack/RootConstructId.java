package dev.logos.stack.aws.cdk.stack;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
public @interface RootConstructId {
}

