package org.jboss.windup.rules.files;

import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.WindupVertexFrame;
import org.jboss.windup.graph.model.resource.FileModel;

/**
 * Listens to events for {@link FileModel} instances.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 *
 */
public interface FileModelListener
{
    /**
     * Called after a {@link FileModel} has been assigned a new {@link WindupVertexFrame} type in the graph.
     */
    public void typeAdded(GraphContext context, FileModel file);
}
