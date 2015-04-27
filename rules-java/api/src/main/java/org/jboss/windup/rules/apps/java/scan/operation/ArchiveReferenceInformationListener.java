package org.jboss.windup.rules.apps.java.scan.operation;

import java.io.File;

import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.ApplicationArchiveModel;
import org.jboss.windup.graph.model.ArchiveModel;
import org.jboss.windup.graph.model.resource.FileModel;
import org.jboss.windup.rules.files.FileModelListener;

/**
 * Sets the archive name for various archive types when they are added to the graph. See also
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ArchiveReferenceInformationListener implements FileModelListener
{
    @Override
    public void typeAdded(GraphContext context, FileModel fileModel)
    {
        File file = new File(fileModel.getFilePath());

        if (fileModel instanceof ArchiveModel)
        {
            ArchiveModel archive = (ArchiveModel) fileModel;
            archive.setArchiveName(file.getName());
        }

        if (fileModel instanceof ApplicationArchiveModel)
        {
            ApplicationArchiveModel archive = (ApplicationArchiveModel) fileModel;
            archive.setApplicationName(file.getName());
        }
    }
}
