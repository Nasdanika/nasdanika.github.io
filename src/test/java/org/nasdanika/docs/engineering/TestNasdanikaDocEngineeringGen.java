package org.nasdanika.docs.engineering;

import org.junit.Test;
import org.nasdanika.docs.DocGenerator;


/**
 * Tests of agile flows.
 * @author Pavel
 *
 */
public class TestNasdanikaDocEngineeringGen /* extends TestBase */ {

	@Test
	public void generate() throws Exception {
		new DocGenerator().generate();
	}
	
}
