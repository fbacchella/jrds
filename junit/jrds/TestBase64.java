package jrds;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class TestBase64 {

    @Test
    public void test1() throws IOException {
        String s = System.getProperties().toString();

        byte[] b = Base64.encodeString(s).getBytes();
        byte[] c = Base64.decode( b, 0, b.length );

        Assert.assertEquals("Decode don't match encode", s, new String(c));

        //		java.io.FileInputStream fis = new java.io.FileInputStream( "tmp/abcd.txt" );
        //		Base64.InputStream b64is = new Base64.InputStream( fis, Base64.DECODE );
        //		int ib = 0;
        //		while( (ib = b64is.read()) > 0 )
        //		{   //System.out.print( new String( ""+(char)ib ) );
        //		}


    }
}
