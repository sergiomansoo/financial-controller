package com.sergio.financial.importer;

public class UnsupportedStatementFormatException extends RuntimeException {
    public static final String MESSAGE = "Formato de extrato não suportado. Envie um CSV Banco Inter em UTF-8.";

    public UnsupportedStatementFormatException() {
        super(MESSAGE);
    }
}
