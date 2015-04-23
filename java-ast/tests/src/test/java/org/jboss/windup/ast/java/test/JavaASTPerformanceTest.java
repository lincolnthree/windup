package org.jboss.windup.ast.java.test;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.furnace.util.Predicate;
import org.jboss.forge.furnace.util.Visitor;
import org.jboss.windup.ast.java.BatchASTProcessor;
import org.jboss.windup.ast.java.data.ClassReference;
import org.jboss.windup.util.file.FileSuffixPredicate;
import org.jboss.windup.util.file.FileVisit;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class JavaASTPerformanceTest extends AbstractJavaASTTest
{
    @Test
    public void testProcessAST() throws Exception
    {
        String sourcePath = "/Users/lb3/Desktop/decompiled_35_mb_app";
        File rootDirectory = new File(sourcePath);
        final Set<String> libraryPaths = new HashSet<>();
        FileVisit.visit(rootDirectory, new FileSuffixPredicate(".jar"), new Visitor<File>()
        {
            @Override
            public void visit(File instance)
            {
                if (instance.isFile())
                {
                    libraryPaths.add(instance.getAbsolutePath());
                }
            }
        });

        final Set<String> sourcePaths = new HashSet<>();
        for (File file : rootDirectory.listFiles())
        {
            sourcePaths.add(file.getAbsolutePath());
        }

        final Set<File> sourceFiles = new LinkedHashSet<>();
        long beginTime = System.currentTimeMillis();
        FileVisit.visit(rootDirectory, new FileSuffixPredicate("\\.java"), new Visitor<File>()
        {
            @Override
            public void visit(File file)
            {
                sourceFiles.add(file);
            }
        });

        final AtomicInteger units = new AtomicInteger();
        final AtomicInteger references = new AtomicInteger();

        BatchASTProcessor.analyzeJavaFiles(libraryPaths, sourcePaths, sourceFiles,
                    new Predicate<CompilationUnit>()
                    {
                        @Override
                        public boolean accept(CompilationUnit type)
                        {
                            units.incrementAndGet();
                            return true;
                        }
                    },
                    new Predicate<ClassReference>()
                    {
                        @Override
                        public boolean accept(ClassReference type)
                        {
                            references.incrementAndGet();
                            return true;
                        }
                    });

        long endTime = System.currentTimeMillis();
        System.out.println("Processed: " + units.get() + "/" + sourceFiles.size() + " files in " + ((endTime - beginTime) / 1000)
                    + " seconds! Found "
                    + references.get() + " references.");
    }

}
