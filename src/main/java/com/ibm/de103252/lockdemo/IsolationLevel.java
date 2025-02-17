package com.ibm.de103252.lockdemo;

import java.sql.Connection;

public enum IsolationLevel {
    ReadUncommitted(Connection.TRANSACTION_READ_UNCOMMITTED),
    ReadCommitted(Connection.TRANSACTION_READ_COMMITTED),
    RepeatableRead(Connection.TRANSACTION_REPEATABLE_READ),
    Serializable(Connection.TRANSACTION_SERIALIZABLE);

    private final int isolation;

    public static IsolationLevel valueOf(int isolation) {
        switch (isolation) {
        case Connection.TRANSACTION_READ_UNCOMMITTED:
            return ReadUncommitted;
        case Connection.TRANSACTION_READ_COMMITTED:
            return ReadCommitted;
        case Connection.TRANSACTION_REPEATABLE_READ:
            return RepeatableRead;
        case Connection.TRANSACTION_SERIALIZABLE:
            return Serializable;
        default:
            return null;
        }
    }

    private IsolationLevel(final int isolation) {
        this.isolation = isolation;
    }

    public int getIsolation() {
        return isolation;
    }

    @Override
    public String toString() {
        switch (isolation) {
        case Connection.TRANSACTION_READ_UNCOMMITTED:
            return "Read uncommitted";
        case Connection.TRANSACTION_READ_COMMITTED:
            return "Read committed";
        case Connection.TRANSACTION_REPEATABLE_READ:
            return "Repeatable Read";
        case Connection.TRANSACTION_SERIALIZABLE:
            return "Serializable";
        default:
            return "???";
        }
    }
}
