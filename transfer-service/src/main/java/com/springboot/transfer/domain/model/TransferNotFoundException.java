package com.springboot.transfer.domain.model;

import com.springboot.common.domain.ResourceNotFoundException;

public class TransferNotFoundException extends ResourceNotFoundException {

    public TransferNotFoundException(String reference) {
        super("Transfer", reference);
    }
}
