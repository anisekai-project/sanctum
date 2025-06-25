package fr.anisekai.sanctum;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Function;

/**
 * Utility class for managing stuff related to {@link Path}.
 */
public final class SanctumUtils {

    private SanctumUtils() {}

    /**
     * Interface defining an action that can throw an exception. (Basically a throwable {@link Runnable})
     */
    public interface Action {

        /**
         * Wrap the provided {@link Action} and if it throws, use the {@link Function} provided to map the exception to a
         * {@link RuntimeException}.
         *
         * @param action
         *         The {@link Action} to wrap
         * @param exceptionWrapper
         *         The {@link Function} used to map an exception to a {@link RuntimeException}.
         */
        static void wrap(Action action, Function<Exception, ? extends RuntimeException> exceptionWrapper) {

            try {
                action.run();
            } catch (Exception e) {
                throw exceptionWrapper.apply(e);
            }
        }

        /**
         * Execute the {@link Action}
         *
         * @throws Exception
         *         If something happens.
         */
        void run() throws Exception;

    }

    /**
     * Recursively deletes the provided {@link Path}. If it's a directory, its content will be deleted first.
     *
     * @param path
     *         The {@link Path} of the directory or file to delete.
     *
     * @throws IOException
     *         If any deletion fails
     */
    public static void delete(Path path) throws IOException {

        if (!Files.exists(path)) {
            return;
        }

        if (Files.isRegularFile(path)) {
            Files.delete(path);
            return;
        }

        if (Files.isDirectory(path)) {
            Files.walkFileTree(
                    path,
                    new SimpleFileVisitor<>() {

                        @Override
                        public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {

                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) throws IOException {

                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    }
            );
            return;
        }

        throw new UnsupportedOperationException("Unable to delete path: " + path);
    }

    /**
     * Copy recursively a {@link Path} to another {@link Path}.
     *
     * @param source
     *         The source {@link Path}
     * @param destination
     *         The destination {@link Path}
     * @param options
     *         An array of {@link CopyOption} to use while copying data.
     *
     * @throws IOException
     *         If the copy fails.
     */
    public static void copy(Path source, Path destination, CopyOption... options) throws IOException {

        if (Files.isDirectory(source)) {
            Files.walkFileTree(
                    source,
                    new SimpleFileVisitor<>() {

                        @Override
                        public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {

                            Files.createDirectories(destination.resolve(source.relativize(dir).toString()));
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {

                            Files.copy(file, destination.resolve(source.relativize(file).toString()), options);
                            return FileVisitResult.CONTINUE;
                        }
                    }
            );
            return;
        }

        if (Files.isRegularFile(source)) {
            Files.copy(source, destination, options);
            return;
        }

        throw new UnsupportedOperationException("Unable to copy source file: " + source);
    }

}
