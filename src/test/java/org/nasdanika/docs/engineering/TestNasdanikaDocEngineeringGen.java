package org.nasdanika.docs.engineering;

import org.junit.jupiter.api.Test;
import org.nasdanika.docs.DocGenerator;


/**
 * Tests of agile flows.
 * @author Pavel
 *
 */
public class TestNasdanikaDocEngineeringGen {

	@Test
	public void generate() throws Exception {
		new DocGenerator().generate();
	}
	
}
