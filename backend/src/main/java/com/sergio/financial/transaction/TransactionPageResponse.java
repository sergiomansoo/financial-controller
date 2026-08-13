package com.sergio.financial.transaction;

import java.util.List;
import org.springframework.data.domain.Page;

public record TransactionPageResponse(List<TransactionResponse> content, int page, int size, long totalElements,
                                      int totalPages) {
    public static TransactionPageResponse from(Page<TransactionResponse> source) {
        return new TransactionPageResponse(source.getContent(), source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages());
    }
}
