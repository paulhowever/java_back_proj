package ru.tischenko.vk.service;

public final class Exceptions {
    private Exceptions() {
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    public static class BusinessRuleException extends RuntimeException {
        public BusinessRuleException(String message) { super(message); }
    }
}
