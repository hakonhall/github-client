package no.ion.gh.cli;

import no.ion.gh.util.GHException;

public class ArgumentIterator {
    private final String[] args;
    private int index = 0;

    public ArgumentIterator(String... args) {
        this.args = args;
    }

    public boolean isValid() { return index < args.length; }
    public boolean isDone() { return !isValid(); }

    public String get() {
        assertValid();
        return args[index];
    }

    public ArgumentIterator increment() {
        assertValid();
        index++;
        return this;
    }

    public String getAndIncrement() {
        assertValid();
        return args[index++];
    }

    /** The current argument must be an option that requires a value. Increment iterator and return the value. */
    public String incrementAndGetOptionValue() {
        String optionName = getAndIncrement();
        if (isDone())
            throw GHException.ofInvalidUsage("Missing argument to option " + optionName);
        return get();
    }

    private void assertValid() {
        if (!isValid())
            throw new IndexOutOfBoundsException(index);
    }
}
