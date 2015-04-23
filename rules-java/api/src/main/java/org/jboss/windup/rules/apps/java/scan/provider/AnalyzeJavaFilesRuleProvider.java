package org.jboss.windup.rules.apps.java.scan.provider;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.jboss.windup.ast.java.BatchASTProcessor;
import org.jboss.windup.ast.java.data.ClassReference;
import org.jboss.windup.ast.java.data.TypeReferenceLocation;
import org.jboss.windup.ast.java.data.annotations.AnnotationArrayValue;
import org.jboss.windup.ast.java.data.annotations.AnnotationClassReference;
import org.jboss.windup.ast.java.data.annotations.AnnotationLiteralValue;
import org.jboss.windup.ast.java.data.annotations.AnnotationValue;
import org.jboss.windup.config.AbstractRuleProvider;
import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.Variables;
import org.jboss.windup.config.metadata.MetadataBuilder;
import org.jboss.windup.config.operation.GraphOperation;
import org.jboss.windup.config.phase.InitialAnalysisPhase;
import org.jboss.windup.config.query.Query;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.WindupVertexFrame;
import org.jboss.windup.graph.model.resource.FileModel;
import org.jboss.windup.graph.service.GraphService;
import org.jboss.windup.rules.apps.java.model.JarArchiveModel;
import org.jboss.windup.rules.apps.java.model.JavaSourceFileModel;
import org.jboss.windup.rules.apps.java.scan.ast.JavaTypeReferenceModel;
import org.jboss.windup.rules.apps.java.scan.ast.TypeInterestFactory;
import org.jboss.windup.rules.apps.java.scan.ast.WindupWildcardImportResolver;
import org.jboss.windup.rules.apps.java.scan.ast.annotations.JavaAnnotationListTypeValueModel;
import org.jboss.windup.rules.apps.java.scan.ast.annotations.JavaAnnotationLiteralTypeValueModel;
import org.jboss.windup.rules.apps.java.scan.ast.annotations.JavaAnnotationTypeReferenceModel;
import org.jboss.windup.rules.apps.java.scan.ast.annotations.JavaAnnotationTypeValueModel;
import org.jboss.windup.rules.apps.java.service.TypeReferenceService;
import org.jboss.windup.rules.apps.java.service.WindupJavaConfigurationService;
import org.jboss.windup.util.ExecutionStatistics;
import org.jboss.windup.util.Logging;
import org.jboss.windup.util.exception.WindupException;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.context.EvaluationContext;

/**
 * Scan the Java Source code files and store the used type information from them.
 * 
 */
public class AnalyzeJavaFilesRuleProvider extends AbstractRuleProvider
{
    public static final String ALL_JAVA_SOURCES = "allJavaSources";
    private static Logger LOG = Logging.get(AnalyzeJavaFilesRuleProvider.class);

    @Inject
    private WindupWildcardImportResolver importResolver;

    public AnalyzeJavaFilesRuleProvider()
    {
        super(MetadataBuilder.forProvider(AnalyzeJavaFilesRuleProvider.class)
                    .setPhase(InitialAnalysisPhase.class)
                    .setHaltOnException(true));
    }

    // @formatter:off
    @Override
    public Configuration getConfiguration(GraphContext context)
    {
        return ConfigurationBuilder.begin()
            .addRule()
            .when(Query.fromType(JavaSourceFileModel.class).as(ALL_JAVA_SOURCES))
            .perform(new ParseSourceOperation());
    }
    // @formatter:on

    private final class ParseSourceOperation extends GraphOperation
    {
        @Override
        public void perform(final GraphRewrite event, EvaluationContext context)
        {
            ExecutionStatistics.get().begin("AnalyzeJavaFilesRuleProvider.analyzeFiles");
            try
            {
                final Set<String> allSourceDirectories = new HashSet<>();
                final WindupJavaConfigurationService windupJavaConfigurationService = new WindupJavaConfigurationService(
                            event.getGraphContext());
                final Iterable<? extends WindupVertexFrame> allJavaSources = Variables.instance(event).findVariable(ALL_JAVA_SOURCES);
                final Set<File> allJavaSourceFiles = new TreeSet<>();
                final Map<File, JavaSourceFileModel> fileToModel = new TreeMap<>();

                final Set<String> unprocessedByName = new TreeSet<>();
                final Set<File> unprocessedByFile = new TreeSet<>();
                final Set<String> processedByName = new TreeSet<>();

                for (WindupVertexFrame frame : allJavaSources)
                {
                    if (!(frame instanceof JavaSourceFileModel))
                        continue;
                    JavaSourceFileModel javaSourceFileModel = (JavaSourceFileModel) frame;
                    if (!windupJavaConfigurationService.shouldScanPackage(javaSourceFileModel.getPackageName()))
                    {
                        // should not analyze this one, skip it
                        continue;
                    }

                    File sourceFile = javaSourceFileModel.asFile();
                    allJavaSourceFiles.add(sourceFile);
                    fileToModel.put(sourceFile, javaSourceFileModel);
                    unprocessedByFile.add(sourceFile);
                    unprocessedByName.add(sourceFile.getName());
                    FileModel rootSourceFolder = javaSourceFileModel.getRootSourceFolder();
                    if (rootSourceFolder != null)
                    {
                        allSourceDirectories.add(rootSourceFolder.getFilePath());
                    }
                }

                final GraphService<JarArchiveModel> libraryService = new GraphService<JarArchiveModel>(event.getGraphContext(), JarArchiveModel.class);

                Iterable<JarArchiveModel> libraries = libraryService.findAll();
                Set<String> libraryPaths = new HashSet<>();
                for (JarArchiveModel library : libraries)
                {
                    if (library.getUnzippedDirectory() != null)
                    {
                        libraryPaths.add(library.getUnzippedDirectory().getFilePath());
                    }
                    else
                    {
                        libraryPaths.add(library.getFilePath());
                    }
                }

                final BatchASTProcessor processor = new BatchASTProcessor(libraryPaths, allSourceDirectories);

                ExecutionStatistics.get().begin("AnalyzeJavaFilesRuleProvider.parseFile");
                try
                {
                    WindupWildcardImportResolver.setGraphContext(event.getGraphContext());
                    final TypeReferenceService typeReferenceService = new TypeReferenceService(event.getGraphContext());
                    final AtomicInteger filesProcessed = new AtomicInteger(0);

                    BatchASTProcessor.BatchASTCallback callback = new BatchASTProcessor.BatchASTCallback()
                    {
                        @Override
                        public void processed(File file, List<ClassReference> references)
                        {
                            unprocessedByFile.remove(file);
                            unprocessedByName.remove(file.getName());
                            processedByName.add(file.getName());
                            JavaSourceFileModel javaSourceFileModel = fileToModel.get(file);
                            try
                            {
                                for (ClassReference reference : references)
                                {
                                    // we are always interested in types + anything that the TypeInterestFactory has registered
                                    if (reference.getLocation() == TypeReferenceLocation.TYPE
                                                || TypeInterestFactory.matchesAny(reference.getQualifiedName(), reference.getLocation()))
                                    {
                                        JavaTypeReferenceModel typeReference = typeReferenceService.createTypeReference(javaSourceFileModel,
                                                reference.getLocation(),
                                                reference.getLineNumber(), reference.getColumn(), reference.getLength(),
                                                reference.getQualifiedName(),
                                                reference.getLine());
                                        if (reference instanceof AnnotationClassReference)
                                        {
                                            Map<String, AnnotationValue> annotationValues = ((AnnotationClassReference) reference)
                                                        .getAnnotationValues();
                                            addAnnotationValues(event.getGraphContext(), typeReference, annotationValues);
                                        }
                                    }
                                }
                                filesProcessed.incrementAndGet();
                                if (filesProcessed.get() % 250 == 0)
                                {
                                    LOG.info("Analyzed: " + filesProcessed.get() + "/" + allJavaSourceFiles.size());
                                }
                                if (filesProcessed.get() % 10 == 0)
                                {
                                    event.getGraphContext().getGraph().getBaseGraph().commit();
                                }
                            }
                            catch (Throwable t)
                            {
                                LOG.log(Level.SEVERE, "Failed to parse file due to: " + t.getMessage(), t);
                            }
                        }
                    };

                    processor.analyzeFiles(allJavaSourceFiles, callback);

                    LOG.info("Processed " + filesProcessed.get() + " of " + allJavaSourceFiles.size() + " total");
                    LOG.info("Unprocessed files by name: " + unprocessedByName.size());
                    for (String name : unprocessedByName)
                    {
                        LOG.info("Unprocessed: " + name + ", did we process a different file with this the same filename? "
                                    + (processedByName.contains(name) ? "yes" : "no"));
                    }

                    LOG.info("Unprocessed by full file: " + unprocessedByFile.size());
                    for (File file : unprocessedByFile)
                    {
                        LOG.info("Unprocessed: " + file + ", did we process a different file with this the same filename? "
                                    + (processedByName.contains(file.getName()) ? "yes" : "no"));
                    }

                    ExecutionStatistics.get().end("AnalyzeJavaFilesRuleProvider.parseFile");
                }
                catch (Exception e)
                {
                    LOG.log(Level.WARNING, "Could not analyze java files due to: " + e.getMessage(), e);
                }
                finally
                {
                    WindupWildcardImportResolver.setGraphContext(null);
                }

            }
            finally
            {
                ExecutionStatistics.get().end("AnalyzeJavaFilesRuleProvider.analyzeFiles");
            }
        }

        /**
         * Adds parameters contained in the annotation into the annotation type reference
         */
        private void addAnnotationValues(GraphContext context, JavaTypeReferenceModel typeReference, Map<String, AnnotationValue> annotationValues)
        {
            GraphService<JavaAnnotationTypeReferenceModel> annotationTypeReferenceService = new GraphService<>(context,
                        JavaAnnotationTypeReferenceModel.class);

            JavaAnnotationTypeReferenceModel javaAnnotationTypeReferenceModel = annotationTypeReferenceService.addTypeToModel(typeReference);

            Map<String, JavaAnnotationTypeValueModel> valueModels = new HashMap<>();
            for (Map.Entry<String, AnnotationValue> entry : annotationValues.entrySet())
            {
                valueModels.put(entry.getKey(), getValueModelForAnnotationValue(context, entry.getValue()));
            }

            javaAnnotationTypeReferenceModel.setAnnotationValues(valueModels);
        }

        private JavaAnnotationTypeValueModel getValueModelForAnnotationValue(GraphContext context, AnnotationValue value)
        {
            JavaAnnotationTypeValueModel result;

            if (value instanceof AnnotationLiteralValue)
            {
                GraphService<JavaAnnotationLiteralTypeValueModel> literalValueService = new GraphService<>(context,
                            JavaAnnotationLiteralTypeValueModel.class);

                AnnotationLiteralValue literal = (AnnotationLiteralValue) value;
                JavaAnnotationLiteralTypeValueModel literalValueModel = literalValueService.create();
                literalValueModel.setLiteralType(literal.getLiteralType().getSimpleName());
                literalValueModel.setLiteralValue(literal.getLiteralValue() == null ? null : literal.getLiteralValue().toString());

                result = literalValueModel;
            }
            else if (value instanceof AnnotationArrayValue)
            {
                GraphService<JavaAnnotationListTypeValueModel> listValueService = new GraphService<>(context, JavaAnnotationListTypeValueModel.class);

                AnnotationArrayValue arrayValues = (AnnotationArrayValue) value;

                JavaAnnotationListTypeValueModel listModel = listValueService.create();
                for (AnnotationValue arrayValue : arrayValues.getValues())
                {
                    listModel.addItem(getValueModelForAnnotationValue(context, arrayValue));
                }

                result = listModel;
            }
            else if (value instanceof AnnotationClassReference)
            {
                GraphService<JavaAnnotationTypeReferenceModel> annotationTypeReferenceService = new GraphService<>(context,
                            JavaAnnotationTypeReferenceModel.class);

                AnnotationClassReference annotationClassReference = (AnnotationClassReference) value;
                Map<String, JavaAnnotationTypeValueModel> valueModels = new HashMap<>();
                for (Map.Entry<String, AnnotationValue> entry : annotationClassReference.getAnnotationValues().entrySet())
                {
                    valueModels.put(entry.getKey(), getValueModelForAnnotationValue(context, entry.getValue()));
                }
                JavaAnnotationTypeReferenceModel annotationTypeReferenceModel = annotationTypeReferenceService.create();
                annotationTypeReferenceModel.setAnnotationValues(valueModels);

                result = annotationTypeReferenceModel;
            }
            else
            {
                throw new WindupException("Unrecognized AnnotationValue subtype: " + value.getClass().getCanonicalName());
            }
            return result;
        }

        @Override
        public String toString()
        {
            return "ParseJavaSource";
        }
    }
}
