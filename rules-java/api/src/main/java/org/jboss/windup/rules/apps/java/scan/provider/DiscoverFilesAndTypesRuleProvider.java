package org.jboss.windup.rules.apps.java.scan.provider;

import org.jboss.windup.config.AbstractRuleProvider;
import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.metadata.MetadataBuilder;
import org.jboss.windup.config.phase.DiscoveryPhase;
import org.jboss.windup.config.query.Query;
import org.jboss.windup.config.query.QueryGremlinCriterion;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.WindupConfigurationModel;
import org.jboss.windup.graph.model.resource.FileModel;
import org.jboss.windup.rules.apps.java.scan.operation.RecurseDirectoryAndAddFiles;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;

public class DiscoverFilesAndTypesRuleProvider extends AbstractRuleProvider
{
    public DiscoverFilesAndTypesRuleProvider()
    {
        super(MetadataBuilder.forProvider(DiscoverFilesAndTypesRuleProvider.class)
                    .setPhase(DiscoveryPhase.class));
    }

    // @formatter:off
    @Override
    public Configuration getConfiguration(GraphContext context)
    {
        return ConfigurationBuilder.begin()

        .addRule()
        .when(Query.fromType(WindupConfigurationModel.class)
                    .piped(new QueryGremlinCriterion()
                    {
                        
                        @Override
                        public void query(GraphRewrite event, GremlinPipeline<Vertex, Vertex> pipeline)
                        {
                            pipeline.out(WindupConfigurationModel.INPUT_PATH);
                            pipeline.has(FileModel.IS_DIRECTORY, true);
                        }
                    })
        )
        .perform(new RecurseDirectoryAndAddFiles().commitEvery(100));
    }
    // @formatter:on
}
