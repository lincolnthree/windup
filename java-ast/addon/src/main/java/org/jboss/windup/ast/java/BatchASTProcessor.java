package org.jboss.windup.ast.java;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IBinding;
import org.jboss.forge.furnace.util.Predicate;
import org.jboss.windup.ast.java.data.ClassReference;
import org.jboss.windup.ast.java.data.ClassReferences;

/**
 * Provides the ability to parse a Java file and return a {@link ClassReferences} object containing the fully qualified
 * names of all of the contained references.
 * 
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 *
 */
public class BatchASTProcessor
{

    private static final int MAX_FILES = 1000;

    private static Logger LOG = Logger.getLogger(BatchASTProcessor.class.getName());

    private final Set<String> libraryPaths;
    private final Set<String> sourcePaths;

    public static void analyzeJavaFiles(Set<String> libraryPaths, Set<String> sourcePaths, Set<File> sourceFiles,
                Predicate<CompilationUnit> unitsCallback, Predicate<ClassReference> referencesCallback)
    {
        new BatchASTProcessor(libraryPaths, sourcePaths).analyzeFiles(sourceFiles, unitsCallback, referencesCallback);
    }

    public BatchASTProcessor(Set<String> libraryPaths, Set<String> sourcePaths)
    {
        this.libraryPaths = libraryPaths;
        this.sourcePaths = sourcePaths;
    }

    public void analyzeFiles(Set<File> sourceFiles, final Predicate<CompilationUnit> unitsCallback, final Predicate<ClassReference> callback)
    {
        String[] libraryPathsArray = libraryPaths.toArray(new String[libraryPaths.size()]);
        String[] sourcePathsArray = sourcePaths.toArray(new String[sourcePaths.size()]);
        String[] bindingKeys = new String[] {};
        String[] encodings = null;

        ASTParser parser = ASTParser.newParser(AST.JLS8);

        int i = 0;
        while (i < sourceFiles.size())
        {
            FileASTRequestor requestor = new FileASTRequestor()
            {
                NoopWildcardImportResolver importResolver = new NoopWildcardImportResolver();

                @Override
                public void acceptAST(String sourceFilePath, CompilationUnit unit)
                {
                    if (!unitsCallback.accept(unit))
                        return;

                    ASTProcessor processor = new ASTProcessor(unit, importResolver);
                    unit.accept(processor);
                    for (ClassReference ref : processor.getJavaClassReferences())
                    {
                        if (!callback.accept(ref))
                            return;
                    }
                }

                @Override
                public void acceptBinding(String bindingKey, IBinding binding)
                {
                    System.out.println(binding);
                }
            };

            IProgressMonitor progressMonitor = new SimpleProgressMonitor(i * 2, sourceFiles.size() * 2);
            Set<String> filePaths = new LinkedHashSet<>();
            List<File> sourceFilesList = new ArrayList<>(sourceFiles);
            for (int j = 0; j < MAX_FILES && i < sourceFilesList.size(); j++, i++)
            {
                filePaths.add(sourceFilesList.get(i).getAbsolutePath());
            }

            LOG.info(filePaths.size() + " files to be parsed to AST");

            String[] sourceFilePaths = filePaths.toArray(new String[] {});

            parser.setEnvironment(
                        libraryPathsArray,
                        sourcePathsArray,
                        null,
                        true);
            parser.setBindingsRecovery(false);
            parser.setResolveBindings(true);
            parser.createASTs(sourceFilePaths, encodings, bindingKeys, requestor, progressMonitor);
        }
    }

    private final class SimpleProgressMonitor implements IProgressMonitor
    {
        private boolean cancelled;
        private String taskName;
        private int progress = 0;
        private int total = 0;
        private int superTotal = 0;
        private int superProgress = 0;

        public SimpleProgressMonitor(int progress, int total)
        {
            this.superProgress = progress;
            this.superTotal = total;
        }

        @Override
        public void worked(int increment)
        {
            progress += increment;
            LOG.info("Worked " + (taskName != null ? taskName : "") + progress + "/" + total + " - " + (superProgress + progress) + "/" + superTotal);
        }

        @Override
        public void subTask(String arg0)
        {
            LOG.info("Begin Subtask " + arg0 + " " + arg0);
        }

        @Override
        public void setTaskName(String arg0)
        {
            this.taskName = arg0;
        }

        @Override
        public void setCanceled(boolean arg0)
        {
            this.cancelled = arg0;
        }

        @Override
        public boolean isCanceled()
        {
            return cancelled;
        }

        @Override
        public void internalWorked(double arg0)
        {
        }

        @Override
        public void done()
        {
            LOG.info("Done");
        }

        @Override
        public void beginTask(String arg0, int total)
        {
            this.total = total;
            LOG.info("Beginning task " + arg0 + " " + total);
        }
    }
}
