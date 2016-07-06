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

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Javadocer class.
 * 
 * Creates pretty printed XML responses from external sources (REST). Use {@link JavadocerParameters} and {@link Javadocer#retrieveContent(JavadocerParameters)}. 
 * Javadocer expects to have property {@value tut.pori.javadocer.Javadocer#PROPERTY_REST_URI} to be set.
 * 
 * This implementation expects the following attributes:
 * <ul>
 * 	<li>body_uri - the uri where to retrieve HTTP Body content for the request. The request is always assumed to be GET and the uri should contain all required parameters. The uri is relative to the configured rest_uri. The value can be left empty or the attribute can be omitted if no body content is required.</li>
 * 	<li>method - the REST method name</li>
 * 	<li>query - query uri without the ? prefix. The value can be left empty or the attribute omitted if no additional query parameters are required</li>
 * 	<li>service - the REST service name</li>
 * 	<li>type - HTTP method type, e.g. GET, POST</li>
 * </ul>
 * 
 * The retrieved body content has a few limitations/features, namely:
 * <ul>
 * 	<li>The content must be valid XML</li>
 * 	<li>If the retrieved content has element named &lt;example&gt; directly below the root element, the content will be stripped to contain only the content inside the &lt;example&gt; element. E.g. new XML document will be created with the first child of &lt;example&gt; element as the root element.</li>
 * </ul>
 * 
 * Example with a configured REST uri http://example.org/rest/ : <br/>
 *  
 * {&#64; service="ts" method="test" type="POST" query="par1=1&par2=2" body_uri="/ts/test2?par3=3"}<br/>
 * 
 * This element would be resolved in the following order:
 * <ul>
 * 	<li>A HTTP GET request would be executed to url http://example.org/rest/ts/test2?par3=3 to retrieve the body content.</li>
 * 	<li>A HTTP POST request would be executed to url http://example.org/rest/ts/test?par1=1&par2=2 using the previously retrieved content as the HTTP body.</li>
 *  <li>The XML content is then returned as a pretty printed string</li>
 * </ul>
 * 
 */
public class Javadocer implements Closeable {
	/** System property name used to define the REST base URI */
	public static final String PROPERTY_REST_URI = "tut.pori.javadocer.rest_uri";
	private static final Logger LOGGER = Logger.getLogger(Javadocer.class);
	private static final String CHARSET = "UTF-8";
	private static final String ELEMENT_EXAMPLE = "example";
	private CloseableHttpClient _client = null;
	private DocumentBuilder _documentBuilder = null;
	private String _restUri = null;
	private Transformer _transformer = null;
	private XPath _xPath = null;

	/**
	 * the type of the HTTP method call
	 *
	 */
	public enum MethodType{
		/** HTTP method type DELETE */
		DELETE("DELETE"),
		/** HTTP method type GET */
		GET("GET"),
		/** HTTP method type POST */
		POST("POST");

		private String _value;

		/**
		 * 
		 * @param value
		 */
		private MethodType(String value){
			_value = value;
		}

		/**
		 * 
		 * @param value
		 * @return the given value as MethodType
		 * @throws IllegalArgumentException
		 */
		public static MethodType fromString(String value) throws IllegalArgumentException{
			for(MethodType t : MethodType.values()){
				if(t._value.equalsIgnoreCase(value)){
					return t;
				}
			}
			throw new IllegalArgumentException("Unknown type: "+value);
		}

		@Override
		public String toString() {
			return name();
		}
	} // enum MethodType

	/**
	 * 
	 * @throws IllegalArgumentException
	 */
	public Javadocer() throws IllegalArgumentException{
		_restUri = System.getProperty(PROPERTY_REST_URI);
		if(StringUtils.isBlank(_restUri)){
			throw new IllegalArgumentException("Bad "+PROPERTY_REST_URI);
		}

		try {
			_documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			_transformer = TransformerFactory.newInstance().newTransformer();
			_transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			_transformer.setOutputProperty(OutputKeys.STANDALONE, "yes"); // because of an issue with java's transformer indent, we need to add standalone attribute 
			_transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			_transformer.setOutputProperty(OutputKeys.ENCODING, CHARSET);
			_transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			_transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		} catch (ParserConfigurationException | TransformerConfigurationException | TransformerFactoryConfigurationError ex) {
			LOGGER.error(ex, ex);
			throw new IllegalArgumentException("Failed to create document builder/transformer instance.");
		}

		_xPath = XPathFactory.newInstance().newXPath();
		_client = HttpClients.createDefault();
	}

	/**
	 * 
	 * @param service
	 * @param method
	 * @param query
	 * @return the uri generated from the given parameters
	 * @throws IllegalArgumentException 
	 */
	private String createUri(String service, String method, String query) throws IllegalArgumentException{
		if(StringUtils.isBlank(service) || StringUtils.isBlank(method)){
			throw new IllegalArgumentException("Invalid service: "+service+" or method: "+method);
		}
		StringBuilder uri = new StringBuilder();
		uri.append(_restUri);
		uri.append(service);
		uri.append('/');
		uri.append(method);
		if(!StringUtils.isBlank(query)){
			uri.append('?');
			uri.append(query);
		}
		return uri.toString();
	}

	/**
	 * 
	 * @param params 
	 * @return content from the path described by the given parameters
	 * @throws IllegalArgumentException
	 */
	public String retrieveContent(JavadocerParameters params) throws IllegalArgumentException {
		MethodType type = params.getType();
		if(type == null){
			throw new IllegalArgumentException("Type is missing.");
		}
		String uri = createUri(params.getService(), params.getMethod(), params.getQuery());
		HttpRequestBase request = null;
		switch(type){
			case DELETE:
				request = new HttpDelete(uri);
				break;
			case GET:
				request = new HttpGet(uri);
				break;
			case POST:
				HttpPost post = new HttpPost(uri);
				if(!StringUtils.isBlank(params.getBodyUri())){
					String bodyUri = _restUri+params.getBodyUri();
					LOGGER.debug("Retrieving body from url: "+bodyUri);
					try(CloseableHttpResponse response = _client.execute(new HttpGet(bodyUri))){
						StatusLine statusLine = response.getStatusLine();
						int statusCode = statusLine.getStatusCode();
						if(statusCode < 200 || statusCode >= 300){
							throw new IllegalArgumentException("Server responded: "+statusCode+" "+statusLine.getReasonPhrase());
						}
						try {
							Node node = getExampleContent(_documentBuilder.parse(response.getEntity().getContent()));
							if(node == null){
								throw new IllegalArgumentException("No example returned by url: "+bodyUri);
							}
							post.setEntity(new StringEntity(toString(node), ContentType.TEXT_XML));
						} catch (IllegalStateException | SAXException ex) {
							LOGGER.error(ex, ex);
							throw new IllegalArgumentException("Failed to parse the response from url: "+bodyUri);
						}
					} catch (IOException ex) {
						LOGGER.error(ex, ex);
						throw new IllegalArgumentException("Failed to retrieve body content from url: "+bodyUri);
					}
				} // if body
				request = post;
				break;
			default:
				throw new IllegalArgumentException("Unknown type: "+params.getType());
		}

		LOGGER.debug("Calling url: "+uri);
		try(CloseableHttpResponse response = _client.execute(request)){
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if(statusCode < 200 || statusCode >= 300){
				throw new IllegalArgumentException("Server responded: "+statusCode+" "+statusLine.getReasonPhrase());
			}
			org.w3c.dom.Document doc = _documentBuilder.parse(response.getEntity().getContent());
			Node node = getExampleContent(doc);
			if(node == null){
				LOGGER.debug("No example content.");
				return toString(doc);
			}else{
				return toString(node);
			}
		} catch (IllegalStateException | SAXException | IOException ex) {
			LOGGER.error(ex, ex);
			throw new IllegalArgumentException("Failed to parse response from url: "+uri);
		}
	}

	/**
	 * 
	 * @param doc
	 * @return node located under ELEMENT_EXAMPLE, or null if not available
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws IllegalStateException 
	 */
	private Node getExampleContent(org.w3c.dom.Document doc) throws IllegalStateException, SAXException, IOException {
		NodeList nodes = doc.getElementsByTagName(ELEMENT_EXAMPLE);
		if(nodes.getLength() < 1){
			LOGGER.debug("No element "+ELEMENT_EXAMPLE);
			return null;
		}

		NodeList childNodes = null;
		Node root = doc.getDocumentElement();
		for(int i=0,count=nodes.getLength();i<count;++i){
			Node node = nodes.item(i);
			if(node.getParentNode().equals(root)){ // do not go deeper than one level below root
				childNodes = node.getChildNodes();
				break;
			}
		}
		if(childNodes == null || childNodes.getLength() < 1){
			LOGGER.debug("No valid element "+ELEMENT_EXAMPLE);
			return null;
		}

		Node node = null;
		for(int i=0,count=childNodes.getLength();i<count;++i){
			Node child = childNodes.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE){
				node = child;
				break;
			}
		}
		return node;
	}

	@Override
	public void close() {
		if(_client != null){
			try {
				_client.close();
			} catch (IOException ex) {
				LOGGER.error(ex, ex);
			}
		}
	}

	/**
	 * converts the given node into an xml document and prints out that xml document
	 * 
	 * @param node
	 * @return the given node as xml string
	 */
	private String toString(Node node){
		StringWriter sw = new StringWriter();
		try {
			if(node.getNodeType() == Node.DOCUMENT_NODE){
				cleanWhiteSpace((org.w3c.dom.Document) node);
			}else{
				cleanWhiteSpace(node.getOwnerDocument());
			}
			_transformer.transform(new DOMSource(node), new StreamResult(sw));
			return sw.toString();
		} catch (TransformerException ex) {
			LOGGER.error(ex, ex);
			throw new IllegalArgumentException("Invalid transformer settings.");
		}
	}

	/**
	 * clean whitespace around tags
	 * 
	 * @param doc
	 * @throws IllegalArgumentException
	 */
	private void cleanWhiteSpace(org.w3c.dom.Document doc) throws IllegalArgumentException{
		try {
			NodeList nodeList = (NodeList) _xPath.evaluate("//text()[normalize-space()='']", doc, XPathConstants.NODESET);
			for (int i=0;i<nodeList.getLength();++i) {
				Node node = nodeList.item(i);
				node.getParentNode().removeChild(node);
			}
		} catch (XPathExpressionException ex) {
			LOGGER.error(ex, ex);
			throw new IllegalArgumentException("Xpath evaluation failed.");
		}
	}
}
