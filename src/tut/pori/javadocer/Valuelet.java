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

import java.util.Map;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.formats.html.markup.RawHtml;
import com.sun.tools.doclets.internal.toolkit.Content;
import com.sun.tools.doclets.internal.toolkit.taglets.TagletWriter;
import com.sun.tools.doclets.internal.toolkit.taglets.ValueTaglet;

/**
 * Extends the default \@value tag by removing the unnecessary "" around the value, has no effect on the RESTlet functionality.
 * 
 * Note that this uses Java's internal classes, see the commented code for implementations for different Java versions.
 * 
 * Optionally, simply using other name for the tag than \@value for values, would remove the need to use internal classes.
 * 
 * For usage instructions, follow the Oracle's guide at <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/javadoc/taglet/overview.html">Taglet Overview</a>.
 */
public class Valuelet extends ValueTaglet {
	
	/*
	 *  for JAVA 7 
	 *  
	 *  Requires: import com.sun.tools.doclets.internal.toolkit.taglets.TagletOutput;
	 *
	@Override
	public TagletOutput getTagletOutput(Tag tag, TagletWriter writer) {
		TagletOutput output = super.getTagletOutput(tag, writer);
		String value = output.toString();
		value = value.replace(">\"", ">"); // remove " " around string values in the href ...a>"value"</a>
		value = value.replace("\"<", "<");
		output.setOutput(value);
		return output;
	}*/

	/**
	 * For JAVA 8
	 * 
	 * Requires: import com.sun.tools.doclets.internal.toolkit.Content
	 */
	@Override
	public Content getTagletOutput(Tag tag, TagletWriter writer) {
		String value = super.getTagletOutput(tag, writer).toString();
		value = value.replace(">\"", ">"); // remove " " around string values in the href ...a>"value"</a>
		value = value.replace("\"<", "<");
		return new RawHtml(value); // create new content with the replaced values
	}

	/**
     * Register this Taglet.
     * @param tagletMap the map to register this tag to.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static void register(Map tagletMap) {
    	Valuelet tag = new Valuelet();
    	tagletMap.put(tag.getName(), tag);
    }
}
