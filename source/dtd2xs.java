/***************************************************************************
* dtd2xs: Translates XML 1.0 DTD to REC-xmlschema-1-20010502
****************************************************************************
* Copyright (C) 07/2001 Ralf Schweiger
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*
* Contact the Author:
*
* University of Giessen
* c/o Ralf Schweiger
* Heinrich-Buff-Ring 44
* 35392 Giessen
* Germany
*
* tel.:   +49-641-9941370
* fax.:   +49-641-9941359
* mailto: Ralf.Schweiger@informatik.med.uni-giessen.de
*
***************************************************************************/

/* Modified to use JAXP by Chuck Morris (Northrop Grumman Information Technology, TASC), June 2003 */

import java.io.*;
import java.util.*;
//import com.sun.xml.tree.XmlDocument;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
//import com.jclark.xsl.dom.*;

// uncomment if your Java environment < v1.4
// import xurl;

/**
 * DTD => REC-xmlschema-1-20010502
 * @author Ralf Schweiger
 * @version 1.00, 06/12/01
 */

public class dtd2xs {

   private String [] delimiter = { ",|", ";/" };

   private final int meaninglessEntity = -1; // no meaningful entity
   private final int enumerationEntity = 0;
   private final int contentModelEntity = 1;
   private final int attributeListEntity = 2;
   private final int unusedEntity = 3;

   private final String schemaPrefix = "xs";

   private final String [] builtInSimpleType = { "string", "normalizedString", "token", "byte", "unsignedByte", "base64Binary", "hexBinary", "integer", "positiveInteger", "negativeInteger", "nonNegativeInteger", "nonPositiveInteger", "int", "unsignedInt", "long", "unsignedLong", "short", "unsignedShort", "decimal", "float", "double", "boolean", "time", "dateTime", "duration", "date", "gMonth", "gYear", "gYearMonth", "gDay", "gMonthDay", "Name", "QName", "NCName", "anyURI", "language", "ID", "IDREF", "IDREFS", "ENTITY", "ENTITIES", "NOTATION", "NMTOKEN", "NMTOKENS" };

   private final int maxNumComments = 1000;
   private int numComments;
   private String [] comment;
   private boolean ignoreComments;
   private String commentLanguage;
   private int conceptHighlight;
   private int conceptOccurrence;
   private int commentLength;
   private String conceptRelation;

// CM: Define jaxp vars
   private DocumentBuilder jaxp_docbuilder;
   private TransformerFactory jaxp_transformer_factory;
   private Transformer jaxp_identity_transformer;

   /**
    * Logging information.
    */
   private PrintStream psLog; // application
   private StringBuffer sbLog; // applet

   private void log(Object item) {
      if (psLog != null) psLog.print(item);
      else if (sbLog != null) sbLog.append(item);
   }

   /**
    * Create a DTD to REC-xmlschema-1-20010502 translator.
    * @param log Where to put logging information.
    */
   public dtd2xs(Object log) {
      comment = new String [maxNumComments];
      psLog = null;
      sbLog = null;
      if (log instanceof PrintStream) psLog = (PrintStream) log;
      else if (log instanceof StringBuffer) sbLog = (StringBuffer) log;

// CM: Initialize jaxp vars
      try {
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         jaxp_docbuilder = dbf.newDocumentBuilder();
         jaxp_transformer_factory = TransformerFactory.newInstance();
         jaxp_identity_transformer = jaxp_transformer_factory.newTransformer();
         jaxp_identity_transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      }
      catch (Exception x) { log("\ndtd2xs: " + x); }
   }

   private Element simpleType(Document schema, String name, String content) {
      Element simpleType = schema.createElement("simpleType");
      if (name != null) {
         schema.getDocumentElement().appendChild(simpleType);
         simpleType.setAttribute("name", name);
         annotate(schema, simpleType);
      }
      Element restriction = schema.createElement("restriction");
      simpleType.appendChild(restriction);
      if (content.startsWith("(")) {
         restriction.setAttribute("base", "string");
         StringTokenizer contentTokenizer = new StringTokenizer(content, "(|)");
         while (contentTokenizer.hasMoreTokens()) {
            Element enumeration = schema.createElement("enumeration");
            restriction.appendChild(enumeration);
            enumeration.setAttribute("value", contentTokenizer.nextToken().trim());
            annotate(schema, enumeration);
         }
      }
      else restriction.setAttribute("base", content.equals("CDATA") ? "string" : content);
      return simpleType; // enumeration
   }

   private void attributeGroup(Document schema, Element group, String content) {
      StringTokenizer contentTokenizer = new StringTokenizer(content);
      String lookahead, name = null;
      while (contentTokenizer.hasMoreTokens()) {
         if (name == null) name = contentTokenizer.nextToken(" \t\n\r\f");
         if (name.startsWith("%") && name.endsWith(";")) {
            Element attributeGroupRef = schema.createElement("attributeGroup");
            group.appendChild(attributeGroupRef);
            attributeGroupRef.setAttribute("ref", name.substring(1, name.length() - 1));
            name = null;
            continue;
         }
         Element attribute = schema.createElement("attribute");
         group.appendChild(attribute);
         String type = contentTokenizer.nextToken(" \t\n\r\f");
         if (type.startsWith("(") && ! type.endsWith(")")) type += contentTokenizer.nextToken(")");
         String use = contentTokenizer.nextToken(") \t\n\r\f");
         String value = null;
         if ("#IMPLIED #REQUIRED #FIXED".indexOf(use.toUpperCase()) < 0) {
            value = use;
            use = "#IMPLIED";
         }
         else if (contentTokenizer.hasMoreTokens())
            value = contentTokenizer.nextToken(" \t\n\r\f");
         String quote = null;
         if (value != null && -1 < "\"'".indexOf(value.charAt(0)))
             quote = String.valueOf(value.charAt(0));
         if (quote != null) {
            if (! value.endsWith(quote) || value.equals(quote))
               value = value.substring(1, value.length()) + contentTokenizer.nextToken(quote);
            else
               value = value.substring(1, value.length() - 1);
            lookahead = null;
         }
         else {
            lookahead = value;
            value = null;
         }
         // log("\ndtd2xs: attribute[" + name + "][" + type + "][" + use + "][" + value + "]");
         if (name.indexOf(':') < 0) { // non-colonized name (NCName)
            attribute.setAttribute("name", name);
            if (type.startsWith("%"))
               attribute.setAttribute("type", type.substring(1, type.length() - 1));
            else if (type.startsWith("("))
               attribute.appendChild(simpleType(schema, null, type));
            else if (type.toUpperCase().equals("CDATA")) // no valid simpleType
               attribute.setAttribute("type", "string");
            else
               attribute.setAttribute("type", type);
         }
         else attribute.setAttribute("ref", name); // colonized name (QName)
         annotate(schema, attribute);
         use = use.toUpperCase();
         if (use.equals("#REQUIRED"))
            attribute.setAttribute("use", "required");
         if (use.equals("#FIXED"))
            attribute.setAttribute("fixed", value);
         else if (value != null)
            attribute.setAttribute("default", value);
         name = lookahead;
      }
   }

   private int occurs(Element el, String content) {
      int length = content.length();
      if (content.endsWith("?"))
         { el.setAttribute("minOccurs", "0"); -- length; }
      else if (content.endsWith("+"))
         { el.setAttribute("maxOccurs", "unbounded"); -- length; }
      else if (content.endsWith("*"))
         { el.setAttribute("minOccurs", "0"); el.setAttribute("maxOccurs", "unbounded"); -- length; }
      return length;
   }

   private Element createGroup(Document schema, String type, StringTokenizer content, int delimiterIndex) {
      Element groupType = schema.createElement(type);
      while (content.hasMoreTokens()) {
         String item = content.nextToken().trim();
         if (item.startsWith("("))
            groupType.appendChild(anonymousGroup(schema, item, delimiterIndex));
         else if (item.startsWith("%")) { // reference to dtd entity respectively xs group
            Element groupRef = schema.createElement("group");
            groupType.appendChild(groupRef);
            groupRef.setAttribute("ref", item.substring(1, occurs(groupRef, item) - 1)); // remove semicolon
         }
         else if (item.startsWith("#"))
            groupType.setAttribute("_dtd2xs_mixed_", "true"); // later moved to complexType!!!
         else {
            Element elRef = schema.createElement("element");
            groupType.appendChild(elRef);
            elRef.setAttribute("ref", item.substring(0, occurs(elRef, item)));
         }
      }
      return groupType;
   }

   private String hideDepth(String content, int delimiterIndex) { // ((a, b), (x, y)) => ((a; b), (x; y)) for correct parsing
      StringBuffer newContent = new StringBuffer("");
      int depth = 0;
      for (int i = 0; i < content.length(); ++ i) {
         char c = content.charAt(i);
         if (c == '(')
            { newContent.append(c); ++ depth; }
         else if (c == ')')
            { newContent.append(c); -- depth; }
         else if (depth > 0 && c == delimiter[delimiterIndex].charAt(0)) // comma
            newContent.append(delimiter[1 - delimiterIndex].charAt(0));
         else if (depth > 0 && c == delimiter[delimiterIndex].charAt(1)) // bar
            newContent.append(delimiter[1 - delimiterIndex].charAt(1));
         else
            newContent.append(c);
      }
      return newContent.toString();
   }

   private void element(Document schema, String name, String content, int delimiterIndex) {
      Element el = schema.createElement("element");
      schema.getDocumentElement().appendChild(el);
      el.setAttribute("name", name);
      annotate(schema, el);
      StringTokenizer contentTokenizer = new StringTokenizer(content, "(,;|/?+* \t\n\r\f)");
      if (contentTokenizer.countTokens() == 1 && contentTokenizer.nextToken().toUpperCase().equals("#PCDATA")) // simpleType
         el.setAttribute("type", "string");
      else {
         Element complexType = schema.createElement("complexType");
         el.appendChild(complexType);
         Element group = anonymousGroup(schema, content, delimiterIndex);
         complexType.appendChild(group);
         /* done by xslt
         if (group.getAttribute("_dtd2xs_mixed_").equals("true")) {
            group.removeAttribute("_dtd2xs_mixed_");
            complexType.setAttribute("mixed", "true");
         }
         */
      }
   }

   private void group(Document schema, String name, String content, int delimiterIndex) {
      Element namedGroup = schema.createElement("group");
      schema.getDocumentElement().appendChild(namedGroup);
      namedGroup.setAttribute("name", name);
      annotate(schema, namedGroup);
      namedGroup.appendChild(anonymousGroup(schema, content, delimiterIndex));
   }

   private Element anonymousGroup(Document schema, String content, int delimiterIndex) {
      Element group, atHolder = schema.createElement("atHolder");
      if (content.startsWith("("))
         content = content.substring(1, occurs(atHolder, content) - 1); // remove parentheses
      else
         content = content.substring(0, occurs(atHolder, content));
      content = hideDepth(content, delimiterIndex);
      StringTokenizer contentTokenizer = new StringTokenizer(content, delimiter[delimiterIndex].substring(1, 2)); // BAR
      if (contentTokenizer.countTokens() > 1)
         group = createGroup(schema, "choice", contentTokenizer, 1 - delimiterIndex);
      else if (content.trim().toUpperCase().equals("EMPTY"))
         group = schema.createElement("_dtd2xs_empty_");
      else if (content.trim().toUpperCase().equals("ANY"))
         group = schema.createElement("_dtd2xs_any_");
      else // default
         group = createGroup(schema, "sequence", new StringTokenizer(content, delimiter[delimiterIndex]), 1 - delimiterIndex); // COMMA SEPARATION
      // optimize
      if (atHolder.getAttribute("minOccurs").length() > 0) group.setAttribute("minOccurs", atHolder.getAttribute("minOccurs"));
      if (atHolder.getAttribute("maxOccurs").length() > 0) group.setAttribute("maxOccurs", atHolder.getAttribute("maxOccurs"));
      return group;
   }

   private Document transform(Document xml, String xsltURI) {
      try {
// CM: Use a JAXP Transformer instead of xt
/*
         Document xslt = XmlDocument.createXmlDocument((new java.net.URL(xsltURI)).openStream(), false);
         Document result = new XmlDocument();
         (new XSLTransformEngine()).createTransform(xslt).transform(xml, result);
         result.getDocumentElement().removeAttribute("xmlns"); // added by xt
         return result;
*/
         if (xsltURI == null) {
            return xml;
         } else {
            java.io.InputStream is = (java.io.InputStream)(new java.net.URL(xsltURI).getContent());
            Transformer transformer = jaxp_transformer_factory.newTransformer(new StreamSource(is));
            Document result = jaxp_docbuilder.newDocument();
            transformer.transform(new DOMSource(xml), new DOMResult(result));
            result.getDocumentElement().removeAttribute("xmlns"); // added by stylesheet
            result.getDocumentElement().removeAttribute("xmlns:xt"); // added by stylesheet
            return result;
         }
      }
      catch (Exception x) { log("\ndtd2xs: " + x); return null; }
   }

   private void minusAtplusNs(Document schema) {
      schema.replaceChild(minusAtplusNs(schema, schema.getDocumentElement()), schema.getDocumentElement());
      schema.getDocumentElement().setAttribute("xmlns:" + schemaPrefix, "http://www.w3.org/2001/XMLSchema");
   }

   private boolean isBuiltInSimpleType(String type) {
      for (int i = 0; i < builtInSimpleType.length; ++ i)
         if (type.equals(builtInSimpleType[i]))
            return true;
      return false;
   }

   private Node minusAtplusNs(Document schema, Element e) {
      Element ee = schema.createElement(schemaPrefix + ':' + e.getTagName());
      // attributes
      NamedNodeMap ats = e.getAttributes();
      for (int i = 0; i < ats.getLength(); ++ i) {
         String name = ats.item(i).getNodeName();
         String value = ats.item(i).getNodeValue();
         if (! name.equals("xmlns") && ! name.equals("_dtd2xs_mixed_")) // remove these attributes
            if ((name.equals("type") || name.equals("base")) && isBuiltInSimpleType(value))
               ee.setAttribute(name, schemaPrefix + ':' + value);
            else
               ee.setAttribute(name, value);
      }
      // elements
      for (Node x = e.getFirstChild(); x != null; x = x.getNextSibling())
         if (x.getNodeType() == Node.ELEMENT_NODE)
            ee.appendChild(minusAtplusNs(schema, (Element) x));
         else
            ee.appendChild(x.cloneNode(true));
      return ee;
   }

/*
   private String replace(String source, String from, String to) {
      StringBuffer goal = new StringBuffer("");
      int i = 0, j = source.indexOf(from);
      while (j >= 0) {
         goal.append(source.substring(i, j));
         goal.append(to);
         i = j + from.length();
         j = source.indexOf(from, i);
      }
      goal.append(source.substring(i));
      return goal.toString();
   }
*/

   private String removeComments(String dtd) {
      StringBuffer dtdWithoutComments = new StringBuffer("");
      int commentStart, commentEnd = 0, from = 0;
      while ((commentStart = dtd.indexOf("<!--", commentEnd)) >= 0) {
         commentEnd = dtd.indexOf("-->", commentStart) + 3;
         dtdWithoutComments.append(dtd.substring(from, commentStart));
         if (! ignoreComments) {
            comment[numComments] = dtd.substring(commentStart + "<!--".length(), commentEnd - "-->".length());
            if (comment[numComments].length() <= commentLength) ++ numComments;
         }
         from = commentEnd;
      }
      return dtdWithoutComments + dtd.substring(from, dtd.length());
   }

   private int weightOf(char c, boolean prefix) {
      if ("\n\t:".indexOf(c) >= 0)
         return 10;
      else if (prefix && "<[{(\"'".indexOf(c) >=0 || ! prefix && ">]})\"'".indexOf(c) >= 0)
         return 100;
      else
         return 1;
   }

   private boolean related(String comment, String concept) {
      int occurrence = 0;
      boolean highlighted = false;
      int conceptStart, conceptEnd = 0;
      while ((conceptStart = comment.indexOf(concept, conceptEnd)) >= 0) {
         conceptEnd = conceptStart + concept.length();
         ++ occurrence;
         int highlightPrefix = 0;
         for (int i = conceptStart; i > 0 && ! Character.isLetterOrDigit(comment.charAt(i - 1)); -- i) {
            highlightPrefix += weightOf(comment.charAt(i - 1), true);
            if (comment.charAt(i - 1) == '\n') break;
         }
         int highlightPostfix = 0;
         for (int i = conceptEnd; i < comment.length() && ! Character.isLetterOrDigit(comment.charAt(i)); ++ i) {
            highlightPostfix += weightOf(comment.charAt(i), false);
            if (comment.charAt(i) == '\n') break;
         }
         if (highlightPrefix > 0 && highlightPostfix > 0)
            highlighted |= (highlightPrefix + highlightPostfix >= conceptHighlight);
      }
      return (highlighted && occurrence >= conceptOccurrence);
   }

   private void annotate(Document schema, Element construct) {
      if (ignoreComments || conceptRelation.indexOf(construct.getTagName()) < 0)
         return;
      Element annotation = schema.createElement("annotation");
      String concept;
      if (! construct.getAttribute("name").equals(""))
         concept = construct.getAttribute("name");
      else if (! construct.getAttribute("ref").equals(""))
         concept = construct.getAttribute("ref");
      else if (! construct.getAttribute("value").equals(""))
         concept = construct.getAttribute("value");
      else
         return;
      int numDocumentation = 0;
      for (int i = 0; i < numComments; ++ i)
         if (related(comment[i], concept)) {
            log("\ndtd2xs: relate(" + concept + ", \"" + comment[i] + "\")");
            Element documentation = schema.createElement("documentation");
            annotation.appendChild(documentation);
            if (commentLanguage != null && ! commentLanguage.equals(""))
               documentation.setAttribute("xml:lang", commentLanguage);
            documentation.appendChild(schema.createTextNode(comment[i]));
            ++ numDocumentation;
         }
      if (numDocumentation > 0) construct.appendChild(annotation);
   }

   private boolean isLexicalUnit(char first, char last) {
      if (Character.isWhitespace(first) && ! Character.isWhitespace(last)) return false;
      if (! Character.isWhitespace(first) && Character.isWhitespace(last)) return false;
      if (first == '(' && last != ')') return false;
      if (first != '(' && last == ')') return false;
      return true;
   }

   private int useOf(String entityName, String entityContent, String dtd, boolean resolveEntity) {
      // log("\ndtd2xs: <useOf entityName=\"" + entityName + "\" entityContent=\"" + entityContent + "\">\n");
      if (resolveEntity || entityContent.length() < 2 || ! isLexicalUnit(entityContent.charAt(0), entityContent.charAt(entityContent.length() - 1)))
         return meaninglessEntity;
      /* does the entity represent a meaningful unit? */
      String entityUse = "%" + entityName + ";";
      int entityUseStart, entityUseEnd = 0, entityIndex = -1, elementIndex = -1, attlistIndex = -1;
      boolean entityUsed = false;
      while ((entityUseStart = dtd.indexOf(entityUse, entityUseEnd)) >= 0) {
         entityUsed = true;
         entityUseEnd = entityUseStart + entityUse.length();
         if (entityIndex < 0) {
            entityIndex = dtd.lastIndexOf("<!ENTITY", entityUseStart);
            if (-1 < entityIndex && dtd.indexOf(">", entityIndex) < entityUseStart)
               entityIndex = -1;
         }
         if (elementIndex < 0) {
            elementIndex = dtd.lastIndexOf("<!ELEMENT", entityUseStart);
            if (-1 < elementIndex && dtd.indexOf(">", elementIndex) < entityUseStart)
               elementIndex = -1;
         }
         if (attlistIndex < 0) {
            attlistIndex = dtd.lastIndexOf("<!ATTLIST", entityUseStart);
            if (-1 < attlistIndex && dtd.indexOf(">", attlistIndex) < entityUseStart)
               attlistIndex = -1;
         }
      }
      if (! entityUsed) return unusedEntity;
      // if (entityIndex >= 0) return meaninglessEntity; // comment out for CDA because of null.code.set
      // -1 < elementIndex means that entity is used in element declaration ...
      int entityMeaning = meaninglessEntity;
      if (elementIndex >= 0 && attlistIndex < 0 || -1 < entityContent.indexOf("#PCDATA") || -1 < entityContent.indexOf('*') || -1 < entityContent.indexOf('?') || -1 < entityContent.indexOf(',') || -1 < entityContent.indexOf('+'))
         entityMeaning = contentModelEntity;
      else if (-1 < "CDATA".indexOf(entityContent) || entityContent.startsWith("(") && entityContent.endsWith(")"))
         entityMeaning = enumerationEntity;
      else if (attlistIndex >= 0 && elementIndex < 0 && (new StringTokenizer(entityContent)).countTokens() >= 2)
         entityMeaning = attributeListEntity;
      return entityMeaning;
   }

   private String resolveEntity(String name, String content, String dtd) {
      StringBuffer resolved = new StringBuffer("");
      int endUse = 0, startUse;
      while (-1 < (startUse = dtd.indexOf("%" + name + ";", endUse))) {
         resolved.append(dtd.substring(endUse, startUse));
         resolved.append(content);
         endUse = startUse + name.length() + 2;
      }
      resolved.append(dtd.substring(endUse, dtd.length()));
      return resolved.toString();
   }

   /**
    * Translate DTD to REC-xmlschema-1-20010502.<P>
    * @param dtdURL Location of the DTD resource.<P>
    * @param resolveEntities Resolve DTD entities. Unused DTD entities are ignored. Map meaningful DTD entities onto named, i.e. reusable XML Schema concepts such as group, attributeGroup and simpleType.<P>
    * @param preserveComments Preserve DTD comments. Map DTD comments onto XML Schema documentation. Locates DTD comments at /schema/annotation[1]/documentation[<I>i</I>] referred to by <BR>&lt;documentation source="#xpointer(/schema/annotation[1]/documentation[<I>i</I>]/text())"/&gt;.<P>
    * @param commentLanguage Examples: "en", "de" ... Result: &lt;documentation xml:lang="en"/&gt;.<P>
    * @param commentLength Ignore DTD comments with length greater than <code>commentLength</code>.<P>
    * @param conceptRelation Relate DTD comments to model concepts. Ignore comments without concept relation.<P>
    * @param schemaConcepts Example: "element enumeration" relates DTD comments to the model concepts value-of(element/@name), value-of(enumeration/@value).<P>
    * @param minRelation Minimum relation between DTD comment and model concept. Heuristics <BR>{ relation = 0; for (concept occurs in comment) relation += highlight(concept); return relation >= minRelation; }<P>
    * @return XML Schema<P>
    */
   public String translate(
            String dtdURI,
            String xsltURI,
            boolean resolveEntities,
            boolean ignoreComments,
            int commentLength,
            String commentLanguage,
            String conceptRelation,
            int conceptHighlight,
            int conceptOccurrence)
   {
      try {
         log("\ndtd2xs: dtdURI " + dtdURI);
         log("\ndtd2xs: resolveEntities " + resolveEntities);
         log("\ndtd2xs: ignoreComments " + ignoreComments);
         log("\ndtd2xs: commentLength " + commentLength);
         log("\ndtd2xs: commentLanguage " + commentLanguage);
         log("\ndtd2xs: conceptHighlight " + conceptHighlight);
         log("\ndtd2xs: conceptOccurrence " + conceptOccurrence);
         log("\ndtd2xs: conceptRelation " + conceptRelation);
// CM: Use a JAXP DocumentBuilder instead of using the proprietary sun XmlDocument
//         Document schema = new XmlDocument();
         Document schema = jaxp_docbuilder.newDocument();
         schema.appendChild(schema.createElement("schema"));
         log("\ndtd2xs: load DTD ... ");
         String dtd = (new xurl()).communicate(dtdURI);
         log("done");
         log("\ndtd2xs: remove comments from DTD ... ");
         this.ignoreComments = ignoreComments;
         this.commentLength = commentLength;
         this.commentLanguage = commentLanguage;
         this.conceptRelation = conceptRelation;
         this.conceptHighlight = conceptHighlight;
         this.conceptOccurrence = conceptOccurrence;
         numComments = 0;
         dtd = removeComments(dtd);
         log("done");
         log("\ndtd2xs: DOM translation ...");
         StringTokenizer dtdTokenizer = new StringTokenizer(dtd, "<>");
         while (dtdTokenizer.hasMoreTokens()) {
            String decl = dtdTokenizer.nextToken();
            if (decl.startsWith("!ENTITY")) {
               StringTokenizer entTokenizer = new StringTokenizer(decl, " \t\n\r\f%");
               entTokenizer.nextToken();
               String name = entTokenizer.nextToken();
               String content = entTokenizer.nextToken("").trim();
               content = content.substring(1, content.length() - 1); // remove quotes
               switch (useOf(name, content, dtd, resolveEntities)) {
                  case enumerationEntity:
                     log("\ndtd2xs: simpleType[" + name + "][" + content + "]");
                     simpleType(schema, name, content);
                     break;
                  case contentModelEntity:
                     log("\ndtd2xs: group[" + name + "][" + content + "]");
                     group(schema, name, content, 0);
                     break;
                  case attributeListEntity:
                     log("\ndtd2xs: attributeGroup[" + name + "]");
                     Element atGroup = schema.createElement("attributeGroup");
                     schema.getDocumentElement().appendChild(atGroup);
                     atGroup.setAttribute("name", name);
                     annotate(schema, atGroup);
                     attributeGroup(schema, atGroup, content);
                     break;
                  case meaninglessEntity: // resolve entity that must be declared before used !!!
                     log("\ndtd2xs: resolve[" + name + "][" + content + "]");
                     dtd = resolveEntity(name, content, dtd);
                     dtd = dtd.substring(dtd.indexOf(decl) + decl.length(), dtd.length()); // entity is processed and can be redefined!
                     dtdTokenizer = new StringTokenizer(dtd, "<>");
                  // ignore unusedEntity

               }
            }
            else if (decl.startsWith("!ELEMENT")) {
               StringTokenizer elTokenizer = new StringTokenizer(decl);
               elTokenizer.nextToken();
               String name = elTokenizer.nextToken();
               String content = elTokenizer.nextToken("").trim();
               // log("\ndtd2xs: element[" + name + "][" + content + "]");
               element(schema, name, content, 0);
            }
            else if (decl.startsWith("!ATTLIST")) {
               StringTokenizer atTokenizer = new StringTokenizer(decl);
               atTokenizer.nextToken();
               String complexTypeRef = atTokenizer.nextToken();
               String content = atTokenizer.nextToken("").trim();
               // attached to element by xsl
               Element attributes = schema.createElement("_dtd2xs_attributes_");
               schema.getDocumentElement().appendChild(attributes);
               attributes.setAttribute("of", complexTypeRef);
               // log("\ndtd2xs: attributeGroup[" + content + "]");
               attributeGroup(schema, attributes, content);
            }
         }
         log("\n... done");
         log("\ndtd2xs: complextype.xsl ... ");
         schema = transform(schema, xsltURI); // move mixed attribute, attach attributes to element, remove redundant groups
         log("done");
         log("\ndtd2xs: add namespace ... ");
         minusAtplusNs(schema);  // remove crap attributes, add xml schema namespace
         log("done");
         // further optimization possible
         StringWriter sw = new StringWriter();
// CM: Use a JAXP Identity Transformer to serialize the XML since we are not using the sun XmlDocument
//         ((XmlDocument) schema).write(sw);
         jaxp_identity_transformer.transform(new DOMSource(schema), new StreamResult(sw));
         return sw.toString();
      }
      catch (Exception x) { log("\ndtd2xs: " + x); return null; }
   }

}

