package org.jboss.windup.rules.apps.javaee.rules;

import static org.joox.JOOX.$;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.metadata.MetadataBuilder;
import org.jboss.windup.config.phase.InitialAnalysisPhase;
import org.jboss.windup.config.query.Query;
import org.jboss.windup.config.ruleprovider.IteratingRuleProvider;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.resource.FileModel;
import org.jboss.windup.graph.service.GraphService;
import org.jboss.windup.graph.service.Service;
import org.jboss.windup.reporting.model.TechnologyTagLevel;
import org.jboss.windup.reporting.model.TechnologyTagModel;
import org.jboss.windup.reporting.service.ClassificationService;
import org.jboss.windup.reporting.service.TechnologyTagService;
import org.jboss.windup.rules.apps.java.model.AmbiguousJavaClassModel;
import org.jboss.windup.rules.apps.java.model.JavaClassModel;
import org.jboss.windup.rules.apps.java.model.JavaSourceFileModel;
import org.jboss.windup.rules.apps.java.model.PhantomJavaClassModel;
import org.jboss.windup.rules.apps.java.service.JavaClassService;
import org.jboss.windup.rules.apps.javaee.model.EjbDeploymentDescriptorModel;
import org.jboss.windup.rules.apps.javaee.model.EjbEntityBeanModel;
import org.jboss.windup.rules.apps.javaee.model.EjbMessageDrivenModel;
import org.jboss.windup.rules.apps.javaee.model.EjbSessionBeanModel;
import org.jboss.windup.rules.apps.javaee.model.EnvironmentReferenceModel;
import org.jboss.windup.rules.apps.javaee.model.EnvironmentReferenceTagType;
import org.jboss.windup.rules.apps.javaee.model.JmsDestinationModel;
import org.jboss.windup.rules.apps.javaee.service.EnvironmentReferenceService;
import org.jboss.windup.rules.apps.javaee.service.JmsDestinationService;
import org.jboss.windup.rules.apps.xml.model.DoctypeMetaModel;
import org.jboss.windup.rules.apps.xml.model.NamespaceMetaModel;
import org.jboss.windup.rules.apps.xml.model.XmlFileModel;
import org.jboss.windup.rules.apps.xml.service.XmlFileService;
import org.jboss.windup.util.xml.DoctypeUtils;
import org.jboss.windup.util.xml.NamespaceUtils;
import org.ocpsoft.rewrite.config.ConditionBuilder;
import org.ocpsoft.rewrite.context.EvaluationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Discovers ejb-jar.xml files and parses the related metadata
 * 
 * @author <a href="mailto:bradsdavis@gmail.com">Brad Davis</a>
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 * 
 */
public class DiscoverEjbConfigurationXmlRuleProvider extends IteratingRuleProvider<XmlFileModel>
{
    private static final Logger LOG = Logger.getLogger(DiscoverEjbConfigurationXmlRuleProvider.class.getSimpleName());

    private static final String TECH_TAG = "EJB XML";
    private static final TechnologyTagLevel TECH_TAG_LEVEL = TechnologyTagLevel.INFORMATIONAL;

    private static final String dtdRegex = "(?i).*enterprise.javabeans.*";

    public DiscoverEjbConfigurationXmlRuleProvider()
    {
        super(MetadataBuilder.forProvider(DiscoverEjbConfigurationXmlRuleProvider.class)
                    .setPhase(InitialAnalysisPhase.class));
    }

    @Override
    public String toStringPerform()
    {
        return "Discover EJB-JAR XML Files";
    }

    @Override
    public ConditionBuilder when()
    {
        return Query.fromType(XmlFileModel.class).withProperty(XmlFileModel.ROOT_TAG_NAME, "ejb-jar")
                    .withProperty(FileModel.FILE_NAME, "ejb-jar.xml");
    }

    public void perform(GraphRewrite event, EvaluationContext context, XmlFileModel payload)
    {
        Document doc = new XmlFileService(event.getGraphContext()).loadDocumentQuiet(context, payload);
        if (doc == null)
        {
            // failed to parse, skip
            return;
        }

        extractMetadata(event, context, payload, doc);
    }

    private void extractMetadata(GraphRewrite event, EvaluationContext context, XmlFileModel xmlModel, Document doc)
    {
        ClassificationService classificationService = new ClassificationService(event.getGraphContext());
        TechnologyTagService technologyTagService = new TechnologyTagService(event.getGraphContext());
        TechnologyTagModel technologyTag = technologyTagService.addTagToFileModel(xmlModel, TECH_TAG, TECH_TAG_LEVEL);
        classificationService.attachClassification(context, xmlModel, "EJB XML",
                    "Enterprise Java Bean XML Descriptor.");

        // otherwise, it is a EJB-JAR XML.
        if (xmlModel.getDoctype() != null)
        {
            // check doctype.
            if (!processDoctypeMatches(xmlModel.getDoctype()))
            {
                // move to next document.
                return;
            }
            String version = processDoctypeVersion(xmlModel.getDoctype());
            extractMetadata(event, context, xmlModel, doc, version);
        }
        else
        {
            String namespace = $(doc).find("ejb-jar").namespaceURI();
            if (StringUtils.isBlank(namespace))
            {
                namespace = doc.getFirstChild().getNamespaceURI();
            }

            String version = $(doc).attr("version");

            // if the version attribute isn't found, then grab it from the XSD name if we can.
            if (StringUtils.isBlank(version))
            {
                for (NamespaceMetaModel ns : xmlModel.getNamespaces())
                {
                    if (StringUtils.equals(ns.getURI(), namespace))
                    {
                        version = NamespaceUtils.extractVersion(ns.getSchemaLocation());
                    }
                }
            }

            if (StringUtils.isNotBlank(version))
            {
                technologyTag.setVersion(version);
            }

            extractMetadata(event, context, xmlModel, doc, version);
        }
    }

    private void extractMetadata(GraphRewrite event, EvaluationContext context, XmlFileModel xml, Document doc, String versionInformation)
    {
        EjbDeploymentDescriptorModel facet = GraphService.addTypeToModel(event.getGraphContext(), xml, EjbDeploymentDescriptorModel.class);

        if (StringUtils.isNotBlank(versionInformation))
        {
            facet.setSpecificationVersion(versionInformation);
        }

        // process all session beans...
        for (Element element : $(doc).find("session").get())
        {
            processSessionBeanElement(event.getGraphContext(), facet, element);
        }

        // process all message driven beans...
        for (Element element : $(doc).find("message-driven").get())
        {
            processMessageDrivenElement(event.getGraphContext(), facet, element);
        }

        // process all entity beans...
        for (Element element : $(doc).find("entity").get())
        {
            processEntityElement(event.getGraphContext(), facet, element);
        }
    }

    private boolean processDoctypeMatches(DoctypeMetaModel entry)
    {
        if (StringUtils.isNotBlank(entry.getPublicId()))
        {
            if (Pattern.matches(dtdRegex, entry.getPublicId()))
            {
                return true;
            }
        }

        if (StringUtils.isNotBlank(entry.getSystemId()))
        {
            if (Pattern.matches(dtdRegex, entry.getSystemId()))
            {
                return true;
            }

        }
        return false;
    }

    private String processDoctypeVersion(DoctypeMetaModel entry)
    {
        String publicId = entry.getPublicId();
        String systemId = entry.getSystemId();

        // extract the version information from the public / system ID.
        String versionInformation = DoctypeUtils.extractVersion(publicId, systemId);
        return versionInformation;
    }

    private void processSessionBeanElement(GraphContext ctx, EjbDeploymentDescriptorModel ejbConfig, Element element)
    {
        JavaClassService javaClassService = new JavaClassService(ctx);

        JavaClassModel home = null;
        JavaClassModel localHome = null;
        JavaClassModel remote = null;
        JavaClassModel local = null;
        JavaClassModel ejb = null;

        String ejbId = extractAttributeAndTrim(element, "id");
        String displayName = extractChildTagAndTrim(element, "display-name");
        String ejbName = extractChildTagAndTrim(element, "ejb-name");

        // get local class.
        String localClz = extractChildTagAndTrim(element, "local");
        if (localClz != null)
        {
            local = getOrCreatePhantom(javaClassService, localClz);
        }

        // get local home class.
        String localHomeClz = extractChildTagAndTrim(element, "local-home");
        if (localHomeClz != null)
        {
            localHome = getOrCreatePhantom(javaClassService, localHomeClz);
        }

        // get home class.
        String homeClz = extractChildTagAndTrim(element, "home");
        if (homeClz != null)
        {
            home = getOrCreatePhantom(javaClassService, homeClz);
        }

        // get remote class.
        String remoteClz = extractChildTagAndTrim(element, "remote");
        if (remoteClz != null)
        {
            remote = getOrCreatePhantom(javaClassService, remoteClz);
        }

        // get the ejb class.
        String ejbClz = extractChildTagAndTrim(element, "ejb-class");
        if (ejbClz != null)
        {
            ejb = getOrCreatePhantom(javaClassService, ejbClz);
        }

        String sessionType = extractChildTagAndTrim(element, "session-type");
        String transactionType = extractChildTagAndTrim(element, "transaction-type");

        Service<EjbSessionBeanModel> sessionBeanService = new GraphService<>(ctx, EjbSessionBeanModel.class);
        EjbSessionBeanModel sessionBean = sessionBeanService.create();
        sessionBean.setEjbId(ejbId);
        sessionBean.setDisplayName(displayName);
        sessionBean.setBeanName(ejbName);
        sessionBean.setEjbLocal(local);
        sessionBean.setEjbLocalHome(localHome);
        sessionBean.setEjbHome(home);
        sessionBean.setEjbRemote(remote);
        sessionBean.setEjbClass(ejb);
        sessionBean.setSessionType(sessionType);
        sessionBean.setTransactionType(transactionType);

        List<EnvironmentReferenceModel> refs = processEnvironmentReference(ctx, element);
        for (EnvironmentReferenceModel ref : refs)
        {
            sessionBean.addEnvironmentReference(ref);
        }

        ejbConfig.addEjbSessionBean(sessionBean);
    }

    private void processMessageDrivenElement(GraphContext ctx, EjbDeploymentDescriptorModel ejbConfig, Element element)
    {
        JavaClassService javaClassService = new JavaClassService(ctx);
        JavaClassModel ejb = null;

        String ejbId = extractAttributeAndTrim(element, "id");
        String displayName = extractChildTagAndTrim(element, "display-name");
        String ejbName = extractChildTagAndTrim(element, "ejb-name");

        // get the ejb class.
        String ejbClz = extractChildTagAndTrim(element, "ejb-class");
        if (ejbClz != null)
        {
            ejb = getOrCreatePhantom(javaClassService, ejbClz);
        }

        String sessionType = extractChildTagAndTrim(element, "session-type");
        String transactionType = extractChildTagAndTrim(element, "transaction-type");

        String destination = null;
        for (Element activationConfigPropertyElement : $($(element).find("activation-config")).find("activation-config-property").get())
        {
            String propName = extractChildTagAndTrim(activationConfigPropertyElement, "activation-config-property-name");
            String propValue = extractChildTagAndTrim(activationConfigPropertyElement, "activation-config-property-value");
            if ("destination".equals(propName))
            {
                destination = propValue;
            }
        }

        destination = StringUtils.trimToNull(destination);

        Service<EjbMessageDrivenModel> sessionBeanService = new GraphService<>(ctx, EjbMessageDrivenModel.class);
        EjbMessageDrivenModel mdb = sessionBeanService.create();
        mdb.setEjbClass(ejb);
        mdb.setBeanName(ejbName);
        mdb.setDisplayName(displayName);
        mdb.setEjbId(ejbId);
        mdb.setSessionType(sessionType);
        mdb.setTransactionType(transactionType);

        if (StringUtils.isNotBlank(destination))
        {
            JmsDestinationService jmsDestinationService = new JmsDestinationService(ctx);
            JmsDestinationModel jndiRef = jmsDestinationService.createUnique(destination);
            mdb.setDestination(jndiRef);
        }

        List<EnvironmentReferenceModel> refs = processEnvironmentReference(ctx, element);
        for (EnvironmentReferenceModel ref : refs)
        {
            mdb.addEnvironmentReference(ref);
        }

        ejbConfig.addMessageDriven(mdb);
    }

    private void processEntityElement(GraphContext ctx, EjbDeploymentDescriptorModel ejbConfig, Element element)
    {
        JavaClassService javaClassService = new JavaClassService(ctx);
        JavaClassModel localHome = null;
        JavaClassModel local = null;
        JavaClassModel ejb = null;

        String ejbId = extractAttributeAndTrim(element, "id");
        String displayName = extractChildTagAndTrim(element, "display-name");
        String ejbName = extractChildTagAndTrim(element, "ejb-name");
        String tableName = extractChildTagAndTrim(element, "table-name");

        // get local class.
        String localClz = extractChildTagAndTrim(element, "local");
        if (localClz != null)
        {
            local = getOrCreatePhantom(javaClassService, localClz);
        }

        // get local home class.
        String localHomeClz = extractChildTagAndTrim(element, "local-home");
        if (localHomeClz != null)
        {
            localHome = getOrCreatePhantom(javaClassService, localHomeClz);
        }

        // get the ejb class.
        String ejbClz = extractChildTagAndTrim(element, "ejb-class");
        if (ejbClz != null)
        {
            ejb = getOrCreatePhantom(javaClassService, ejbClz);
        }

        String persistenceType = extractChildTagAndTrim(element, "persistence-type");

        // create new entity facet.
        Service<EjbEntityBeanModel> ejbEntityService = new GraphService<>(ctx, EjbEntityBeanModel.class);
        EjbEntityBeanModel entity = ejbEntityService.create();
        entity.setPersistenceType(persistenceType);
        entity.setEjbId(ejbId);
        entity.setDisplayName(displayName);
        entity.setBeanName(ejbName);
        entity.setTableName(tableName);
        entity.setEjbClass(ejb);
        entity.setEjbLocalHome(localHome);
        entity.setEjbLocal(local);

        List<EnvironmentReferenceModel> refs = processEnvironmentReference(ctx, element);
        for (EnvironmentReferenceModel ref : refs)
        {
            entity.addEnvironmentReference(ref);
        }

        ejbConfig.addEjbEntityBean(entity);
    }

    private List<EnvironmentReferenceModel> processEnvironmentReference(GraphContext context, Element element)
    {
        EnvironmentReferenceService environmentReferenceService = new EnvironmentReferenceService(context);
        List<EnvironmentReferenceModel> resources = new LinkedList<EnvironmentReferenceModel>();

        // find Environment Resource references...
        for (Element e : $(element).find("resource-ref").get())
        {
            String id = $(e).attr("id");
            String type = $(e).child("res-type").text();
            String name = $(e).child("res-ref-name").text();

            type = StringUtils.trim(type);
            name = StringUtils.trim(name);

            EnvironmentReferenceModel ref = environmentReferenceService.findEnvironmentReference(name, EnvironmentReferenceTagType.RESOURCE_REF);
            if (ref == null)
            {
                ref = environmentReferenceService.create();
                ref.setName(name);
                ref.setReferenceId(id);
                ref.setReferenceType(type);
                ref.setReferenceTagType(EnvironmentReferenceTagType.RESOURCE_REF);
            }
            LOG.info("Reference: " + name + ", Type: " + type);
            resources.add(ref);
        }

        for (Element e : $(element).find("resource-env-ref").get())
        {
            String id = $(e).attr("id");
            String type = $(e).child("resource-env-ref-type").text();
            String name = $(e).child("resource-env-ref-name").text();

            type = StringUtils.trim(type);
            name = StringUtils.trim(name);

            EnvironmentReferenceModel ref = environmentReferenceService.findEnvironmentReference(name, EnvironmentReferenceTagType.RESOURCE_ENV_REF);
            if (ref == null)
            {
                ref = environmentReferenceService.create();
                ref.setReferenceId(id);
                ref.setName(name);
                ref.setReferenceType(type);
                ref.setReferenceTagType(EnvironmentReferenceTagType.RESOURCE_ENV_REF);
            }
            LOG.info("Reference: " + name + ", Type: " + type + ", Tag: " + ref.getReferenceTagType());
            resources.add(ref);
        }

        for (Element e : $(element).find("message-destination-ref").get())
        {
            String id = $(e).attr("id");
            String type = $(e).child("message-destination-type").text();
            String name = $(e).child("message-destination-ref-name").text();

            type = StringUtils.trim(type);
            name = StringUtils.trim(name);

            EnvironmentReferenceModel ref = environmentReferenceService.findEnvironmentReference(name, EnvironmentReferenceTagType.MSG_DESTINATION_REF);
            if (ref == null)
            {
                ref = environmentReferenceService.create();
                ref.setReferenceId(id);
                ref.setName(name);
                ref.setReferenceType(type);
                ref.setReferenceTagType(EnvironmentReferenceTagType.MSG_DESTINATION_REF);
            }
            LOG.info("Reference: " + name + ", Type: " + type + ", Tag: " + ref.getReferenceTagType());
            resources.add(ref);
        }

        for (Element e : $(element).find("ejb-local-ref").get())
        {
            String id = $(e).attr("id");
            String type = $(e).child("ejb-ref-type").text();
            String name = $(e).child("ejb-ref-name").text();

            type = StringUtils.trim(type);
            name = StringUtils.trim(name);

            EnvironmentReferenceModel ref = environmentReferenceService.findEnvironmentReference(name, EnvironmentReferenceTagType.EJB_LOCAL_REF);
            if (ref == null)
            {
                ref = environmentReferenceService.create();
                ref.setReferenceId(id);
                ref.setName(name);
                ref.setReferenceType(type);
                ref.setReferenceTagType(EnvironmentReferenceTagType.EJB_LOCAL_REF);
            }
            LOG.info("Reference: " + name + ", Type: " + type + ", Tag: " + ref.getReferenceTagType());
            resources.add(ref);
        }

        for (Element e : $(element).find("ejb-ref").get())
        {
            String id = $(e).attr("id");
            String type = $(e).child("ejb-ref-type").text();
            String name = $(e).child("ejb-ref-name").text();

            type = StringUtils.trim(type);
            name = StringUtils.trim(name);

            EnvironmentReferenceModel ref = environmentReferenceService.findEnvironmentReference(name, EnvironmentReferenceTagType.EJB_REF);
            if (ref == null)
            {
                ref = environmentReferenceService.create();
                ref.setReferenceId(id);
                ref.setName(name);
                ref.setReferenceType(type);
                ref.setReferenceTagType(EnvironmentReferenceTagType.EJB_REF);
            }
            LOG.info("Reference: " + name + ", Type: " + type + ", Tag: " + ref.getReferenceTagType());
            resources.add(ref);
        }

        return resources;
    }

    private JavaClassModel getOrCreatePhantom(JavaClassService service, String fqcn)
    {
        JavaClassModel classModel = service.getOrCreatePhantom(fqcn);
        if (classModel instanceof AmbiguousJavaClassModel)
        {
            for (JavaClassModel reference : ((AmbiguousJavaClassModel) classModel).getReferences())
            {
                markAsReportReportable(reference);
            }
        }
        else if (!(classModel instanceof PhantomJavaClassModel))
        {
            markAsReportReportable(classModel);
        }
        return classModel;
    }

    private void markAsReportReportable(JavaClassModel reference)
    {
        JavaSourceFileModel originalSource = reference.getOriginalSource();
        JavaSourceFileModel decompiledSource = reference.getDecompiledSource();
        if (originalSource != null)
            originalSource.setGenerateSourceReport(true);
        if (decompiledSource != null)
            decompiledSource.setGenerateSourceReport(true);
    }

    private String extractAttributeAndTrim(Element element, String property)
    {
        String result = $(element).attr(property);
        return StringUtils.trimToNull(result);
    }

    private String extractChildTagAndTrim(Element element, String property)
    {
        String result = $(element).find(property).first().text();
        return StringUtils.trimToNull(result);
    }
}
