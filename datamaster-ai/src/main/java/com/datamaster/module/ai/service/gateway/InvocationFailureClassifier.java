package com.datamaster.module.ai.service.gateway;

import dev.langchain4j.exception.HttpException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

/** Inspect exception types/status only: never store upstream messages, payloads or credentials. */
final class InvocationFailureClassifier {
    private InvocationFailureClassifier() { }

    static void mark(AiInvocationGateway.Invocation call, Throwable failure) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable error = failure; error != null && visited.add(error); error = error.getCause()) {
            if (error instanceof HttpException) {
                call.failure("HTTP", ((HttpException) error).statusCode());
                return;
            }
            if (error instanceof SocketTimeoutException || error instanceof HttpTimeoutException
                    || error instanceof TimeoutException || error instanceof dev.langchain4j.exception.TimeoutException) {
                call.failure("TIMEOUT", null);
                return;
            }
            if (error instanceof ConnectException || error instanceof UnknownHostException) {
                call.failure("NETWORK", null);
                return;
            }
            if (error instanceof CancellationException || error instanceof InterruptedException) {
                call.failure("CANCELLED", null);
                return;
            }
        }
        call.failure("PROVIDER", null);
    }
}
