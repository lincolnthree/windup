package org.jboss.windup.rules.files;

import java.util.logging.Logger;

import javax.inject.Inject;

import org.jboss.windup.config.AbstractRuleLifecycleListener;
import org.jboss.windup.config.GraphRewrite;

public class FileMappingLifecycleListener extends AbstractRuleLifecycleListener
{
    private static final Logger LOG = Logger.getLogger(FileMappingLifecycleListener.class.getSimpleName());

    @Inject
    private FileMappingGraphChangedListener listener;

    @Override
    public void beforeExecution(GraphRewrite event)
    {
        LOG.info("Registered " + FileMappingGraphChangedListener.class.getSimpleName()
                    + "- Mapped file types will be added to the graph automatically.");
        listener.setGraphRewrite(event);
        event.getGraphContext().getGraph().addListener(listener);
    }
}
