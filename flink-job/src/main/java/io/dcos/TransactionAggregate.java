package io.dcos;

import java.util.List;
import java.util.Vector;

public class TransactionAggregate {
    private List<Transaction> transactionVector;
    private long startTimestamp;
    private long endTimestamp;
    private long amount;

    public TransactionAggregate() {
        this.transactionVector = new Vector<>();
    }

    public List<Transaction> getTransactionVector() {
        return transactionVector;
    }

    public void setTransactionVector(List<Transaction> transactionVector) {
        this.transactionVector = transactionVector;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("TransactionAggregate {");
        result.append("startTimestamp=").append(startTimestamp)
              .append(", endTimestamp=").append(endTimestamp)
              .append(", totalAmount=").append(amount)
              .append(":");

        for (Transaction transaction : transactionVector) {
            result.append(System.lineSeparator()).append(transaction.toString());
        }

        result.append("}");
        return result.toString();
    }
}
