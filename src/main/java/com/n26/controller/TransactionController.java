package com.n26.controller;

import com.n26.service.StaleTransactionException;
import com.n26.service.TransactionService;
import com.n26.vo.TransactionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping(value = "/transactions")
    public ResponseEntity saveTransaction(@RequestBody final TransactionVO transactionVO) throws StaleTransactionException {
        transactionService.save(transactionVO);
        return new ResponseEntity(HttpStatus.CREATED);
    }
}
