package dev.logos.service.storage.pg.exporter.module.annotation;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
public @interface BuildPackage {
}