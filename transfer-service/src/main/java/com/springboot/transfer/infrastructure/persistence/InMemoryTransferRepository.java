package com.springboot.transfer.infrastructure.persistence;

import com.springboot.transfer.domain.model.Transfer;
import com.springboot.transfer.domain.model.TransferReference;
import com.springboot.transfer.domain.port.out.TransferRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory audit store.
 *
 * <p>Adequate for a development reference environment and nothing more: the
 * record does not survive a restart. Because the domain talks to
 * {@link TransferRepositoryPort} rather than to this class, replacing it with a
 * JPA adapter is a single new class and one bean - no domain or application
 * code changes.
 */
@Component
class InMemoryTransferRepository implements TransferRepositoryPort {

    private final Map<String, Transfer> store = new ConcurrentHashMap<>();

    @Override
    public Transfer save(Transfer transfer) {
        store.put(transfer.reference().value(), transfer);
        return transfer;
    }

    @Override
    public Optional<Transfer> findByReference(TransferReference reference) {
        return Optional.ofNullable(store.get(reference.value()));
    }

    @Override
    public List<Transfer> findAll() {
        return List.copyOf(store.values());
    }
}
