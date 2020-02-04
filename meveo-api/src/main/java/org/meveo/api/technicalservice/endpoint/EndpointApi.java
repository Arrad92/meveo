/*
 * (C) Copyright 2018-2019 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. This program is
 * not suitable for any direct or indirect application in MILITARY industry See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.api.technicalservice.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.UserNotAuthorizedException;
import org.meveo.api.BaseCrudApi;
import org.meveo.api.dto.technicalservice.endpoint.EndpointDto;
import org.meveo.api.dto.technicalservice.endpoint.TSParameterMappingDto;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.observers.OntologyObserver;
import org.meveo.api.rest.swagger.SwaggerHelper;
import org.meveo.api.rest.technicalservice.EndpointExecution;
import org.meveo.api.rest.technicalservice.EndpointScript;
import org.meveo.api.utils.JSONata;
import org.meveo.commons.utils.StringUtils;
import org.meveo.keycloak.client.KeycloakAdminClientConfig;
import org.meveo.keycloak.client.KeycloakAdminClientService;
import org.meveo.keycloak.client.KeycloakUtils;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.persistence.JacksonUtil;
import org.meveo.model.scripts.CustomScript;
import org.meveo.model.scripts.Function;
import org.meveo.model.scripts.Sample;
import org.meveo.model.technicalservice.endpoint.Endpoint;
import org.meveo.model.technicalservice.endpoint.EndpointHttpMethod;
import org.meveo.model.technicalservice.endpoint.EndpointParameter;
import org.meveo.model.technicalservice.endpoint.EndpointPathParameter;
import org.meveo.model.technicalservice.endpoint.EndpointVariables;
import org.meveo.model.technicalservice.endpoint.TSParameterMapping;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.script.ConcreteFunctionService;
import org.meveo.service.script.FunctionService;
import org.meveo.service.script.ScriptInterface;
import org.meveo.service.script.ScriptUtils;
import org.meveo.service.technicalservice.endpoint.ESGenerator;
import org.meveo.service.technicalservice.endpoint.EndpointResult;
import org.meveo.service.technicalservice.endpoint.EndpointService;
import org.meveo.service.technicalservice.endpoint.PendingResult;
import org.meveo.util.ClassUtils;
import org.meveo.util.Version;

import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.util.Json;

/**
 * API for managing technical services endpoints
 *
 * @author clement.bareth
 * @author Edward P. Legaspi | <czetsuya@gmail.com>
 * @since 01.02.2019
 * @version 6.5.0
 */
@Stateless
public class EndpointApi extends BaseCrudApi<Endpoint, EndpointDto> {

	@EJB
	private EndpointService endpointService;

	@Inject
	private ConcreteFunctionService concreteFunctionService;
	
	@Inject
	private CustomEntityTemplateService customEntityTemplateService;
	
	@Inject
	private OntologyObserver ontologyObserver;
	
	@Inject
	private CustomFieldTemplateService customFieldTemplateService;

	@EJB
	private KeycloakAdminClientService keycloakAdminClientService;

	public EndpointApi() {
		super(Endpoint.class, EndpointDto.class);
	}

	public EndpointApi(EndpointService endpointService, ConcreteFunctionService concreteFunctionService) {
		super(Endpoint.class, EndpointDto.class);
		this.endpointService = endpointService;
		this.concreteFunctionService = concreteFunctionService;
	}

	public String getEndpointScript(String code) throws EntityDoesNotExistsException {
		Endpoint endpoint = endpointService.findByCode(code);
		if (endpoint == null) {
			throw new EntityDoesNotExistsException(Endpoint.class, code);
		}
		
		if(!isUserAuthorized(endpoint)) {
			throw new UserNotAuthorizedException();
		}

		return ESGenerator.generateFile(endpoint);
	}
	
	public PendingResult executeAsync(Endpoint endpoint, EndpointExecution endpointExecution) throws BusinessException, ExecutionException, InterruptedException {
		Function service = endpoint.getService();
		final FunctionService<?, ScriptInterface> functionService = concreteFunctionService.getFunctionService(service.getCode());
		Map<String, Object> parameterMap = new HashMap<>(endpointExecution.getParameters());
		final ScriptInterface executionEngine = getEngine(endpoint, endpointExecution, service, functionService, parameterMap);
		
        CompletableFuture<EndpointResult> future = CompletableFuture.supplyAsync(() -> {
            try {
        		Map<String, Object> result = execute(endpointExecution, functionService, parameterMap, executionEngine);
                String data = transformData(endpoint, result);
                return new EndpointResult(data, endpoint.getContentType());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
		PendingResult pendingResult = new PendingResult();
		pendingResult.setEngine(executionEngine);
		pendingResult.setResult(future);
		
		return pendingResult;
	}
	
	/**
	 * Execute the technical service associated to the endpoint
	 *
	 * @param endpoint  Endpoint to execute
	 * @param execution Parameters of the execution
	 * @return The result of the execution
	 * @throws BusinessException if error occurs while execution
	 */
	@SuppressWarnings("rawtypes")
	public Map<String, Object> execute(Endpoint endpoint, EndpointExecution execution) throws BusinessException, ExecutionException, InterruptedException {

		Function service = endpoint.getService();
		final FunctionService<?, ScriptInterface> functionService = concreteFunctionService.getFunctionService(service.getCode());
		Map<String, Object> parameterMap = new HashMap<>(execution.getParameters());

		final ScriptInterface executionEngine = getEngine(endpoint, execution, service, functionService, parameterMap);

		return execute(execution, functionService, parameterMap, executionEngine);

	}

	/**
	 * @param execution
	 * @param functionService
	 * @param parameterMap
	 * @param executionEngine
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws BusinessException
	 */
	public Map<String, Object> execute(EndpointExecution execution, final FunctionService<?, ScriptInterface> functionService, Map<String, Object> parameterMap, final ScriptInterface executionEngine) throws InterruptedException, ExecutionException, BusinessException {
		// Start endpoint script with timeout if one was set
		if (execution.getDelayValue() != null) {
			try {
				final CompletableFuture<Map<String, Object>> resultFuture = CompletableFuture.supplyAsync(() -> {
					try {
						return functionService.execute(executionEngine, parameterMap);
					} catch (BusinessException e) {
						throw new RuntimeException(e);
					}
				});
				return resultFuture.get(execution.getDelayValue(), execution.getDelayUnit());
			} catch (TimeoutException e) {
				return executionEngine.cancel();
			}
		} else {
			return functionService.execute(executionEngine, parameterMap);
		}
	}

	/**
	 * @param endpoint
	 * @param execution
	 * @param service
	 * @param functionService
	 * @param parameterMap
	 * @return
	 * @throws IllegalArgumentException
	 */
	public ScriptInterface getEngine(Endpoint endpoint, EndpointExecution execution, Function service, final FunctionService<?, ScriptInterface> functionService, Map<String, Object> parameterMap) throws IllegalArgumentException {
		List<String> pathParameters = new ArrayList<>(Arrays.asList(execution.getPathInfo()).subList(2, execution.getPathInfo().length));

		// Set budget variables
		parameterMap.put(EndpointVariables.MAX_BUDGET, execution.getBugetMax());
		parameterMap.put(EndpointVariables.BUDGET_UNIT, execution.getBudgetUnit());

		// Assign path parameters
		for (EndpointPathParameter pathParameter : endpoint.getPathParameters()) {
			parameterMap.put(pathParameter.toString(), pathParameters.get(pathParameter.getPosition()));
		}

		// Assign query or post parameters
		for (TSParameterMapping tsParameterMapping : endpoint.getParametersMapping()) {
			Object parameterValue = execution.getParameters().get(tsParameterMapping.getParameterName());

			// Use default value if parameter not provided
			if (parameterValue == null) {
				parameterValue = tsParameterMapping.getDefaultValue();
			} else {
				// Handle cases where parameter is multivalued
				if (parameterValue instanceof String[]) {
					String[] arrValue = (String[]) parameterValue;
					if (tsParameterMapping.isMultivalued()) {
						parameterValue = new ArrayList<>(Arrays.asList(arrValue));
					} else if (arrValue.length == 1) {
						parameterValue = arrValue[0];
					} else {
						throw new IllegalArgumentException("Parameter " + tsParameterMapping.getParameterName() + "should not be multivalued");
					}
				} else if (parameterValue instanceof Collection) {
					Collection colValue = (Collection) parameterValue;
					if (!tsParameterMapping.isMultivalued() && colValue.size() == 1) {
						parameterValue = colValue.iterator().next();
					} else if (!tsParameterMapping.isMultivalued()) {
						throw new IllegalArgumentException("Parameter " + tsParameterMapping.getParameterName() + "should not be multivalued");
					}
				}
			}
			String paramName = tsParameterMapping.getEndpointParameter().toString();
			parameterMap.remove(tsParameterMapping.getParameterName());
			parameterMap.put(paramName, parameterValue);
		}


		final ScriptInterface executionEngine = functionService.getExecutionEngine(service.getCode(), parameterMap);

		if (executionEngine instanceof EndpointScript) {
			// Explicitly pass the request and response information to the script
			((EndpointScript) executionEngine).setEndpointRequest(execution.getRequest());
			if (endpoint.isSynchronous()) {
				((EndpointScript) executionEngine).setEndpointResponse(execution.getResponse());
			}
		} else {
			// Implicitly pass the request and response information to the script
			parameterMap.put("request", execution.getRequest());
			if (endpoint.isSynchronous()) {
				parameterMap.put("response", execution.getResponse());
			}
		}
		return executionEngine;
	}

	/**
	 * Create an endpoint
	 *
	 * @param endpointDto Configuration of the endpoint
	 * @return the created Endpoint
	 */
	public Endpoint create(EndpointDto endpointDto) throws BusinessException {
		validateCompositeRoles(endpointDto);
		Endpoint endpoint = fromDto(endpointDto);
		endpointService.create(endpoint);
		return endpoint;
	}

	/**
	 * Create or update an endpoint
	 *
	 * @param endpointDto Configuration of the endpoint
	 * @return null if endpoint was updated, the endpoint if it was created
	 */
	public Endpoint createOrReplace(EndpointDto endpointDto) throws BusinessException {
		Endpoint endpoint = endpointService.findByCode(endpointDto.getCode());
		if (endpoint != null) {
			update(endpoint, endpointDto);
			return endpoint;
		} else {
			return create(endpointDto);
		}
	}

	/**
	 * Remove the endpoint with the given code
	 *
	 * @param code Code of the endpoint to remove
	 * @throws BusinessException if endpoint does not exists
	 */
	public void delete(String code) throws BusinessException, EntityDoesNotExistsException {

		// Retrieve existing entity
		Endpoint endpoint = endpointService.findByCode(code);
		if (endpoint == null) {
			throw new EntityDoesNotExistsException("Endpoint with code " + code + " does not exists");
		}

		endpointService.remove(endpoint);

	}

	/**
	 * Retrieve an endpoint by its code
	 *
	 * @param code Code of the endpoint to retrieve
	 * @return DTO of the endpoint corresponding to the given code
	 */
	public EndpointDto findByCode(String code) {
		return toDto(endpointService.findByCode(code));
	}

	/**
	 * Retrieve endpoints by service code
	 *
	 * @param code Code of the service used to filter endpoints
	 * @return DTOs of the endpoints that are associated to the given technical
	 *         service
	 */
	public List<EndpointDto> findByServiceCode(String code) {
		return endpointService.findByServiceCode(code).stream().map(this::toDto).collect(Collectors.toList());
	}

	/**
	 * Retrieve all endpoints
	 *
	 * @return DTOs for all stored endpoints
	 */
	public List<EndpointDto> list() {
		return endpointService.list().stream().map(this::toDto).collect(Collectors.toList());
	}

	private void update(Endpoint endpoint, EndpointDto endpointDto) throws BusinessException {
		validateCompositeRoles(endpointDto);
		Endpoint updatedEndpoint = new Endpoint();
		updatedEndpoint.setId(endpoint.getId());
		updatedEndpoint.setCode(endpointDto.getCode());
		updatedEndpoint.setDescription(endpointDto.getDescription());

		updatedEndpoint.setSynchronous(endpointDto.isSynchronous());
		updatedEndpoint.setMethod(endpointDto.getMethod());

		final List<EndpointPathParameter> endpointPathParameters = getEndpointPathParameters(endpointDto, endpoint);
		updatedEndpoint.setPathParameters(endpointPathParameters);

		final List<TSParameterMapping> parameterMappings = getParameterMappings(endpointDto, endpoint);
		updatedEndpoint.setParametersMapping(parameterMappings);

		updatedEndpoint.setReturnedVariableName(endpointDto.getReturnedVariableName());
		updatedEndpoint.setJsonataTransformer(endpointDto.getJsonataTransformer());
		updatedEndpoint.setSerializeResult(endpointDto.isSerializeResult());
		updatedEndpoint.setContentType(endpointDto.getContentType());
		updatedEndpoint.setRoles(endpointDto.getRoles());

		endpointService.update(updatedEndpoint);
	}

	@Override
	public EndpointDto toDto(Endpoint endpoint) {
		EndpointDto endpointDto = new EndpointDto();
		endpointDto.setCode(endpoint.getCode());
		endpointDto.setDescription(endpoint.getDescription());
		endpointDto.setMethod(endpoint.getMethod());
		endpointDto.setServiceCode(endpoint.getService().getCode());
		endpointDto.setSynchronous(endpoint.isSynchronous());
		endpointDto.setReturnedVariableName(endpoint.getReturnedVariableName());
		endpointDto.setSerializeResult(endpoint.isSerializeResult());
		List<String> pathParameterDtos = new ArrayList<>();
		endpointDto.setPathParameters(pathParameterDtos);
		for (EndpointPathParameter pathParameter : endpoint.getPathParameters()) {
			pathParameterDtos.add(pathParameter.getEndpointParameter().getParameter());
		}
		List<TSParameterMappingDto> mappingDtos = new ArrayList<>();
		endpointDto.setParameterMappings(mappingDtos);
		for (TSParameterMapping tsParameterMapping : endpoint.getParametersMapping()) {
			TSParameterMappingDto mappingDto = new TSParameterMappingDto();
			mappingDto.setDefaultValue(tsParameterMapping.getDefaultValue());
			mappingDto.setParameterName(tsParameterMapping.getParameterName());
			mappingDto.setServiceParameter(tsParameterMapping.getEndpointParameter().getParameter());
			mappingDtos.add(mappingDto);
		}
		endpointDto.setJsonataTransformer(endpoint.getJsonataTransformer());
		endpointDto.setContentType(endpoint.getContentType());
		endpointDto.setRoles(endpoint.getRoles());
		return endpointDto;
	}

	@Override
	public Endpoint fromDto(EndpointDto endpointDto) {

		Endpoint endpoint = new Endpoint();

		// Code
		endpoint.setCode(endpointDto.getCode());

		// Description
		endpoint.setDescription(endpointDto.getDescription());

		// Method
		endpoint.setMethod(endpointDto.getMethod());

		// Synchronous
		endpoint.setSynchronous(endpointDto.isSynchronous());

		// JSONata query
		endpoint.setJsonataTransformer(endpointDto.getJsonataTransformer());

		// Returned variable name
		endpoint.setReturnedVariableName(endpointDto.getReturnedVariableName());

		// Technical Service
		final FunctionService<?, ScriptInterface> functionService = concreteFunctionService.getFunctionService(endpointDto.getServiceCode());
		Function service = functionService.findByCode(endpointDto.getServiceCode());
		endpoint.setService(service);

		// Parameters mappings
		List<TSParameterMapping> tsParameterMappings = getParameterMappings(endpointDto, endpoint);
		endpoint.setParametersMapping(tsParameterMappings);

		// Path parameters
		List<EndpointPathParameter> endpointPathParameters = getEndpointPathParameters(endpointDto, endpoint);
		endpoint.setPathParameters(endpointPathParameters);

		endpoint.setSerializeResult(endpointDto.isSerializeResult());

		endpoint.setContentType(endpointDto.getContentType());

		endpoint.setRoles(endpointDto.getRoles());

		return endpoint;
	}

	private List<EndpointPathParameter> getEndpointPathParameters(EndpointDto endpointDto, Endpoint endpoint) {
		List<EndpointPathParameter> endpointPathParameters = new ArrayList<>();
		for (String pathParameter : endpointDto.getPathParameters()) {
			EndpointPathParameter endpointPathParameter = new EndpointPathParameter();
			EndpointParameter endpointParameter = buildEndpointParameter(endpoint, pathParameter);
			endpointPathParameter.setEndpointParameter(endpointParameter);
			endpointPathParameters.add(endpointPathParameter);
		}
		return endpointPathParameters;
	}

	private List<TSParameterMapping> getParameterMappings(EndpointDto endpointDto, Endpoint endpoint) {
		List<TSParameterMapping> tsParameterMappings = new ArrayList<>();
		for (TSParameterMappingDto parameterMappingDto : endpointDto.getParameterMappings()) {
			TSParameterMapping tsParameterMapping = new TSParameterMapping();
			tsParameterMapping.setDefaultValue(parameterMappingDto.getDefaultValue());
			tsParameterMapping.setParameterName(parameterMappingDto.getParameterName());
			EndpointParameter endpointParameter = buildEndpointParameter(endpoint, parameterMappingDto.getServiceParameter());
			tsParameterMapping.setEndpointParameter(endpointParameter);
			tsParameterMappings.add(tsParameterMapping);
		}
		return tsParameterMappings;
	}

	private EndpointParameter buildEndpointParameter(Endpoint endpoint, String param) {
		EndpointParameter endpointParameter = new EndpointParameter();
		endpointParameter.setEndpoint(endpoint);
		endpointParameter.setParameter(param);
		return endpointParameter;
	}

	public List<String> validateCompositeRoles(EndpointDto endpointDto) throws IllegalArgumentException {
		KeycloakAdminClientConfig keycloakAdminClientConfig = KeycloakUtils.loadConfig();
		List<String> roles = keycloakAdminClientService.getCompositeRolesByRealmClientId(keycloakAdminClientConfig.getClientId(), keycloakAdminClientConfig.getRealm());
		if (CollectionUtils.isNotEmpty(roles)) {
			for (String selectedRole : endpointDto.getRoles()) {
				if (!roles.contains(selectedRole)) {
					throw new IllegalArgumentException("The role does not exists");
				}
			}
		}
		return roles;
	}

	public boolean isUserAuthorized(Endpoint endpoint) {
		try {
			Set<String> currentUserRoles = keycloakAdminClientService.getCurrentUserRoles(EndpointService.ENDPOINTS_CLIENT);
			if (!currentUserRoles.contains(EndpointService.getEndpointPermission(endpoint))) {
				// If does not directly contained, for each role of meveo-web, check the role
				// mappings for endpoints
				KeycloakAdminClientConfig keycloakConfig = KeycloakUtils.loadConfig();
				currentUserRoles = keycloakAdminClientService.getCurrentUserRoles(keycloakConfig.getClientId());
				for (String userRole : currentUserRoles) {
					if (endpoint.getRoles().contains(userRole)) {
						return true;
					}
				}

				return false;
			}

			return true;

		} catch (Exception e) {
			log.info("User not authorized to access endpoint due to error : {}", e.getMessage());
			return false;
		}
	}

	@Override
	public EndpointDto find(String code) throws MeveoApiException, org.meveo.exceptions.EntityDoesNotExistsException {
		return findByCode(code);
	}

	@Override
	public Endpoint createOrUpdate(EndpointDto dtoData) throws MeveoApiException, BusinessException {
		return createOrReplace(dtoData);
	}

	@Override
	public IPersistenceService<Endpoint> getPersistenceService() {
		return endpointService;
	}

	@Override
	public boolean exists(EndpointDto dto) {
		return findByCode(dto.getCode()) != null;
	}

	public Response generateOpenApiJson(@NotNull String baseUrl, @NotNull String code) {

		Endpoint endpoint = endpointService.findByCode(code);
		if (endpoint == null) {
			return Response.noContent().build();
		}
		
		if(!isUserAuthorized(endpoint)) {
			return Response.status(403).entity("You are not authorized to access this endpoint").build();
		}

		Info info = new Info();
		info.setTitle(endpoint.getCode());
		info.setDescription(endpoint.getDescription());
		info.setVersion(Version.appVersion);

		Map<String, Path> paths = new HashMap<>();
		Path path = new Path();

		Operation operation = new Operation();

		if (endpoint.getMethod().equals(EndpointHttpMethod.GET)) {
			path.setGet(operation);

		} else if (endpoint.getMethod().equals(EndpointHttpMethod.POST)) {
			path.setPost(operation);
		}

		if (!Objects.isNull(endpoint.getPathParameters())) {
			for (EndpointPathParameter endpointPathParameter : endpoint.getPathParameters()) {
				Parameter parameter = new PathParameter();
				parameter.setName(endpointPathParameter.getEndpointParameter().getParameter());
				path.addParameter(parameter);
			}
		}

		paths.put(endpoint.getEndpointUrl(), path);

		List<Sample> samples = endpoint.getService().getSamples();

		if (!Objects.isNull(endpoint.getParametersMapping())) {
			List<Parameter> operationParameter = new ArrayList<>();

			for (TSParameterMapping tsParameterMapping : endpoint.getParametersMapping()) {

				if (endpoint.getMethod().equals(EndpointHttpMethod.GET)) {
					QueryParameter queryParameter = new QueryParameter();
					queryParameter.setName(tsParameterMapping.getParameterName());
					operationParameter.add(queryParameter);

					if(samples != null && !samples.isEmpty()) {
						Object inputExample = samples.get(0).getInputs().get(tsParameterMapping.getParameterName());
						queryParameter.setExample(String.valueOf(inputExample));
					}

				} else if (endpoint.getMethod().equals(EndpointHttpMethod.POST)) {
					BodyParameter bodyParameter = new BodyParameter();
					bodyParameter.setName(tsParameterMapping.getParameterName());
					
					if (tsParameterMapping.getEndpointParameter() != null) {
						String serviceParameter = tsParameterMapping.getEndpointParameter().getParameter();
						CustomEntityTemplate cet = customEntityTemplateService.findByCodeOrDbTablename(serviceParameter);

						if (cet != null) {
							bodyParameter.setSchema(cetToModel(cet));
						}
					}
					
					operationParameter.add(bodyParameter);

					if(samples != null && !samples.isEmpty()) {
						Object inputExample = samples.get(0).getInputs().get(tsParameterMapping.getParameterName());
						String mediaType = endpoint.getContentType() != null ? endpoint.getContentType() : "application/json";
						String inputExampleSerialized = inputExample.getClass().isPrimitive() ? String.valueOf(inputExample) : JacksonUtil.toString(inputExample);
						bodyParameter.addExample(mediaType, inputExampleSerialized);
					}
				}
			}

			operation.setParameters(operationParameter);
		}

		Map<String, io.swagger.models.Response> responses = new HashMap<>();
		io.swagger.models.Response response = new io.swagger.models.Response();

		if(samples != null && !samples.isEmpty()) {
			Object outputExample = samples.get(0).getOutputs();
			String mediaType = endpoint.getContentType() != null ? endpoint.getContentType() : "application/json";
			response.example(mediaType, outputExample);
		}

		responses.put("200", response);

		if (!StringUtils.isBlank(endpoint.getReturnedVariableName()) && endpoint.getService() != null && endpoint.getService() instanceof CustomScript) {
			String returnedVariableType = ScriptUtils.getReturnedVariableType(endpoint.getService(), endpoint.getReturnedVariableName());

			Model returnModelSchema;

			if (ClassUtils.isPrimitiveOrWrapperType(returnedVariableType)) {
				returnModelSchema = SwaggerHelper.buildPrimitiveResponse(endpoint.getReturnedVariableName(), returnedVariableType);

			} else {

				CustomEntityTemplate returnedCet = customEntityTemplateService.findByDbTablename(returnedVariableType);
				if (returnedCet != null) {
					returnModelSchema = cetToModel(returnedCet);

				} else {
					returnModelSchema = SwaggerHelper.buildObjectResponse(endpoint.getReturnedVariableName());
				}
			}

			response.setResponseSchema(returnModelSchema);
		}
		
		Swagger swagger = new Swagger();
		swagger.setInfo(info);
		swagger.setBasePath(baseUrl);
		swagger.setSchemes(Arrays.asList(Scheme.HTTP, Scheme.HTTPS));
		swagger.setProduces(Collections.singletonList(endpoint.getContentType()));
		if(endpoint.getMethod() == EndpointHttpMethod.POST) {
			swagger.setConsumes(Arrays.asList("application/json", "application/xml"));
		}
		swagger.setPaths(paths);
		swagger.setResponses(responses);

		return Response.ok(Json.pretty(swagger)).build();
	}
	
	public ModelImpl cetToModel(CustomEntityTemplate cet) {

		Map<String, CustomFieldTemplate> cfts = customFieldTemplateService.findByAppliesTo(cet.getAppliesTo());

		ModelImpl result = new ModelImpl();
		result.setType(cet.getCode());
		result.setName(cet.getName());
		result.setDescription(cet.getDescription());

		result.setProperties(SwaggerHelper.convertCftsToProperties(cfts));
		result.setRequired(SwaggerHelper.getRequiredFields(cfts));

		return result;
	}

	/**
	 * Extract variable pointed by returned variable name and apply JSONata query if defined
	 * If endpoint is not configured to serialize the result and that returned variable name is set, do not serialize result. Otherwise serialize it.
	 *
	 * @param endpoint Endpoint endpoxecuted
	 * @param result Result of the endpoint execution
	 * @return the transformed JSON result if JSONata query was defined or the serialized result if query was not defined.
	 */
	public String transformData(Endpoint endpoint, Map<String, Object> result){
	    final boolean returnedVarNameDefined = !StringUtils.isBlank(endpoint.getReturnedVariableName());
	    boolean shouldSerialize = !returnedVarNameDefined || endpoint.isSerializeResult();
	
		Object returnValue = result;
		if(returnedVarNameDefined) {
			Object extractedValue = result.get(endpoint.getReturnedVariableName());
			if(extractedValue != null){
				returnValue = extractedValue;
			}else {
				log.warn("[Endpoint {}] Variable {} cannot be extracted from context", endpoint.getCode(), endpoint.getReturnedVariableName());
			}
		}
	
	    if (!shouldSerialize) {
	        return returnValue.toString();
	    }
	
	    if(returnValue instanceof Map){
	    	((Map<?, ?>) returnValue).remove("response");
	    	((Map<?, ?>) returnValue).remove("request");
	    }
	    
	    final String serializedResult = JacksonUtil.toStringPrettyPrinted(returnValue);
	    if (StringUtils.isBlank(endpoint.getJsonataTransformer())) {
	        return serializedResult;
	    }
	
	    return JSONata.transform(endpoint.getJsonataTransformer(), serializedResult);
	
	}
}
