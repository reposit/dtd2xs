/***************************************************************************
* dtd2xsd: applet wrapper for dtd2xs
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
* tel:    +49-641-9941370
* fax:    +49-641-9941359
* mailto: ralf.schweiger@informatik.med.uni-giessen.de
*
***************************************************************************/

import java.util.*;
// uncomment if your Java environment < v1.4
// import dtd2xs;
import java.net.*;
import java.io.*;

public class dtd2xs_applet extends java.applet.Applet {

   StringBuffer sbLog;
   String xsd;

   private void log(Object item) { sbLog.append(item); }

   public void translate(
            String dtdURI,
            boolean resolveEntity,
            boolean ignoreComment,
            String commentLength,
            String commentLanguage,
            String conceptRelation,
            String conceptHighlight,
            String conceptOccurrence,
            String xsdURI)
   {
      sbLog = new StringBuffer("");
      try {
         Date date0 = new Date();
         /*****
         log("\ndtd2xsd: dtdURI " + dtdURI);
         log("\ndtd2xsd: resolveEntity " + resolveEntity);
         log("\ndtd2xsd: ignoreComment " + ignoreComment);
         log("\ndtd2xsd: commentLength " + commentLength);
         log("\ndtd2xsd: commentLanguage " + commentLanguage);
         log("\ndtd2xsd: conceptRelation " + conceptRelation);
         log("\ndtd2xsd: conceptHighlight " + conceptHighlight);
         log("\ndtd2xsd: conceptOccurrence " + conceptOccurrence);
         log("\ndtd2xsd: xsdURI " + xsdURI);
         *****/
         dtd2xs translator = new dtd2xs(sbLog);
         String xsd =
            translator.translate(
               getCodeBase() + dtdURI,
               getCodeBase() + "complextype.xsl",
               resolveEntity,
               ignoreComment,
               Integer.valueOf(commentLength).intValue(),
               commentLanguage,
               conceptRelation,
               Integer.valueOf(conceptHighlight).intValue(),
               Integer.valueOf(conceptOccurrence).intValue()
            );
         log("\ndtd2xsd: " + ((new Date()).getTime() - date0.getTime()) + "ms");
         log("\n\n******************** Copy & Paste ********************\n\n" + xsd);
         /* DOES NOT YET WORK
         if (! xsdURI.equals("")) {
            URL u = new URL(getCodeBase() + xsdURI);
            URLConnection uc = u.openConnection();
            uc.setUseCaches(false);
            uc.setDoOutput(true);
            OutputStream os = uc.getOutputStream();
            PrintWriter pw = new PrintWriter(os); // uc.getOutputStream() does not work with file: protocol
            // PrintWriter pw = new PrintWriter(new FileOutputStream(getCodeBase() + xsdURI));
            pw.print(xsd);
            pw.close();
         }
         */
      }
      catch (Exception x) { log("\ndtd2xsd: " + x); }
   }

   public String getLog() { return sbLog.toString(); }

}

