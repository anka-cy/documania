package com.documania.backend.config;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.Jackson3JsonFormatMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

public class HibernateJsonFormatMapper implements FormatMapper {

    private final Jackson3JsonFormatMapper delegate;

    public HibernateJsonFormatMapper() {
        JsonMapper jsonMapper = JsonMapper.builderWithJackson2Defaults()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
        this.delegate = new Jackson3JsonFormatMapper(jsonMapper);
    }

    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        return delegate.fromString(charSequence, javaType, wrapperOptions);
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        return delegate.toString(value, javaType, wrapperOptions);
    }
}