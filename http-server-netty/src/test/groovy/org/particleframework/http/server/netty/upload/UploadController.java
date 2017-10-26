/*
 * Copyright 2017 original authors
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
package org.particleframework.http.server.netty.upload;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.particleframework.core.type.Argument;
import org.particleframework.http.HttpResponse;
import org.particleframework.http.MediaType;
import org.particleframework.http.annotation.Body;
import org.particleframework.http.annotation.Part;
import org.particleframework.stereotype.Controller;
import org.particleframework.web.router.annotation.Post;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@Singleton
@Controller
public class UploadController {

    @Post(consumes = MediaType.MULTIPART_FORM_DATA)
    public String receiveJson(Data data, String title) {
        return title + ": " + data.toString();
    }

    @Post(consumes = MediaType.MULTIPART_FORM_DATA)
    public String receivePlain(String data, String title) {
        return title + ": " + data;
    }

    @Post(consumes = MediaType.MULTIPART_FORM_DATA)
    public Publisher<HttpResponse> receivePublisher(@Part Flowable<byte[]> data/*, @Part Flowable<String> title*/) {
        StringBuilder builder = new StringBuilder();
        AtomicLong length = new AtomicLong(0);
        PublishSubject<HttpResponse> subject = PublishSubject.create();
        data
            .subscribeOn(Schedulers.io())
            .subscribe(
        new Subscriber<byte[]>() {
            Subscription subscription;
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
                this.subscription = s;
            }

            @Override
            public void onNext(byte[] bytes) {
                builder.append(new String(bytes));
                length.addAndGet(bytes.length);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                subject.onError(t);
            }

            @Override
            public void onComplete() {
                subject.onNext(HttpResponse.ok(builder.toString()));
                subject.onComplete();
            }
        });
        return subject.toFlowable(BackpressureStrategy.ERROR);
    }

    public static class Data {
        String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "title='" + title + '\'' +
                    '}';
        }
    }
}


