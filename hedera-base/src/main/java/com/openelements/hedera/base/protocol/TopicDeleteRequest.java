package com.openelements.hedera.base.protocol;

import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.TopicId;
import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

public record TopicDeleteRequest(Hbar maxTransactionFee,

                                 Duration transactionValidDuration,

                                 @NonNull TopicId topicId) implements TransactionRequest {

    public TopicDeleteRequest {
       Objects.requireNonNull(topicId, "TopicId cannot be null");
    }

    public static TopicDeleteRequest of(@NonNull TopicId topicId) {
        return new TopicDeleteRequest(DEFAULT_MAX_TRANSACTION_FEE, DEFAULT_TRANSACTION_VALID_DURATION, topicId);
    }
}
