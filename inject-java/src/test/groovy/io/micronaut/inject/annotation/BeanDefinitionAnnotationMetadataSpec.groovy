/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.inject.annotation

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.EachBean
import io.micronaut.context.annotation.Executable
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requirements
import io.micronaut.context.annotation.Requires
import io.micronaut.inject.AbstractTypeElementSpec
import io.micronaut.inject.BeanConfiguration
import io.micronaut.inject.BeanDefinition

import javax.inject.Scope
import javax.inject.Singleton

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class BeanDefinitionAnnotationMetadataSpec extends AbstractTypeElementSpec {

    void "test alias for existing member values within annotation values"() {
        given:
        BeanDefinition definition = buildBeanDefinition('test.Test','''\
package test;

import io.micronaut.inject.annotation.*;
import io.micronaut.context.annotation.*;

@javax.inject.Singleton
@TestCachePut("test")
@TestCachePut("blah")
class Test {

}
''')
        expect:
        definition != null
        definition.getAnnotation(TestCachePuts.class).value()[0].value() == (['test'] as String[])
        definition.getAnnotation(TestCachePuts.class).value()[0].cacheNames() == (['test'] as String[])
        definition.getAnnotation(TestCachePuts.class).value()[1].value() == (['blah'] as String[])
        definition.getAnnotation(TestCachePuts.class).value()[1].cacheNames() == (['blah'] as String[])
    }

    void "test alias for existing member values"() {
        given:
        BeanDefinition definition = buildBeanDefinition('test.Test','''\
package test;

import io.micronaut.inject.annotation.*;
import io.micronaut.context.annotation.*;

@javax.inject.Singleton
@TestCachePut("test")
class Test {

}
''')
        expect:
        definition != null
        definition.getAnnotation(TestCachePut.class).value() == (['test'] as String[])
        definition.getAnnotation(TestCachePut.class).cacheNames() == (['test'] as String[])
    }

    void "test repeated annotation values"() {
        given:
        BeanDefinition definition = buildBeanDefinition('test.Test','''\
package test;

import io.micronaut.context.annotation.*;

@javax.inject.Singleton
@Requires(property="foo", value="bar")
@Requires(property="baz", value="stuff")
class Test {

    @Executable
    void sometMethod() {}
}
''')
        expect:
        definition != null
        definition.getAnnotation(Requirements.class).value()[0].property() == 'foo'
        definition.getAnnotation(Requirements.class).value()[0].value() == 'bar'
        definition.getAnnotation(Requirements.class).value()[1].property() == 'baz'
        definition.getAnnotation(Requirements.class).value()[1].value() == 'stuff'
    }

    void "test basic method annotation metadata"() {
        given:
        BeanDefinition definition = buildBeanDefinition('test.Test','''\
package test;

import io.micronaut.context.annotation.*;

@javax.inject.Singleton
class Test {

    @Executable
    void sometMethod() {}
}
''')
        expect:
        definition != null
        definition.hasDeclaredAnnotation(Singleton)
        definition.findMethod('sometMethod').isPresent()
        definition.findMethod('sometMethod').get().annotationMetadata.hasDeclaredAnnotation(Executable)
    }

    void "test build configuration"() {
        given:
        BeanConfiguration configuration = buildBeanConfiguration("test", '''
@Configuration
@Requires(property="foo")
package test;
import io.micronaut.context.annotation.*;

''')
        expect:
        configuration != null
        configuration.getAnnotationMetadata().hasStereotype(Requires)
    }

    void "test build bean basic definition"() {
        given:
        BeanDefinition definition = buildBeanDefinition("test.Test", '''
package test;

@javax.inject.Singleton
class Test {

}
''')
        expect:
        definition != null
        definition.hasDeclaredAnnotation(Singleton)
        definition.hasDeclaredStereotype(Scope)
        definition.hasStereotype(Scope)
        !definition.hasStereotype(Primary)
    }

    void "test factory bean definition"() {
        given:
        ClassLoader classLoader = buildClassLoader("test.Test", '''
package test;

import io.micronaut.context.annotation.*;
import java.util.concurrent.*;

@Factory
class Test {

    @EachBean(Test.class)
    @Bean(preDestroy = "shutdown")
    public ExecutorService executorService(Test test) {
        return null;
    }
}

''')
        BeanDefinition definition = classLoader.loadClass('test.$Test$ExecutorServiceDefinition').newInstance()
        expect:
        definition != null
        !definition.hasDeclaredAnnotation(Singleton)
        definition.hasDeclaredAnnotation(Bean)
        definition.hasDeclaredAnnotation(EachBean)
    }
}
