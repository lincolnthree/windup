package org.jboss.windup.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.forge.furnace.util.Streams;
import org.jboss.windup.util.exception.WindupException;

public class ZipUtil
{
    private static final Logger log = Logger.getLogger(ZipUtil.class.getName());

    private static Set<String> supportedExtensions;

    /**
     * Unzip a classpath resource using the given {@link Class} as the resource root path.
     */
    public static void unzipFromClassResource(Class<?> clazz, String resourcePath, File extractToPath) throws IOException
    {
        File inputFile = File.createTempFile("windup-resource-to-unzip-", ".zip");
        try
        {
            try (final InputStream stream = clazz.getResourceAsStream(resourcePath))
            {
                FileUtils.copyInputStreamToFile(stream, inputFile);
            }
            extractToPath.mkdirs();
            ZipUtil.unzipToFolder(inputFile, extractToPath);
        }
        finally
        {
            inputFile.delete();
        }
    }

    /**
     * Unzip the given {@link File} to the specified directory.
     */
    public static void unzipToFolder(File inputFile, File outputDir) throws IOException
    {
        if (inputFile == null)
            throw new IllegalArgumentException("Argument inputFile is null.");
        if (outputDir == null)
            throw new IllegalArgumentException("Argument outputDir is null.");

        try (ZipFile zipFile = new ZipFile(inputFile))
        {
            Enumeration<? extends ZipEntry> entryEnum = zipFile.entries();
            while (entryEnum.hasMoreElements())
            {
                ZipEntry entry = entryEnum.nextElement();
                String entryName = entry.getName();
                File destFile = new File(outputDir, entryName);
                if (!entry.isDirectory())
                {
                    File parentDir = destFile.getParentFile();
                    if (!parentDir.isDirectory() && !parentDir.mkdirs())
                    {
                        throw new WindupException("Unable to create directory: " + parentDir.getAbsolutePath());
                    }
                    try (InputStream zipInputStream = zipFile.getInputStream(entry))
                    {
                        try (FileOutputStream outputStream = new FileOutputStream(destFile))
                        {
                            Streams.write(zipInputStream, outputStream);
                        }
                    }
                }
            }
        }
    }

    public static String getEndsWithZipRegularExpression()
    {
        Set<String> zipExtensions = getZipExtensions();
        final String regex;
        if (zipExtensions.size() == 1)
        {
            regex = ".+\\." + zipExtensions.iterator().next() + "$";
        }
        else
        {
            StringBuilder builder = new StringBuilder();
            builder.append("\\b(");
            for (String value : zipExtensions)
            {
                builder.append("|");
                builder.append(value);
            }
            builder.append(")\\b");
            regex = ".+\\." + builder.toString() + "$";
        }
        return regex;
    }

    public static boolean endsWithZipExtension(String path)
    {
        for (String extension : getZipExtensions())
        {
            if (StringUtils.endsWith(path, "." + extension))
            {
                return true;
            }
        }
        return false;
    }

    public static Set<String> getZipExtensions()
    {
        if (supportedExtensions == null)
        {
            Set<String> extensions = new HashSet<String>();
            extensions.add("war");
            extensions.add("ear");
            extensions.add("jar");
            extensions.add("sar");
            extensions.add("rar");
            supportedExtensions = extensions;
        }

        return supportedExtensions;
    }
}
