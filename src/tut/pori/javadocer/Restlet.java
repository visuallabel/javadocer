/**
 * Copyright 2015 Tampere University of Technology, Pori Unit
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

import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * Class Restlet is used for retrieving example XML queries by the Javadoc. This inline tag aims to help with maintenance of the documentation.
 *
 */
public class Restlet implements Taglet {
	/** The name of the tag. Use this name in your code as {&#64;NAME} */
    private static final String NAME = "doc.restlet";
    private static final int STATUS_EXCEPTION = -1;
    private static final Logger LOGGER = Logger.getLogger(Restlet.class);
	
    /**
     * Return the name of this custom tag. Use this name in your code. E.g. {&#64;NAME}.
     */
    @Override
	public String getName() {
        return NAME;
    }

    @Override
	public boolean inField() {
        return false;
    }

    @Override
	public boolean inConstructor() {
        return false;
    }

    @Override
	public boolean inMethod() {
        return false;
    }

    @Override
	public boolean inOverview() {
        return false;
    }

    @Override
	public boolean inPackage() {
        return false;
    }

    @Override
	public boolean inType() {
        return false;
    }
    
    /**
     * Returns true because this taglet is only used as a inline tag.
     */
    @Override
	public boolean isInlineTag() {
        return true;
    }
    
    /**
     * Register this Taglet.
     * @param tagletMap  the map to register this tag to.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static void register(Map tagletMap) {
       Restlet tag = new Restlet();
       tagletMap.put(tag.getName(), tag);
    }

    /**
     * @param tag the tag detected by Javadoc.
     * @return The output formatted as HTML.
     */
    @Override
	public String toString(Tag tag) {
    	String content = null;
		try(Javadocer jdocer = new Javadocer()){
			content = jdocer.retrieveContent(JavadocerParameters.parse(tag.text()));
		}catch(Throwable ex){ // javadoc will not fail regardless of the exception, so manually abort
			SourcePosition position = tag.position(); // for some reason, the position is somewhat of by a few lines, but at least it gives a hint of where to look for the error.
			LOGGER.error("Aborting on exception. File: "+position.file().getAbsolutePath()+", line: "+position.line()+", column: "+position.column(), ex);
			System.exit(STATUS_EXCEPTION);
		}
		if(StringUtils.isBlank(content)){
			LOGGER.warn("Failed to retrieve content.");
			return null;
		}else{
			return "<pre>"+StringEscapeUtils.escapeHtml4(content)+"</pre>"; // simply use pre tags to preserve any pretty print, we could also do more fine-tuned format, we could also print the request here as we know all parameters utilized
		}
    }
    
    /**
     * Not used when using inline tags.
     */
    @Override
	public String toString(Tag[] tags) {
    	return null;
    }
}
