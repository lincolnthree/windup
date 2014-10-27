package org.jboss.windup.config.parameters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.util.Iterators;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.windup.config.DefaultEvaluationContext;
import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.RuleSubset;
import org.jboss.windup.config.Variables;
import org.jboss.windup.config.WindupRuleProvider;
import org.jboss.windup.config.condition.GraphCondition;
import org.jboss.windup.config.operation.Iteration;
import org.jboss.windup.config.operation.ruleelement.AbstractIterationOperation;
import org.jboss.windup.config.query.Query;
import org.jboss.windup.config.query.QueryBuilderFrom;
import org.jboss.windup.config.query.QueryPropertyComparisonType;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.GraphContextFactory;
import org.jboss.windup.graph.model.WindupVertexFrame;
import org.jboss.windup.graph.service.GraphService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ocpsoft.rewrite.config.ConditionBuilder;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.context.EvaluationContext;
import org.ocpsoft.rewrite.param.DefaultParameterStore;
import org.ocpsoft.rewrite.param.DefaultParameterValueStore;
import org.ocpsoft.rewrite.param.ParameterStore;
import org.ocpsoft.rewrite.param.ParameterValueStore;
import org.ocpsoft.rewrite.param.Parameterized;
import org.ocpsoft.rewrite.param.ParameterizedPatternResult;
import org.ocpsoft.rewrite.param.RegexParameterizedPatternParser;
import org.ocpsoft.rewrite.util.Maps;

@RunWith(Arquillian.class)
public class ParameterWiringTest
{
    @Deployment
    @Dependencies({
                @AddonDependency(name = "org.jboss.windup.config:windup-config"),
                @AddonDependency(name = "org.jboss.forge.furnace.container:cdi")
    })
    public static ForgeArchive getDeployment()
    {
        final ForgeArchive archive = ShrinkWrap
                    .create(ForgeArchive.class)
                    .addClasses(ParameterWiringTestModel.class)
                    .addBeansXML()
                    .addAsAddonDependencies(
                                AddonDependencyEntry.create("org.jboss.windup.config:windup-config"),
                                AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi")
                    );
        return archive;
    }

    @Inject
    private GraphContextFactory factory;

    @Test
    public void testIterationVariableResolving() throws Exception
    {
        final Path folder = OperatingSystemUtils.createTempDir().toPath();
        try (final GraphContext context = factory.create(folder))
        {

            GraphRewrite event = new GraphRewrite(context);
            final DefaultEvaluationContext evaluationContext = new DefaultEvaluationContext();
            final DefaultParameterValueStore values = new DefaultParameterValueStore();
            evaluationContext.put(ParameterValueStore.class, values);

            GraphService<ParameterWiringTestModel> service = new GraphService<>(context, ParameterWiringTestModel.class);

            ParameterWiringTestModel model1 = service.create();
            model1.setValue("The quick brown fox jumped over the lazy dog.");

            ParameterWiringTestModel model2 = service.create();
            model2.setValue("The lazy dog slept under the quick brown fox.");

            ParameterWiringTestModel model3 = service.create();
            model3.setValue("The lazy fox jumped over the quick brown dog.");

            ParameterWiringTestModel model4 = service.create();
            model4.setValue("The lazy fox slept under the quick brown dog.");

            ParameterWiringTestModel model5 = service.create();
            model5.setValue("The quick brown fox jumped over the lazy fox.");

            ParameterWiringTestModel model6 = service.create();
            model6.setValue("The lazy fox slept under the quick brown fox.");

            ParameterWiringTestModel model7 = service.create();
            model7.setValue("The lazy fox slept under the lazy fox.");

            ParameterWiringTestModel model8 = service.create();
            model8.setValue("The quick brown fox slept under the quick brown fox.");

            ParameterWiringTestModel model9 = service.create();
            model9.setValue("The stupid fox slept under the stupid fox.");

            ParameterWiringTestRuleProvider provider = new ParameterWiringTestRuleProvider();
            RuleSubset.create(provider.getConfiguration(context)).perform(event, evaluationContext);

            Assert.assertEquals(2, provider.getMatchCount());
            Assert.assertTrue(provider.getResults().contains(model7));
            Assert.assertTrue(provider.getResults().contains(model8));
        }
    }

    @Test
    public void testIterationVariableResolving2() throws Exception
    {
        final Path folder = OperatingSystemUtils.createTempDir().toPath();
        try (final GraphContext context = factory.create(folder))
        {

            GraphRewrite event = new GraphRewrite(context);
            final DefaultEvaluationContext evaluationContext = new DefaultEvaluationContext();
            final DefaultParameterValueStore values = new DefaultParameterValueStore();
            evaluationContext.put(ParameterValueStore.class, values);

            GraphService<ParameterWiringTestModel> service = new GraphService<>(context, ParameterWiringTestModel.class);

            ParameterWiringTestModel model1 = service.create();
            model1.setValue("The quick brown fox jumped over the lazy dog.");

            ParameterWiringTestModel model2 = service.create();
            model2.setValue("The lazy dog slept under the quick brown fox.");

            ParameterWiringTestModel model3 = service.create();
            model3.setValue("The lazy fox jumped over the quick brown dog.");

            ParameterWiringTestModel model4 = service.create();
            model4.setValue("The lazy fox slept under the quick brown dog.");

            ParameterWiringTestModel model5 = service.create();
            model5.setValue("The quick brown fox jumped over the lazy fox.");

            ParameterWiringTestModel model6 = service.create();
            model6.setValue("The lazy fox slept under the quick brown fox.");

            ParameterWiringTestModel model7 = service.create();
            model7.setValue("The lazy fox slept under the lazy fox.");

            ParameterWiringTestModel model8 = service.create();
            model8.setValue("The quick brown fox slept under the quick brown fox.");

            ParameterWiringTestModel model9 = service.create();
            model9.setValue("The stupid fox slept under the stupid fox.");

            ParameterWiringTestRuleProvider2 provider = new ParameterWiringTestRuleProvider2();
            RuleSubset.create(provider.getConfiguration(context)).perform(event, evaluationContext);

            Assert.assertEquals(3, provider.getMatchCount());
            Assert.assertTrue(provider.getResults().contains(model7));
            Assert.assertTrue(provider.getResults().contains(model8));
            Assert.assertTrue(provider.getResults().contains(model9));
        }
    }

    private static class ParameterWiringTestRuleProvider extends WindupRuleProvider
    {
        private int matchCount;
        private List<ParameterWiringTestModel> results = new ArrayList<>();

        @Override
        public Configuration getConfiguration(GraphContext context)
        {
            return ConfigurationBuilder.begin()
                        .addRule()
                        .when(ParameterWiringTestModelCondition.matchesValue("{*}{adjective} {animal} {verb}{*}")
                                    .as("1")
                                    .and(ParameterWiringTestModelCondition.matchesValue("{*}{adjective} {animal}.")
                                                .from("1").as("result"))
                        )
                        .perform(Iteration.over("result").perform(
                                    new AbstractIterationOperation<ParameterWiringTestModel>()
                                    {
                                        @Override
                                        public void perform(GraphRewrite event, EvaluationContext context,
                                                    ParameterWiringTestModel payload)
                                        {
                                            matchCount++;
                                            results.add(payload);
                                        }
                                    })
                                    .endIteration()
                        )
                        .where("adjective").matches("lazy|quick brown")
                        .where("animal").matches("fox")
                        .where("verb").matches("\\w+");
        }

        public int getMatchCount()
        {
            return matchCount;
        }

        public List<ParameterWiringTestModel> getResults()
        {
            return results;
        }
    }

    private static class ParameterWiringTestRuleProvider2 extends WindupRuleProvider
    {
        private int matchCount;
        private List<ParameterWiringTestModel> results = new ArrayList<>();

        @Override
        public Configuration getConfiguration(GraphContext context)
        {
            return ConfigurationBuilder.begin()
                        .addRule()
                        .when(ParameterWiringTestModelCondition.matchesValue("{*}{adjective} {animal} {verb}{*}")
                                    .as("1")
                                    .and(ParameterWiringTestModelCondition.matchesValue("{*}{adjective} {animal}.")
                                                .from("1").as("result"))
                        )
                        .perform(Iteration.over("result").perform(
                                    new AbstractIterationOperation<ParameterWiringTestModel>()
                                    {
                                        @Override
                                        public void perform(GraphRewrite event, EvaluationContext context,
                                                    ParameterWiringTestModel payload)
                                        {
                                            matchCount++;
                                            results.add(payload);
                                        }
                                    })
                                    .endIteration()
                        )
                        .where("adjective").matches("\\w+")
                        .where("animal").matches("fox")
                        .where("verb").matches("\\w+");
        }

        public int getMatchCount()
        {
            return matchCount;
        }

        public List<ParameterWiringTestModel> getResults()
        {
            return results;
        }
    }

    public static class ParameterWiringTestModelCondition extends GraphCondition implements Parameterized
    {
        private static final String PARAM_VALUE_STORE_LIST = "PVSMAP";
        private RegexParameterizedPatternParser pattern;
        private String varname = Iteration.DEFAULT_VARIABLE_LIST_STRING;
        private String fromVarname;

        public ParameterWiringTestModelCondition(String pattern)
        {
            this.pattern = new RegexParameterizedPatternParser(pattern);
        }

        public static ParameterWiringTestModelCondition matchesValue(String pattern)
        {
            return new ParameterWiringTestModelCondition(pattern);
        }

        public ParameterWiringTestModelCondition from(String varname)
        {
            this.fromVarname = varname;
            return this;
        }

        public ConditionBuilder as(String varname)
        {
            this.varname = varname;
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean evaluate(GraphRewrite event, final EvaluationContext context)
        {
            Map<ParameterValueStore, Map<String, Iterable<WindupVertexFrame>>> cachedStores = (Map<ParameterValueStore, Map<String, Iterable<WindupVertexFrame>>>) context
                        .get(PARAM_VALUE_STORE_LIST);
            final Map<ParameterValueStore, Map<String, Iterable<WindupVertexFrame>>> valueStores = cachedStores == null ? new ConcurrentHashMap<ParameterValueStore, Map<String, Iterable<WindupVertexFrame>>>()
                        : cachedStores;

            ParameterValueStore previousValueStore = DefaultParameterValueStore.getInstance(context);
            try
            {
                if (valueStores.isEmpty())
                {
                    FrameCreationContext frameCreationContext = new FrameCreationContext()
                    {
                        private ParameterValueStore current;
                        private DefaultParameterValueStore original = (DefaultParameterValueStore) DefaultParameterValueStore
                                    .getInstance(context);

                        @Override
                        public void beginNew(Map<String, Iterable<WindupVertexFrame>> variables)
                        {
                            ParameterValueStore clone = clone(original);
                            this.current = clone;
                            context.put(ParameterValueStore.class, clone);

                            if (variables == null)
                                variables = new LinkedHashMap<String, Iterable<WindupVertexFrame>>();
                            valueStores.put(clone, variables);
                        }

                        private ParameterValueStore clone(DefaultParameterValueStore instance)
                        {
                            DefaultParameterValueStore clone = new DefaultParameterValueStore(instance);
                            return clone;
                        }

                        @Override
                        public void rollback()
                        {
                            if (current != null)
                                valueStores.remove(current);
                        }
                    };

                    return evaluateAndPopulateValueStores(event, context, frameCreationContext);
                }
                else
                {
                    Set<WindupVertexFrame> resultSet = new LinkedHashSet<>();
                    boolean result = false;

                    DefaultParameterValueStore original = (DefaultParameterValueStore) DefaultParameterValueStore
                                .getInstance(context);
                    for (Entry<ParameterValueStore, Map<String, Iterable<WindupVertexFrame>>> entry : valueStores
                                .entrySet())
                    {
                        ParameterValueStore valueStore = entry.getKey();
                        Map<String, Iterable<WindupVertexFrame>> variables = entry.getValue();
                        try
                        {
                            final AtomicBoolean rejected = new AtomicBoolean(false);
                            FrameContext frame = new FrameContext()
                            {
                                @Override
                                public void reject()
                                {
                                    rejected.set(true);
                                }
                            };

                            try
                            {
                                context.put(ParameterValueStore.class, valueStore);

                                /*
                                 * Each ValueStore must correspond with the variables map which which it was created.
                                 */
                                Variables.instance(event).push(variables);
                                if (evaluateWithValueStore(event, context, frame))
                                    result = true;

                                if (rejected.get())
                                    valueStores.remove(valueStore);
                            }
                            finally
                            {
                                context.put(ParameterValueStore.class, original);
                            }
                        }
                        finally
                        {
                            Iterable<WindupVertexFrame> variable = Variables.instance(event).findVariable(varname);
                            if (variable != null)
                            {
                                resultSet.addAll(Iterators.asSet(variable));
                            }
                            Variables.instance(event).pop();
                        }
                    }
                    Variables.instance(event).setVariable(varname, resultSet);
                    return result;
                }
            }
            finally
            {
                context.put(ParameterValueStore.class, previousValueStore);
                context.put(PARAM_VALUE_STORE_LIST, valueStores);
            }
        }

        private boolean evaluateAndPopulateValueStores(GraphRewrite event, EvaluationContext context,
                    FrameCreationContext frameCreationContext)
        {
            ParameterStore store = DefaultParameterStore.getInstance(context);

            QueryBuilderFrom query = Query.find(ParameterWiringTestModel.class);
            if (!StringUtils.isBlank(fromVarname))
            {
                query = Query.from(fromVarname);
            }

            Pattern compiledPattern = pattern.getCompiledPattern(store);
            query.withProperty(ParameterWiringTestModel.VALUE, QueryPropertyComparisonType.REGEX,
                        compiledPattern.pattern());

            String uuid = UUID.randomUUID().toString();
            query.as(uuid);
            if (query.evaluate(event, context))
            {
                Iterable<WindupVertexFrame> frames = Variables.instance(event).findVariable(uuid);
                for (WindupVertexFrame frame : frames)
                {
                    ParameterWiringTestModel model = (ParameterWiringTestModel) frame;

                    ParameterizedPatternResult parseResult = pattern.parse(model.getValue());
                    if (parseResult.matches())
                    {
                        Map<String, List<WindupVertexFrame>> variables = new LinkedHashMap<String, List<WindupVertexFrame>>();
                        frameCreationContext.beginNew((Map) variables);
                        if (parseResult.submit(event, context))
                        {
                            Maps.addListValue(variables, varname, model);
                        }
                        else
                        {
                            System.out.println("nope: " + model);
                            frameCreationContext.rollback();
                        }
                    }
                    else
                    {
                        System.out.println("nope: " + model);
                    }
                }
                Variables.instance(event).removeVariable(uuid);
                return true;
            }

            return false;
        }

        private boolean evaluateWithValueStore(GraphRewrite event, EvaluationContext context, FrameContext frameContext)
        {
            ParameterStore store = DefaultParameterStore.getInstance(context);

            QueryBuilderFrom query = Query.find(ParameterWiringTestModel.class);
            if (!StringUtils.isBlank(fromVarname))
            {
                query = Query.from(fromVarname);
            }

            Pattern compiledPattern = pattern.getCompiledPattern(store);
            query.withProperty(ParameterWiringTestModel.VALUE, QueryPropertyComparisonType.REGEX,
                        compiledPattern.pattern());

            String uuid = UUID.randomUUID().toString();
            query.as(uuid);
            if (query.evaluate(event, context))
            {
                boolean result = false;
                List<WindupVertexFrame> results = new ArrayList<>();
                Iterable<WindupVertexFrame> frames = Variables.instance(event).findVariable(uuid);
                for (WindupVertexFrame frame : frames)
                {
                    ParameterWiringTestModel model = (ParameterWiringTestModel) frame;

                    String value = model.getValue();
                    ParameterizedPatternResult parseResult = pattern.parse(value);
                    if (parseResult.submit(event, context))
                    {
                        result = true;
                        results.add(model);
                    }
                }

                Variables.instance(event).removeVariable(uuid);
                if (result)
                {
                    Variables.instance(event).setVariable(varname, results);
                    return true;
                }
            }

            frameContext.reject();
            return false;
        }

        @Override
        public Set<String> getRequiredParameterNames()
        {
            return pattern.getRequiredParameterNames();
        }

        @Override
        public void setParameterStore(ParameterStore store)
        {
            pattern.setParameterStore(store);
        }

    }

    public interface FrameContext
    {
        /**
         * Reject the current frame and remove it from future processing.
         */
        void reject();
    }

    public interface FrameCreationContext
    {
        /**
         * Add a new {@link ParameterValueStore} frame for future processing.
         */
        void beginNew(Map<String, Iterable<WindupVertexFrame>> variables);

        /**
         * Remove the previously added {@link ParameterValueStore} frame.
         */
        void rollback();
    }

}
