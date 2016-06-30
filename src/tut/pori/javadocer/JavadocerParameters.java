/**
 * Copyright 2015 Tampere University of Technology, Pori Department
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tut.pori.javadocer;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import tut.pori.javadocer.Javadocer.MethodType;

/**
 * Parser for Javadocer parameters
 */
public class JavadocerParameters{
	/** service method parameter declaration */
	public static final String ATTRIBUTE_BODY_URI = "body_uri";
	/** service method parameter declaration */
	public static final String ATTRIBUTE_METHOD = "method";
	/** service method parameter declaration */
	public static final String ATTRIBUTE_QUERY = "query";
	/** service method parameter declaration */
	public static final String ATTRIBUTE_SERVICE = "service";
	/** service method parameter declaration */
	public static final String ATTRIBUTE_TYPE = "type";
	private static final Logger LOGGER = Logger.getLogger(JavadocerParameters.class);
	private String _bodyUri = null;
	private String _method = null;
	private String _query = null;
	private String _service = null;
	private MethodType _type = null;
	
	/**
	 * @see #parse(String)
	 */
	private JavadocerParameters(){
		// nothing needed
	}
	
	/**
	 * {@link JavadocerParameters#parse} expects the following attributes:
	 * <ul>
	 * 	<li>body_uri - the uri where to retrieve HTTP Body content for the request. The request is always assumed to be GET and the uri should contain all required parameters. The uri is relative to the configured rest_uri. The value can be left empty or the attribute can be omitted if no body content is required.</li>
	 * 	<li>method - the REST method name</li>
	 * 	<li>query - query uri without the ? prefix. The value can be left empty or the attribute omitted if no additional query parameters are required</li>
	 * 	<li>service - the REST service name</li>
	 * 	<li>type - HTTP method type, e.g. GET, POST</li>
	 * </ul>
	 * 
	 * Parameter values inside [] are assumed to be initialized constants (e.g. package.class#constant for static final String constant in class class and package package), and the values will be resolved before calling the uris.
	 * 
	 * @param params String of parameters, e.g. <br/><code>service="ts" method="test" type="POST" query="par1=1&par2=2" body_uri="/ts/test2?par3=3"</code>
	 * @return the parsed parameters
	 */
	public static JavadocerParameters parse(String params){
		Pattern pattern = Pattern.compile("\\w+=\"[\\S]*\"");
        Matcher matcher = pattern.matcher(params);        
        JavadocerParameters javadocerParameters = null;
        while(matcher.find()){        	
        	String param = matcher.group(0);
        	if(StringUtils.contains(param, "=")){
        		String tokens[] = StringUtils.split(param, "=", 2);
        		String value = null;
        		if(tokens.length<2){
        			LOGGER.debug("Ignored parameter without value.");
        			continue;
        		}else{
        			if(javadocerParameters == null){
        				javadocerParameters = new JavadocerParameters();
        			}
        			value = resolveValue(tokens[1]);
        		}
        		switch(tokens[0]){
        			case ATTRIBUTE_BODY_URI:
        				javadocerParameters.setBodyUri(value);
        				break;
        			case ATTRIBUTE_METHOD:
        				javadocerParameters.setMethod(value);
        				break;
        			case ATTRIBUTE_QUERY:
        				javadocerParameters.setQuery(value);
        				break;
        			case ATTRIBUTE_SERVICE:
        				javadocerParameters.setService(value);
        				break;
        			case ATTRIBUTE_TYPE:
        				javadocerParameters.setType(MethodType.fromString(value));
        				break;
        			default:
        				break;
        		}
        	}
        }
        return javadocerParameters;
	}
	
	/**
	 * Strips the given value of surrounding "" and resolves all constant references marked with []
	 * 
	 * @param value
	 * @return the value as its cleaned form
	 * @throws IllegalArgumentException 
	 */
	private static String resolveValue(String value) throws IllegalArgumentException{
		String retValue = StringUtils.remove(value, "\"");
		if(StringUtils.isBlank(retValue)){
			LOGGER.debug("Blank value.");
			return null;
		}
		int valueLength = retValue.length();
		StringBuilder valueBuilder = new StringBuilder(valueLength);
		StringBuilder resolveBuilder = new StringBuilder(valueLength);
		boolean inBrackets = false;
		for(int i=0;i<valueLength;++i){
			char c = retValue.charAt(i);
			if(c == '['){
				inBrackets = true;
			}else if(c == ']'){
				if(!inBrackets){
					throw new IllegalArgumentException("Invalid value: "+value);
				}
				inBrackets = false;
				valueBuilder.append(lookupVariable(resolveBuilder.toString()));
				resolveBuilder.setLength(0);
			}else if(inBrackets){
				resolveBuilder.append(c);
			}else{ // not in brackets
				valueBuilder.append(c);
			}
		}
		if(inBrackets){
			throw new IllegalArgumentException("Invalid value: "+value);
		}
		
		return valueBuilder.toString();
	}
	
	/**
	 * 
	 * @param path valid initialized constant path, e.g. package.class#constant
	 * @return the value referenced by the given path
	 * @throws IllegalArgumentException 
	 */
	protected static Object lookupVariable(String path) throws IllegalArgumentException {
		String[] parts = StringUtils.split(path, '#');
		if(parts.length != 2){
			throw new IllegalArgumentException("Invalid path: "+path);
		}		
				
		try {
			Field field = Class.forName(parts[0]).getDeclaredField(parts[1]);
			field.setAccessible(true); // override protected and private
			return field.get(null);
		} catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalAccessException ex) {
			LOGGER.error(ex, ex);
			throw new IllegalArgumentException("Invalid path: "+path);
		}    	
	}

	/**
	 * @return the bodyUri
	 */
	public String getBodyUri() {
		return _bodyUri;
	}

	/**
	 * @param bodyUri the bodyUri to set
	 */
	public void setBodyUri(String bodyUri) {
		_bodyUri = bodyUri;
	}

	/**
	 * @return the method
	 */
	public String getMethod() {
		return _method;
	}

	/**
	 * @param method the method to set
	 */
	public void setMethod(String method) {
		_method = method;
	}

	/**
	 * @return the query
	 */
	public String getQuery() {
		return _query;
	}

	/**
	 * @param query the query to set
	 */
	public void setQuery(String query) {
		_query = query;
	}

	/**
	 * @return the service
	 */
	public String getService() {
		return _service;
	}

	/**
	 * @param service the service to set
	 */
	public void setService(String service) {
		_service = service;
	}

	/**
	 * @return the type
	 */
	public MethodType getType() {
		return _type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(MethodType type) {
		_type = type;
	}
	
}
