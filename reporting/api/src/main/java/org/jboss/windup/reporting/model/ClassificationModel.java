package org.jboss.windup.reporting.model;

import org.jboss.windup.graph.Indexed;
import org.jboss.windup.graph.model.WindupVertexFrame;
import org.jboss.windup.graph.model.resource.FileModel;
import org.jboss.windup.reporting.model.association.LinkableModel;
import org.ocpsoft.rewrite.config.Rule;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;
import java.util.Set;
import org.jboss.windup.graph.SetInProperties;

/**
 * This classifies files and provides general background information about a specific {@link FileModel}. (For instance,
 * an XML file may be classified as a "XYZ Configuration File".) A {@link ClassificationModel} may also contain links to
 * additional information, or auto-translated/generated/updated versions of the source file.
 */
@TypeValue(ClassificationModel.TYPE)
public interface ClassificationModel extends EffortReportModel, LinkableModel
{
    static final String TYPE = "ClassificationModel";
    static final String TYPE_PREFIX = TYPE + ":";
    static final String RULE_ID = TYPE_PREFIX + "ruleID";
    static final String CLASSIFICATION = TYPE_PREFIX + "classification";
    static final String DESCRIPTION = TYPE_PREFIX + "description";

    static final String FILE_MODEL = TYPE_PREFIX + "classificationModelToFileModel";

    /**
     * Add a {@link FileModel} associated with this {@link ClassificationModel}.
     */
    @Adjacency(label = FILE_MODEL, direction = Direction.OUT)
    void addFileModel(FileModel fileModel);

    /**
     * Get the {@link FileModel} associated with this {@link ClassificationModel}.
     */
    @Adjacency(label = FILE_MODEL, direction = Direction.OUT)
    Iterable<FileModel> getFileModels();

    /**
     * Set text of this {@link ClassificationModel}.
     */
    @Indexed
    @Property(CLASSIFICATION)
    void setClassification(String classification);

    /**
     * Get text of this {@link ClassificationModel}.
     */
    @Property(CLASSIFICATION)
    String getClassification();

    /**
     * Set the description text of this {@link ClassificationModel}.
     */
    @Property(DESCRIPTION)
    void setDescription(String ruleID);

    /**
     * Get the description text of this {@link ClassificationModel}.
     */
    @Property(DESCRIPTION)
    String getDescription();

    /**
     * Set ID of the {@link Rule} that added this {@link ClassificationModel}.
     */
    @Property(RULE_ID)
    void setRuleID(String ruleID);

    /**
     * Get ID of the {@link Rule} that added this {@link ClassificationModel}.
     */
    @Property(RULE_ID)
    String getRuleID();


    /**
     * Add a tag associated with this {@link ClassificationModel}
     */
    @SetInProperties(propertyPrefix = "tag")
    InlineHintModel addTag(String tag);

    /**
     * Set the set of tags associated with this {@link ClassificationModel}
     */
    @SetInProperties(propertyPrefix = "tag")
    InlineHintModel setTags(Set<String> tags);

    /**
     * Get the set of tags associated with this {@link ClassificationModel}
     */
    @SetInProperties(propertyPrefix = "tag")
    Set<String> getTags();

}
