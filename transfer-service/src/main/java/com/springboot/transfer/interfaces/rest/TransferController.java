package com.springboot.transfer.interfaces.rest;

import com.springboot.common.domain.Money;
import com.springboot.common.web.ApiResponse;
import com.springboot.transfer.domain.model.Transfer;
import com.springboot.transfer.domain.model.TransferReference;
import com.springboot.transfer.domain.port.in.QueryTransferUseCase;
import com.springboot.transfer.domain.port.in.SubmitTransferUseCase;
import com.springboot.transfer.interfaces.rest.dto.SubmitTransferRequest;
import com.springboot.transfer.interfaces.rest.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Driving adapter for HTTP.
 *
 * <p>A rejected transfer is still a successfully processed request: the caller
 * asked for an evaluation and received one, with a durable reference. So the
 * response is 201 with a REJECTED status rather than a 4xx - the distinction
 * matters for clients that retry on error codes.
 */
@RestController
@RequestMapping(path = "/api/v1/transfers", produces = "application/json")
public class TransferController {

    private final SubmitTransferUseCase submitTransfer;
    private final QueryTransferUseCase queryTransfer;

    public TransferController(SubmitTransferUseCase submitTransfer, QueryTransferUseCase queryTransfer) {
        this.submitTransfer = submitTransfer;
        this.queryTransfer = queryTransfer;
    }

    @GetMapping
    public ApiResponse<List<TransferResponse>> findAll() {
        return ApiResponse.ok(queryTransfer.findAll().stream().map(TransferResponse::from).toList());
    }

    @GetMapping("/{reference}")
    public ApiResponse<TransferResponse> findByReference(@PathVariable String reference) {
        Transfer transfer = queryTransfer.getByReference(TransferReference.of(reference));
        return ApiResponse.ok(TransferResponse.from(transfer));
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<ApiResponse<TransferResponse>> submit(@Valid @RequestBody SubmitTransferRequest request) {
        Transfer transfer = submitTransfer.submit(new SubmitTransferUseCase.SubmitTransferCommand(
                request.fromAccountNumber(),
                request.toAccountNumber(),
                Money.of(request.amount(), request.currency())));

        return ResponseEntity
                .created(URI.create("/api/v1/transfers/" + transfer.reference().value()))
                .body(ApiResponse.ok("Transfer " + transfer.status(), TransferResponse.from(transfer)));
    }
}
