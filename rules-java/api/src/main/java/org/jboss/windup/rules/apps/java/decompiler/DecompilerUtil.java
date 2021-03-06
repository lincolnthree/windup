package org.jboss.windup.rules.apps.java.decompiler;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.WindupConfigurationModel;
import org.jboss.windup.graph.service.WindupConfigurationService;
import org.jboss.windup.rules.apps.java.model.JavaClassFileModel;

/**
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 */
public class DecompilerUtil
{
    /**
     * Returns an appropriate output directory for the decompiled data based upon the provided {@link JavaClassFileModel}.
     *
     * This should be the top-level directory for the package (eg, /tmp/project/foo for the file /tmp/project/foo/com/example/Foo.class).
     *
     * This could be the same directory as the file itself, if the file is already in the output directory. If the .class file is referencing a file
     * in the input directory, then this will be a classes folder underneath the output directory.
     */
    static File getOutputDirectoryForClass(GraphContext context, JavaClassFileModel fileModel)
    {
        final File result;
        WindupConfigurationModel configuration = WindupConfigurationService.getConfigurationModel(context);
        String inputPath = configuration.getInputPath().getFilePath();
        if (fileModel.getFilePath().startsWith(inputPath))
        {
            String outputPath = configuration.getOutputPath().getFilePath();
            result = Paths.get(outputPath).resolve("classes").toFile();
        }
        else
        {

            String packageName = fileModel.getPackageName();
            if (StringUtils.isBlank(packageName))
                return fileModel.asFile().getParentFile();

            String[] packageComponents = packageName.split("\\.");
            File rootFile = fileModel.asFile().getParentFile();
            for (int i = 0; i < packageComponents.length; i++)
            {
                rootFile = rootFile.getParentFile();
            }
            result = rootFile;
        }
        return result;
    }
}
