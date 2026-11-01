package org.cloudburstmc.server.level;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudLevelLightSubmitTest {

    @Test
    void synchronousRejectionRethrows() {
        CompletableFuture<Void> rejected = new CompletableFuture<>();
        rejected.completeExceptionally(new RejectedExecutionException("test"));

        assertThrows(RejectedExecutionException.class, () -> CloudLevel.rethrowIfRejected(rejected));
    }

    @Test
    void otherOutcomesDoNotThrow() {
        CloudLevel.rethrowIfRejected(new CompletableFuture<>());
        CloudLevel.rethrowIfRejected(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("test"));
        CloudLevel.rethrowIfRejected(failed);
    }
}
