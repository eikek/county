package org.eknet.county.backend

import java.io.ByteArrayOutputStream
import java.security.{MessageDigest, DigestOutputStream}
import javax.xml.bind.DatatypeConverter

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 26.03.13 15:00
 * 
 */
object Digest {

   /**
    * Returns the md5 checksum of the given string. This can be used to normalize
    * long names in something shorter, but unique.
    *
    * @param str
    * @return
    */
   def digest(str: String): String = {
     val bout = new ByteArrayOutputStream()
     val out = new DigestOutputStream(bout, MessageDigest.getInstance("MD5"))
     out.write(str.getBytes("UTF-8"))
     out.close()
     DatatypeConverter.printHexBinary(out.getMessageDigest.digest())
   }
}
