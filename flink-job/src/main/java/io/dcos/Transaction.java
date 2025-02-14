package io.dcos;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class Transaction {
    private long timestamp;
    private int origin;
    private int target;
    private int amount;

    // Конструктор по умолчанию
    public Transaction() { }

    // Основной конструктор
    public Transaction(long timestamp, int origin, int target, int amount) {
        this.timestamp = timestamp;
        this.origin = origin;
        this.target = target;
        this.amount = amount;
    }

    // Конструктор для строки
    public Transaction(String transaction) {
        if (transaction == null || transaction.isEmpty()) {
            throw new IllegalArgumentException("Transaction string cannot be null or empty");
        }

        String[] parts = transaction.split(";");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Transaction string must have exactly 4 parts separated by semicolons");
        }

        try {
            this.timestamp = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(parts[0])).toEpochMilli();
            this.origin = Integer.parseInt(parts[1]);
            this.target = Integer.parseInt(parts[2]);
            this.amount = Integer.parseInt(parts[3]);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid timestamp format. Expected ISO-8601 format, e.g., 2024-12-28T10:00:00Z", e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Origin, target, and amount must be valid integers", e);
        }
    }

    // Геттеры и сеттеры
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getOrigin() {
        return origin;
    }

    public void setOrigin(int origin) {
        this.origin = origin;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    // Переопределение метода toString()
    @Override
    public String toString() {
        return "Transaction{" +
                "timestamp=" + timestamp +
                ", origin=" + origin +
                ", target=" + target +
                ", amount=" + amount +
                '}';
    }

    // Переопределение equals() и hashCode()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return timestamp == that.timestamp &&
                origin == that.origin &&
                target == that.target &&
                amount == that.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, origin, target, amount);
    }
}
