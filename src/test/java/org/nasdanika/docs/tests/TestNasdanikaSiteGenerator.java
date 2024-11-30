package org.nasdanika.docs.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.Test;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceCapabilityFactory.Requirement;
import org.nasdanika.capability.emf.ResourceSetRequirement;
import org.nasdanika.common.PrintStreamProgressMonitor;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.docs.Generator;
import org.nasdanika.models.app.Label;

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
	
	@Test
	public void testDumpModel() throws Exception {
		// TODO - representation filtering from capabilities
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		Requirement<ResourceSetRequirement, ResourceSet> requirement = ServiceCapabilityFactory.createRequirement(ResourceSet.class);		
		ResourceSet resourceSet = capabilityLoader.loadOne(requirement, progressMonitor);
				
		File nadDiagramFile = new File("model/nasdanika.drawio").getCanonicalFile();
		Resource nsdResource = resourceSet.getResource(URI.createFileURI(nadDiagramFile.getAbsolutePath()), true);
		System.out.println(nsdResource.getContents());
		nsdResource.getContents().forEach(e -> dump(e, 0));
		
		Resource nsdResourceDump = resourceSet.createResource(URI.createFileURI(new File("target/nasdanika.xml").getAbsolutePath()));
		nsdResourceDump.getContents().addAll(EcoreUtil.copyAll(nsdResource.getContents()));
		nsdResourceDump.save(null);
		assertEquals(1, nsdResource.getContents().size());
	}
		
	private void dump(EObject element, int indent) {
		for (int i = 0; i < indent; ++i) {
			System.out.print("  ");			
		}
		if (element instanceof Label) {
			Label label = (Label) element;
			System.out.println("[" + label.getUuid() + "] " + label.getText());
			for (EObject child: label.getChildren()) {
				dump(child, indent + 1);
			}
		} else {
			System.out.println(element);
		}
	}
	
	
}
