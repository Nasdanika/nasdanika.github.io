package org.nasdanika.docs.tests;

import org.junit.jupiter.api.Test;
import org.nasdanika.docs.Generator;

public class TestNasdanikaSiteGenerator {

	@Test
	public void testGenerateNasdanikaSite() throws Exception {
		Generator.main(null);
	}
	
	@Test
	public void testLayer() throws Exception {
		Module generatorModule = Generator.class.getModule();
		System.out.println(generatorModule.getLayer());
	}
	
}
