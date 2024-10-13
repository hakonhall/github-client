package no.ion.gh.util;

import java.io.IOException;
import java.io.UncheckedIOException;

public class Exceptions {
    @FunctionalInterface
    public interface SupplierThrowingIOException<T> {
        T get() throws IOException;
    }

    public static <T> T uncheckIO(SupplierThrowingIOException<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    public interface SupplierThrowingInterruptedException<T> {
        T get() throws InterruptedException;
    }

    public static <T> T uncheckInterrupted(SupplierThrowingInterruptedException<T> supplier) {
        try {
            return supplier.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
