package org.jboss.windup.rules.apps.java.scan.operation;

import java.io.File;
import java.util.logging.Logger;

import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.operation.Commit;
import org.jboss.windup.config.operation.iteration.AbstractIterationOperation;
import org.jboss.windup.graph.model.resource.FileModel;
import org.jboss.windup.graph.service.FileService;
import org.jboss.windup.util.Logging;
import org.ocpsoft.rewrite.context.EvaluationContext;

public class RecurseDirectoryAndAddFiles extends AbstractIterationOperation<FileModel>
{
    private static final Logger LOG = Logging.get(RecurseDirectoryAndAddFiles.class);

    private Commit commitOperation;
    int totalFilesAdded = 0;

    private RecurseDirectoryAndAddFiles(String variableName)
    {
        super(variableName);
    }

    /**
     * Let the variable name to be set by the current Iteration.
     */
    public RecurseDirectoryAndAddFiles()
    {
        super();
    }

    public static RecurseDirectoryAndAddFiles startingAt(String variableName)
    {
        return new RecurseDirectoryAndAddFiles(variableName);
    }

    public RecurseDirectoryAndAddFiles commitEvery(int period)
    {
        this.commitOperation = Commit.every(period);
        return this;
    }

    @Override
    public void perform(GraphRewrite event, EvaluationContext context, FileModel resourceModel)
    {
        FileService fileModelService = new FileService(event.getGraphContext());
        recurseAndAddFiles(event, context, fileModelService, resourceModel);
    }

    /**
     * Recurses the given folder and adds references to these files to the graph as {@link FileModel} instances.
     */
    private void recurseAndAddFiles(GraphRewrite event, EvaluationContext context, FileService fileService, FileModel fileModel)
    {
        File file = fileModel.asFile();
        if (file.isDirectory())
        {
            File[] children = file.listFiles();
            if (children != null)
            {
                for (File child : children)
                {
                    FileModel subFile = fileService.createByFilePath(fileModel, child.getAbsolutePath());
                    totalFilesAdded++;
                    if (commitOperation != null)
                        commitOperation.perform(event, context);

                    if (totalFilesAdded % 1000 == 0)
                    {
                        LOG.info("Found [" + totalFilesAdded + "] files");
                    }

                    recurseAndAddFiles(event, context, fileService, subFile);
                }
            }
        }

    }

    @Override
    public String toString()
    {
        return "RecurseDirectoryAndAddFiles";
    }
}
