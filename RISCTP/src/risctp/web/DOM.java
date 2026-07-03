// ---------------------------------------------------------------------------
// DOM.java
// Document handling based on the Document Object Model (DOM).
// $Id: DOM.java,v 1.16 2023/12/07 17:55:40 schreine Exp $
//
// Author: Wolfgang Schreiner <Wolfgang.Schreiner@risc.jku.at>
// Copyright (C) 2022-, Research Institute for Symbolic Computation (RISC)
// Johannes Kepler University, Linz, Austria, https://www.risc.jku.at
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ----------------------------------------------------------------------------
package risctp.web;

import java.io.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class DOM
{ 
  /**************************************************************************
   * Create document from file
   * @param path the path of the file.
   * @return the document (null, if an error occurred)
   *************************************************************************/
  public static Document readDocument(String path)
  {
    try
    {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      File file = new File(path);
      if (file.exists()) return defineIdAttribute(builder.parse(file));
      InputStream stream = risctp.Main.class.getResourceAsStream("/" + path);
      if (stream != null) return defineIdAttribute(builder.parse(stream));
      throw new FileNotFoundException(file.getAbsolutePath());
    }
    catch (ParserConfigurationException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    catch (SAXException e)
    {
      e.printStackTrace();
    }
    return null;
  }
  
  /****************************************************************************
   * Define in document the id attribute for all nodes with an attribute "id".
   * @param doc the document.
   ***************************************************************************/
  private static Document defineIdAttribute(Document doc)
  {
    defineIdAttribute(doc.getDocumentElement());
    return doc;
  }
  
  /****************************************************************************
   * Define in element tree the id attribute for all nodes with an attribute "id".
   * @param e the root of the tree.
   ***************************************************************************/
  private static void defineIdAttribute(Element e) 
  {
    if (e.getAttribute("id").length() > 0)
      e.setIdAttribute("id", true);
    for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling())
      if (child.getNodeType() == Document.ELEMENT_NODE)
        defineIdAttribute((Element) child);
  }
  
  /**************************************************************************
   * Create a fresh document with denoted root tag.
   * @param tag the tag of the root.
   * @return the created document (null, if an error occurred)
   *************************************************************************/
  public static Document createDocument(String tag)
  {
    try
    {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.newDocument();
      Element root = document.createElement(tag);
      document.appendChild(root);
      return document;
    }
    catch (ParserConfigurationException e)
    {
    }
    return null;
  }
  
  /**************************************************************************
   * Print document.
   * @param document the document to print.
   * @param doctype true if DOCTYPE is to be added.
   * @param out the writer on which to print.
   *************************************************************************/
  public static void printDocument(Document document, boolean doctype, PrintWriter out)
  {
    try
    {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      if (doctype)
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "about:legacy-compat");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      // transformer.setOutputProperty(OutputKeys.INDENT, "no");
      transformer.transform(new DOMSource(document), new StreamResult(out));
      // transformer.transform(new DOMSource(document), 
      //   new StreamResult(new OutputStreamWriter(System.out)));
    }
    catch(TransformerConfigurationException e)
    {
    }
    catch(TransformerException e)
    {
    }
  }
  
  /***************************************************************************
   * Create colorized version of string representation of a formula.
   * @param string the string representation.
   * @param an element with the colorized version of the representation.
   **************************************************************************/
  public static Element colorizedText(Document document, String string)
  {
    Element span = document.createElement("span");
    int n = string.length();
    for (int i = 0; i < n; i++)
    {
      char ch = string.charAt(i);
      Text text = document.createTextNode(ch + "");
      switch (ch)
      {
      case '[' :
      {
        if (i == 0 || string.charAt(i-1) != ':')
        {
          span.appendChild(text);
          break;
        }
        int pos = string.indexOf(']', i+1);
        if (pos == -1) { span.appendChild(text); break; }
        Element span0 = document.createElement("span");
        span0.setAttribute("style", "color:purple");
        Text text0 = document.createTextNode(string.substring(i, pos+1));
        span0.appendChild(text0);
        span.appendChild(span0);
        i = pos;
        break;
      }
      case '∀' : case '∃' :
      {
        Element span0 = document.createElement("span");
        span0.setAttribute("style", "color:red");
        span0.appendChild(text);
        span.appendChild(span0);
        break;
      }
      case '¬' : case '∧' : case '∨' : case '⇒' : case '⇔' : case '⊤': case '⊥' :
      {
        Element span0 = document.createElement("span");
        span0.setAttribute("style", "color:blue");
        span0.appendChild(text);
        span.appendChild(span0);
        break;
      }
      case '→' :
      {
        Element span0 = document.createElement("span");
        span0.setAttribute("style", "color:darkgreen");
        span0.appendChild(text);
        span.appendChild(span0);
        break;
      } 
      default: 
        span.appendChild(text);
        break;
      }
    }
    return span;
  }
}
