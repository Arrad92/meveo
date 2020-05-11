package org.meveo.service.technicalservice.endpoint;

import java.io.File;
import java.io.IOException;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.technicalservice.endpoint.Endpoint;
import org.meveo.model.technicalservice.endpoint.EndpointHttpMethod;
import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.script.ScriptUtils;
import org.meveo.service.technicalservice.endpoint.schema.EndpointSchemaService;

/**
 * This service is use to build the endpoint interface from a template file. The
 * output is Javascript code to be use by frontend client.
 * 
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @since 6.8.0
 * @version 6.9.0
 */
@Stateless
public class ESGeneratorService {

	@Inject
	private CustomEntityTemplateService customEntityTemplateService;

	@Inject
	private EndpointSchemaService endpointSchemaService;

	/**
	 * Generates an endpoint interface in js code using a template file.
	 * 
	 * @param endpoint endpoint
	 * @see Endpoint
	 * @return it returns the endpoint interface in js
	 * @throws IOException when template is not found
	 */
	public String buildJSInterfaceFromTemplate(Endpoint endpoint) {
		return buildJSInterfaceFromTemplate("", endpoint);
	}

	/**
	 * Generates an endpoint interface in js code using a template file.
	 * 
	 * @param baseUrl  base url of the request
	 * @param endpoint endpoint
	 * @see Endpoint
	 * @return it returns the endpoint interface in js
	 * @throws IOException when template is not found
	 */
	public String buildJSInterfaceFromTemplate(String baseUrl, Endpoint endpoint) {

		EndpointJSInterface endpointJSInterface = new EndpointJSInterface();
		endpointJSInterface.setEndpointCode(endpoint.getCode());
		endpointJSInterface.setEndpointDescription(endpoint.getDescription());
		endpointJSInterface.setHttpMethod(endpoint.getMethod());

		String returnedVariableType = ScriptUtils.findScriptVariableType(endpoint.getService(), endpoint.getReturnedVariableName());
		CustomEntityTemplate returnedCet = customEntityTemplateService.findByDbTablename(returnedVariableType);
		if (returnedCet != null) {
			endpointJSInterface.setCet(true);
		}

		endpointJSInterface.setResponseSchema(endpointSchemaService.generateResponseSchema(endpoint));

		try {
			if (endpoint.getMethod() == EndpointHttpMethod.GET) {
				endpointJSInterface.setRequestSchema(endpointSchemaService.generateRequestSchema(endpoint));
				endpointJSInterface
						.setTemplate(FileUtils.readFileToString(new File(ESGenerator.class.getClassLoader().getResource("endpoint-js-template/get-template.js").getFile())));

			} else if (endpoint.getMethod() == EndpointHttpMethod.POST) {
				endpointJSInterface.setRequestSchema(endpointSchemaService.generateRequestSchema(endpoint));
				endpointJSInterface
						.setTemplate(FileUtils.readFileToString(new File(ESGenerator.class.getClassLoader().getResource("endpoint-js-template/post-template.js").getFile())));
			}
		} catch (IOException e) {
			return "Missing template";
		}

		return endpointJSInterface.build();
	}
}
