package com.springboot.transfer.domain.port.out;

import com.springboot.transfer.domain.model.Transfer;
import com.springboot.transfer.domain.model.TransferReference;

import java.util.List;
import java.util.Optional;

/** Outbound port for the transfer audit record. */
public interface TransferRepositoryPort {

    Transfer save(Transfer transfer);

    Optional<Transfer> findByReference(TransferReference reference);

    List<Transfer> findAll();
}
