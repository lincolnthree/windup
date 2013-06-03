/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *  
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *  
 *  Contributors:
 *      Brad Davis - bradsdavis@gmail.com - Initial API and implementation
*/
package org.jboss.windup.interrogator;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.windup.interrogator.impl.DecoratorPipeline;
import org.jboss.windup.resource.type.archive.ArchiveMeta;
import org.jboss.windup.resource.type.archive.ZipMeta;
import org.jboss.windup.util.RecursiveZipMetaFactory;


/**
 * Takes the Interrogators, and applies them against the ZipMeta. It then collects the results, which
 * are returned as an ArchiveResult.
 * 
 * @author bdavis
 * 
 */
public class ZipInterrogationEngine {
	private static final Log LOG = LogFactory.getLog(ZipInterrogationEngine.class);

	protected RecursiveZipMetaFactory recursiveExtractor;
	protected DecoratorPipeline<ZipMeta> decoratorPipeline;

	public void setDecoratorPipeline(DecoratorPipeline<ZipMeta> decoratorPipeline) {
		this.decoratorPipeline = decoratorPipeline;
	}

	public void setRecursiveExtractor(RecursiveZipMetaFactory recursiveExtractor) {
		this.recursiveExtractor = recursiveExtractor;
	}

	public ZipMeta process(File outputDirectory, File targetArchive) {

		ZipMeta archiveMeta;
		LOG.info("Processing: " + targetArchive.getName());
		try {
			ZipFile zf = new ZipFile(targetArchive);
			archiveMeta = recursiveExtractor.recursivelyExtract(zf);
		}
		catch (Exception e) {
			LOG.error("Error unzipping file.", e);
			return null;
		}

		List<ZipMeta> archiveMetas = new LinkedList<ZipMeta>();
		unfoldRecursion(archiveMeta, archiveMetas);

		int i = 1;
		int j = archiveMetas.size();

		for (ZipMeta archive : archiveMetas) {
			LOG.info("Interrogating (" + i + " of " + j + "): " + archive.getRelativePath());
			File archiveOutputDirectory = new File(outputDirectory + File.separator + archive.getRelativePath());
			archive.setArchiveOutputDirectory(archiveOutputDirectory);

			decoratorPipeline.processMeta(archive);
			i++;
		}

		recursiveExtractor.releaseTempFiles();
		return archiveMeta;
	}

	protected void unfoldRecursion(ZipMeta base, Collection<ZipMeta> archiveMetas) {
		for (ArchiveMeta meta : base.getNestedArchives()) {
			ZipMeta zipMeta = (ZipMeta)meta;
			
			unfoldRecursion(zipMeta, archiveMetas);
		}
		archiveMetas.add(base);
	}

}