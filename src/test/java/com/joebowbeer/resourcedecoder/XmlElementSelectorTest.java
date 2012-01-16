package com.joebowbeer.resourcedecoder;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class XmlElementSelectorTest {
    
    public XmlElementSelectorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testXmlSelector() {
        XmlElementSelector foo = new XmlElementSelector("foo");
        assertTrue(foo.equals(new XmlElementSelector("foo")));
        assertFalse(foo.equals(new XmlElementSelector("bar")));
        XmlElementSelector fbz = new XmlElementSelector("foo[bar=baz]");
        assertTrue(fbz.equals(fbz));
        assertFalse(fbz.equals(new XmlElementSelector("foo")));
        assertFalse(fbz.equals(new XmlElementSelector("foo[x=y]")));
        assertFalse(fbz.equals(new XmlElementSelector("x[bar=baz]")));
        XmlElementSelector foo1 =
                new XmlElementSelector("foo[bar=baz][acme=true][zebra=coyote]");
        XmlElementSelector bogus =
                new XmlElementSelector("bogus[bar=baz][acme=true][zebra=coyote]");
        XmlElementSelector foo2 =
                new XmlElementSelector("foo[acme=true][bar=baz][zebra=coyote]");
        XmlElementSelector foo3 =
                new XmlElementSelector("foo[acme=true][bar=baz]");
        assertTrue(foo1.equals(foo1));
        assertTrue(foo1.equals(foo2));
        assertTrue(foo2.equals(foo1));
        assertFalse(foo1.equals(bogus));
        assertFalse(foo1.equals(foo3));
        assertTrue(foo1.equals(foo1));
    }
}
