package com.dbmetadoc.app.config;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * Fastjson2 序列化配置。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Configuration
public class Fastjson2Config implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);

        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        config.setCharset(StandardCharsets.UTF_8);
        config.setWriterFeatures(JSONWriter.Feature.WriteMapNullValue);
        converter.setFastJsonConfig(config);
        converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
        converters.add(0, converter);
    }
}
