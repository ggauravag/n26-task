package com.n26.service;

import com.n26.vo.TransactionVO;

public interface TransactionService {

    void save(TransactionVO transactionVO) throws StaleTransactionException;

}
