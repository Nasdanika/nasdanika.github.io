package org.nasdanika.docs;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.nasdanika.common.Context;
import org.nasdanika.common.ExecutionException;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.html.model.app.gen.ActionSiteGenerator;
import org.nasdanika.html.model.app.util.AppDrawioResourceFactory;

public class Generator {
	
	public static void main(String[] args) throws Exception {
		ActionSiteGenerator actionSiteGenerator = new ActionSiteGenerator() {
			
			@Override
			protected ResourceSet createResourceSet(Context context, ProgressMonitor progressMonitor) {
				ResourceSet resourceSet = super.createResourceSet(context, progressMonitor);
				resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("drawio", new AppDrawioResourceFactory(uri -> resourceSet.getEObject(uri, true)));						
				return resourceSet;
			}
			
			@Override
			protected boolean isDeleteOutputPath(String path) {
				return !"CNAME".equals(path) && !"favicon.ico".equals(path) && !path.startsWith("images/");
			}			
			
		};
		
		File nasdanikaDiagramFile = new File("model/nasdanika.drawio").getCanonicalFile();
		String pageTemplateResource = "model/page-template.yml";
		URI pageTemplateURI = URI.createFileURI(new File(pageTemplateResource).getAbsolutePath());
		
		Map<String, Collection<String>> errors = actionSiteGenerator.generate(
				URI.createFileURI(nasdanikaDiagramFile.getAbsolutePath()).appendFragment("/"), 
				pageTemplateURI, 
				"https://docs.nasdanika.org", 
				new File("docs"),  
				new File("target/doc-site-work-dir"), 
				true);
				
		int errorCount = 0;
		for (Entry<String, Collection<String>> ee: errors.entrySet()) {
			System.err.println(ee.getKey());
			for (String error: ee.getValue()) {
				System.err.println("\t" + error);
				++errorCount;
			}
		}
		
		System.out.println("There are " + errorCount + " site errors");
		
		if (errors.size() != 11) {
			throw new ExecutionException("There are problems with pages: " + errorCount);
		}				
		
	}

}
