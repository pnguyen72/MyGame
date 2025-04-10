package mygame.multiplayer.services;

import java.nio.file.Path;

import mygame.multiplayer.Protocol;

/**
 * Monitors changes in a file.
 * <p>
 * This periodically (see {@link Scheduler#repeat}) checks a file for changes,
 * when there are, calls the callbacks with the new content.
 * <p>
 * If the file is deleted, the callbacks will get a {@code null} value.
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class FileMonitor extends Monitor<String>
{
    private final Path file;

    private String currentContent;

    /**
     * Creates a file change monitor.
     * The monitor does not start until a callback is added.
     *
     * @param file path to the file
     */
    public FileMonitor(final Path file)
    {
        this.file           = file;
        this.currentContent = Protocol.read(file);
    }

    /**
     * Checks whether the file content has changed since the last poll.
     * If it has, calls the callbacks with the new content.
     */
    @Override
    void poll()
    {
        final String newContent;
        newContent = Protocol.read(file);

        if(newContent != null && !newContent.equals(currentContent) ||
           currentContent != null && !currentContent.equals(newContent))
        {
            publish(newContent);
            currentContent = newContent;
        }
    }
}