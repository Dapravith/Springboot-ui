package com.springboot.transfer.domain.port.in;

import com.springboot.transfer.domain.model.Transfer;
import com.springboot.transfer.domain.model.TransferReference;

import java.util.List;

/** Inbound port: read-side access to submitted transfers. */
public interface QueryTransferUseCase {

    List<Transfer> findAll();

    /**
     * @throws com.springboot.transfer.domain.model.TransferNotFoundException if absent
     */
    Transfer getByReference(TransferReference reference);
}
