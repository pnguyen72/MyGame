package mygame.multiplayer.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import mygame.multiplayer.Protocol;

/**
 * Monitors changes in a directory.
 * <p>
 * This periodically (see {@link Scheduler#repeat}) checks a directory for
 * creation/deletion of its files, when there are,
 * calls the callbacks with the changed paths.
 * <p>
 * This only applies to files directly contained in the directory,
 * not recursively.
 * <p>
 * This does not monitor changes in the files content.
 * For that, use {@link FileMonitor}
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class DirectoryMonitor extends Monitor<Path>
{
    private final Path       directory;
    private final List<Path> currentContent;

    /**
     * Creates a directory change monitor.
     * The monitor does not start until a callback is added.
     *
     * @param directory path to the directory
     */
    public DirectoryMonitor(final Path directory)
    {
        this.directory = directory;
        currentContent = Protocol.listDir(directory);
    }

    /**
     * Checks whether any directories have been created/deleted since the last poll.
     * Calls the callbacks with all changes (multiple calls, one changed path each).
     */
    @Override
    void poll()
    {
        if(!Files.exists(directory))
        {
            return;
        }

        final Iterator<Path> currentContentIter;
        currentContentIter = currentContent.iterator();
        while(currentContentIter.hasNext())
        {
            final Path path;
            path = currentContentIter.next();
            if(!Files.exists(path))
            {
                publish(path);
                currentContentIter.remove();
            }
        }

        Protocol.listDir(directory)
                .stream()
                .filter(Predicate.not(currentContent::contains))
                .forEach(path ->
                         {
                             publish(path);
                             currentContent.add(path);
                         });
    }
}
