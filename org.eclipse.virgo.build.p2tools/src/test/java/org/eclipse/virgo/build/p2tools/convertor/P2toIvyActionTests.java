package org.eclipse.virgo.build.p2tools.convertor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class P2toIvyActionTests {
    P2toIvyAction action = new P2toIvyAction();
    
    @Test
    public void testVersionCreate() {
        String[] parts = {"bsn", "major.minor.micro", "qualifier", "1.2.3"};
        String version = this.action.createBundleVersion(parts);
        assertEquals("major.minor.micro_qualifier_1.2.3", version);
    }
}
