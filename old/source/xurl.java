/***************************************************************************
* xuri
****************************************************************************
* Copyright (C) 05/2001 Ralf Schweiger
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
import java.net.*;
import java.io.*;
import java.util.Date;
// import javax.servlet.*;

class xurl {

   private final String defaultEncoding = "ISO-8859-1";
   private String charset;
   private StringBuffer toSend;
   private PrintStream psLog; // application
   private StringBuffer sbLog; // applet

   private void log(Object item) {
      if (psLog != null) psLog.print(item);
      if (sbLog != null) sbLog.append(item);
   }

   public xurl() { charset = defaultEncoding; psLog = null; sbLog = null; toSend = new StringBuffer(""); }

   public xurl(String charset) { this.charset = charset; psLog = null; sbLog = null; toSend = new StringBuffer(""); }

   public xurl(StringBuffer sbLog) { charset = defaultEncoding; psLog = null; this.sbLog = sbLog; toSend = new StringBuffer(""); }

   public xurl(String charset, StringBuffer sbLog) { this.charset = charset; psLog = null; this.sbLog = sbLog; toSend = new StringBuffer(""); }

   public void setCharset(String charset) { this.charset = charset; }

   public void send(String name, Object value) {
      if (toSend.length() > 0)
         toSend.append("&");
      toSend.append(name).append("=").append(URLEncoder.encode(value.toString()));
   }

   public String communicate(String url) { // send & receive
      Date date0 = new Date();
      log("<xurl:communicate>" + url + "\n<xurl:send charset=" + charset + ">" + toSend);
      try {
         URL u = new URL(url);
         URLConnection uc = u.openConnection();
         uc.setDoInput(true);
         uc.setUseCaches(false);
         uc.setRequestProperty("content-type", "application/x-www-form-urlencoded");
         uc.setRequestProperty("charset", charset);
         if (toSend.toString().length() > 0) {
            uc.setDoOutput(true);
            PrintWriter pw = new PrintWriter(uc.getOutputStream());
            pw.print(toSend);
            toSend = new StringBuffer("");
            pw.close();
         }
         StringBuffer receive = new StringBuffer("");
         String enc = uc.getContentEncoding(); // equals to charset if xurl is used for communication
         InputStreamReader isr = new InputStreamReader(uc.getInputStream(), enc != null ? enc : charset);
         int c;
         while ((c = isr.read()) != -1) receive.append((char) c);
         isr.close();
         log("</xurl:send>\n<xurl:receive charset=" + enc + ">" + receive + "</xurl:receive>\n" + ((new Date()).getTime() - date0.getTime()) + " ms</xurl:communicate>");
         return receive.toString();
      }
      catch (Exception x) { log(x); return null; }
   }

/*******************************************

   public String receive(ServletRequest req) {
      Date date0 = new Date();
      if (log != null) log.put("<xurl:receive charset=" + req.getCharacterEncoding() + ">");
      try {
         StringBuffer content = new StringBuffer("");
         BufferedReader br = req.getReader();
         int c;
         while ((c = br.read()) != -1) content.append((char) c);
         br.close();
         if (log != null) log.put(((new Date()).getTime() - date0.getTime()) + " ms</xurl:receive>");
         return content.toString();
      }
      catch (Exception x) { if (log != null) log.put(x); return null; }
   }

   public void send(ServletResponse res, Object content) {
      Date date0 = new Date();
      if (log != null) log.put("<xurl:send charset=" + charset + ">" + content);
      try {
         res.setContentType("text/plain; charset=" + charset);
         PrintWriter pw = res.getWriter();
         pw.print(content);
         pw.close();
         if (log != null) log.put(((new Date()).getTime() - date0.getTime()) + " ms</xurl:send>");
      }
      catch (Exception x) { if (log != null) log.put(x); }
   }

************************************************/

}
