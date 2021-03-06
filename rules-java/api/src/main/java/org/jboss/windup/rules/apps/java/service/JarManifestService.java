package org.jboss.windup.rules.apps.java.service;

import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.service.GraphService;
import org.jboss.windup.rules.apps.java.model.JarManifestModel;

/**
 * Manages the creation, querying, and deletion of {@link JarManifestModel}s.
 * 
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 * 
 */
public class JarManifestService extends GraphService<JarManifestModel>
{
    public JarManifestService(GraphContext context)
    {
        super(context, JarManifestModel.class);
    }
}
