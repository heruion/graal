/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.polyglot;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.impl.AbstractPolyglotImpl;

final class HostClassCache {
    private final HostAccess hostAccess;
    private final BiFunction<HostAccess, AnnotatedElement, Boolean> access;
    private final boolean publicAccess;

    private HostClassCache(BiFunction<HostAccess, AnnotatedElement, Boolean> access, HostAccess conf) {
        this.access = access;
        this.hostAccess = conf;
        this.publicAccess = access.apply(conf, HostClassCache.class);
    }

    public static HostClassCache find(AbstractPolyglotImpl.APIAccess apiAccess, HostAccess conf) {
        return apiAccess.connectHostAccess(HostClassCache.class, conf, new Factory(conf));
    }

    private final ClassValue<HostClassDesc> descs = new ClassValue<HostClassDesc>() {
        @Override
        protected HostClassDesc computeValue(Class<?> type) {
            return new HostClassDesc(HostClassCache.this, type);
        }
    };

    @TruffleBoundary
    public static HostClassCache forInstance(HostObject receiver) {
        return receiver.getEngine().getHostClassCache();
    }

    @TruffleBoundary
    HostClassDesc forClass(Class<?> clazz) {
        return descs.get(clazz);
    }

    boolean allowsAccess(Method m) {
        return access.apply(hostAccess, m);
    }

    boolean allowsAccess(Field f) {
        return access.apply(hostAccess, f);
    }

    boolean checkHostAccess(HostAccess toVerify) {
        return this.hostAccess == toVerify;
    }

    boolean isPublicAccess() {
        return publicAccess;
    }

    private static class Factory implements Function<BiFunction<HostAccess, AnnotatedElement, Boolean>, HostClassCache> {
        private final HostAccess conf;

        Factory(HostAccess conf) {
            this.conf = conf;
        }

        @Override
        public HostClassCache apply(BiFunction<HostAccess, AnnotatedElement, Boolean> access) {
            return new HostClassCache(access, conf);
        }
    }

}
