/**
 * { XML 1.0 DTD } to { REC-xmlschema-1-20010502 }
 */

import java.io.*;
// uncomment if your Java environment < v1.4
// import dtd2xs;

public class dtd2xsd {

   private static dtd2xs translator = new dtd2xs(System.err);

   private static void usage() {
      System.err.println(
         "usage: java dtd2xsd <options> <dtd file>" +
         "\n<options>" +
         "\n   -entity (Map DTD entity onto XML Schema <group>, <attributeGroup>, <simpleType>)" +
         "\n   -comment <parameters> (Map DTD comment onto XML Schema <documentation> according to subsequent parameters)" +
         "\n<parameters>" +
         "\n   - (100), 1000 ... (Comments with more characters are ignored)" +
         "\n   - (undefined), en (English), de (German), fr (French) ... (Language of comments)" +
         "\n   - (\"element attribute\") ... (XML Schema concepts to annotate)" +
         "\n   - (no), 1 (by space/punctuation), (2) (by quotes/parentheses) (Required highlighting of model concept in DTD comment)" +
         "\n   - (1) (Required minimum occurrence of model concept in DTD comment)"
      );
      System.exit(1);
   }

   public static void main(String [] argument) {
      try {
         int i = 0; // index of current argument
         if (argument.length < 1) usage();
         boolean resolveEntity = ! argument[i].startsWith("-entity");
         if (! resolveEntity)
            if (argument.length < 2) usage(); else ++ i;
         boolean ignoreComment = ! argument[i].startsWith("-comment");
         int commentLength = 100;
         String commentLanguage = null;
         String conceptRelation = "element attribute";
         int conceptHighlight = 2;
         int conceptOccurrence = 1;
         if (! ignoreComment)
            if (i + 6 < argument.length) {
               if (! argument[i + 1].equals("-")) commentLength = Integer.valueOf(argument[i + 1]).intValue();
               if (! argument[i + 2].equals("-")) commentLanguage = argument[i + 2];
               if (! argument[i + 3].equals("-")) conceptRelation = argument[i + 3];
               if (! argument[i + 4].equals("-")) conceptHighlight = Integer.valueOf(argument[i + 4]).intValue();
               if (! argument[i + 5].equals("-")) conceptOccurrence = Integer.valueOf(argument[i + 5]).intValue();
               i += 6;
            }
            else usage();
         File dtd = new File(argument[i]);
         File xslt = new File("complextype.xsl");
         String xsd =
            translator.translate(
               "file:///" + dtd.getAbsolutePath(),
               "file:///" + xslt.getAbsolutePath(),
               resolveEntity,
               ignoreComment,
               commentLength,
               commentLanguage,
               conceptRelation,
               conceptHighlight,
               conceptOccurrence
            );
         System.out.println(xsd);
         System.exit(0);
      }
      catch (Exception x) { System.err.println(x); System.exit(1); }
   }

}

