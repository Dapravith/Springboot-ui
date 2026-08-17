package com.springboot.account.interfaces.rest;

import com.springboot.account.domain.model.Account;
import com.springboot.account.domain.model.AccountNumber;
import com.springboot.account.domain.port.in.OpenAccountUseCase;
import com.springboot.account.domain.port.in.QueryAccountUseCase;
import com.springboot.account.domain.port.in.TransferFundsUseCase;
import com.springboot.account.interfaces.rest.dto.AccountResponse;
import com.springboot.account.interfaces.rest.dto.OpenAccountRequest;
import com.springboot.account.interfaces.rest.dto.TransferRequest;
import com.springboot.common.domain.Money;
import com.springboot.common.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Driving adapter for HTTP. Translates between the wire format and the inbound
 * ports, and nothing else.
 */
@RestController
@RequestMapping(path = "/api/v1/accounts", produces = "application/json")
public class AccountController {

    private final OpenAccountUseCase openAccount;
    private final TransferFundsUseCase transferFunds;
    private final QueryAccountUseCase queryAccount;

    public AccountController(OpenAccountUseCase openAccount,
                             TransferFundsUseCase transferFunds,
                             QueryAccountUseCase queryAccount) {
        this.openAccount = openAccount;
        this.transferFunds = transferFunds;
        this.queryAccount = queryAccount;
    }

    @GetMapping
    public ApiResponse<List<AccountResponse>> findAll(@RequestParam(required = false) String customerNumber) {
        List<Account> accounts = (customerNumber == null)
                ? queryAccount.findAll()
                : queryAccount.findByCustomerNumber(customerNumber);

        return ApiResponse.ok(accounts.stream().map(AccountResponse::from).toList());
    }

    @GetMapping("/{accountNumber}")
    public ApiResponse<AccountResponse> findByNumber(@PathVariable String accountNumber) {
        Account account = queryAccount.getByAccountNumber(AccountNumber.of(accountNumber));
        return ApiResponse.ok(AccountResponse.from(account));
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<ApiResponse<AccountResponse>> open(@Valid @RequestBody OpenAccountRequest request) {
        Account opened = openAccount.open(new OpenAccountUseCase.OpenAccountCommand(
                request.customerNumber(),
                Money.of(request.openingBalance(), request.currency())));

        return ResponseEntity
                .created(URI.create("/api/v1/accounts/" + opened.accountNumber().value()))
                .body(ApiResponse.ok("Account opened", AccountResponse.from(opened)));
    }

    /**
     * Authoritative balance movement. Returns the resulting state of the source
     * account so the caller can confirm the outcome without a second request.
     */
    @PostMapping(path = "/transfers", consumes = "application/json")
    public ApiResponse<AccountResponse> transfer(@Valid @RequestBody TransferRequest request) {
        AccountNumber from = AccountNumber.of(request.fromAccountNumber());

        transferFunds.transfer(new TransferFundsUseCase.TransferCommand(
                from,
                AccountNumber.of(request.toAccountNumber()),
                Money.of(request.amount(), request.currency())));

        return ApiResponse.ok("Transfer posted", AccountResponse.from(queryAccount.getByAccountNumber(from)));
    }
}
