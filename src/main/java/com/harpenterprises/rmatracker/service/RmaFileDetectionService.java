package com.harpenterprises.rmatracker.service;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds candidate Hart-style RMA numbers in filenames. Does not read or import files. */
public final class RmaFileDetectionService {
    private static final Pattern RMA_PATTERN = Pattern.compile(
            "(?<![A-Z0-9])V[0-9]{6}(?![A-Z0-9])", Pattern.CASE_INSENSITIVE);

    public List<String> detectAll(String filename) {
        Objects.requireNonNull(filename, "filename");
        String name = filename.toUpperCase(Locale.ROOT);
        Matcher matcher = RMA_PATTERN.matcher(name);
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        while (matcher.find()) candidates.add(matcher.group());
        return List.copyOf(candidates);
    }

    public List<String> detectAll(Path file) {
        Objects.requireNonNull(file, "file");
        return detectAll(file.getFileName().toString());
    }

    /** Returns a result only when exactly one distinct candidate exists. */
    public Optional<String> detect(String filename) {
        List<String> matches = detectAll(filename);
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    public Optional<String> detect(Path file) {
        Objects.requireNonNull(file, "file");
        return detect(file.getFileName().toString());
    }
}
