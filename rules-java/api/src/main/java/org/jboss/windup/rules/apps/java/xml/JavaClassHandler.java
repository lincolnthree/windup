package org.jboss.windup.rules.apps.java.xml;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jboss.windup.ast.java.data.TypeReferenceLocation;
import org.jboss.windup.config.exception.ConfigurationException;
import org.jboss.windup.config.parser.ElementHandler;
import org.jboss.windup.config.parser.NamespaceElementHandler;
import org.jboss.windup.config.parser.ParserContext;
import org.jboss.windup.config.parser.xml.RuleProviderHandler;
import org.jboss.windup.rules.apps.java.condition.JavaClass;
import org.jboss.windup.rules.apps.java.condition.JavaClassBuilder;
import org.jboss.windup.rules.apps.java.condition.JavaClassBuilderAt;
import org.jboss.windup.util.exception.WindupException;
import org.ocpsoft.rewrite.config.Condition;
import org.w3c.dom.Element;

/**
 *
 * Represents a {@link JavaClass} {@link Condition}.
 *
 * Example:
 *
 * <pre>
 * &lt;javaclass type="javax.servlet.http.HttpServletRequest"&gt;
 *         &lt;location&gt;METHOD_PARAMETER&lt;/location&gt;
 * &lt;/javaclass&gt;
 * </pre>
 *
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 *
 */
@NamespaceElementHandler(elementName = JavaClassHandler.ELEM_NAME, namespace = RuleProviderHandler.WINDUP_RULE_NAMESPACE)
public class JavaClassHandler implements ElementHandler<JavaClassBuilderAt>
{
    public static final String ELEM_NAME = "javaclass";
    private static final String ATTR_REFERENCES = "references";

    @Override
    public JavaClassBuilderAt processElement(ParserContext handlerManager, Element element)
                throws ConfigurationException
    {
        String type = $(element).attr(ATTR_REFERENCES);
        String as = $(element).attr("as");
        String from = $(element).attr("from");
        String matchesSource = $(element).attr("matchesSource");
        if (StringUtils.isBlank(type))
        {
            throw new WindupException("The '" + ELEM_NAME + "' element must have a non-empty '" + ATTR_REFERENCES + "' attribute");
        }

        List<TypeReferenceLocation> locations = new ArrayList<TypeReferenceLocation>();
        List<Element> children = $(element).children("location").get();
        for (Element child : children)
        {
            TypeReferenceLocation location = handlerManager.processElement(child);
            locations.add(location);
        }
        JavaClassBuilder javaClassReferences;
        if (from != null)
        {
            javaClassReferences = JavaClass.from(from).references(type);
        }
        else
        {
            javaClassReferences = JavaClass.references(type);
        }
        if(matchesSource !=null) {
            javaClassReferences.matchesSource(matchesSource);
        }

        String namePattern = $(element).attr("in");
        if (!StringUtils.isBlank(namePattern))
        {
            javaClassReferences.inType(namePattern);
        }

        JavaClassBuilderAt javaClass = javaClassReferences.at(
                    locations.toArray(new TypeReferenceLocation[locations.size()]));
        if (as != null)
        {
            javaClass.as(as);
        }
        return javaClass;
    }
}
