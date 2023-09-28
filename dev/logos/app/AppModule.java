package dev.logos.app;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import dev.logos.stack.service.storage.EntityStorage;
import io.grpc.BindableService;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

record EntityStorageType(Type entityType) implements ParameterizedType {
    @Override
    public Type[] getActualTypeArguments() {
        return new Type[]{entityType};
    }

    @Override
    public Type getRawType() {
        return EntityStorage.class;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}

public abstract class AppModule extends AbstractModule {
    public void addService(Class<? extends BindableService> serviceClass) {
        Multibinder
                .newSetBinder(binder(), BindableService.class)
                .addBinding().to(serviceClass);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <EntityStorageClass extends EntityStorage<?>> void addStorage(Class<EntityStorageClass> storageClass) {
        try {
            ParameterizedType genericSuperclass = (ParameterizedType) storageClass.getGenericSuperclass();
            bind(TypeLiteral.get(new EntityStorageType(genericSuperclass.getActualTypeArguments()[0])))
                    .to((Class) storageClass);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("The provided class must be a subclass of a parameterized EntityStorage");
        }
    }
}
