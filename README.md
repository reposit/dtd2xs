
DTD to XML Schema translator
----------------------------
Translate a Document Type Definition (XML 1.0 DTD) into an XML schema
(REC-xmlschema-1-20010502). The translator can map meaningful
DTD entities onto named and therefore reusable XML Schema constructs 
such as <simpleType>, <attributeGroup> and <group>. The translator can map 
DTD comments onto XML Schema <documentation> elements.

Free available as Java class, Standalone application and as Web tool.

http://www.lumrix.de/dtd2xs/
or
http://puvogel.informatik.med.uni-giessen.de/lumrix/#dtd


Installation
-------------

  Java Version >1.4
  -----------------
  
  Unzip the archive into a new directory (e.g. C:\dtd2xs). Start
  your favorite Web browser (Mozilla, Netscape Navigator 4.5 or later version,
  Internet Explorer 5 or later version) and ensure that you have Java and
  JavaScript enabled in your browser preferences.

  Open the file dtd2xsd.html.

  Test "test.dtd" with varying parameters (e.g. Map DTD comments).
  

  Java Version <1.4
  ----------------------

  Unzip the archive into a new directory (e.g. C:\dtd2xs). Start
  your favorite Web browser (Mozilla, Netscape Navigator 4.5 or later version,
  Internet Explorer 5 or later version) and ensure that you have Java and
  JavaScript enabled in your browser preferences.

  Open the file dtd2xsd.html located in directory old.

  Test "test.dtd" with varying parameters (e.g. Map DTD comments).


For Developers
--------------

  Only for Java Version <1.4  
  --------------------------
   
  For a successful re-compilation of the Java source code you have to tell 
  the Java compiler where to find the Java archives "xml.jar" and "xt.jar"
  (located in your dtd2xs\old directory). Set your classpath accordingly.

If you want to use "dtd2xs.class", see applet wrapper "dtd2xs_applet.java" 
or standalone application "dtd2xsd.java".


Feedback
--------
We would like to here from you, please send questions or requests for
information about our latest developments to:

Ralf.Schweiger@informatik.med.uni-giessen.de

