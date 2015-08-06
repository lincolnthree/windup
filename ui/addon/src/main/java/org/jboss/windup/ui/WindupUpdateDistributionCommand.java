package org.jboss.windup.ui;

import org.jboss.windup.exec.updater.RulesetsUpdater;
import javax.inject.Inject;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Version;


/**
 * Provides a basic UI command updating the whole windup distribution.
 *
 * @author mbriskar
 * @author ozizka
 */
public class WindupUpdateDistributionCommand implements UICommand
{

    @Inject
    Addon currentAddon;

    @Inject
    private DependencyResolver dependencyResolver;

    @Inject
    private RulesetsUpdater updater;

    @Inject
    private DistributionUpdater distUpdater;



    @Override
    public UICommandMetadata getMetadata(UIContext ctx)
    {
        return Metadata.forCommand(getClass()).name("Windup Update Distribution")
                .description("Update the whole windup installation")
                .category(Categories.create("Platform", "Migration"));
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception
    {
        if (!context.getPrompt().promptBoolean(
            "Are you sure you want to continue? This command will delete current directories: addons, bin, lib, rules/migration-core"))
        {
            return Results.fail("Updating distribution was aborted.");
        }

        // Find the latest version.
        Coordinate latestDist = this.updater.getLatestReleaseOf("org.jboss.windup", "windup-distribution");
        Version latestVersion = new SingleVersion(latestDist.getVersion());
        Version installedVersion = currentAddon.getId().getVersion();
        if (latestVersion.compareTo(installedVersion) <= 0)
        {
            return Results.fail("Windup is already in the most updated version.");
        }

        distUpdater.replaceWindupDirectoryWithDistribution(latestDist);

        return Results.success("Sucessfully updated Windup to version " + latestDist.getVersion() + ". Please restart Windup.");
    }



    @Override
    public boolean isEnabled(UIContext context)
    {
        return true;
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception
    {

    }

    @Override
    public void validate(UIValidationContext context)
    {

    }

}
