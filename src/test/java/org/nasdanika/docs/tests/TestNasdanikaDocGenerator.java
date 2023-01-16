package org.nasdanika.docs.tests;

import org.junit.jupiter.api.Test;
import org.nasdanika.docs.DocGenerator;


/**
 * Tests of agile flows.
 * @author Pavel
 *
 */
public class TestNasdanikaDocGenerator {

	@Test
	public void generate() throws Exception {
		new DocGenerator().generate();
	}
	
}
