package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by greg on 5/5/16.
 */
public class WorkflowStrategyService extends ChainedProviderService<WorkflowStrategy> implements DescribableService,
        PluggableProviderService<WorkflowStrategy>
{
    private static final String SERVICE_NAME = ServiceNameConstants.WorkflowStrategy;
    private List<ProviderService<WorkflowStrategy>> serviceList;
    private final PluggableProviderService<WorkflowStrategy> pluginService;
    private final Map<String, String> builtinProviderSynonyms = new HashMap<>();

    public String getName() {
        return SERVICE_NAME;
    }

    private WorkflowStrategyService(final Framework framework) {
        this.serviceList = new ArrayList<>();
        /*
         * WorkflowExecutionService chains several other services:
         * 1. builtin providers
         * 2. plugin providers
         */
        HashMap<String, Class<? extends WorkflowStrategy>> builtinProviders =
                new HashMap<String, Class<? extends WorkflowStrategy>>() {{
                    put(SequentialWorkflowStrategy.PROVIDER_NAME, SequentialWorkflowStrategy.class);
                    //backwards compatibility synonym
//                    put("step-first", SequentialWorkflowStrategy.class);
                    put(ParallelWorkflowStrategy.PROVIDER_NAME, ParallelWorkflowStrategy.class);
                }};
        builtinProviderSynonyms.put("step-first", SequentialWorkflowStrategy.PROVIDER_NAME);

        final ProviderService<WorkflowStrategy> primaryService = ServiceFactory.builtinService(
                framework,
                SERVICE_NAME,
                builtinProviders
        );

        pluginService = ServiceFactory.pluginService(SERVICE_NAME, framework, WorkflowStrategy.class);


        serviceList.add(primaryService);
        serviceList.add(pluginService);
    }

    @Override
    protected List<ProviderService<WorkflowStrategy>> getServiceList() {
        return serviceList;
    }

    public static WorkflowStrategyService getInstanceForFramework(Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final WorkflowStrategyService service = new WorkflowStrategyService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (WorkflowStrategyService) framework.getService(SERVICE_NAME);
    }

    /**
     * Get unconfigured strategy instance
     *
     * @param workflow
     *
     * @return
     *
     * @throws ExecutionServiceException
     */
    public WorkflowStrategy getStrategyForWorkflow(final WorkflowExecutionItem workflow)
            throws ExecutionServiceException
    {
        return getStrategyForWorkflow(workflow, null);
    }

    /**
     * Get a configured strategy instance
     *
     * @param workflow workflow
     * @param config   config data
     *
     * @return instance with configuration applied
     *
     * @throws ExecutionServiceException
     */
    public WorkflowStrategy getStrategyForWorkflow(final WorkflowExecutionItem workflow, Map<String, Object> config)
            throws ExecutionServiceException
    {
        String provider = workflow.getWorkflow().getStrategy();
        String s = builtinProviderSynonyms.get(provider);
        if (null != s) {
            provider = s;
        }
        WorkflowStrategy workflowStrategy = providerOfType(provider);
        if (null != config) {
            final PropertyResolver resolver = PropertyResolverFactory.createInstanceResolver(config);

            Description description = DescribableServiceUtil.descriptionForProvider(
                    true,
                    workflowStrategy
            );
            if (description != null) {
                config = PluginAdapterUtility.configureProperties(
                        resolver,
                        description,
                        workflowStrategy,
                        PropertyScope.Instance
                );
            }
        }
        return workflowStrategy;
    }

    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }

    @Override
    public boolean isValidProviderClass(final Class clazz) {
        return pluginService.isValidProviderClass(clazz);
    }

    @Override
    public <X extends WorkflowStrategy> WorkflowStrategy createProviderInstance(final Class<X> clazz, final String name)
            throws PluginException, ProviderCreationException
    {
        return pluginService.createProviderInstance(clazz, name);
    }

    @Override
    public boolean isScriptPluggable() {
        return pluginService.isScriptPluggable();
    }

    @Override
    public WorkflowStrategy createScriptProviderInstance(final ScriptPluginProvider provider) throws PluginException {
        return pluginService.createScriptProviderInstance(provider);
    }
}